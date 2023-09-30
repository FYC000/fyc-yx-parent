package com.example.yx.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.yx.model.order.OrderInfo;
import com.example.yx.vo.order.OrderConfirmVo;
import com.example.yx.vo.order.OrderSubmitVo;
import com.example.yx.vo.order.OrderUserQueryVo;

public interface OrderInfoService extends IService<OrderInfo> {

    //确认订单
    OrderConfirmVo confirmOrder();

    //生成订单
    Long submitOrder(OrderSubmitVo orderParamVo);

    //订单详情
    OrderInfo getOrderInfoById(Long orderId);
    //根据OrderNo查询订单信息
    OrderInfo getOrderInfoByOrderNo(String orderNo);

    void orderPay(String orderNo);

    IPage<OrderInfo> findUserOrderPage(Page<OrderInfo> pageParam, OrderUserQueryVo orderUserQueryVo);
}
