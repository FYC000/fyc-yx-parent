package com.example.yx.sys.service;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.yx.model.sys.RegionWare;
import com.example.yx.vo.sys.RegionWareQueryVo;

public interface RegionWareService extends IService<RegionWare> {

    IPage<RegionWare> selectPage(Page<RegionWare> page1, RegionWareQueryVo vo);

    void saveRegionWare(RegionWare regionWare);

    void updateStatus(Long id, Integer status);
}
