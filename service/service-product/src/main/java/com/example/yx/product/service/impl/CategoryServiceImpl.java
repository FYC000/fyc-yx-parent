package com.example.yx.product.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.yx.model.product.Category;
import com.example.yx.product.mapper.CategoryMapper;
import com.example.yx.product.service.CategoryService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.yx.vo.product.CategoryQueryVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Override
    public IPage<Category> selectPage(Page<Category> page1, CategoryQueryVo vo) {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        String name = vo.getName();
        if(!StringUtils.isEmpty(name)){
            wrapper.like(Category::getName,name);
        }
        IPage<Category> page = baseMapper.selectPage(page1, wrapper);
        return page;
    }
}
