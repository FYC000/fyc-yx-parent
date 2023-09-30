package com.example.yx.search.Controller;

import com.example.yx.common.result.Result;
import com.example.yx.model.search.SkuEs;
import com.example.yx.search.service.SkuService;
import com.example.yx.vo.search.SkuEsQueryVo;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/search/sku")
////@CrossOrigin
public class SkuApiController {
    @Autowired
    private SkuService skuService;

    @ApiOperation(value = "上架商品")
    @GetMapping("inner/upperSku/{skuId}")
    public Result upperSku(@PathVariable("skuId") Long skuId) {
        skuService.upperSku(skuId);
        return Result.ok();
    }

    @ApiOperation(value = "下架商品")
    @GetMapping("inner/lowerSku/{skuId}")
    public Result lowerSku(@PathVariable("skuId") Long skuId) {
        skuService.lowerSku(skuId);
        return Result.ok();
    }
    @ApiOperation(value = "获取爆品商品")
    @GetMapping("inner/findHotSkuList")
    public List<SkuEs> findHotSkuList() {
        return skuService.findHotSkuList();
    }

    @ApiOperation(value = "搜索商品")
    @GetMapping("{page}/{limit}")
    public Result list(@PathVariable Integer page,@PathVariable Integer limit,SkuEsQueryVo searchParamVo) {
        Pageable pageable = PageRequest.of(page-1, limit);
        Page<SkuEs> pageModel =  skuService.search(pageable, searchParamVo);
        return Result.ok(pageModel);
    }
    @ApiOperation(value = "更新商品incrHotScore")
    @GetMapping("inner/incrHotScore/{skuId}")
    public Boolean incrHotScore(@PathVariable("skuId") Long skuId) {
        // 调用服务层
        skuService.incrHotScore(skuId);
        return true;
    }
}
