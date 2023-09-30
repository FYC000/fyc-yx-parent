package com.example.yx.product.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.yx.model.product.SkuAttrValue;
import com.example.yx.model.product.SkuImage;
import com.example.yx.product.mapper.SkuAttrValueMapper;
import com.example.yx.product.service.SkuAttrValueService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SkuAttrValueServiceImpl extends ServiceImpl<SkuAttrValueMapper, SkuAttrValue> implements SkuAttrValueService {

    @Override
    public List<SkuAttrValue> getSkuAttrValueList(Long id) {
        LambdaQueryWrapper<SkuAttrValue> wrapper = new LambdaQueryWrapper<SkuAttrValue>().eq(SkuAttrValue::getSkuId, id);
        return baseMapper.selectList(wrapper);
    }
}
