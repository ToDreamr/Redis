package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.ILockImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 春江花朝秋月夜
 * @since 2023-7-22
 */
@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedissonClient redissonClient;
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("./lua/order_kill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    //添加阻塞队列
    private static final BlockingQueue<VoucherOrder> orderTask = new ArrayBlockingQueue<>(1024 * 1024);

    @PostConstruct
    private void init() {
        // TODO 需要秒杀下单功能的同学自己解开下面的注释

        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }
    @Override
    public Result seckillVoucher(Long voucherId) {
        //return withNoRedisLock(voucherId);//非分布式锁实现,悲观锁和乐观锁一类
        //return RedisLock(voucherId);//基于Redis的分布式锁来实现业务
         return RedissonLock(voucherId);//基于redisson实现的分布式锁
        //return withScript(voucherId);
    }

    //--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    private Result withNoRedisLock(Long voucherId) {
        //1:判断库存是否充足
        if (seckillVoucherService.getById(voucherId).getStock() < 1) {
            return Result.fail("库存不足，不足以参加秒杀");
        }
        //由于spring管理了事务
        Long userId = UserHolder.getUser().getId();
        synchronized (userId.toString().intern()) {
            //拿到事务代理对象，锁需要包裹事务
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.CREATE(voucherId);
            //创建到实现类里面，才可以使用代理方法
        }
    }

    private Result RedisLock(Long voucherId) {
        //1:判断库存是否充足
        if (seckillVoucherService.getById(voucherId).getStock() < 1) {
            return Result.fail("库存不足，不足以参加秒杀");
        }
        //由于spring管理了事务
        Long userId = UserHolder.getUser().getId();
        //创建锁对象
        ILockImpl lock = new ILockImpl(stringRedisTemplate, "order:" + userId);
        //获取锁
        boolean isLock = lock.tryLock(1200);
        //建议写反逻辑
        if (!isLock) {
            //获取错误，非法请求
            log.error("非法重复下单");
            return Result.fail("用户重复下单");
        }
        try {
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.CREATE(voucherId);
        } finally {
            lock.unlock();
        }
    }

    private Result RedissonLock(Long voucherId) {
        Long userId = UserHolder.getUser().getId();
        //1:判断库存是否充足
        if (seckillVoucherService.getById(voucherId).getStock() < 1) {
            return Result.fail("库存不足，不足以参加秒杀");
        }
        //获取锁
        RLock lock = redissonClient.getLock("order:" + userId);
        if (!lock.tryLock()) {
            log.error("非法重复下单");
            return Result.fail("用户重复下单");
        }
        try {
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.CREATE(voucherId);
        } finally {
            lock.unlock();
        }
    }
    //--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    @Transactional//事务将会失效，对函数加事务，而不是代理对象
    public Result CREATE(Long voucherId) {

        Long userId = UserHolder.getUser().getId();
        long orderId = redisIdWorker.nextId("order");
        //ctrl+q 显示方法说明
        //内部加锁，先释放锁才会提交事务，当锁释放，还没提交，其他线程进入，同样出现并发问题
        //2:实现一人一单,判断是否存在，不能判断是否发生变化，需要用悲观锁对业务逻辑进行封装
        int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        if (count > 0) {
            return Result.fail("用户已经下单过，不可重复下单");
        }
        //3执行库存扣减操作
        boolean success = seckillVoucherService.update().setSql("stock=stock-1").gt("stock", 0).eq("voucher_id", voucherId).update();//只需要库存大于0
        if (!success) {
            return Result.fail("库存不足，不足以参加渺少活动");
        }
        //4:创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setVoucherId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);
        return Result.ok(orderId);
    }
    //--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    @Transactional
    public void ORDER(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        int count = query().eq("user_id", userId).eq("voucher_id", voucherOrder.getVoucherId()).count();
        if (count > 0) {
            log.error("用户已经购买过一次！");
            return;
        }
        boolean success = seckillVoucherService.update().setSql("stock=stock-1").gt("stock", 0).eq("voucher_id", voucherOrder.getVoucherId()).update();//只需要库存大于0
        if (!success) {
            log.error("库存不足，不足以参加渺少活动");
            return;
        }
        save(voucherOrder);
    }

    public Result withScript(Long voucherId) {
        //执行lua脚本
        Long userId = UserHolder.getUser().getId();
        Long res = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                userId.toString()
        );
        //判断结果
        //不为0
        assert res != null;
        int intValue = res.intValue();
        if (intValue != 0) {
            return Result.fail(intValue == 1 ? "库存不足" : "重复下单");
        }
        //为0，保存下单信息到阻塞队列
        long orderId = redisIdWorker.nextId("order");
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setVoucherId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        orderTask.add(voucherOrder);
        //获取代理对象

        // 返回订单id
        return Result.ok(orderId);
    }
    private IVoucherOrderService proxy;
    private void HandleOrder(VoucherOrder voucherOrder){
        Long userId = voucherOrder.getId();
        // 创建锁对象,单点测试
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        // 获取锁
        boolean isLock = lock.tryLock();
        // 判断是否获取锁成功
        if(!isLock){
            // 获取锁失败，返回错误或重试
            log.error("不允许重复下单");
            return;
        }
        try {
            // 获取代理对象（事务）
            proxy.ORDER(voucherOrder);
        } finally {
            // 释放锁
            lock.unlock();
        }
    }

    private  class VoucherOrderHandler implements Runnable {
        @Override
        public void run() {
            while (true){
                try {
                    VoucherOrder voucherOrder=orderTask.take();
                   // RedissonLock(voucherOrder.getId());//处理订单生成业务
                    HandleOrder(voucherOrder);
                }catch (InterruptedException e){
                    log.error("处理异常"+e.getMessage());
                }
            }
        }
    }
}
