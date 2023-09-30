package com.example.yx.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.yx.model.product.SkuImage;
import com.example.yx.model.product.SkuPoster;

import java.util.List;

public interface SkuPosterService extends IService<SkuPoster> {

    List<SkuPoster> getSkuPosterList(Long id);

}
