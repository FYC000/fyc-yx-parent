package com.example.yx.cart.service.impl;

import com.example.yx.cart.service.CartInfoService;
import com.example.yx.client.product.ProductFeignClient;
import com.example.yx.common.auth.AuthContextHolder;
import com.example.yx.common.constant.RedisConst;
import com.example.yx.common.exception.yxException;
import com.example.yx.common.result.Result;
import com.example.yx.common.result.ResultCodeEnum;
import com.example.yx.enums.SkuType;
import com.example.yx.model.order.CartInfo;
import com.example.yx.model.product.SkuInfo;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CartInfoServiceImpl implements CartInfoService {
    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private RedisTemplate redisTemplate;
    //封装购物车在redis的key
    public String getCartKey(Long userId){
        //user:UserId:cart
        return RedisConst.USER_KEY_PREFIX + userId + RedisConst.USER_CART_KEY_SUFFIX;
    }

    @Override
    public void addToCart(Long userId, Long skuId, Integer skuNum) {
        //获取redis的hash的key值
        String cartKey = this.getCartKey(userId);
        //判断当前redis中是否含有商品（field是否存在）
        BoundHashOperations<String,String,CartInfo> hashOperations = redisTemplate.boundHashOps(cartKey);
        CartInfo cartInfo=null;
        if(hashOperations.hasKey(skuId.toString())){
            //若存在field，说明商品数量已经加入购物车了
            //获取商品，并进行更新
            cartInfo = hashOperations.get(skuId.toString());
            int currentKeyNum = cartInfo.getSkuNum() + skuNum;
            if(currentKeyNum<1)return ;
            //更新购物车数据
            cartInfo.setSkuNum(currentKeyNum);
            cartInfo.setCurrentBuyNum(currentKeyNum);
            //获取用户当前已经购买的sku个数，sku限量，每天不能超买
            Integer perLimit = cartInfo.getPerLimit();
            if(currentKeyNum>perLimit){
                throw new yxException(ResultCodeEnum.SKU_LIMIT_ERROR);
            }
            //添加购物车数量
            cartInfo.setIsChecked(1);
            cartInfo.setUpdateTime(new Date());
        }else{
            //若不存在field，说明商品数量未加入购物车了
            skuNum=1;
            //远程调用，通过skuId获取skuInfo
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            if(skuInfo==null)throw new yxException(ResultCodeEnum.DATA_ERROR);
            cartInfo=new CartInfo();
            cartInfo.setSkuId(skuId);
            cartInfo.setCategoryId(skuInfo.getCategoryId());
            cartInfo.setSkuType(skuInfo.getSkuType());
            cartInfo.setIsNewPerson(skuInfo.getIsNewPerson());
            cartInfo.setUserId(userId);
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuNum(skuNum);
            cartInfo.setCurrentBuyNum(skuNum);
            cartInfo.setSkuType(SkuType.COMMON.getCode());
            cartInfo.setPerLimit(skuInfo.getPerLimit());
            cartInfo.setImgUrl(skuInfo.getImgUrl());
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setWareId(skuInfo.getWareId());
            cartInfo.setIsChecked(1);
            cartInfo.setStatus(1);
            cartInfo.setCreateTime(new Date());
            cartInfo.setUpdateTime(new Date());
        }
        //更新缓存
        hashOperations.put(skuId.toString(),cartInfo);
        //设置过期时间
        this.setExpire(cartKey);
    }
    //根据skuId删除购物车中的商品
    @Override
    public void deleteCart(Long skuId, Long userId) {
        BoundHashOperations<String,String,CartInfo> hashOperations = redisTemplate.boundHashOps(getCartKey(userId));
        hashOperations.delete(skuId.toString());
    }
    //清空购物车
    @Override
    public void deleteAllCart(Long userId) {
        BoundHashOperations<String,String,CartInfo> hashOperations = redisTemplate.boundHashOps(getCartKey(userId));
        List<CartInfo> cartInfoList = hashOperations.values();
        cartInfoList.forEach(cartInfo->hashOperations.delete(cartInfo.getSkuId().toString()));
    }
    //批量删除购物车的商品
    @Override
    public void batchDeleteCart(List<Long> skuIdList, Long userId) {
        BoundHashOperations<String,String,CartInfo> hashOperations = redisTemplate.boundHashOps(getCartKey(userId));
        skuIdList.forEach(skuId->{
                hashOperations.delete(skuId.toString());
        });
    }
    //查看购物车列表
    @Override
    public List<CartInfo> getCartList(Long userId) {
        List<CartInfo> cartInfoArrayList = new ArrayList<>();
        if(StringUtils.isEmpty(userId.toString()))return cartInfoArrayList;
        String cartKey = getCartKey(userId);
        BoundHashOperations<String,String,CartInfo> hashOperations = redisTemplate.boundHashOps(cartKey);
        cartInfoArrayList=hashOperations.values();
        if(!CollectionUtils.isEmpty(cartInfoArrayList)){
            cartInfoArrayList.sort(new Comparator<CartInfo>() {
                @Override
                public int compare(CartInfo cartInfo, CartInfo t1) {
                    return cartInfo.getCreateTime().compareTo(t1.getCreateTime());
                }
            });
        }
        return cartInfoArrayList;
    }

    @Override
    public void checkCart(Long userId, Integer isChecked, Long skuId) {
        String cartKey = this.getCartKey(userId);
        BoundHashOperations<String,String,CartInfo> hashOperations = redisTemplate.boundHashOps(cartKey);
        CartInfo cartInfo = hashOperations.get(skuId.toString());
        if(cartInfo!=null){
            cartInfo.setIsChecked(isChecked);
            hashOperations.put(skuId.toString(),cartInfo);
            this.setExpire(cartKey);
        }

    }

    @Override
    public void checkAllCart(Long userId, Integer isChecked) {
        String cartKey = this.getCartKey(userId);
        BoundHashOperations<String,String,CartInfo> hashOperations = redisTemplate.boundHashOps(cartKey);
        List<CartInfo> cartInfoList = hashOperations.values();
        cartInfoList.forEach(cartInfo -> {
            cartInfo.setIsChecked(isChecked);
            hashOperations.put(cartInfo.getSkuId().toString(),cartInfo);
        });
        this.setExpire(cartKey);

    }

    @Override
    public void batchCheckCart(List<Long> skuIdList, Long userId, Integer isChecked) {
        String cartKey = this.getCartKey(userId);
        BoundHashOperations<String,String,CartInfo> hashOperations = redisTemplate.boundHashOps(cartKey);
        skuIdList.forEach(skuId->{
            CartInfo cartInfo = hashOperations.get(skuId.toString());
            cartInfo.setIsChecked(isChecked);
            hashOperations.put(skuId.toString(),cartInfo);
        });
        this.setExpire(cartKey);
    }

    @Override
    public List<CartInfo> getCartCheckedList(Long userId) {
        String cartKey = this.getCartKey(userId);
        BoundHashOperations<String,String,CartInfo> hashOperations = redisTemplate.boundHashOps(cartKey);
        List<CartInfo> cartInfoList = hashOperations.values();
        List<CartInfo> checkedCartInfoList = cartInfoList.stream().filter(cartInfo -> cartInfo.getIsChecked().intValue() == 1).collect(Collectors.toList());
        return checkedCartInfoList;
    }

    @Override
    public void deleteCartChecked(Long userId) {
        List<CartInfo> cartCheckedList = getCartCheckedList(userId);
        List<Long> skuIdList = cartCheckedList.stream().map(cartInfo -> cartInfo.getSkuId()).collect(Collectors.toList());
        String cartKey = getCartKey(userId);
        BoundHashOperations hashOperations = redisTemplate.boundHashOps(cartKey);
        skuIdList.forEach(item->hashOperations.delete(item.toString()));
    }

    //设置缓存过期时间
    public void setExpire(String key){
        redisTemplate.expire(key,RedisConst.USER_CART_EXPIRE, TimeUnit.SECONDS);
    }
}
