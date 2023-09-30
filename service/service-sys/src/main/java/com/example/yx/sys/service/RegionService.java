package com.example.yx.sys.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.example.yx.model.sys.Region;

public interface RegionService extends IService<Region> {

    Object findRegionByKeyword(String keyword);
}
