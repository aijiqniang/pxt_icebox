package com.szeastroc.icebox.newprocess.consumer;

import com.alibaba.fastjson.JSONObject;
import com.rabbitmq.client.Channel;
import com.szeastroc.common.entity.icebox.vo.IceInspectionReportMsg;
import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.icebox.newprocess.service.IceInspectionReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @ClassName: IceInspectioReportConsumer
 * @Description:
 * @Author: 陈超
 * @Date: 2020/12/22 16:01
 **/
@Slf4j
@Component
public class IceInspectionReportConsumer {
    @Autowired
    private IceInspectionReportService iceInspectionReportService;

    @RabbitListener(queues = MqConstant.iceInspectionReportQueue,ackMode = "MANUAL")
    public void task(IceInspectionReportMsg reportMsg, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,@Header(AmqpHeaders.CHANNEL) Channel channel) throws IOException {
        log.info("巡检报表触发变更，消息{}", JSONObject.toJSONString(reportMsg));
        iceInspectionReportService.task(reportMsg,channel,deliveryTag);
    }

}
