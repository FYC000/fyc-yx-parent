package com.example.yx.acl.service;

import com.example.yx.model.acl.Role;
import com.example.yx.vo.acl.RoleQueryVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface RoleService extends IService<Role> {

	//角色分页列表
	IPage<Role> selectPage(Page<Role> pageParam, RoleQueryVo roleQueryVo);

    Map<String, Object> findRoleByUserId(Long adminId);

	void saveUserRoleRealtionShip(Long adminId, Long[] roleId);
}