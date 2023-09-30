package com.example.yx.user.controller;

import com.alibaba.fastjson2.JSONObject;
import com.example.yx.common.auth.AuthContextHolder;
import com.example.yx.common.constant.RedisConst;
import com.example.yx.common.exception.yxException;
import com.example.yx.common.result.Result;
import com.example.yx.common.result.ResultCodeEnum;
import com.example.yx.common.utis.helper.JwtHelper;
import com.example.yx.enums.UserType;
import com.example.yx.enums.user.User;
import com.example.yx.user.service.UserService;
import com.example.yx.user.utils.ConstantPropertiesUtil;
import com.example.yx.user.utils.HttpClientUtils;
import com.example.yx.vo.user.LeaderAddressVo;
import com.example.yx.vo.user.UserLoginVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/user/weixin")
public class WeixinApiController {
    @Autowired
    private UserService userService;
    @Autowired
    private RedisTemplate redisTemplate;
    @ApiOperation(value = "微信登录获取openid(小程序)")
    @GetMapping("/wxLogin/{code}")
    public Result loginWx(@PathVariable String code) {
        //1.首先获取临时票据code

        //2.然后通过code+小程序id+小程序密钥获取微信接口服务
        //使用httpclient
        String wxOpenAppId = ConstantPropertiesUtil.WX_OPEN_APP_ID;
        String wxOpenAppSecret = ConstantPropertiesUtil.WX_OPEN_APP_SECRET;
        //访问微信接口服务需要拼接get请求的url+参数
        StringBuffer baseAccessTokenUrl = new StringBuffer()
                .append("https://api.weixin.qq.com/sns/jscode2session")
                .append("?appid=%s")
                .append("&secret=%s")
                .append("&js_code=%s")
                .append("&grant_type=authorization_code");

        String accessTokenUrl = String.format(baseAccessTokenUrl.toString(),
                wxOpenAppId,
                wxOpenAppSecret,
                code);

        String result = null;
        try {
            result = HttpClientUtils.get(accessTokenUrl);
        } catch (Exception e) {
            throw new yxException(ResultCodeEnum.FETCH_ACCESSTOKEN_FAILD);
        }

        //3.获取返回的sessionKey+openid
        JSONObject jsonObject = JSONObject.parseObject(result);
        String session_key = jsonObject.getString("session_key");
        String openid = jsonObject.getString("openid");
        //4.通过查询user表的openid判断是否第一次登录，第一次登录要更新用户表
        User user=userService.getUserByOpenId(openid);
        if(user==null){
            user=new User();
            user.setOpenId(openid);
            user.setNickName(user.getNickName());
            user.setPhotoUrl("");
            user.setUserType(UserType.USER);
            user.setIsNew(0);
            userService.save(user);
        }
        //5.通过userId获取团长和提货点信息user->user_delivery->leader
        LeaderAddressVo leaderAddressVo=userService.getLeaderAddressVoByUserId(user.getId());
        //6.生成token字符串
        String token = JwtHelper.createToken(user.getId(), user.getNickName());
        //7.获取当前用户登录信息，存储到redis中并设置缓存时间
        UserLoginVo userLoginVo=userService.getUserLoginVo(user.getId());
        redisTemplate.opsForValue().set(RedisConst.USER_LOGIN_KEY_PREFIX+user.getId(),userLoginVo
                                        ,RedisConst.USERKEY_TIMEOUT, TimeUnit.DAYS);
        //8.将数据封装到map中返回
        Map<String, Object> map = new HashMap<>();
        map.put("user", user);
        map.put("leaderAddressVo", leaderAddressVo);
        map.put("token", token);
        return Result.ok(map);
    }
    @PostMapping("/auth/updateUser")
    @ApiOperation(value = "更新用户昵称与头像")
    public Result updateUser(@RequestBody User user) {
        User user1 = userService.getById(AuthContextHolder.getUserId());
        //把昵称更新为微信用户
        user1.setNickName(user.getNickName().replaceAll("[ue000-uefff]", "*"));
        user1.setPhotoUrl(user.getPhotoUrl());
        userService.updateById(user1);
        return Result.ok(null);
    }
}