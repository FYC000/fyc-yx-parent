package com.example.yx.acl.service;

import com.example.yx.model.acl.Permission;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface PermissionService extends IService<Permission> {

    //获取所有菜单列表
    List<Permission> queryAllMenu();

    //递归删除
    boolean removeChildById(Long id);

    List<Permission> findPermissionByRoleId(Long roleId);

    void saveRolePermissionRealtionShip(Long roleId, Long[] permissionId);
}