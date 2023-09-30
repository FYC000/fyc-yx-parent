package com.example.yx.activity.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.yx.activity.mapper.*;
import com.example.yx.activity.service.ActivityInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.yx.activity.service.CouponInfoService;
import com.example.yx.client.product.ProductFeignClient;
import com.example.yx.common.exception.yxException;
import com.example.yx.common.result.ResultCodeEnum;
import com.example.yx.enums.ActivityType;
import com.example.yx.enums.CouponStatus;
import com.example.yx.model.activity.*;
import com.example.yx.model.order.CartInfo;
import com.example.yx.model.product.SkuInfo;
import com.example.yx.vo.activity.ActivityRuleVo;
import com.example.yx.vo.order.CartInfoVo;
import com.example.yx.vo.order.OrderConfirmVo;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ActivityInfoServiceImpl extends ServiceImpl<ActivityInfoMapper, ActivityInfo> implements ActivityInfoService {
    @Autowired
    private ActivityRuleMapper activityRuleMapper;
    @Autowired
    private ActivitySkuMapper activitySkuMapper;
    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private CouponInfoService couponInfoService;
    @Autowired
    private CouponRangeMapper couponRangeMapper;
    @Autowired
    private CouponUseMapper couponUseMapper;
    @Override
    public IPage<ActivityInfo> selectPage(Page<ActivityInfo> pageParam) {
        //分页查询对象里面获取列表数据
        IPage<ActivityInfo> page = baseMapper.selectPage(pageParam, null);
        //遍历activity集合，得到每个AcitivityInfo对象
        //向Acitivity对象封装活动类型得到activityTypeString属性里面
        List<ActivityInfo> activityInfoList = page.getRecords();
        activityInfoList.stream().forEach(item->{
            item.setActivityTypeString(item.getActivityType().getComment());
        });
        return page;
    }

    @Override
    public Map<String, Object> findActivityRuleList(Long id) {
        Map<String, Object> map = new HashMap<>();
        //1.首先根据activityId获取活动规则信息->Activity_Rule
        LambdaQueryWrapper<ActivityRule> activityRuleWapper = new LambdaQueryWrapper<ActivityRule>();
        List<ActivityRule> activityRuleList = activityRuleMapper.selectList(activityRuleWapper.eq(ActivityRule::getActivityId, id));
        //2.然后根据activityId获取活动范围信息->Activity_Sku
        LambdaQueryWrapper<ActivitySku> activitySkuWrapper = new LambdaQueryWrapper<ActivitySku>();
        List<ActivitySku> activitySkuList = activitySkuMapper.selectList(activitySkuWrapper.eq(ActivitySku::getActivityId, id));
        //获取skuId列表
        List<Long>skuIdList=activitySkuList.stream().map(item->item.getSkuId()).collect(Collectors.toList());
        //3.由于Activity_Sku表只有skuId列表，所以需要远程调用sku_product表获取sku_info信息
        List<SkuInfo> skuInfoList = productFeignClient.findSkuInfoList(skuIdList);
        map.put("activityRuleList", activityRuleList);
        map.put("skuInfoList", skuInfoList);
        return map;
    }

    @Override
    public void saveActivityRule(ActivityRuleVo activityRuleVo) {
        //与之前的多表数据添加操作一致，先删除各表中的数据，在添加数据
        //1.先删除Activity_Sku和Activity_Rule中关于ActivityId的数据
        Long activityId = activityRuleVo.getActivityId();
        activityRuleMapper.delete(new LambdaQueryWrapper<ActivityRule>().eq(ActivityRule::getActivityId,activityId));
        activitySkuMapper.delete(new LambdaQueryWrapper<ActivitySku>().eq(ActivitySku::getActivityId,activityId));
        //2.添加Activity_Sku和Activity_Rule中关于ActivityId的数据
        List<ActivityRule> activityRuleList = activityRuleVo.getActivityRuleList();
        List<ActivitySku> activitySkuList = activityRuleVo.getActivitySkuList();
        ActivityInfo activityInfo = baseMapper.selectById(activityId);
        for (ActivityRule activityRule : activityRuleList) {
            activityRule.setActivityId(activityId);
            activityRule.setActivityType(activityInfo.getActivityType());
            activityRuleMapper.insert(activityRule);
        }
        for (ActivitySku activitySku : activitySkuList) {
            activitySku.setActivityId(activityId);
            activitySkuMapper.insert(activitySku);
        }
    }

    @Override
    public Object findSkuInfoByKeyword(String keyword) {
        //第一步，根据关键字获取sku列表
            //(1)service-product模块创建接口:根据关键字获取sku列表
            //(2)service-activity远程调用该接口
        List<SkuInfo> skuInfoList = productFeignClient.findSkuInfoByKeyword(keyword);
        if(skuInfoList.size()==0)return skuInfoList;
        //第二步，判断商品是否参加过活动，且该活动是否结束，若任然进行，则排除该商品
        //先获取SkuInfoId的列表
        List<Long> SkuIdList = skuInfoList.stream().map(item -> item.getId()).collect(Collectors.toList());
            //(1)查询activity_info和activity_sku两张表，编写SQL语句实现
        List<Long> existSkuIdList = baseMapper.selectExistSkuIdList(SkuIdList);
        String existSkuIdString = "," + StringUtils.join(existSkuIdList.toArray(), ",") + ",";
            //(2)判断处理
        List<SkuInfo> notExistSkuInfoList = new ArrayList<>();
        for(SkuInfo skuInfo : skuInfoList) {
            if(existSkuIdString.indexOf(","+skuInfo.getId()+",") == -1) {
                notExistSkuInfoList.add(skuInfo);
            }
        }
        return notExistSkuInfoList;
    }

    //根据skuId列表获取促销信息
    @Override
    public Map<Long, List<String>> findActivity(List<Long> skuIdList) {
        Map<Long, List<String>> result = new HashMap<>();
        //skuIdList遍历，得到每个skuId
        skuIdList.forEach(skuId -> {
            //根据skuId进行查询，查询sku对应活动里面规则列表
            List<ActivityRule> activityRuleList =
                    baseMapper.findActivityRule(skuId);
            //数据封装，规则名称
            if(!CollectionUtils.isEmpty(activityRuleList)) {
                List<String> ruleList = new ArrayList<>();
                //把规则名称处理
                for (ActivityRule activityRule:activityRuleList) {
                    ruleList.add(this.getRuleDesc(activityRule));
                }
                result.put(skuId,ruleList);
            }
        });
        return result;
    }

    @Override
    public Map<String, Object> findActivityAndCoupon(Long skuId, Long userId) {
        //1.通过skuId获取规则描述
        List<ActivityRule> activityRuleList = baseMapper.findActivityRule(skuId);
        //2.查询优惠卷信息
        List<CouponInfo>couponInfoList=couponInfoService.findCouponInfo(skuId,userId);
        //3.封装数据
        Map<String, Object> map = new HashMap<>();
        map.put("activityRuleList", activityRuleList);
        map.put("couponInfoList", couponInfoList);
        return map;
    }

    @Override
    public OrderConfirmVo findCartActivityAndCoupon(List<CartInfo> cartInfoList, Long userId) {
        //1.获取封装了活动规则和相应购物车项的数据
        List<CartInfoVo> cartInfoVoList = this.findCartActivityList(cartInfoList);
        //2.计算参与活动之后金额
        BigDecimal activityReduceAmount = cartInfoVoList.stream().
                filter(cartInfoVo -> cartInfoVo.getActivityRule() != null).
                map(cartInfoVo -> cartInfoVo.getActivityRule().getReduceAmount()).
                reduce(BigDecimal.ZERO, BigDecimal::add);
        //3.获取购物车可以使用优惠券列表
        List<CouponInfo>couponInfoList=couponInfoService.findCartCouponInfo(cartInfoList,userId);
        couponInfoList.forEach(item->{
            item.setIsSelect(1);
        });
        //4.计算商品使用优惠券之后金额，一次只能使用一次优惠券
        BigDecimal couponReduceAmount = new BigDecimal(0);
        if(!CollectionUtils.isEmpty(couponInfoList)){
            couponReduceAmount= couponInfoList.stream()
                    .filter(couponInfo -> couponInfo.getIsOptimal().intValue() == 1)
                    .map(couponInfo -> couponInfo.getAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        //5.计算没有参与活动，没有使用优惠券原始金额
        BigDecimal originalTotalAmount = cartInfoList.stream()
                .filter(cartInfo -> cartInfo.getIsChecked() == 1)
                .map(cartInfo -> cartInfo.getCartPrice().multiply(new BigDecimal(cartInfo.getSkuNum())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        //6.最终金额
        BigDecimal totalAmount=originalTotalAmount.subtract(activityReduceAmount).subtract(couponReduceAmount);
        //7.封装数据到OrderConfirmVo，返回
        OrderConfirmVo orderTradeVo = new OrderConfirmVo();
        orderTradeVo.setCarInfoVoList(cartInfoVoList);
        orderTradeVo.setActivityReduceAmount(activityReduceAmount);
        orderTradeVo.setCouponInfoList(couponInfoList);
        orderTradeVo.setCouponReduceAmount(couponReduceAmount);
        orderTradeVo.setOriginalTotalAmount(originalTotalAmount);
        orderTradeVo.setTotalAmount(totalAmount);
        return orderTradeVo;

    }

    @Override
    public List<CartInfoVo> findCartActivityList(List<CartInfo> cartInfoList) {
        //封装最终的数据
        ArrayList<CartInfoVo> cartInfoVoList = new ArrayList<>();
        //获取skuId列表
        List<Long> skuIdList = cartInfoList.stream().map(CartInfo::getSkuId).collect(Collectors.toList());
        //通过skuId列表获取AcitivitySku列表
        List<ActivitySku>activitySkuList= baseMapper.selectCartActivityList(skuIdList);
        //通过activityId进行分组
        //map里面key是分组字段，代表activityId，value里面是skuId列表数据
        Map<Long, Set<Long>> activityIdToSkuIdListMap = activitySkuList.stream().collect(Collectors.groupingBy(ActivitySku::getActivityId,
                Collectors.mapping(ActivitySku::getSkuId, Collectors.toSet())));
        //获取活动里面的规则数据 key代表活动id,val代表活动规则数据
        Map<Long,List<ActivityRule>>activityIdToActivityRuleListMap=new HashMap<>();
        Set<Long> activityIdSet = activitySkuList.stream().map(ActivitySku::getActivityId).collect(Collectors.toSet());
        if(!CollectionUtils.isEmpty(activityIdSet)){
            LambdaQueryWrapper<ActivityRule> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(ActivityRule::getActivityId,activityIdSet);
            wrapper.orderByDesc(ActivityRule::getConditionAmount,ActivityRule::getConditionNum);
            List<ActivityRule> activityRuleList = activityRuleMapper.selectList(wrapper);
            activityIdToActivityRuleListMap = activityRuleList.stream().collect(Collectors.groupingBy(activityRule -> activityRule.getActivityId()));
        }
        //获取拥有活动的购物车列表
        Set<Long>activitySkuIdSet=new HashSet<>();
        if(!CollectionUtils.isEmpty(activityIdToSkuIdListMap)){
            //遍历activityIdToSkuIdListMap集合
            Iterator<Map.Entry<Long, Set<Long>>> iterator = activityIdToSkuIdListMap.entrySet().iterator();
            if(iterator.hasNext()){
                Map.Entry<Long, Set<Long>> next = iterator.next();
                //活动id
                Long activityId = next.getKey();
                //每个活动id对应的skuId列表
                Set<Long> currentActivitySkuIdSet = next.getValue();
                //获取当前活动对应的购物车列表
                List<CartInfo> currentActivityCartInfoList = cartInfoList.stream().filter(cartInfo -> currentActivitySkuIdSet.contains(cartInfo.getSkuId())).collect(Collectors.toList());
                //计算购物车总金额和总数量
                BigDecimal totalAmount = computeTotalAmount(currentActivityCartInfoList);
                int cartNum = computeCartNum(currentActivityCartInfoList);
                //计算活动对应规则
                //根据activityId获取活动对应规则
                List<ActivityRule> activityRuleList = activityIdToActivityRuleListMap.get(activityId);
                ActivityType activityType = activityRuleList.get(0).getActivityType();
                ActivityRule activityRule=null;
                if(activityType==ActivityType.FULL_REDUCTION){
                    activityRule = computeFullReduction(totalAmount, activityRuleList);
                }else if(activityType==ActivityType.FULL_DISCOUNT){
                    activityRule = computeFullDiscount(cartNum, totalAmount, activityRuleList);
                }
                //CartInfoVo封装
                CartInfoVo cartInfoVo = new CartInfoVo();
                cartInfoVo.setActivityRule(activityRule);
                cartInfoVo.setCartInfoList(currentActivityCartInfoList);
                cartInfoVoList.add(cartInfoVo);
                //保存参与活动的skuId列表
                activitySkuIdSet.addAll(currentActivitySkuIdSet);
            }
        }
        //获取没有参与活动的SkuId列表
        skuIdList.removeAll(activityIdSet);
        if(!CollectionUtils.isEmpty(skuIdList)){
            Map<Long, CartInfo> skuIdCartInfoMap = cartInfoList.stream().collect(Collectors.toMap(CartInfo::getSkuId, CartInfo -> CartInfo));
            ArrayList<CartInfo> cartInfos = new ArrayList<>();
            for (Long aLong : skuIdList) {
                CartInfo cartInfo = skuIdCartInfoMap.get(aLong);
                cartInfos.add(cartInfo);
            }
            CartInfoVo cartInfoVo = new CartInfoVo();
            cartInfoVo.setActivityRule(null);
            cartInfoVo.setCartInfoList(cartInfos);
            cartInfoVoList.add(cartInfoVo);
        }
        return cartInfoVoList;
    }

    @Override
    public CouponInfo findRangeSkuIdList(List<CartInfo> cartInfoList, Long couponId) {
        if(CollectionUtils.isEmpty(cartInfoList))throw new yxException(ResultCodeEnum.DATA_ERROR);
        CouponInfo couponInfo = couponInfoService.getById(couponId);
        List<CouponRange> couponRangeList = couponRangeMapper.selectList(new LambdaQueryWrapper<CouponRange>().eq(CouponRange::getCouponId, couponId));
        Map<Long, List<Long>> couponIdToSkuIdMap = couponInfoService.findCouponIdToSkuIdMap(cartInfoList, couponRangeList);
        Iterator<Map.Entry<Long, List<Long>>> iterator = couponIdToSkuIdMap.entrySet().iterator();
        Map.Entry<Long, List<Long>> next = iterator.next();
        List<Long> skuIdList = next.getValue();
        couponInfo.setSkuIdList(skuIdList);
        return couponInfo;
    }
    //更新优惠券状态
    @Override
    public Boolean updateCouponInfoUseStatus(Long couponId, Long userId, Long orderId) {
        CouponUse couponUse = new CouponUse();
        couponUse.setOrderId(orderId);
        couponUse.setCouponStatus(CouponStatus.USED);
        couponUse.setUsingTime(new Date());

        QueryWrapper<CouponUse> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("coupon_id", couponId);
        queryWrapper.eq("user_id", userId);
        couponUseMapper.update(couponUse, queryWrapper);
        return true;
    }

    /**
     * 计算满量打折最优规则
     * @param totalNum
     * @param activityRuleList //该活动规则skuActivityRuleList数据，已经按照优惠折扣从大到小排序了
     */
    private ActivityRule computeFullDiscount(Integer totalNum, BigDecimal totalAmount, List<ActivityRule> activityRuleList) {
        ActivityRule optimalActivityRule = null;
        //该活动规则skuActivityRuleList数据，已经按照优惠金额从大到小排序了
        for (ActivityRule activityRule : activityRuleList) {
            //如果订单项购买个数大于等于满减件数，则优化打折
            if (totalNum.intValue() >= activityRule.getConditionNum()) {
                BigDecimal skuDiscountTotalAmount = totalAmount.multiply(activityRule.getBenefitDiscount().divide(new BigDecimal("10")));
                BigDecimal reduceAmount = totalAmount.subtract(skuDiscountTotalAmount);
                activityRule.setReduceAmount(reduceAmount);
                optimalActivityRule = activityRule;
                break;
            }
        }
        if(null == optimalActivityRule) {
            //如果没有满足条件的取最小满足条件的一项
            optimalActivityRule = activityRuleList.get(activityRuleList.size()-1);
            optimalActivityRule.setReduceAmount(new BigDecimal("0"));
            optimalActivityRule.setSelectType(1);

            StringBuffer ruleDesc = new StringBuffer()
                    .append("满")
                    .append(optimalActivityRule.getConditionNum())
                    .append("元打")
                    .append(optimalActivityRule.getBenefitDiscount())
                    .append("折，还差")
                    .append(totalNum-optimalActivityRule.getConditionNum())
                    .append("件");
            optimalActivityRule.setRuleDesc(ruleDesc.toString());
        } else {
            StringBuffer ruleDesc = new StringBuffer()
                    .append("满")
                    .append(optimalActivityRule.getConditionNum())
                    .append("元打")
                    .append(optimalActivityRule.getBenefitDiscount())
                    .append("折，已减")
                    .append(optimalActivityRule.getReduceAmount())
                    .append("元");
            optimalActivityRule.setRuleDesc(ruleDesc.toString());
            optimalActivityRule.setSelectType(2);
        }
        return optimalActivityRule;
    }

    /**
     * 计算满减最优规则
     * @param totalAmount
     * @param activityRuleList //该活动规则skuActivityRuleList数据，已经按照优惠金额从大到小排序了
     */
    private ActivityRule computeFullReduction(BigDecimal totalAmount, List<ActivityRule> activityRuleList) {
        ActivityRule optimalActivityRule = null;
        //该活动规则skuActivityRuleList数据，已经按照优惠金额从大到小排序了
        for (ActivityRule activityRule : activityRuleList) {
            //如果订单项金额大于等于满减金额，则优惠金额
            if (totalAmount.compareTo(activityRule.getConditionAmount()) > -1) {
                //优惠后减少金额
                activityRule.setReduceAmount(activityRule.getBenefitAmount());
                optimalActivityRule = activityRule;
                break;
            }
        }
        if(null == optimalActivityRule) {
            //如果没有满足条件的取最小满足条件的一项
            optimalActivityRule = activityRuleList.get(activityRuleList.size()-1);
            optimalActivityRule.setReduceAmount(new BigDecimal("0"));
            optimalActivityRule.setSelectType(1);

            StringBuffer ruleDesc = new StringBuffer()
                    .append("满")
                    .append(optimalActivityRule.getConditionAmount())
                    .append("元减")
                    .append(optimalActivityRule.getBenefitAmount())
                    .append("元，还差")
                    .append(totalAmount.subtract(optimalActivityRule.getConditionAmount()))
                    .append("元");
            optimalActivityRule.setRuleDesc(ruleDesc.toString());
        } else {
            StringBuffer ruleDesc = new StringBuffer()
                    .append("满")
                    .append(optimalActivityRule.getConditionAmount())
                    .append("元减")
                    .append(optimalActivityRule.getBenefitAmount())
                    .append("元，已减")
                    .append(optimalActivityRule.getReduceAmount())
                    .append("元");
            optimalActivityRule.setRuleDesc(ruleDesc.toString());
            optimalActivityRule.setSelectType(2);
        }
        return optimalActivityRule;
    }

    private BigDecimal computeTotalAmount(List<CartInfo> cartInfoList) {
        BigDecimal total = new BigDecimal("0");
        for (CartInfo cartInfo : cartInfoList) {
            //是否选中
            if(cartInfo.getIsChecked().intValue() == 1) {
                BigDecimal itemTotal = cartInfo.getCartPrice().multiply(new BigDecimal(cartInfo.getSkuNum()));
                total = total.add(itemTotal);
            }
        }
        return total;
    }

    private int computeCartNum(List<CartInfo> cartInfoList) {
        int total = 0;
        for (CartInfo cartInfo : cartInfoList) {
            //是否选中
            if(cartInfo.getIsChecked().intValue() == 1) {
                total += cartInfo.getSkuNum();
            }
        }
        return total;
    }
    //构造规则名称的方法
    private String getRuleDesc(ActivityRule activityRule) {
        ActivityType activityType = activityRule.getActivityType();
        StringBuffer ruleDesc = new StringBuffer();
        if (activityType == ActivityType.FULL_REDUCTION) {
            ruleDesc
                    .append("满")
                    .append(activityRule.getConditionAmount())
                    .append("元减")
                    .append(activityRule.getBenefitAmount())
                    .append("元");
        } else {
            ruleDesc
                    .append("满")
                    .append(activityRule.getConditionNum())
                    .append("元打")
                    .append(activityRule.getBenefitDiscount())
                    .append("折");
        }
        return ruleDesc.toString();
    }
}
