package com.example.yx.product.controller.api;

import com.example.yx.model.product.Category;
import com.example.yx.model.product.SkuInfo;
import com.example.yx.product.service.CategoryService;
import com.example.yx.product.service.SkuInfoService;
import com.example.yx.vo.product.SkuInfoVo;
import com.example.yx.vo.product.SkuStockLockVo;
import io.swagger.annotations.ApiOperation;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product")
//@CrossOrigin
public class ProductInnnerController {
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private SkuInfoService skuInfoService;
    @ApiOperation(value = "根据分类id获取分类信息")
    @GetMapping("inner/getCategory/{categoryId}")
    public Category getCategory(@PathVariable Long categoryId) {
        return categoryService.getById(categoryId);
    }
    @ApiOperation(value = "根据skuId获取sku信息")
    @GetMapping("inner/getSkuInfo/{skuId}")
    public SkuInfo getSkuInfo(@PathVariable("skuId") Long skuId) {
        return skuInfoService.getById(skuId);
    }
    @ApiOperation(value = "根据SkuId列表获取sku信息")
    @PostMapping("inner/findSkuInfoList")
    public List<SkuInfo> findSkuInfoList(@RequestBody List<Long>skuIdList){
        List<SkuInfo> list=skuInfoService.findSkuInfoList(skuIdList);
        return list;
    }
    @ApiOperation("根据categoryId列表获取category信息")
    @PostMapping("inner/findCategoryList")
    public List<Category>findCategoryList(@RequestBody List<Long>categoryIdList){
        List<Category> categoryList=categoryService.listByIds(categoryIdList);
        return categoryList;
    }
    @ApiOperation("根据关键字匹配sku列表")
    @GetMapping("inner/findSkuInfoByKeyword/{keyword}")
    public List<SkuInfo>findSkuInfoByKeyword(@PathVariable("keyword")String keyword){
        return skuInfoService.findSkuInfoByKeyword(keyword);
    }
    @ApiOperation("获取所有分类")
    @GetMapping("inner/findAllCategoryList")
    public List<Category> findAllCategoryList(){
        return categoryService.list();
    }
    @ApiOperation("获取新人专享商品")
    @GetMapping("inner/findNewPersonSkuInfoList")
    public List<SkuInfo> findNewPersonSkuInfoList() {
        return skuInfoService.findNewPersonList();
    }
    @ApiOperation(value = "根据skuId获取sku信息")
    @GetMapping("inner/getSkuInfoVo/{skuId}")
    public SkuInfoVo getSkuInfoVo(@PathVariable("skuId") Long skuId) {
        return skuInfoService.getSkuInfoVo(skuId);
    }
    @ApiOperation(value = "验证并锁定库存")
    @PostMapping("inner/checkAndLock/{orderNo}")
    public Boolean checkAndLock(@RequestBody List<SkuStockLockVo> skuStockLockVoList,@PathVariable("orderNo") String orderNo) {
        return skuInfoService.checkAndLock(skuStockLockVoList, orderNo);
    }
}
