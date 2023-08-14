package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * @author Rainy-Heights
 * @version 1.0
 * @Date 2023/8/7 0:15
 */
public class ILockImpl implements ILock{
    private StringRedisTemplate stringRedisTemplate;
    private String name;
    private static final String KEY_PREFIX="lock:";
    private static final String ID_PREFIX= UUID.randomUUID().toString(true)+"-";
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;//ctrl+shift+u:转换成大写字母

    static {
        UNLOCK_SCRIPT=new DefaultRedisScript<>();//指定脚本
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("./lua/kill.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }
    public ILockImpl(StringRedisTemplate stringRedisTemplate, String name) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.name = name;
    }

    @Override
    public boolean tryLock(long timeoutSec) {
        //获取锁，执行setnx
        //获取线程标识
        String threadId = ID_PREFIX+Thread.currentThread().getId();

        Boolean absent = stringRedisTemplate.opsForValue().setIfAbsent(
                KEY_PREFIX+name,
                threadId +" ",
                 timeoutSec, TimeUnit.SECONDS);
        //自动拆箱，可能产生空指针异常
        return Boolean.TRUE.equals(absent);
    }

    @Override
    public void unlock() {
//        Nolua();
        lua();
    }

    private void Nolua() {
        //释放锁之前需要判断锁标识，那么这样获取锁的时候需要假如锁的标识
        String threadId = ID_PREFIX+Thread.currentThread().getId();
        String id = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);

        //用equals方法来判断
        if (threadId.equals(id)){
            stringRedisTemplate.delete(KEY_PREFIX+name);
        }
    }
    private void lua() {
        //调用lua脚本
        stringRedisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(KEY_PREFIX+name),ID_PREFIX+Thread.currentThread().getId());
    }


}
