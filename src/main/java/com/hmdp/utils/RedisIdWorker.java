package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {
    /**
     * 开始时间戳
     */
    private static final long BEGIN_TIMESTAMP = 1690280700L;
    /**
     * 序列号的位数
     */
    private static final int COUNT_BITS = 32;//向左移动32位

    private StringRedisTemplate stringRedisTemplate;

    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    //用前缀区分不同的业务
    public long nextId(String keyPrefix) {
        // 1.生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);//当前秒数
        long timestamp = nowSecond - BEGIN_TIMESTAMP;//间隔时间,时间戳

        // 2.生成序列号
        // 2.1.获取当前日期，精确到天
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        // 2.2.自增长，单个key自增长：2^64
        long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);//不同天采用不同的key

        // 3.拼接并返回
        return timestamp << COUNT_BITS | count;//不用加减，因为位运算更快
    }

    public static void main(String[] args) {
        LocalDateTime of = LocalDateTime.of(2023, 7, 25, 10, 25);
        long second = of.toEpochSecond(ZoneOffset.UTC);
        System.out.println(second);
    }
}
