package com.example.yx.acl.controller;

import com.example.yx.common.result.Result;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Api(tags = "登录接口")
@RestController
@RequestMapping("/admin/acl/index")
//@CrossOrigin
public class IndexController {
    /**
     * 1、请求登陆的login
     */
    @PostMapping("login")
    public Result login(){
        //返回token值
        HashMap<String, String> map = new HashMap<>();
        map.put("token","admin-token");
        return Result.ok(map);
    }
    /**
     * 2 获取用户信息
     */
    @GetMapping("info")
    public Result info(){
        Map<String,Object> map = new HashMap<>();
        map.put("name","user");
        map.put("avatar","https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif");
        return Result.ok(map);
    }

    /**
     * 3 退出
     */
    @PostMapping("logout")
    public Result logout(){
        return Result.ok(null);
    }
}
