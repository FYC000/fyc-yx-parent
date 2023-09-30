package com.example.yx.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.yx.model.product.SkuAttrValue;

import java.util.List;

public interface SkuAttrValueService extends IService<SkuAttrValue> {

    List<SkuAttrValue> getSkuAttrValueList(Long id);
}
