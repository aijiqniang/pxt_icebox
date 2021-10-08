package com.szeastroc.icebox.newprocess.consumer;

import com.alibaba.fastjson.JSON;
import com.szeastroc.common.entity.icebox.vo.IceBoxRequest;
import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.icebox.newprocess.service.DisplayShelfPutApplyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ShelfPutApplyExamineConsumer {


    @Autowired
    private DisplayShelfPutApplyService shelfPutApplyService;

    @RabbitListener(queues = MqConstant.SHELF_PUT_APPLY_Q, containerFactory = "shelfPutApplyContainer")
    public void task(IceBoxRequest iceBoxRequest) {
        log.info("货架审批触发事件,传递的参数-->[{}]", JSON.toJSONString(iceBoxRequest));
        try {
            shelfPutApplyService.updateStatus(iceBoxRequest);
        } catch (Exception e) {
            log.info("货架审批触发事件,传递的参数-->[{}],报错信息为-->[{}]", JSON.toJSONString(iceBoxRequest), e.getMessage());
        }
    }

   }