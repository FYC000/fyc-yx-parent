package com.example.yx.cart.service;

import com.example.yx.model.order.CartInfo;

import java.util.List;

public interface CartInfoService {
    void addToCart(Long userId, Long skuId, Integer skuNum);

    void deleteCart(Long skuId, Long userId);

    void deleteAllCart(Long userId);

    void batchDeleteCart(List<Long> skuIdList, Long userId);

    List<CartInfo> getCartList(Long userId);

    void checkCart(Long userId, Integer isChecked, Long skuId);

    void checkAllCart(Long userId, Integer isChecked);

    void batchCheckCart(List<Long> skuIdList, Long userId, Integer isChecked);

    List<CartInfo> getCartCheckedList(Long userId);

    void deleteCartChecked(Long userId);
}
