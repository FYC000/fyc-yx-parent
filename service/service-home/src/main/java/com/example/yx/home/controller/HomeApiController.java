package com.example.yx.home.controller;

import com.example.yx.common.auth.AuthContextHolder;
import com.example.yx.common.result.Result;
import com.example.yx.home.service.HomeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@Api(tags = "首页接口")
@RequestMapping("api/home")
public class HomeApiController {
    @Autowired
    private HomeService homeService;

    @ApiOperation("首页数据显示接口")
    @GetMapping("index")
    public Result index(){
        Long userId = AuthContextHolder.getUserId();
        Map<String,Object> map=homeService.homeData(userId);
        return Result.ok(map);
    }
}
