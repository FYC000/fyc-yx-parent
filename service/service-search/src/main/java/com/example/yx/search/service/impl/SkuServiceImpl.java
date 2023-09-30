package com.example.yx.search.service.impl;

import com.example.yx.activity.client.ActivityFeignClient;
import com.example.yx.client.product.ProductFeignClient;
import com.example.yx.common.auth.AuthContextHolder;
import com.example.yx.enums.SkuType;
import com.example.yx.model.product.Category;
import com.example.yx.model.product.SkuInfo;
import com.example.yx.model.search.SkuEs;
import com.example.yx.search.repository.SkuRepository;
import com.example.yx.search.service.SkuService;
import com.example.yx.vo.search.SkuEsQueryVo;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SkuServiceImpl implements SkuService {
    @Autowired
    private SkuRepository skuRepository;
    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private ActivityFeignClient activityFeignClient;
    @Autowired
    private RedisTemplate redisTemplate;
    @Override
    public void upperSku(Long skuId) {
        //先获取sku商品信息
        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        //然后获取category信息
        Category category = productFeignClient.getCategory(skuInfo.getCategoryId());
        //封装skuEs
        SkuEs skuEs = new SkuEs();
        if (category != null) {
            skuEs.setCategoryId(category.getId());
            skuEs.setCategoryName(category.getName());
        }
        if(null == skuInfo) return;
        skuEs.setId(skuInfo.getId());
        skuEs.setKeyword(skuInfo.getSkuName()+","+skuEs.getCategoryName());
        skuEs.setWareId(skuInfo.getWareId());
        skuEs.setIsNewPerson(skuInfo.getIsNewPerson());
        skuEs.setImgUrl(skuInfo.getImgUrl());
        skuEs.setTitle(skuInfo.getSkuName());
        if(skuInfo.getSkuType() == SkuType.COMMON.getCode()) {//当成普通商品
            skuEs.setSkuType(0);
            skuEs.setPrice(skuInfo.getPrice().doubleValue());
            skuEs.setStock(skuInfo.getStock());
            skuEs.setSale(skuInfo.getSale());
            skuEs.setPerLimit(skuInfo.getPerLimit());
        }
        //调用方法添加skuES
        skuRepository.save(skuEs);
    }

    @Override
    public void lowerSku(Long skuId) {
        skuRepository.deleteById(skuId);
    }

    @Override
    public List<SkuEs> findHotSkuList() {
        //SpringData规范->find,get,read开头
        //关联条件关键字 findByOrderByHotScoreDesc()
        //使用分页语句,从0开始
        PageRequest page = PageRequest.of(0, 10);
        Page<SkuEs> pageModel=skuRepository.findByOrderByHotScoreDesc(page);
        List<SkuEs> content = pageModel.getContent();
        return content;
    }

    @Override
    public Page<SkuEs> search(Pageable pageable, SkuEsQueryVo searchParamVo) {
        //1.向SkuEsQueryVo设置wareId,当前登录用户的仓库id
        searchParamVo.setWareId(AuthContextHolder.getWareId());
        //2.调用SkuRepository方法，根据springData命名规则定义方法，进行条件查询
        Page<SkuEs>pageModel=null;
        String keyword = searchParamVo.getKeyword();
        if(keyword==""){
            //判断keyword是否为空，若为空，则根据仓库id+分类id查询
            pageModel=skuRepository.findByCategoryIdAndWareId(searchParamVo.getCategoryId(),searchParamVo.getWareId(),pageable);
        }else{
            //若不为空，则根据keyword+仓库id查询
            pageModel=skuRepository.findByKeywordAndWareId(searchParamVo.getKeyword(),searchParamVo.getWareId(),pageable);
        }
        //3.查询商品参加优惠互动弄规则
        List<SkuEs> skuEsList = pageModel.getContent();
        if(!CollectionUtils.isEmpty(skuEsList)){
            List<Long> skuIdList = skuEsList.stream().map(skuEs -> skuEs.getId()).collect(Collectors.toList());
            //远程调用service-activity模块接口得到各个上架商品的优惠规则描述
            Map<Long, List<String>> map =activityFeignClient.findActivity(skuIdList);
            for (SkuEs skuEs : skuEsList) {
                skuEs.setRuleList(map.get(skuEs.getId()));
            }
        }
        return pageModel;
    }

    @Override
    public void incrHotScore(Long skuId) {
        //由于es索引、查询很方便，然后操作数据较为麻烦，所以每次更新数据，花费更大的代价，所以使用redis来保存数据
        //定义key
        String key="hotScore";
        Double aDouble = redisTemplate.opsForZSet().incrementScore(key, "skuId:" + skuId, 1);
        if(aDouble%10==0){
            //更新es
            Optional<SkuEs> optional = skuRepository.findById(skuId);
            SkuEs skuEs = optional.get();
            skuEs.setHotScore(Math.round(aDouble));
            skuRepository.save(skuEs);
        }
    }
}
