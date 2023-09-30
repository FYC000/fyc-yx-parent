package com.example.yx.acl.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.yx.acl.mapper.AdminRoleMapper;
import com.example.yx.acl.mapper.RoleMapper;
import com.example.yx.acl.service.AdminRoleService;
import com.example.yx.acl.service.RoleService;
import com.example.yx.model.acl.AdminRole;
import com.example.yx.model.acl.Role;
import com.example.yx.vo.acl.RoleQueryVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {
	@Autowired
	private AdminRoleService adminRoleService;
	//角色分页列表
	@Override
	public IPage<Role> selectPage(Page<Role> pageParam, RoleQueryVo roleQueryVo) {
		//获取条件值：角色名称
		String roleName = roleQueryVo.getRoleName();
		//创建条件构造器对象
		LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
		//判断条件值是否为空
		if(!StringUtils.isEmpty(roleName)) {
			//封装条件
			wrapper.like(Role::getRoleName,roleName);
		}
		//调用mapper方法实现条件分页查询
		IPage<Role> pageModel = baseMapper.selectPage(pageParam, wrapper);
		return pageModel;
	}

	@Override
	public Map<String, Object> findRoleByUserId(Long adminId) {
		//获取所有角色
		List<Role> roleList = baseMapper.selectList(null);
		QueryWrapper<AdminRole> wrapper = new QueryWrapper<>();
		wrapper.eq("admin_id",adminId);
		//获取所有角色id
		List<AdminRole> adminRoleList = adminRoleService.list(wrapper);
		List<Long> roleIdList = adminRoleList.stream().map(item -> item.getRoleId()).collect(Collectors.toList());
		ArrayList<Role> roles = new ArrayList<>();
		for (Role role : roleList) {
			if(roleIdList.contains(role.getId()))roles.add(role);
		}
		Map<String, Object> roleMap = new HashMap<>();
		roleMap.put("assignRoles", roles);
		roleMap.put("allRolesList", roleList);

		return roleMap;
	}

	@Override
	public void saveUserRoleRealtionShip(Long adminId, Long[] roleId) {
		LambdaQueryWrapper<AdminRole> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(AdminRole::getAdminId,adminId);
		adminRoleService.remove(wrapper);
		ArrayList<AdminRole> adminRoles = new ArrayList<>();
		for (Long aLong : roleId) {
			if(StringUtils.isEmpty(aLong))continue;
			AdminRole adminRole = new AdminRole();
			adminRole.setAdminId(adminId);
			adminRole.setRoleId(aLong);
			adminRoles.add(adminRole);
		}
		adminRoleService.saveBatch(adminRoles);
	}

}