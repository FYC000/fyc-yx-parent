package com.example.yx.home.controller;

import com.example.yx.common.result.Result;
import com.example.yx.home.service.ItemService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Api(tags = "商品详情")
@RequestMapping("api/home")
public class ItemApiController {
    @Autowired
    private ItemService itemService;

    @ApiOperation("获取sku详细信息")
    @GetMapping("item/{id}")
    public Result item(@PathVariable("id") Long id){
        Map<String,Object> map=itemService.item(id);
        return Result.ok(map);
    }
}
