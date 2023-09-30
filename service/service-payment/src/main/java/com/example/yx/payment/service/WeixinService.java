package com.example.yx.payment.service;

import java.util.Map;

public interface WeixinService {
    Map<String, String> createJsapi(String orderNo);
}
