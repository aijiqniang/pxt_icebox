package com.szeastroc.icebox.newprocess.consumer;

import com.alibaba.fastjson.JSONObject;
import com.szeastroc.common.entity.icebox.vo.IceInspectionReportMsg;
import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.icebox.newprocess.service.IceInspectionReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

    @RabbitListener(queues = MqConstant.iceInspectionReportQueue)
    public void task(IceInspectionReportMsg reportMsg) {
        log.info("巡检报表触发变更，消息{}", JSONObject.toJSONString(reportMsg));
        iceInspectionReportService.task(reportMsg);
    }

}
