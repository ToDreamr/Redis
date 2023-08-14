package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    private static final ObjectMapper mapper=new ObjectMapper();
    @Resource
    private ShopTypeMapper typeMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Result queryTypeList() throws JsonProcessingException {
        //1：先查redis，存在则返回list，不存在则查询mysql数据库
        String typeList =stringRedisTemplate.opsForValue().get("type");
        if (typeList==null){
            //手动序列化和反序列化
            LambdaQueryWrapper<ShopType> queryWrapper = new LambdaQueryWrapper<>();
            log.info("请求打到数据库上面");
            Object shopTypeList = typeMapper.selectList(queryWrapper);
            String json=mapper.writeValueAsString(shopTypeList);//转化为json对象,writeValueAsString把值的形式写成String类型
            stringRedisTemplate.opsForValue().set("type",json,2L, TimeUnit.MINUTES);//调用stringRedisTemplate把值写成json对象
           //反序列化
            String  readJson=stringRedisTemplate.opsForValue().get("type");
            List type=mapper.readValue(readJson,List.class);
            return Result.ok(type);
        }
        List res = mapper.readValue(typeList, List.class);
        return Result.ok(res);
    }
}
