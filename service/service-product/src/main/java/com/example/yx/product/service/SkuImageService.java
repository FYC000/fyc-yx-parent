package com.example.yx.product.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.example.yx.model.product.SkuImage;

import java.util.List;

public interface SkuImageService extends IService<SkuImage> {

    List<SkuImage> getSkuImageList(Long id);
}
