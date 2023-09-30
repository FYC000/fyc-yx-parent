package com.example.yx.payment.service.impl;

import com.example.yx.common.exception.yxException;
import com.example.yx.common.result.ResultCodeEnum;
import com.example.yx.model.order.PaymentInfo;
import com.example.yx.payment.service.PaymentInfoService;
import com.example.yx.payment.service.WeixinService;
import com.example.yx.utis.ConstantPropertiesUtils;
import com.example.yx.utis.HttpClient;
import com.example.yx.vo.user.UserLoginVo;
import com.github.wxpay.sdk.WXPayUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class WeixinServiceImpl implements WeixinService {

    @Autowired
    private PaymentInfoService paymentInfoService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Override
    public Map<String, String> createJsapi(String orderNo) {
        //1.为了防止重复支付，通过查询orderNo能否得到PaymentInfo来判断
        PaymentInfo paymentInfo=paymentInfoService.getPaymentByOrderNo(orderNo);
        if(paymentInfo==null){
            //2.将PaymentInfo状态改为待支付状态
            paymentInfo=paymentInfoService.savePaymentInfo(orderNo);
        }
        //2.封装微信支付系统接口需要参数
        Map<String,String>paramMap=new HashMap<>();
        paramMap.put("appid", ConstantPropertiesUtils.APPID);
        paramMap.put("mch_id", ConstantPropertiesUtils.PARTNER);
        paramMap.put("nonce_str", WXPayUtil.generateNonceStr());
        paramMap.put("body", paymentInfo.getSubject());
        paramMap.put("out_trade_no", paymentInfo.getOrderNo());
        int totalFee = paymentInfo.getTotalAmount().multiply(new BigDecimal(100)).intValue();
        paramMap.put("total_fee", String.valueOf(totalFee));
        paramMap.put("spbill_create_ip", "127.0.0.1");
        paramMap.put("notify_url", ConstantPropertiesUtils.NOTIFYURL);
        paramMap.put("trade_type", "JSAPI");
//			paramMap.put("openid", "o1R-t5trto9c5sdYt6l1ncGmY5iY");
        //获取openId
        UserLoginVo userLoginVo = (UserLoginVo)redisTemplate.opsForValue().get("user:login:" + paymentInfo.getUserId());
        if(null != userLoginVo && !StringUtils.isEmpty(userLoginVo.getOpenId())) {
            paramMap.put("openid", userLoginVo.getOpenId());
        } else {
            paramMap.put("openid", "oD7av4igt-00GI8PqsIlg5FROYnI");
        }
        //3.使用HttpClient调用微信支付系统接口
        HttpClient client = new HttpClient("https://api.mch.weixin.qq.com/pay/unifiedorder");
        //设置参数，xml格式
        try {
            client.setXmlParam(WXPayUtil.generateSignedXml(paramMap, ConstantPropertiesUtils.PARTNERKEY));
            client.setHttps(true);
            client.post();
            //4.调用微信支付系统接口之后，返回结果prepay_id
            String xml=client.getContent();
            Map<String, String> resultMap = WXPayUtil.xmlToMap(xml);

            //5.封装需要数据-包含预付单标识 prepay_id
            Map<String, String> parameterMap = new HashMap<>();
            String prepayId = String.valueOf(resultMap.get("prepay_id"));
            String packages = "prepay_id=" + prepayId;
            parameterMap.put("appId", ConstantPropertiesUtils.APPID);
            parameterMap.put("nonceStr", resultMap.get("nonce_str"));
            parameterMap.put("package", packages);
            parameterMap.put("signType", "MD5");
            parameterMap.put("timeStamp", String.valueOf(new Date().getTime()));
            String sign = WXPayUtil.generateSignature(parameterMap, ConstantPropertiesUtils.PARTNERKEY);

            //6.返回结果
            Map<String, String> result = new HashMap();
            result.put("timeStamp", parameterMap.get("timeStamp"));
            result.put("nonceStr", parameterMap.get("nonceStr"));
            result.put("signType", "MD5");
            result.put("paySign", sign);
            result.put("package", packages);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
