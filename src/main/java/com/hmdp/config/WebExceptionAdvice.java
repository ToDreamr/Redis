package com.hmdp.config;

import com.hmdp.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.javassist.NotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
//全局异常类
public class WebExceptionAdvice {

    //处理运行时异常，可以完善异常页面
    @ExceptionHandler(RuntimeException.class)
    public Result handleRuntimeException(RuntimeException e) {
        log.error(e.toString(), e);
        return Result.fail("服务器异常");
    }

    @ExceptionHandler(NotFoundException.class)
    public Result handleRuntimeException(NotFoundException e) {
        log.error(e.toString(), e);
        return Result.fail("没有找到当前页面");
    }
}
