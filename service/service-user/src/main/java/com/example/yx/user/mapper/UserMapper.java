package com.example.yx.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.yx.enums.user.User;
import org.springframework.stereotype.Repository;

@Repository
public interface UserMapper extends BaseMapper<User> {
}
