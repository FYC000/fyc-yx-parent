package com.example.yx.common.auth;

import com.example.yx.common.constant.RedisConst;
import com.example.yx.common.exception.yxException;
import com.example.yx.common.result.ResultCodeEnum;
import com.example.yx.common.utis.helper.JwtHelper;
import com.example.yx.vo.user.UserLoginVo;
import org.checkerframework.checker.units.qual.A;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class UserLoginInterceptor implements HandlerInterceptor {
    private RedisTemplate redisTemplate;
    public UserLoginInterceptor(RedisTemplate redisTemplate){
        this.redisTemplate=redisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        this.initUserLoginVo(request);
        return true;
    }

    private void initUserLoginVo(HttpServletRequest request) {
        String token = request.getHeader("token");
        Long userId = JwtHelper.getUserId(token);
        UserLoginVo userLoginVo = (UserLoginVo) redisTemplate.opsForValue().get(RedisConst.USER_LOGIN_KEY_PREFIX + userId);
        if(userLoginVo!=null){
            AuthContextHolder.setUserId(userLoginVo.getUserId());
            AuthContextHolder.setUserLoginVo(userLoginVo);
            AuthContextHolder.setWareId(userLoginVo.getWareId());
        }else{
            throw new yxException(ResultCodeEnum.FETCH_USERINFO_ERROR);
        }
    }
}
