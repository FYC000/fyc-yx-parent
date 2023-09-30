package com.example.yx.cart.receiver;

import com.example.yx.cart.service.CartInfoService;
import com.example.yx.mq.constant.MqConst;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;

import java.io.IOException;

@ComponentScan
public class CartReceiver {

    @Autowired
    private CartInfoService cartInfoService;

    @RabbitListener(bindings = @QueueBinding(
            value=@Queue(value = MqConst.QUEUE_DELETE_CART,durable = "true"),
            exchange=@Exchange(value = MqConst.EXCHANGE_ORDER_DIRECT),
            key={MqConst.ROUTING_DELETE_CART}
    ))
    public void deleteCart(Long userId, Message message, Channel channel) throws IOException {
        if(userId!=null){
            cartInfoService.deleteCartChecked(userId);
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}
