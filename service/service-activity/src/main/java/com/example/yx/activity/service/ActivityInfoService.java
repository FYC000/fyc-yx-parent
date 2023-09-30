package com.example.yx.activity.service;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.yx.model.activity.ActivityInfo;
import com.example.yx.model.activity.CouponInfo;
import com.example.yx.model.order.CartInfo;
import com.example.yx.vo.activity.ActivityRuleVo;
import com.example.yx.vo.order.CartInfoVo;
import com.example.yx.vo.order.OrderConfirmVo;

import java.util.List;
import java.util.Map;

public interface ActivityInfoService extends IService<ActivityInfo> {

    IPage<ActivityInfo> selectPage(Page<ActivityInfo> pageParam);

    Map<String, Object> findActivityRuleList(Long id);

    void saveActivityRule(ActivityRuleVo activityRuleVo);

    Object findSkuInfoByKeyword(String keyword);

    Map<Long,List<String>> findActivity(List<Long> skuIdList);

    Map<String, Object> findActivityAndCoupon(Long skuId, Long userId);

    OrderConfirmVo findCartActivityAndCoupon(List<CartInfo> cartInfoList, Long userId);

    List<CartInfoVo> findCartActivityList(List<CartInfo> cartInfoList);

    CouponInfo findRangeSkuIdList(List<CartInfo> cartInfoList, Long couponId);

    Boolean updateCouponInfoUseStatus(Long couponId, Long userId, Long orderId);
}
