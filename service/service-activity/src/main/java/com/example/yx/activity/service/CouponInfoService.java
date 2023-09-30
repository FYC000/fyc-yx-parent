package com.example.yx.activity.service;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.yx.model.activity.CouponInfo;
import com.example.yx.model.activity.CouponRange;
import com.example.yx.model.order.CartInfo;
import com.example.yx.vo.activity.CouponRuleVo;

import java.util.List;
import java.util.Map;

public interface CouponInfoService extends IService<CouponInfo> {

    IPage<CouponInfo> selectPage(Page<CouponInfo> pageParam);

    CouponInfo getCouponInfo(String id);

    Object findCouponRuleList(Long id);

    void saveCouponRule(CouponRuleVo couponRuleVo);

    List<CouponInfo> findCouponInfo(Long skuId, Long userId);

    List<CouponInfo> findCartCouponInfo(List<CartInfo> cartInfoList, Long userId);

    Map<Long, List<Long>> findCouponIdToSkuIdMap(List<CartInfo> cartInfoList, List<CouponRange> couponRangeList);
}
