package com.hmdp.utils;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RedisData {
    //不采用继承，因为具有侵入性，改变了原有的逻辑
    private LocalDateTime expireTime;//设置过期时间
    private Object data;//插入的数据对象，存储数据对象
}
