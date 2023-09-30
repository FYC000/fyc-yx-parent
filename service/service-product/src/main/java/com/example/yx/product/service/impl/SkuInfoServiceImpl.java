package com.example.yx.product.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.api.R;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.yx.common.config.RedissonConfig;
import com.example.yx.common.constant.RedisConst;
import com.example.yx.common.exception.yxException;
import com.example.yx.common.result.Result;
import com.example.yx.common.result.ResultCodeEnum;
import com.example.yx.model.product.SkuAttrValue;
import com.example.yx.model.product.SkuImage;
import com.example.yx.model.product.SkuInfo;
import com.example.yx.model.product.SkuPoster;
import com.example.yx.mq.constant.MqConst;
import com.example.yx.mq.service.RabbitService;
import com.example.yx.product.mapper.SkuInfoMapper;
import com.example.yx.product.service.SkuAttrValueService;
import com.example.yx.product.service.SkuImageService;
import com.example.yx.product.service.SkuInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.yx.product.service.SkuPosterService;
import com.example.yx.vo.product.SkuInfoQueryVo;
import com.example.yx.vo.product.SkuInfoVo;
import com.example.yx.vo.product.SkuStockLockVo;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.ArrayList;
import java.util.List;

@Service
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoMapper, SkuInfo> implements SkuInfoService {
    //sku海报
    @Autowired
    private SkuPosterService skuPosterService;
    //sku图片
    @Autowired
    private SkuImageService skuImageService;
    //sku平台谁信
    @Autowired
    private SkuAttrValueService skuAttrValueService;
    //mq封装的service
    @Autowired
    private RabbitService rabbitService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;

    @Override
    public IPage<SkuInfo> selectPage(Page<SkuInfo> pageParam, SkuInfoQueryVo skuInfoQueryVo) {
        //获取条件值
        String keyword = skuInfoQueryVo.getKeyword();
        String skuType = skuInfoQueryVo.getSkuType();
        Long categoryId = skuInfoQueryVo.getCategoryId();
        //封装条件
        LambdaQueryWrapper<SkuInfo> wrapper = new LambdaQueryWrapper<>();
        if(!StringUtils.isEmpty(keyword)) {
            wrapper.like(SkuInfo::getSkuName,keyword);
        }
        if(!StringUtils.isEmpty(skuType)) {
            wrapper.eq(SkuInfo::getSkuType,skuType);
        }
        if(categoryId!=null) {
            wrapper.eq(SkuInfo::getCategoryId,categoryId);
        }
        //调用方法查询
        IPage<SkuInfo> skuInfoPage = baseMapper.selectPage(pageParam, wrapper);
        return skuInfoPage;
    }

    @Override
    public void saveSkuInfo(SkuInfoVo skuInfoVo) {
        //1.添加skuInfo数据
        SkuInfo skuInfo = new SkuInfo();
        //将skuInfo中有关skuInfo的属性复制到skuInfo中
        BeanUtils.copyProperties(skuInfoVo,skuInfo);
        baseMapper.insert(skuInfo);
        //2.添加sku图片数据
        List<SkuImage> skuImagesList = skuInfoVo.getSkuImagesList();
        if(!CollectionUtils.isEmpty(skuImagesList)){
            //保存skuId
            for (SkuImage skuImage : skuImagesList) {
                skuImage.setSkuId(skuInfo.getId());
            }
            skuImageService.saveBatch(skuImagesList);
        }
        //3.添加sku海报数据
        List<SkuPoster> skuPosterList = skuInfoVo.getSkuPosterList();
        if(!CollectionUtils.isEmpty(skuPosterList)){
            //保存skuId
            for (SkuPoster skuPoster : skuPosterList) {
                skuPoster.setSkuId(skuInfo.getId());
            }
            skuPosterService.saveBatch(skuPosterList);
        }
        //4.添加平台属性数据
        List<SkuAttrValue> skuAttrValueList = skuInfoVo.getSkuAttrValueList();
        if(!CollectionUtils.isEmpty(skuAttrValueList)){
            //保存skuId
            for (SkuAttrValue skuAttrValue : skuAttrValueList) {
                skuAttrValue.setSkuId(skuInfo.getId());
            }
            skuAttrValueService.saveBatch(skuAttrValueList);
        }
    }

    @Override
    public SkuInfoVo getSkuInfoVo(Long id) {
        //获取sku商品信息
        SkuInfo skuInfo = baseMapper.selectById(id);
        SkuInfoVo skuInfoVo = new SkuInfoVo();
        //获取商品海报列表信息
        List<SkuPoster>skuPosterList=skuPosterService.getSkuPosterList(id);
        //获取商品图片列表信息
        List<SkuImage>skuImageList=skuImageService.getSkuImageList(id);
        //获取商品属性信息
        List<SkuAttrValue>skuAttrValueList=skuAttrValueService.getSkuAttrValueList(id);
        BeanUtils.copyProperties(skuInfo,skuInfoVo);
        skuInfoVo.setSkuImagesList(skuImageList);
        skuInfoVo.setSkuAttrValueList(skuAttrValueList);
        skuInfoVo.setSkuPosterList(skuPosterList);
        return skuInfoVo;
    }

    @Override
    public void updateSkuInfo(SkuInfoVo skuInfoVo) {
        //大体思路:商品海报、商品图像、商品属性先删除原本数据在添加数据，sku商品信息直接调用方法修改即可
        Long skuInfoId = skuInfoVo.getId();
        SkuInfo skuInfo = new SkuInfo();
        BeanUtils.copyProperties(skuInfoVo,skuInfo);
        baseMapper.updateById(skuInfo);
        //修改商品海报信息
        LambdaQueryWrapper<SkuPoster> wrapper1 = new LambdaQueryWrapper<SkuPoster>().eq(SkuPoster::getSkuId, skuInfoId);
        skuPosterService.remove(wrapper1);
        List<SkuPoster> skuPosterList = skuInfoVo.getSkuPosterList();
        if(!CollectionUtils.isEmpty(skuPosterList)){
            //保存skuId
            for (SkuPoster skuPoster : skuPosterList) {
                skuPoster.setSkuId(skuInfo.getId());
            }
            skuPosterService.saveBatch(skuPosterList);
        }
        //修改商品图像信息
        LambdaQueryWrapper<SkuImage> wrapper2 = new LambdaQueryWrapper<SkuImage>().eq(SkuImage::getSkuId, skuInfoId);
        skuImageService.remove(wrapper2);
        List<SkuImage> skuImagesList = skuInfoVo.getSkuImagesList();
        if(!CollectionUtils.isEmpty(skuImagesList)){
            //保存skuId
            for (SkuImage skuImage : skuImagesList) {
                skuImage.setSkuId(skuInfo.getId());
            }
            skuImageService.saveBatch(skuImagesList);
        }
        //修改商品属性信息
        LambdaQueryWrapper<SkuAttrValue> wrapper3 = new LambdaQueryWrapper<SkuAttrValue>().eq(SkuAttrValue::getSkuId, skuInfoId);
        skuAttrValueService.remove(wrapper3);
        List<SkuAttrValue> skuAttrValueList = skuInfoVo.getSkuAttrValueList();
        if(!CollectionUtils.isEmpty(skuAttrValueList)){
            //保存skuId
            for (SkuAttrValue skuAttrValue : skuAttrValueList) {
                skuAttrValue.setSkuId(skuInfo.getId());
            }
            skuAttrValueService.saveBatch(skuAttrValueList);
        }
    }

    @Override
    public void check(Long skuId, Integer status) {
        // 更改发布状态
        SkuInfo skuInfoUp = new SkuInfo();
        skuInfoUp.setId(skuId);
        skuInfoUp.setCheckStatus(status);
        baseMapper.updateById(skuInfoUp);
    }

    @Override
    public void publish(Long skuId, Integer status) {
        // 更改发布状态
        if(status == 1) {
            SkuInfo skuInfoUp = new SkuInfo();
            skuInfoUp.setId(skuId);
            skuInfoUp.setPublishStatus(1);
            baseMapper.updateById(skuInfoUp);
            //商品上架 后续会完善：发送mq消息更新es数据
            rabbitService.sendMessage(MqConst.EXCHANGE_GOODS_DIRECT,MqConst.ROUTING_GOODS_UPPER,skuId);
        } else {
            SkuInfo skuInfoUp = new SkuInfo();
            skuInfoUp.setId(skuId);
            skuInfoUp.setPublishStatus(0);
            baseMapper.updateById(skuInfoUp);
            //商品下架 后续会完善：发送mq消息更新es数据
            rabbitService.sendMessage(MqConst.EXCHANGE_GOODS_DIRECT,MqConst.ROUTING_GOODS_LOWER,skuId);
        }
    }

    @Override
    public void isNewPerson(Long skuId, Integer status) {
        SkuInfo skuInfoUp = new SkuInfo();
        skuInfoUp.setId(skuId);
        skuInfoUp.setIsNewPerson(status);
        baseMapper.updateById(skuInfoUp);
    }

    @Override
    public List<SkuInfo> findSkuInfoList(List<Long> skuIdList) {
        List<SkuInfo> skuInfos = new ArrayList<>();
        for (Long aLong : skuIdList) {
            SkuInfo skuInfo = baseMapper.selectById(aLong);
            skuInfos.add(skuInfo);
        }
        return skuInfos;
    }

    @Override
    public List<SkuInfo> findSkuInfoByKeyword(String keyword) {
        List<SkuInfo> skuInfoList = baseMapper.selectList(new LambdaQueryWrapper<SkuInfo>().like(SkuInfo::getSkuName,keyword));
        return skuInfoList;
    }

    @Override
    public List<SkuInfo> findNewPersonList() {
        //is_new_person->1,publish_status->1,只显示其中三条
        Page<SkuInfo> page = new Page<>(1, 3);
        LambdaQueryWrapper<SkuInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SkuInfo::getIsNewPerson,1);
        wrapper.eq(SkuInfo::getPublishStatus,1);
        wrapper.orderByDesc(SkuInfo::getStock);//库存排序
        Page<SkuInfo> skuInfoPage = baseMapper.selectPage(page, wrapper);
        List<SkuInfo> records = skuInfoPage.getRecords();
        return records;
    }

    //验证并锁定库存
    @Override
    public Boolean checkAndLock(List<SkuStockLockVo> skuStockLockVoList, String orderNo) {
        //1.判断skuStockLockVoList集合是否为空
        if(skuStockLockVoList==null)throw new yxException(ResultCodeEnum.DATA_ERROR);
        //2.遍历skuStockLockVoList得到每个商品，验证库存并锁定库存，具备原子性
        skuStockLockVoList.forEach(item->this.checkLock(item));
        //3.只要有一个商品锁定失败，其余锁定的商品全解锁
        boolean flag = skuStockLockVoList.stream().anyMatch(skuStockLockVo -> !skuStockLockVo.getIsLock());
        if(flag){
            //所有锁定商品全解锁
            skuStockLockVoList.stream().filter(SkuStockLockVo::getIsLock)
                    .forEach(item->baseMapper.unlockStock(item.getSkuId(),item.getSkuNum()));
            //返回失败的状态
            return false;
        }
        //4.所有商品锁定成功后，redis缓存相关数据，为了方便后面解锁和减库存
        redisTemplate.opsForValue().set(RedisConst.STOCK_INFO+orderNo,skuStockLockVoList);
        return true;
    }

    @Override
    public void minusStock(String orderNo) {
        List<SkuStockLockVo>skuStockLockVoList=(List<SkuStockLockVo>)redisTemplate.opsForValue().get(RedisConst.STOCK_INFO+orderNo);
        if (CollectionUtils.isEmpty(skuStockLockVoList)){
            return ;
        }
        // 减库存
        skuStockLockVoList.forEach(skuStockLockVo -> {
            baseMapper.minusStock(skuStockLockVo.getSkuId(), skuStockLockVo.getSkuNum());
        });

        // 解锁库存之后，删除锁定库存的缓存。以防止重复解锁库存
        this.redisTemplate.delete(RedisConst.STOCK_INFO + orderNo);
    }

    //验证库存并锁定库存
    private void checkLock(SkuStockLockVo skuStockLockVo) {
        //获取锁(公平锁)
        RLock lock = redissonClient.getFairLock(RedisConst.SKUKEY_PREFIX + skuStockLockVo.getSkuId());
        lock.lock();
        try {
            //验证库存
            SkuInfo skuInfo=baseMapper.checkStock(skuStockLockVo.getSkuId(),skuStockLockVo.getSkuNum());
            if(skuInfo==null){
                skuStockLockVo.setIsLock(false);
                return ;
            }
            //锁定库存
            Integer row=baseMapper.lockStock(skuStockLockVo.getSkuId(),skuStockLockVo.getSkuNum());
            if (row==1){
                skuStockLockVo.setIsLock(true);
            }
        }finally {
            lock.unlock();
        }
    }

}
