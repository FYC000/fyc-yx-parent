package com.example.yx.product.service;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.yx.model.product.AttrGroup;
import com.example.yx.vo.product.AttrGroupQueryVo;

public interface AttrGroupService extends IService<AttrGroup> {

    IPage<AttrGroup> selectPage(Page<AttrGroup> pageParam, AttrGroupQueryVo attrGroupQueryVo);

    Object findAllList();
}
