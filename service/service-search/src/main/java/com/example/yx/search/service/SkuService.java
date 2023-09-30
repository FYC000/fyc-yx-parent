package com.example.yx.search.service;

import com.example.yx.model.search.SkuEs;
import com.example.yx.vo.search.SkuEsQueryVo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SkuService {
    void upperSku(Long skuId);

    void lowerSku(Long skuId);

    List<SkuEs> findHotSkuList();

    Page<SkuEs> search(Pageable pageable, SkuEsQueryVo searchParamVo);

    void incrHotScore(Long skuId);
}
