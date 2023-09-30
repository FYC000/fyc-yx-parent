package com.example.yx.product.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.yx.model.product.SkuImage;
import com.example.yx.model.product.SkuPoster;
import com.example.yx.product.mapper.SkuImageMapper;
import com.example.yx.product.service.SkuImageService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SkuImageServiceImpl extends ServiceImpl<SkuImageMapper, SkuImage> implements SkuImageService {

    @Override
    public List<SkuImage> getSkuImageList(Long id) {
        LambdaQueryWrapper<SkuImage> wrapper = new LambdaQueryWrapper<SkuImage>().eq(SkuImage::getSkuId, id);
        return baseMapper.selectList(wrapper);
    }
}
