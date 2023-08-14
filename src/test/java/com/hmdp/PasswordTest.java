package com.hmdp;

import cn.hutool.core.util.RandomUtil;
import com.hmdp.utils.PasswordEncoder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.DigestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * @author Rainy-Heights
 * @version 1.0
 * @Date 2023/3/16 22:56
 */
@SpringBootTest
public class PasswordTest {

    @Test
    public void ClassLoaders(){
        String password="123456";
        System.out.println(PasswordEncoder.encode("123456"));//有静态方法，不需要加入注册为bean，业务启动时候就已经有加载了类
        String md5DigestAsHex = DigestUtils.md5DigestAsHex((password + RandomUtil.randomString(20)).getBytes(StandardCharsets.UTF_8));
        //消费者模式

        System.out.println(Arrays.toString(password.getBytes(StandardCharsets.UTF_8)));
        System.out.println(md5DigestAsHex);
    }
}
