package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
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

    public ILockImpl(StringRedisTemplate stringRedisTemplate, String name) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.name = name;
    }

    @Override
    public boolean tryLock(long timeoutSec) {
        //获取锁，执行setnx
        //获取线程标识
        Boolean absent = stringRedisTemplate.opsForValue().setIfAbsent(
                KEY_PREFIX+name,
                Thread.currentThread().getId()+" ",
                 timeoutSec, TimeUnit.SECONDS);
        //自动拆箱，可能产生空指针异常
        return Boolean.TRUE.equals(absent);
    }

    @Override
    public void unlock() {
        stringRedisTemplate.delete(KEY_PREFIX+name);
    }
}
