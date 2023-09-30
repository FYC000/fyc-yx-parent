package com.example.yx.activity.api;

import com.example.yx.activity.service.ActivityInfoService;
import com.example.yx.model.activity.CouponInfo;
import com.example.yx.model.order.CartInfo;
import com.example.yx.vo.order.CartInfoVo;
import com.example.yx.vo.order.OrderConfirmVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Api(tags = "促销与优惠券接口")
@RestController
@RequestMapping("/api/activity")
@Slf4j
public class ActivityApiController {

	@Resource
	private ActivityInfoService activityInfoService;

	@ApiOperation(value = "根据skuId列表获取促销信息")
	@PostMapping("inner/findActivity")
	public Map<Long, List<String>> findActivity(@RequestBody List<Long> skuIdList) {
		return activityInfoService.findActivity(skuIdList);
	}

	@ApiOperation(value = "根据skuId获取促销与优惠券信息")
	@GetMapping("inner/findActivityAndCoupon/{skuId}/{userId}")
	public Map<String, Object> findActivityAndCoupon(@PathVariable Long skuId, @PathVariable("userId") Long userId) {
		return activityInfoService.findActivityAndCoupon(skuId, userId);
	}
	@ApiOperation(value = "获取购物车满足条件的促销与优惠券信息")
	@PostMapping("inner/findCartActivityAndCoupon/{userId}")
	public OrderConfirmVo findCartActivityAndCoupon(@RequestBody List<CartInfo> cartInfoList, @PathVariable("userId") Long userId) {
		return activityInfoService.findCartActivityAndCoupon(cartInfoList, userId);
	}
	@ApiOperation("获取购物车对应的规则数据")
	@PostMapping("inner/findCartActivityList")
	public List<CartInfoVo>findCartActivityList(@RequestBody List<CartInfo>cartInfoList){
		return activityInfoService.findCartActivityList(cartInfoList);
	}
	@ApiOperation("获取购物车对应的优惠券信息")
	@PostMapping("inner/findRangeSkuIdList/{couponId}")
	public CouponInfo findRangeSkuIdList(@RequestBody List<CartInfo>cartInfoList,
										 @PathVariable("couponId")Long couponId){
		return activityInfoService.findRangeSkuIdList(cartInfoList,couponId);
	}
	@ApiOperation("更新优惠券使用状态")
	@GetMapping("inner/updateCouponInfoUseStatus/{couponId}/{userId}/{orderId}")
	public Boolean updateCouponInfoUseStatus(@PathVariable("couponId")Long couponId,
											 @PathVariable("userId")Long userId,
											 @PathVariable("orderId")Long orderId){
		return activityInfoService.updateCouponInfoUseStatus(couponId,userId,orderId);
	}

}