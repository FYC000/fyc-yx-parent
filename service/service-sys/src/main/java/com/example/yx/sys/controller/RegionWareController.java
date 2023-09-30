package com.example.yx.sys.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.yx.common.result.Result;
import com.example.yx.model.sys.RegionWare;
import com.example.yx.sys.service.RegionWareService;
import com.example.yx.vo.sys.RegionWareQueryVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "开通区域接口")
@RestController
@RequestMapping("/admin/sys/regionWare")
//@CrossOrigin
public class RegionWareController {

    @Autowired
    private RegionWareService regionWareService;

    //开通区域列表
    /*url: `${api_name}/${page}/${limit}`,
    method: 'get',
    params: searchObj*/
    @ApiOperation(value ="获取开通区域列表")
    @GetMapping("{page}/{limit}")
    public Result index(@PathVariable Long page, @PathVariable Long limit, RegionWareQueryVo vo){
        Page<RegionWare> page1 = new Page<>(page, limit);
        IPage<RegionWare> PageModel= regionWareService.selectPage(page1,vo);
        return Result.ok(PageModel);
    }

    //添加开通区域
    /*url: `${api_name}/save`,
    method: 'post',
    data: role*/
    @ApiOperation(value = "添加开通区域")
    @PostMapping("save")
    public Result save(@RequestBody RegionWare regionWare){
        regionWareService.saveRegionWare(regionWare);
        return Result.ok();
    }

    //删除开通区域
    @ApiOperation(value = "删除")
    @DeleteMapping("remove/{id}")
    public Result remove(@PathVariable Long id) {
        regionWareService.removeById(id);
        return Result.ok();
    }

    @ApiOperation(value = "取消开通区域")
    @PostMapping("updateStatus/{id}/{status}")
    public Result updateStatus(@PathVariable Long id,@PathVariable Integer status) {
        regionWareService.updateStatus(id, status);
        return Result.ok();
    }
}

