package com.example.yx.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.yx.model.product.Attr;

public interface AttrService extends IService<Attr> {

    Object findByAttrGroupId(Long attrGroupId);
}
