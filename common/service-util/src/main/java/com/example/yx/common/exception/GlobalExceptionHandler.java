package com.example.yx.common.exception;

import com.example.yx.common.result.Result;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Result error(Exception e){
        e.printStackTrace();
        return Result.fail();
    }
    //自定义异常处理方法
    @ExceptionHandler(yxException.class)
    @ResponseBody
    public Result error(yxException e){
        return Result.build(null,e.getCode(),e.getMessage());
    }

}
