package com.example.yx.home.service.impl;

import com.example.yx.client.product.ProductFeignClient;
import com.example.yx.client.search.SkuFeignClient;
import com.example.yx.client.user.UserFeignClient;
import com.example.yx.home.service.HomeService;
import com.example.yx.model.product.Category;
import com.example.yx.model.product.SkuInfo;
import com.example.yx.model.search.SkuEs;
import com.example.yx.vo.user.LeaderAddressVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HomeServiceImpl implements HomeService {
    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private UserFeignClient userFeignClient;
    @Autowired
    private SkuFeignClient skuFeignClient;
    //首页数据显示接口
    @Override
    public Map<String, Object> homeData(Long userId) {
        //1.根据当前userId获取登录用户提货地址信息
        //需要使用远程调用service-user模块接口获取需要数据
        LeaderAddressVo leaderAddressVo = userFeignClient.getUserAddressByUserId(userId);
        //2.获取所有分类
        //主要远程他调用service-product模块接口
        List<Category> categoryList = productFeignClient.findAllCategoryList();
        //3.获取新人专享商品
        //远程调用service-product
        List<SkuInfo> newPersonSkuInfoList = productFeignClient.findNewPersonSkuInfoList();
        //4.获取爆款商品
        //远程调用service-search模块接口
        List<SkuEs> hotSkuList = skuFeignClient.findHotSkuList();
        //5.封装获取数据到map集合，返回
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("leaderAddressVo",leaderAddressVo);
        map.put("categoryList",categoryList);
        map.put("newPersonSkuInfoList",newPersonSkuInfoList);
        map.put("hotSkuList",hotSkuList);
        return map;
    }
}
