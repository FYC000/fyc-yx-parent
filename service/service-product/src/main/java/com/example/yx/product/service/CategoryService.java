package com.example.yx.product.service;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.yx.model.product.Category;
import com.example.yx.vo.product.CategoryQueryVo;

public interface CategoryService extends IService<Category> {

    IPage<Category> selectPage(Page<Category> page1, CategoryQueryVo vo);
}
