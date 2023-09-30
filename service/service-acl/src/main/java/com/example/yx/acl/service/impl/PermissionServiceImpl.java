package com.example.yx.acl.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.yx.acl.service.RolePermissionService;
import com.example.yx.common.utis.PermissionHelper;
import com.example.yx.acl.mapper.PermissionMapper;
import com.example.yx.acl.service.PermissionService;
import com.example.yx.model.acl.Permission;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.yx.model.acl.RolePermission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission>
												implements PermissionService {
	@Autowired
	private RolePermissionService rolePermissionService;
	//获取所有菜单
	@Override
	public List<Permission> queryAllMenu() {
		//获取全部权限数据
		List<Permission> allPermissionList = baseMapper.selectList(new QueryWrapper<Permission>().orderByAsc("CAST(id AS SIGNED)"));

		//把权限数据构建成树形结构数据
		List<Permission> result = PermissionHelper.bulid(allPermissionList);
		return result;
	}

	//递归删除菜单
	@Override
	public boolean removeChildById(Long id) {
		List<Long> idList = new ArrayList<>();
		this.selectChildListById(id, idList);
		idList.add(id);
		baseMapper.deleteBatchIds(idList);
		return true;
	}

	@Override
	public List<Permission> findPermissionByRoleId(Long roleId) {
		List<RolePermission> rolePermissionList = rolePermissionService.lambdaQuery().select(RolePermission::getPermissionId).eq(RolePermission::getRoleId, roleId).list();
		List<Long> permissionIds = rolePermissionList.stream().map(RolePermission::getPermissionId).collect(Collectors.toList());
		List<Permission> allPermissionList = this.list();
		Map<Long, Permission> map = allPermissionList.stream().collect(Collectors.toMap(Permission::getId, v -> v));
			List<Permission> result = allPermissionList.stream().filter(item -> item.getPid().equals(0L)).collect(Collectors.toList());
		result.forEach(item->item.setLevel(1));
		allPermissionList.forEach(item->{
			if(permissionIds.contains(item.getId()))item.setSelect(true);
			item.setChildren(new ArrayList<>());
			Permission permission = map.get(item.getPid());
			if(permission!=null){
				item.setLevel(permission.getLevel()+1);
				permission.getChildren().add(item);
			}
		});
		return result;
	}
	@Override
	public void saveRolePermissionRealtionShip(Long roleId, Long[] permissionId) {
		rolePermissionService.remove(new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId,roleId));
		for (Long aLong : permissionId) {
			if(StringUtils.isEmpty(permissionId))continue;
			RolePermission rolePermission = new RolePermission();
			rolePermission.setRoleId(roleId);
			rolePermission.setPermissionId(aLong);
			rolePermissionService.save(rolePermission);
		}
	}

	/**
	 *	递归获取子节点
	 * @param id
	 * @param idList
	 */
	private void selectChildListById(Long id, List<Long> idList) {
		List<Permission> childList = baseMapper.selectList(new QueryWrapper<Permission>().eq("pid", id).select("id"));
		childList.stream().forEach(item -> {
			idList.add(item.getId());
			this.selectChildListById(item.getId(), idList);
		});
	}
}