package com.szeastroc.icebox.rabbitMQ;

import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.icebox.vo.DataPack;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @Author xiao
 * @Date create in 2020/6/12 17:29
 * @Description:
 */
@Component
@RequiredArgsConstructor(onConstructor = @_(@Autowired))
public class DirectProducer extends Producer {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void sendMsg(String routingKey, DataPack dataPack) {
        rabbitTemplate.convertAndSend(MqConstant.directExchange, routingKey, dataPack);
    }
}
