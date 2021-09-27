package com.szeastroc.icebox.newprocess.consumer;

import com.alibaba.fastjson.JSON;
import com.szeastroc.common.entity.icebox.vo.IceBoxRequest;
import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.icebox.newprocess.service.DisplayShelfBackApplyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ShelfBackApplyExamineConsumer {

    @Autowired
    private DisplayShelfBackApplyService shelfBackApplyService;

    @RabbitListener(queues = MqConstant.SHELF_RETURN_APPLY_Q, containerFactory = "shelfBackApplyContainer")
    public void task(IceBoxRequest iceBoxRequest) {
        log.info("货架退还审批触发事件,传递的参数-->[{}]", JSON.toJSONString(iceBoxRequest));
        try {
            shelfBackApplyService.updateBackStatus(iceBoxRequest);
        } catch (Exception e) {
            log.info("货架退还审批触发事件,传递的参数-->[{}],报错信息为-->[{}]", JSON.toJSONString(iceBoxRequest), e.getMessage());
        }
    }
}
