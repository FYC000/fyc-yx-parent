package com.example.yx.acl.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.yx.acl.mapper.RolePermissionMapper;
import com.example.yx.acl.service.RolePermissionService;
import com.example.yx.model.acl.RolePermission;
import org.springframework.stereotype.Service;

@Service
public class RolePermissionServiceImpl extends ServiceImpl<RolePermissionMapper, RolePermission>implements RolePermissionService {
}
