package com.example.yx.activity.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.yx.activity.mapper.CouponInfoMapper;
import com.example.yx.activity.mapper.CouponRangeMapper;
import com.example.yx.activity.service.CouponInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.yx.client.product.ProductFeignClient;
import com.example.yx.enums.CouponRangeType;
import com.example.yx.model.activity.CouponInfo;
import com.example.yx.model.activity.CouponRange;
import com.example.yx.model.order.CartInfo;
import com.example.yx.model.product.Category;
import com.example.yx.model.product.SkuInfo;
import com.example.yx.vo.activity.CouponRuleVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CouponInfoServiceImpl extends ServiceImpl<CouponInfoMapper, CouponInfo> implements CouponInfoService {
    @Autowired
    private CouponRangeMapper couponRangeMapper;
    @Autowired
    private ProductFeignClient productFeignClient;
    //优惠卷分页查询
    @Override
    public IPage selectPage(Page<CouponInfo> pageParam) {
        //  构造排序条件
        QueryWrapper<CouponInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("id");
        IPage<CouponInfo> page = baseMapper.selectPage(pageParam, queryWrapper);
        page.getRecords().stream().forEach(item -> {
            item.setCouponTypeString(item.getCouponType().getComment());
            if(null != item.getRangeType()) {
                item.setRangeTypeString(item.getRangeType().getComment());
            }
        });
        //  返回数据集合
        return page;
    }

    //根据id获取优惠券
    @Override
    public CouponInfo getCouponInfo(String id) {
        CouponInfo couponInfo = this.getById(id);
        couponInfo.setCouponTypeString(couponInfo.getCouponType().getComment());
        if(null != couponInfo.getRangeType()) {
            couponInfo.setRangeTypeString(couponInfo.getRangeType().getComment());
        }
        return couponInfo;
    }

    @Override
    public Object findCouponRuleList(Long id) {
        HashMap<String, Object> map = new HashMap<>();
        //1.首先获得优惠券信息
        CouponInfo couponInfo = baseMapper.selectById(id);
        //2.获取优惠券类型信息
            //若优惠券类型是SKU,则range_id为sku_id，若优惠券类型是category，则range_id为category_id
        List<CouponRange> couponRanges = couponRangeMapper.selectList(new LambdaQueryWrapper<CouponRange>().eq(CouponRange::getCouponId, id));
        //获取range_id列表
        List<Long> CouponRangeIds = couponRanges.stream().map(item -> item.getRangeId()).collect(Collectors.toList());
        //3.获取sku信息或者category信息
        if(couponInfo.getRangeType()== CouponRangeType.SKU){
            List<SkuInfo> skuInfoList = productFeignClient.findSkuInfoList(CouponRangeIds);
            map.put("skuInfoList", skuInfoList);
        }else if(couponInfo.getRangeType()== CouponRangeType.CATEGORY){
            List<Category> categoryList = productFeignClient.findCategoryList(CouponRangeIds);
            map.put("categoryList", categoryList);
        }
        return map;
    }

    @Override
    public void saveCouponRule(CouponRuleVo couponRuleVo) {
        Long couponId = couponRuleVo.getCouponId();
        //先删除优惠券使用范围
        couponRangeMapper.delete(new LambdaQueryWrapper<CouponRange>().eq(CouponRange::getCouponId,couponId));
        //然后更新优惠券信息
        CouponInfo couponInfo = this.getById(couponId);
        couponInfo.setRangeType(couponRuleVo.getRangeType());
        couponInfo.setConditionAmount(couponRuleVo.getConditionAmount());
        couponInfo.setAmount(couponRuleVo.getAmount());
        couponInfo.setConditionAmount(couponRuleVo.getConditionAmount());
        couponInfo.setRangeDesc(couponRuleVo.getRangeDesc());
        baseMapper.updateById(couponInfo);
        //插入优惠券数据
        List<CouponRange> couponRangeList = couponRuleVo.getCouponRangeList();
        for (CouponRange couponRange : couponRangeList) {
            couponRange.setCouponId(couponId);
            couponRangeMapper.insert(couponRange);
        }
    }

    @Override
    public List<CouponInfo> findCouponInfo(Long skuId, Long userId) {
        //1.通过skuId获取SkuInfo
        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        if(skuInfo==null)return new ArrayList<CouponInfo>();
        //2.通过skuId、userId、categoryId获取优惠卷信息
        return baseMapper.selectCouponInfoList(skuId,skuInfo.getCategoryId(),userId);

    }

    @Override
    public List<CouponInfo> findCartCouponInfo(List<CartInfo> cartInfoList, Long userId) {
        //1.获取全部优惠券
        List<CouponInfo>userAllCouponInfoList=baseMapper.selectCartCouponInfoList(userId);
        if(CollectionUtils.isEmpty(userAllCouponInfoList)) return null;
        //2.获取优惠券id列表
        List<Long> couponIdList = userAllCouponInfoList.stream().map(couponInfo -> couponInfo.getId()).collect(Collectors.toList());
        //3.获取优惠券范围列表
        List<CouponRange> couponRangeList = couponRangeMapper.selectList(new LambdaQueryWrapper<CouponRange>().in(CouponRange::getCouponId, couponIdList));
        //4.获取优惠券id对应的skuId列表的map
        Map<Long,List<Long>> couponIdToSkuIdMap=this.findCouponIdToSkuIdMap(cartInfoList,couponRangeList);
        //5.遍历全部优惠券集合，判断优惠券类型
        BigDecimal reduceAmount=new BigDecimal(0);
        //记录最优优惠券
        CouponInfo optimalCouponInfo = null;
        for (CouponInfo couponInfo : userAllCouponInfoList) {
            //全场通用
            if(couponInfo.getRangeType()==CouponRangeType.ALL){
                BigDecimal totalAmount = computeTotalAmount(cartInfoList);
                if(totalAmount.subtract(couponInfo.getConditionAmount()).intValue()>=0){
                    couponInfo.setIsSelect(1);
                }
            }else{
                List<Long> currentSkuIdList = couponIdToSkuIdMap.get(couponInfo.getId());
                List<CartInfo> currentCartInfoList = cartInfoList.stream().filter(cartInfo -> currentSkuIdList.contains(cartInfo.getSkuId())).collect(Collectors.toList());
                BigDecimal totalAmount = computeTotalAmount(currentCartInfoList);
                if(totalAmount.subtract(couponInfo.getConditionAmount()).doubleValue() >= 0){
                    couponInfo.setIsSelect(1);
                }
            }
            if (couponInfo.getIsSelect().intValue() == 1 && couponInfo.getAmount().subtract(reduceAmount).doubleValue() > 0) {
                reduceAmount = couponInfo.getAmount();
                optimalCouponInfo = couponInfo;
            }
        }
        //6.返回List<CouponInfo>
        if(null != optimalCouponInfo) {
            optimalCouponInfo.setIsOptimal(1);
        }
        return userAllCouponInfoList;
    }

    @Override
    public Map<Long, List<Long>> findCouponIdToSkuIdMap(List<CartInfo> cartInfoList, List<CouponRange> couponRangeList) {
        Map<Long,List<Long>>couponIdToSkuIdMap=new HashMap<>();
        //获取couponRangeList数据进行处理后，以优惠券id分组
        Map<Long, List<CouponRange>> couponRangeToRangeListMap = couponRangeList.stream().collect(Collectors.groupingBy(couponRange -> couponRange.getCouponId()));
        //遍历map集合
        Iterator<Map.Entry<Long, List<CouponRange>>> iterator = couponRangeToRangeListMap.entrySet().iterator();
        while(iterator.hasNext()){
            Map.Entry<Long, List<CouponRange>> next = iterator.next();
            Long couponId = next.getKey();
            List<CouponRange> couponRanges = next.getValue();
            Set<Long>skuIdSet=new HashSet<>();
            for (CartInfo cartInfo : cartInfoList) {
                for (CouponRange couponRange : couponRanges) {
                    if(couponRange.getRangeType()==CouponRangeType.SKU&&couponRange.getRangeId()==cartInfo.getSkuId()){
                        skuIdSet.add(cartInfo.getSkuId());
                    }else if(couponRange.getRangeType()==CouponRangeType.CATEGORY&&couponRange.getRangeId()==cartInfo.getCategoryId()) {
                        skuIdSet.add(cartInfo.getSkuId());
                    }
                }
            }
            couponIdToSkuIdMap.put(couponId,new ArrayList<>(skuIdSet));
        }
        return couponIdToSkuIdMap;
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
}
