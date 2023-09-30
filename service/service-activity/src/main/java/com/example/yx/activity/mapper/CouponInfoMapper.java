package com.example.yx.activity.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.yx.model.activity.CouponInfo;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CouponInfoMapper extends BaseMapper<CouponInfo> {

    List<CouponInfo> selectCouponInfoList(Long skuId, Long categoryId, Long userId);

    List<CouponInfo> selectCartCouponInfoList(Long userId);
}
