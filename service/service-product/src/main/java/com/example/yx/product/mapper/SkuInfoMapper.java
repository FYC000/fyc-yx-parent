package com.example.yx.product.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.yx.model.product.SkuInfo;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SkuInfoMapper extends BaseMapper<SkuInfo> {
    //解锁库存
    void unlockStock(@Param("skuId") Long skuId,@Param("skuNum") Integer skuNum);
    //验证库存
    SkuInfo checkStock(@Param("skuId") Long skuId,@Param("skuNum") Integer skuNum);
    //锁定库存
    Integer lockStock(@Param("skuId") Long skuId,@Param("skuNum") Integer skuNum);

    void minusStock(@Param("skuId") Long skuId,@Param("skuNum") Integer skuNum);
}
