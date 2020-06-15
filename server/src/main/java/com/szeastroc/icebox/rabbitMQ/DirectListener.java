package com.szeastroc.icebox.rabbitMQ;

import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.icebox.newprocess.service.IceBoxService;
import com.szeastroc.icebox.newprocess.vo.request.IceBoxPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @Author xiao
 * @Date create in 2020/6/12 17:18
 * @Description:
 */
@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @_(@Autowired))
public class DirectListener {

    private final IceBoxService iceBoxService;

    //    @RabbitHandler
    @RabbitListener(queues = MqConstant.directQueue)
    public void listener(DataPack dataPack) throws Exception {

        String methodName = dataPack.getMethodName();  // 接口名称
        if (StringUtils.isBlank(methodName)) {
            return;
        }
        log.info("methodName-->{}", methodName);
        if (methodName.equals(MethodNameOfMQ.EXPORT_EXCEL_METHOD)) { // 冰柜导出
            IceBoxPage iceBoxPage = (IceBoxPage) dataPack.getObj(); // 数据
            iceBoxService.exportExcel(iceBoxPage);
        } else {
            // log.info("暂无对应的方法可以调用...");

        }
    }
}
