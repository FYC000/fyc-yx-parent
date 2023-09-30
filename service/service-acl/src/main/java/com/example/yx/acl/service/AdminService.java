package com.example.yx.acl.service;

import com.example.yx.model.acl.Admin;
import com.example.yx.vo.acl.AdminQueryVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

public interface AdminService extends IService<Admin> {

	/**
	 * 用户分页列表
	 * @param pageParam
	 * @param userQueryVo
	 * @return
	 */
	IPage<Admin> selectPage(Page<Admin> pageParam, AdminQueryVo userQueryVo);

}