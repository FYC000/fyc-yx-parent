package com.example.yx.order.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.yx.CartFeignClient;
import com.example.yx.activity.client.ActivityFeignClient;
import com.example.yx.client.product.ProductFeignClient;
import com.example.yx.client.user.UserFeignClient;
import com.example.yx.common.auth.AuthContextHolder;
import com.example.yx.common.constant.RedisConst;
import com.example.yx.common.exception.yxException;
import com.example.yx.common.result.ResultCodeEnum;
import com.example.yx.common.utis.DateUtil;
import com.example.yx.enums.*;
import com.example.yx.model.activity.ActivityRule;
import com.example.yx.model.activity.CouponInfo;
import com.example.yx.model.order.CartInfo;
import com.example.yx.model.order.OrderInfo;
import com.example.yx.model.order.OrderItem;
import com.example.yx.mq.constant.MqConst;
import com.example.yx.mq.service.RabbitService;
import com.example.yx.order.mapper.OrderInfoMapper;
import com.example.yx.order.mapper.OrderItemMapper;
import com.example.yx.order.service.OrderInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.yx.vo.order.CartInfoVo;
import com.example.yx.vo.order.OrderConfirmVo;
import com.example.yx.vo.order.OrderSubmitVo;
import com.example.yx.vo.order.OrderUserQueryVo;
import com.example.yx.vo.product.SkuStockLockVo;
import com.example.yx.vo.user.LeaderAddressVo;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {
    @Autowired
    private UserFeignClient userFeignClient;
    @Autowired
    private CartFeignClient cartFeignClient;
    @Autowired
    private ActivityFeignClient activityFeignClient;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private ProductFeignClient productFeignClient ;
    @Autowired
    private RabbitService rabbitService;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private OrderInfoMapper orderInfoMapper;
    //确认订单
    @Override
    public OrderConfirmVo confirmOrder() {
        //获取用户id
        Long userId = AuthContextHolder.getUserId();
        //获取用户对应的团长信息
        LeaderAddressVo leaderAddressVo = userFeignClient.getUserAddressByUserId(userId);
        //获取购物车中选中的状态
        List<CartInfo> cartInfoList = cartFeignClient.getCartCheckedList(userId);
        //获取购物车满足条件的活动和优惠券
        OrderConfirmVo orderTradeVo = activityFeignClient.findCartActivityAndCoupon(cartInfoList, userId);
        //生成唯一订单
        String orderNo = System.currentTimeMillis() + "";
        redisTemplate.opsForValue().set(RedisConst.ORDER_REPEAT+orderNo,orderNo,24, TimeUnit.HOURS);
        //封装OrderConfirmVo其他值
        orderTradeVo.setLeaderAddressVo(leaderAddressVo);
        orderTradeVo.setOrderNo(orderNo);
        return orderTradeVo;
    }
    //生成订单
    @Override
    public Long submitOrder(OrderSubmitVo orderParamVo) {
        //1.设置哪个用户生成的订单
        Long userId = AuthContextHolder.getUserId();
        orderParamVo.setUserId(userId);
        //2.订单不能重复提交，重复提交验证（redis+lua脚本进行判断）
        ////获取传递过来的orderNo，判断redis中是否存在orderNo，若存在，则证明正常提交订单，删除redis中的orderNo
        ////否则表示表单重复提交了，不在进行提交
        String orderNo = orderParamVo.getOrderNo();
        if(StringUtils.isEmpty(orderNo)){
            throw new yxException(ResultCodeEnum.ILLEGAL_REQUEST);
        }
        String script="if(redis.call('get', KEYS[1]) == ARGV[1]) then return redis.call('del', KEYS[1]) else return 0 end";
        Boolean execute = (Boolean)redisTemplate.execute(new DefaultRedisScript(script, Boolean.class), Arrays.asList(RedisConst.ORDER_REPEAT + orderNo), orderNo);
        if(!execute){
            throw new yxException(ResultCodeEnum.REPEAT_SUBMIT);
        }
        //3.验证库存并锁定（并不是真正的减库存）库存
        ////1.远程调用service-cart获取购物车中选中的商品
        List<CartInfo> cartInfoList = cartFeignClient.getCartCheckedList(userId);
        ////2.商品有各种类型，主要处理普通商品
        List<CartInfo> commonSkuList = cartInfoList.stream().filter(cartInfo -> cartInfo.getSkuType() == SkuType.COMMON.getCode()).collect(Collectors.toList());
        ////3.将处理的商品集合转换为List<SkuStockLockVo>
        if(!CollectionUtils.isEmpty(commonSkuList)){
            List<SkuStockLockVo> commonSkuStockLockVo = commonSkuList.stream().map(item -> {
                SkuStockLockVo skuStockLockVo = new SkuStockLockVo();
                skuStockLockVo.setSkuId(item.getSkuId());
                skuStockLockVo.setSkuNum(item.getSkuNum());
                return skuStockLockVo;
            }).collect(Collectors.toList());
            ////4.远程调用service-product模块实现锁定商品
            Boolean isLockSuccess = productFeignClient.checkAndLock(commonSkuStockLockVo, orderNo);
            if(!isLockSuccess)throw new yxException(ResultCodeEnum.ORDER_STOCK_FALL);
        }
        //4.下单过程->向order_info和order_item添加数据
        Long orderId=this.saveOrder(orderParamVo,cartInfoList);
        //5.返回订单id
        return orderId;
    }
    @Transactional(rollbackFor = {Exception.class})
    public Long saveOrder(OrderSubmitVo orderParamVo, List<CartInfo> cartInfoList) {
        //判断购物项是否为空
        if(CollectionUtils.isEmpty(cartInfoList))throw new yxException(ResultCodeEnum.DATA_ERROR);
        Long userId = orderParamVo.getUserId();
        LeaderAddressVo leaderAddressVo = userFeignClient.getUserAddressByUserId(userId);
        if(leaderAddressVo==null){
            throw new yxException(ResultCodeEnum.DATA_ERROR);
        }
        //计算金额
        ////营销活动金额
        Map<String, BigDecimal> computeActivitySplitAmount = computeActivitySplitAmount(cartInfoList);
        ////优惠券金额
        Map<String, BigDecimal> couponInfoSplitAmount = computeCouponInfoSplitAmount(cartInfoList,orderParamVo.getCouponId());
        List<OrderItem>orderItemList=new ArrayList<>();
        for (CartInfo cartInfo : cartInfoList) {
            OrderItem orderItem = new OrderItem();
            orderItem.setId(null);
            orderItem.setCategoryId(cartInfo.getCategoryId());
            if(cartInfo.getSkuType() == SkuType.COMMON.getCode()) {
                orderItem.setSkuType(SkuType.COMMON);
            } else {
                orderItem.setSkuType(SkuType.SECKILL);
            }
            orderItem.setSkuId(cartInfo.getSkuId());
            orderItem.setSkuName(cartInfo.getSkuName());
            orderItem.setSkuPrice(cartInfo.getCartPrice());
            orderItem.setImgUrl(cartInfo.getImgUrl());
            orderItem.setSkuNum(cartInfo.getSkuNum());
            orderItem.setLeaderId(orderParamVo.getLeaderId());
            //营销活动金额
            BigDecimal activityAmount = computeActivitySplitAmount.get("activity:" + orderItem.getSkuId());
            if(activityAmount==null)activityAmount=new BigDecimal(0);
            //优惠券金额
            BigDecimal couponAmount = couponInfoSplitAmount.get("coupon:" + orderItem.getSkuId());
            if(couponAmount==null)couponAmount=new BigDecimal(0);
            //总金额
            BigDecimal skuTotalAmount = orderItem.getSkuPrice().multiply(new BigDecimal(orderItem.getSkuNum()));
            ////减去优惠后的总金额
            BigDecimal splitTotalAmount = skuTotalAmount.subtract(activityAmount).subtract(couponAmount);
            orderItem.setSplitActivityAmount(activityAmount);
            orderItem.setSplitCouponAmount(couponAmount);
            orderItem.setSplitTotalAmount(splitTotalAmount);
            orderItemList.add(orderItem);
        }
        //封装订单OrderInfo数据
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setUserId(userId);
        orderInfo.setOrderNo(orderParamVo.getOrderNo());
        orderInfo.setOrderStatus(OrderStatus.UNPAID);//订单状态，生成成功未支付
        orderInfo.setProcessStatus(ProcessStatus.UNPAID);
        orderInfo.setCouponId(orderParamVo.getCouponId());
        orderInfo.setLeaderId(orderParamVo.getLeaderId());
        orderInfo.setLeaderName(leaderAddressVo.getLeaderName());
        orderInfo.setLeaderPhone(leaderAddressVo.getLeaderPhone());
        orderInfo.setTakeName(leaderAddressVo.getTakeName());
        orderInfo.setReceiverName(orderParamVo.getReceiverName());
        orderInfo.setReceiverPhone(orderParamVo.getReceiverPhone());
        orderInfo.setReceiverProvince(leaderAddressVo.getProvince());
        orderInfo.setReceiverCity(leaderAddressVo.getCity());
        orderInfo.setReceiverDistrict(leaderAddressVo.getDistrict());
        orderInfo.setReceiverAddress(leaderAddressVo.getDetailAddress());
        orderInfo.setWareId(cartInfoList.get(0).getWareId());

        //计算订单金额
        BigDecimal originalTotalAmount = this.computeTotalAmount(cartInfoList);
        BigDecimal activityAmount = computeActivitySplitAmount.get("activity:total");
        if(null == activityAmount) activityAmount = new BigDecimal(0);
        BigDecimal couponAmount = couponInfoSplitAmount.get("coupon:total");
        if(null == couponAmount) couponAmount = new BigDecimal(0);
        BigDecimal totalAmount = originalTotalAmount.subtract(activityAmount).subtract(couponAmount);
        //计算订单金额
        orderInfo.setOriginalTotalAmount(originalTotalAmount);
        orderInfo.setActivityAmount(activityAmount);
        orderInfo.setCouponAmount(couponAmount);
        orderInfo.setTotalAmount(totalAmount);

        //计算团长佣金
        BigDecimal profitRate = new BigDecimal(0);//orderSetService.getProfitRate();
        BigDecimal commissionAmount = orderInfo.getTotalAmount().multiply(profitRate);
        orderInfo.setCommissionAmount(commissionAmount);

        //添加数据到订单基本信息表
        orderInfoMapper.insert(orderInfo);

        //添加订单项到数据库中
        orderItemList.forEach(item->{
            item.setOrderId(orderInfo.getId());
            orderItemMapper.insert(item);
        });

        //如果使用过优惠券，则需要更改优惠券使用状态
        if(orderInfo.getCouponId()!=null){
            activityFeignClient.updateCouponInfoUseStatus(orderInfo.getCouponId(),userId,orderInfo.getId());
        }

        //下单成功，记录用户购物商品数量，保存到redis中
        String orderSkuKey = RedisConst.ORDER_SKU_MAP + userId;
        BoundHashOperations<String, String, Integer> hashOperations = redisTemplate.boundHashOps(orderSkuKey);
        cartInfoList.forEach(cartInfo -> {
            if(hashOperations.hasKey(cartInfo.getSkuId().toString())) {
                Integer orderSkuNum = hashOperations.get(cartInfo.getSkuId().toString()) + cartInfo.getSkuNum();
                hashOperations.put(cartInfo.getSkuId().toString(), orderSkuNum);
            }
        });
        redisTemplate.expire(orderSkuKey, DateUtil.getCurrentExpireTimes(), TimeUnit.SECONDS);
        //下单完成，删除购物车选中商品的记录
        //发送mq消息
        rabbitService.sendMessage(MqConst.EXCHANGE_ORDER_DIRECT,MqConst.ROUTING_DELETE_CART,userId);
        return orderInfo.getId();
    }

    //订单详情
    @Override
    public OrderInfo getOrderInfoById(Long orderId) {
        //根据orderId获取订单基本信息
        OrderInfo orderInfo = baseMapper.selectById(orderId);
        //根据orderId查询订单所有订单项列表
        List<OrderItem> orderItemList = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        //封装数据
        orderInfo.setOrderItemList(orderItemList);
        return orderInfo;
    }

    @Override
    public OrderInfo getOrderInfoByOrderNo(String orderNo) {
        OrderInfo orderInfo = baseMapper.selectOne(new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getOrderNo, orderNo));
        return orderInfo;
    }
    //支付成功,修改订单状态为已支付
    @Override
    public void orderPay(String orderNo) {
        OrderInfo orderInfo = baseMapper.selectOne(new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getOrderNo, orderNo));
        //判断orderInfo是否处于未支付状态
        if(orderInfo==null||orderInfo.getOrderStatus()!=OrderStatus.UNPAID)return ;
        orderInfo.setOrderStatus(OrderStatus.WAITING_DELEVER);
        orderInfo.setProcessStatus(ProcessStatus.WAITING_DELEVER);
        baseMapper.updateById(orderInfo);
        //调用rabbitmq扣减库存
        rabbitService.sendMessage(MqConst.EXCHANGE_ORDER_DIRECT, MqConst.ROUTING_MINUS_STOCK, orderNo);
    }
    //查询订单
    @Override
    public IPage<OrderInfo> findUserOrderPage(Page<OrderInfo> pageParam, OrderUserQueryVo orderUserQueryVo) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getUserId, orderUserQueryVo.getUserId())
                .eq(OrderInfo::getOrderStatus, orderUserQueryVo.getOrderStatus());
        Page<OrderInfo> page = baseMapper.selectPage(pageParam, wrapper);

        //获取每个订单，把每个订单里面订单项查询封装
        List<OrderInfo> orderInfoList = page.getRecords();
        for (OrderInfo orderInfo : orderInfoList) {
            //根据订单id查询里面所有订单项列表
            List<OrderItem> orderItemList = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>()
                            .eq(OrderItem::getOrderId, orderInfo.getId())
            );
            //把订单项集合封装到每个订单里面
            orderInfo.setOrderItemList(orderItemList);
            //封装订单状态名称
            orderInfo.getParam().put("orderStatusName",orderInfo.getOrderStatus().getComment());
        }
        return page;
    }

    private BigDecimal computeTotalAmount(List<CartInfo> cartInfoList) {
        BigDecimal total = new BigDecimal(0);
        for (CartInfo cartInfo : cartInfoList) {
            BigDecimal itemTotal = cartInfo.getCartPrice().multiply(new BigDecimal(cartInfo.getSkuNum()));
            total = total.add(itemTotal);
        }
        return total;
    }

    /**
     * 计算购物项分摊的优惠减少金额
     * 打折：按折扣分担
     * 现金：按比例分摊
     * @param cartInfoParamList
     * @return
     */
    private Map<String, BigDecimal> computeActivitySplitAmount(List<CartInfo> cartInfoParamList) {
        Map<String, BigDecimal> activitySplitAmountMap = new HashMap<>();

        //促销活动相关信息
        List<CartInfoVo> cartInfoVoList = activityFeignClient.findCartActivityList(cartInfoParamList);

        //活动总金额
        BigDecimal activityReduceAmount = new BigDecimal(0);
        if(!CollectionUtils.isEmpty(cartInfoVoList)) {
            for(CartInfoVo cartInfoVo : cartInfoVoList) {
                ActivityRule activityRule = cartInfoVo.getActivityRule();
                List<CartInfo> cartInfoList = cartInfoVo.getCartInfoList();
                if(null != activityRule) {
                    //优惠金额， 按比例分摊
                    BigDecimal reduceAmount = activityRule.getReduceAmount();
                    activityReduceAmount = activityReduceAmount.add(reduceAmount);
                    if(cartInfoList.size() == 1) {
                        activitySplitAmountMap.put("activity:"+cartInfoList.get(0).getSkuId(), reduceAmount);
                    } else {
                        //总金额
                        BigDecimal originalTotalAmount = new BigDecimal(0);
                        for(CartInfo cartInfo : cartInfoList) {
                            BigDecimal skuTotalAmount = cartInfo.getCartPrice().multiply(new BigDecimal(cartInfo.getSkuNum()));
                            originalTotalAmount = originalTotalAmount.add(skuTotalAmount);
                        }
                        //记录除最后一项是所有分摊金额， 最后一项=总的 - skuPartReduceAmount
                        BigDecimal skuPartReduceAmount = new BigDecimal(0);
                        if (activityRule.getActivityType() == ActivityType.FULL_REDUCTION) {
                            for(int i=0, len=cartInfoList.size(); i<len; i++) {
                                CartInfo cartInfo = cartInfoList.get(i);
                                if(i < len -1) {
                                    BigDecimal skuTotalAmount = cartInfo.getCartPrice().multiply(new BigDecimal(cartInfo.getSkuNum()));
                                    //sku分摊金额
                                    BigDecimal skuReduceAmount = skuTotalAmount.divide(originalTotalAmount, 2, RoundingMode.HALF_UP).multiply(reduceAmount);
                                    activitySplitAmountMap.put("activity:"+cartInfo.getSkuId(), skuReduceAmount);

                                    skuPartReduceAmount = skuPartReduceAmount.add(skuReduceAmount);
                                } else {
                                    BigDecimal skuReduceAmount = reduceAmount.subtract(skuPartReduceAmount);
                                    activitySplitAmountMap.put("activity:"+cartInfo.getSkuId(), skuReduceAmount);
                                }
                            }
                        } else {
                            for(int i=0, len=cartInfoList.size(); i<len; i++) {
                                CartInfo cartInfo = cartInfoList.get(i);
                                if(i < len -1) {
                                    BigDecimal skuTotalAmount = cartInfo.getCartPrice().multiply(new BigDecimal(cartInfo.getSkuNum()));

                                    //sku分摊金额
                                    BigDecimal skuDiscountTotalAmount = skuTotalAmount.multiply(activityRule.getBenefitDiscount().divide(new BigDecimal("10")));
                                    BigDecimal skuReduceAmount = skuTotalAmount.subtract(skuDiscountTotalAmount);
                                    activitySplitAmountMap.put("activity:"+cartInfo.getSkuId(), skuReduceAmount);

                                    skuPartReduceAmount = skuPartReduceAmount.add(skuReduceAmount);
                                } else {
                                    BigDecimal skuReduceAmount = reduceAmount.subtract(skuPartReduceAmount);
                                    activitySplitAmountMap.put("activity:"+cartInfo.getSkuId(), skuReduceAmount);
                                }
                            }
                        }
                    }
                }
            }
        }
        activitySplitAmountMap.put("activity:total", activityReduceAmount);
        return activitySplitAmountMap;
    }

    private Map<String, BigDecimal> computeCouponInfoSplitAmount(List<CartInfo> cartInfoList, Long couponId) {
        Map<String, BigDecimal> couponInfoSplitAmountMap = new HashMap<>();

        if(null == couponId) return couponInfoSplitAmountMap;
        CouponInfo couponInfo = activityFeignClient.findRangeSkuIdList(cartInfoList, couponId);

        if(null != couponInfo) {
            //sku对应的订单明细
            Map<Long, CartInfo> skuIdToCartInfoMap = new HashMap<>();
            for (CartInfo cartInfo : cartInfoList) {
                skuIdToCartInfoMap.put(cartInfo.getSkuId(), cartInfo);
            }
            //优惠券对应的skuId列表
            List<Long> skuIdList = couponInfo.getSkuIdList();
            if(CollectionUtils.isEmpty(skuIdList)) {
                return couponInfoSplitAmountMap;
            }
            //优惠券优化总金额
            BigDecimal reduceAmount = couponInfo.getAmount();
            if(skuIdList.size() == 1) {
                //sku的优化金额
                couponInfoSplitAmountMap.put("coupon:"+skuIdToCartInfoMap.get(skuIdList.get(0)).getSkuId(), reduceAmount);
            } else {
                //总金额
                BigDecimal originalTotalAmount = new BigDecimal(0);
                for (Long skuId : skuIdList) {
                    CartInfo cartInfo = skuIdToCartInfoMap.get(skuId);
                    BigDecimal skuTotalAmount = cartInfo.getCartPrice().multiply(new BigDecimal(cartInfo.getSkuNum()));
                    originalTotalAmount = originalTotalAmount.add(skuTotalAmount);
                }
                //记录除最后一项是所有分摊金额， 最后一项=总的 - skuPartReduceAmount
                BigDecimal skuPartReduceAmount = new BigDecimal(0);
                if (couponInfo.getCouponType() == CouponType.CASH || couponInfo.getCouponType() == CouponType.FULL_REDUCTION) {
                    for(int i=0, len=skuIdList.size(); i<len; i++) {
                        CartInfo cartInfo = skuIdToCartInfoMap.get(skuIdList.get(i));
                        if(i < len -1) {
                            BigDecimal skuTotalAmount = cartInfo.getCartPrice().multiply(new BigDecimal(cartInfo.getSkuNum()));
                            //sku分摊金额
                            BigDecimal skuReduceAmount = skuTotalAmount.divide(originalTotalAmount, 2, RoundingMode.HALF_UP).multiply(reduceAmount);
                            couponInfoSplitAmountMap.put("coupon:"+cartInfo.getSkuId(), skuReduceAmount);

                            skuPartReduceAmount = skuPartReduceAmount.add(skuReduceAmount);
                        } else {
                            BigDecimal skuReduceAmount = reduceAmount.subtract(skuPartReduceAmount);
                            couponInfoSplitAmountMap.put("coupon:"+cartInfo.getSkuId(), skuReduceAmount);
                        }
                    }
                }
            }
            couponInfoSplitAmountMap.put("coupon:total", couponInfo.getAmount());
        }
        return couponInfoSplitAmountMap;
    }
}
