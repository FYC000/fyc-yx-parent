package com.example.yx.home.service.impl;

import com.example.yx.activity.client.ActivityFeignClient;
import com.example.yx.client.product.ProductFeignClient;
import com.example.yx.client.search.SkuFeignClient;
import com.example.yx.common.auth.AuthContextHolder;
import com.example.yx.common.result.Result;
import com.example.yx.home.service.ItemService;
import com.example.yx.model.product.SkuInfo;
import com.example.yx.vo.product.SkuInfoVo;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
@Service
public class ItemServiceImpl implements ItemService {
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;
    @Autowired
    private SkuFeignClient skuFeignClient;
    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private ActivityFeignClient activityFeignClient;
    @SneakyThrows
    @Override
    public Map<String, Object> item(Long id) {
        HashMap<String, Object> map = new HashMap<>();
        Long userId = AuthContextHolder.getUserId();
        //1.获取sku商品信息
        CompletableFuture<Void> skuInfoCompletableFuture = CompletableFuture.runAsync(() -> {
            //远程调用获取商品信息
            SkuInfoVo skuInfoVo = productFeignClient.getSkuInfoVo(id);
            map.put("skuInfoVo", skuInfoVo);
        },threadPoolExecutor);

        //2.通过skuId获取商品活动和优惠券信息
        CompletableFuture<Void> activityCompletableFuture = CompletableFuture.runAsync(() -> {
            //远程调用获取优惠券信息
            Map<String,Object> activityMap= activityFeignClient.findActivityAndCoupon(id,userId);
            map.putAll(activityMap);
        },threadPoolExecutor);
        //3.更新商品热度
        CompletableFuture<Void> hotCompletableFuture = CompletableFuture.runAsync(() -> {
            skuFeignClient.incrHotScore(id);
        },threadPoolExecutor);
        CompletableFuture<Void> allOf = CompletableFuture.allOf(skuInfoCompletableFuture, activityCompletableFuture, hotCompletableFuture);
        allOf.join();
        return map;
    }
}
