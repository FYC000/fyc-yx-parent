package com.example.yx.common.exception;

import com.example.yx.common.result.ResultCodeEnum;
import lombok.Data;

@Data
public class yxException extends RuntimeException{

    //异常状态码
    private Integer code;

    //通过状态码和错误消息创建异常对象
    public yxException(String message, Integer code) {
        super(message);
        this.code = code;
    }

    //接收枚举类型对象
    public yxException(ResultCodeEnum resultCodeEnum) {
        super(resultCodeEnum.getMessage());
        this.code = resultCodeEnum.getCode();
    }

    @Override
    public String toString() {
        return "GuliException{" +
                "code=" + code +
                ", message=" + this.getMessage() +
                '}';
    }
}
