package com.example.yx.activity.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.yx.model.activity.ActivityInfo;
import com.example.yx.model.activity.ActivityRule;
import com.example.yx.model.activity.ActivitySku;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityInfoMapper extends BaseMapper<ActivityInfo> {

    List<Long> selectExistSkuIdList(@Param("skuIdList") List<Long> skuIdList);

    List<ActivityRule> findActivityRule(Long skuId);

    List<ActivitySku> selectCartActivityList(List<Long> skuIdList);
}
