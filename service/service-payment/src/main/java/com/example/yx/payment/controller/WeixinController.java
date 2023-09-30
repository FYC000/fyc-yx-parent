package com.example.yx.payment.controller;

import com.example.yx.common.exception.yxException;
import com.example.yx.common.result.Result;
import com.example.yx.common.result.ResultCodeEnum;
import com.example.yx.payment.service.PaymentInfoService;
import com.example.yx.payment.service.WeixinService;
import com.sun.corba.se.spi.orbutil.fsm.Guard;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Api(tags = "微信支付接口")
@RestController
@RequestMapping("/api/payment/weixin")
@Slf4j
public class WeixinController {

    @Autowired
    private WeixinService weixinService;

    @Autowired
    private PaymentInfoService paymentInfoService;

    //调用微信支付系统生成预付单
    @GetMapping("/createJsapi/{orderNo}")
    public Result createJsapi(@PathVariable("orderNo")String orderNo){
        Map<String,String> map=weixinService.createJsapi(orderNo);
        return Result.ok(map);
    }

    //查询订单支付状态
    @GetMapping("/queryPayStatus/{orderNo}")
    public Result queryPayStatus(@PathVariable("orderNo")String orderNo){
        //1.通过调用微信支付系统查询支付状态
        Map<String,String>resultMap=paymentInfoService.queryPayStatus(orderNo);
        //2.微信支付系统返回值为NULL，支付失败
        if(resultMap==null){
            return Result.build(null,ResultCodeEnum.PAYMENT_FAIL);
        }
        //3.如果有返回值，则代表支付成功
        if("SUCCESS".equals(resultMap.get("trade_state"))){
            //更改订单状态，处理支付结果
            String out_trade_no = resultMap.get("out_trade_no");
            paymentInfoService.paySuccess(out_trade_no,resultMap);
            return Result.ok();
        }
        //4.支付中等待
        return Result.build(null,ResultCodeEnum.PAYMENT_FAIL);
    }
}
