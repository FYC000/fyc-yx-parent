package com.example.yx.client.product;

import com.example.yx.model.product.Category;
import com.example.yx.model.product.SkuInfo;
import com.example.yx.model.search.SkuEs;
import com.example.yx.vo.product.SkuInfoVo;
import com.example.yx.vo.product.SkuStockLockVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @author FuYiCheng
 * @date 2023-09-01 16:45
 * @description:
 * @version:
 */
@FeignClient(value = "service-product")
public interface ProductFeignClient {
    @GetMapping("/api/product/inner/getCategory/{categoryId}")
    public Category getCategory(@PathVariable("categoryId") Long categoryId);
    @GetMapping("/api/product/inner/getSkuInfo/{skuId}")
    public SkuInfo getSkuInfo(@PathVariable("skuId") Long skuId);
    @PostMapping("/api/product/inner/findSkuInfoList")
    public List<SkuInfo> findSkuInfoList(@RequestBody List<Long>skuIdList);
    @GetMapping("/api/product/inner/findSkuInfoByKeyword/{keyword}")
    public List<SkuInfo>findSkuInfoByKeyword(@PathVariable("keyword")String keyword);
    @PostMapping("/api/product/inner/findCategoryList")
    public List<Category>findCategoryList(@RequestBody List<Long>categoryIdList);
    @GetMapping("/api/product/inner/findAllCategoryList")
    public List<Category> findAllCategoryList();
    @GetMapping("/api/product/inner/findNewPersonSkuInfoList")
    public List<SkuInfo> findNewPersonSkuInfoList();
    @GetMapping("/api/product/inner/getSkuInfoVo/{skuId}")
    public SkuInfoVo getSkuInfoVo(@PathVariable("skuId") Long skuId);
    @PostMapping("/api/product/inner/checkAndLock/{orderNo}")
    public Boolean checkAndLock(@RequestBody List<SkuStockLockVo> skuStockLockVoList, @PathVariable("orderNo") String orderNo);
}
