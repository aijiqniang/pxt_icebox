package com.szeastroc.icebox.rabbitMQ;

import com.szeastroc.icebox.config.MqConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * @Author xiao
 * @Date create in 2020/6/12 17:18
 * @Description:
 */
@Slf4j
@Component
public class DirectListener {

    @RabbitListener(queues = MqConstant.directQueue)
    public void listener(DataPack dataPack) throws Exception {

        String methodName = dataPack.getMethodName();  // 接口名称
        Object obj = dataPack.getObj(); // 数据


        log.info("methodName-->{}", methodName);
    }
}
