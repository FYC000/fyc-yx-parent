package com.example.yx.mq.constant;

public class MqConst {
    //消息补偿
    public static final String MQ_KEY_PREFIX = "yx.mq:list";
    public static final int RETRY_COUNT = 3;

    //商品上下架
    public static final String EXCHANGE_GOODS_DIRECT = "yx.goods.direct";
    public static final String ROUTING_GOODS_UPPER = "yx.goods.upper";
    public static final String ROUTING_GOODS_LOWER = "yx.goods.lower";
    //队列
    public static final String QUEUE_GOODS_UPPER  = "yx.goods.upper";
    public static final String QUEUE_GOODS_LOWER  = "yx.goods.lower";

    //团长上下线
    public static final String EXCHANGE_LEADER_DIRECT = "yx.leader.direct";
    public static final String ROUTING_LEADER_UPPER = "yx.leader.upper";
    public static final String ROUTING_LEADER_LOWER = "yx.leader.lower";
    //队列
    public static final String QUEUE_LEADER_UPPER  = "yx.leader.upper";
    public static final String QUEUE_LEADER_LOWER  = "yx.leader.lower";

    //订单
    public static final String EXCHANGE_ORDER_DIRECT = "yx.order.direct";
    public static final String ROUTING_ROLLBACK_STOCK = "yx.rollback.stock";
    public static final String ROUTING_MINUS_STOCK = "yx.minus.stock";

    public static final String ROUTING_DELETE_CART = "yx.delete.cart";
    //解锁普通商品库存
    public static final String QUEUE_ROLLBACK_STOCK = "yx.rollback.stock";
    public static final String QUEUE_SECKILL_ROLLBACK_STOCK = "yx.seckill.rollback.stock";
    public static final String QUEUE_MINUS_STOCK = "yx.minus.stock";
    public static final String QUEUE_DELETE_CART = "yx.delete.cart";

    //支付
    public static final String EXCHANGE_PAY_DIRECT = "yx.pay.direct";
    public static final String ROUTING_PAY_SUCCESS = "yx.pay.success";
    public static final String QUEUE_ORDER_PAY  = "yx.order.pay";
    public static final String QUEUE_LEADER_BILL  = "yx.leader.bill";

    //取消订单
    public static final String EXCHANGE_CANCEL_ORDER_DIRECT = "yx.cancel.order.direct";
    public static final String ROUTING_CANCEL_ORDER = "yx.cancel.order";
    //延迟取消订单队列
    public static final String QUEUE_CANCEL_ORDER  = "yx.cancel.order";

    //定时任务
    public static final String EXCHANGE_DIRECT_TASK = "yx.exchange.direct.task";
    public static final String ROUTING_TASK_23 = "yx.task.23";
    //队列
    public static final String QUEUE_TASK_23  = "yx.queue.task.23";
}