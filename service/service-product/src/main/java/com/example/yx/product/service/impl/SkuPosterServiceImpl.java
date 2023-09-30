package com.example.yx.product.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.yx.model.product.SkuImage;
import com.example.yx.model.product.SkuPoster;
import com.example.yx.product.mapper.SkuPosterMapper;
import com.example.yx.product.service.SkuPosterService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SkuPosterServiceImpl extends ServiceImpl<SkuPosterMapper, SkuPoster> implements SkuPosterService {

    @Override
    public List<SkuPoster> getSkuPosterList(Long id) {
        LambdaQueryWrapper<SkuPoster> wrapper = new LambdaQueryWrapper<SkuPoster>().eq(SkuPoster::getSkuId, id);
        return baseMapper.selectList(wrapper);
    }
}
