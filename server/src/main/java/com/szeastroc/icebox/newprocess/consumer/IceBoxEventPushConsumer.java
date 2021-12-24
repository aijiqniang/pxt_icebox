package com.szeastroc.icebox.newprocess.consumer;

import com.alibaba.fastjson.JSONObject;
import com.szeastroc.common.utils.ExecutorServiceFactory;
import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.icebox.oldprocess.service.IceEventRecordService;
import com.szeastroc.icebox.oldprocess.vo.HisenseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;


@Slf4j
@Component
public class IceBoxEventPushConsumer {
    @Autowired
    private IceEventRecordService iceEventRecordService;

    @RabbitListener(queues = MqConstant.ICEBOX_EVENT_PUSH_QUEUE, containerFactory = "iceboxEventPushContainer")
    public void task(HisenseDTO hisenseDTO) {

        log.info("冰柜推送事件触发，消息{}", JSONObject.toJSONString(hisenseDTO));
        try{
            CompletableFuture.runAsync(()->iceEventRecordService.eventPushConsumer(hisenseDTO), ExecutorServiceFactory.getInstance());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
