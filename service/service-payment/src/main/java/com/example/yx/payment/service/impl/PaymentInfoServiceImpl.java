package com.example.yx.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.yx.common.exception.yxException;
import com.example.yx.common.result.ResultCodeEnum;
import com.example.yx.enums.PaymentStatus;
import com.example.yx.enums.PaymentType;
import com.example.yx.model.order.OrderInfo;
import com.example.yx.model.order.PaymentInfo;
import com.example.yx.mq.constant.MqConst;
import com.example.yx.mq.service.RabbitService;
import com.example.yx.order.client.OrderFeignClient;
import com.example.yx.payment.mapper.PaymentInfoMapper;
import com.example.yx.payment.service.PaymentInfoService;
import com.example.yx.utis.ConstantPropertiesUtils;
import com.example.yx.utis.HttpClient;
import com.github.wxpay.sdk.WXPayUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo> implements PaymentInfoService {

    @Autowired
    private OrderFeignClient orderFeignClient;
    @Autowired
    private RabbitService rabbitService;
    @Override
    public PaymentInfo getPaymentByOrderNo(String orderNo) {
        PaymentInfo paymentInfo = baseMapper.selectOne(new LambdaQueryWrapper<PaymentInfo>().eq(PaymentInfo::getOrderNo, orderNo));
        return paymentInfo;
    }

    @Override
    public PaymentInfo savePaymentInfo(String orderNo) {
        //远程调用service-order模块来获取订单信息
        OrderInfo orderInfo=orderFeignClient.getOrderInfo(orderNo);
        if(null == orderInfo) {
            throw new yxException(ResultCodeEnum.DATA_ERROR);
        }
        //封装到PaymentInfo对象
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setOrderId(orderInfo.getId());
        paymentInfo.setPaymentType(PaymentType.WEIXIN);
        paymentInfo.setUserId(orderInfo.getUserId());
        paymentInfo.setOrderNo(orderInfo.getOrderNo());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID);
        String subject = "userId:"+orderInfo.getUserId()+"下了订单";
        paymentInfo.setSubject(subject);
        //paymentInfo.setTotalAmount(order.getTotalAmount());
        //TODO:为了测试
        paymentInfo.setTotalAmount(new BigDecimal("0.01"));
        //调用方法实现添加
        baseMapper.insert(paymentInfo);
        return paymentInfo;
    }

    @Override
    public Map<String, String> queryPayStatus(String orderNo) {
        try {
            //1、封装参数
            Map paramMap = new HashMap<>();
            paramMap.put("appid", ConstantPropertiesUtils.APPID);
            paramMap.put("mch_id", ConstantPropertiesUtils.PARTNER);
            paramMap.put("out_trade_no", orderNo);
            paramMap.put("nonce_str", WXPayUtil.generateNonceStr());

            //2、设置请求
            HttpClient client = new HttpClient("https://api.mch.weixin.qq.com/pay/orderquery");
            client.setXmlParam(WXPayUtil.generateSignedXml(paramMap, ConstantPropertiesUtils.PARTNERKEY));
            client.setHttps(true);
            client.post();
            //3、返回第三方的数据
            String xml = client.getContent();
            Map<String, String> resultMap = WXPayUtil.xmlToMap(xml);
            //6、转成Map
            //7、返回
            return resultMap;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void paySuccess(String orderNo, Map<String, String> resultMap) {
        //3.1将记录支付表的支付系统状态设置为已经支付
        ////判断是否已经支付过了
        LambdaQueryWrapper<PaymentInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PaymentInfo::getOrderNo, orderNo);
        PaymentInfo paymentInfo = baseMapper.selectOne(queryWrapper);
        if (paymentInfo.getPaymentStatus() != PaymentStatus.UNPAID) {
            return;
        }
        ////若没有支付则进行支付
        paymentInfo.setPaymentStatus(PaymentStatus.PAID);
        paymentInfo.setTradeNo(resultMap.get("ransaction_id"));
        paymentInfo.setCallbackContent(resultMap.toString());
        baseMapper.updateById(paymentInfo);
        //3.2 调用rabbitmq实现订单记录表的订单状态改为已经支付，库存扣减
        rabbitService.sendMessage(MqConst.EXCHANGE_PAY_DIRECT,MqConst.ROUTING_PAY_SUCCESS,orderNo);
    }
}
