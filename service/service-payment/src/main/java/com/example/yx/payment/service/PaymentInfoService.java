package com.example.yx.payment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.yx.model.order.PaymentInfo;

import java.util.Map;

public interface PaymentInfoService extends IService<PaymentInfo> {
    //根据OrderNo查询支付信息
    PaymentInfo getPaymentByOrderNo(String orderNo);
    //添加支付信息
    PaymentInfo savePaymentInfo(String orderNo);
    //通过调用微信支付系统查询支付状态
    Map<String, String> queryPayStatus(String orderNo);
    //如果有返回值，则代表支付成功
    void paySuccess(String orderNo, Map<String, String> resultMap);
}
