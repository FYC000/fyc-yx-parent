package com.example.yx.sys.controller;


import com.example.yx.common.result.Result;
import com.example.yx.sys.service.RegionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@Api(tags="区域接口")
@RestController
//@CrossOrigin
@RequestMapping("/sys/region")
public class RegionController {
    @Resource
    private RegionService regionService;

    @ApiOperation(value = "根据关键字获取地区列表")
    @GetMapping("findRegionByKeyword/{keyword}")
    /*url: `${api_name}/findRegionByKeyword/${keyword}`,
    method: 'get'*/
    public Result findSkuInfoByKeyword(@PathVariable("keyword") String keyword) {
        return Result.ok(regionService.findRegionByKeyword(keyword));
    }
}

