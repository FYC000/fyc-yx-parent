package com.example.yx.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.yx.enums.user.User;
import com.example.yx.vo.user.LeaderAddressVo;
import com.example.yx.vo.user.UserLoginVo;

public interface UserService extends IService<User> {
    User getUserByOpenId(String openid);

    LeaderAddressVo getLeaderAddressVoByUserId(Long id);

    UserLoginVo getUserLoginVo(Long id);
}
