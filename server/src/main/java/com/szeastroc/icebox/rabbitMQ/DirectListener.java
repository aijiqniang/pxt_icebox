package com.szeastroc.icebox.rabbitMQ;

import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.icebox.newprocess.service.IceBackOrderService;
import com.szeastroc.icebox.newprocess.service.IceBoxAssetsReportService;
import com.szeastroc.icebox.newprocess.service.IceBoxService;
import com.szeastroc.icebox.newprocess.vo.request.IceBoxPage;
import com.szeastroc.icebox.oldprocess.vo.query.IceDepositPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

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
    private final IceBackOrderService iceBackOrderService;
    private final IceBoxAssetsReportService iceBoxAssetsReportService;

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
        } else if (methodName.equals(MethodNameOfMQ.EXPORT_ICE_REFUND)){
            IceDepositPage iceDepositPage = (IceDepositPage) dataPack.getObj();

            iceBackOrderService.exportRefundTransfer(iceDepositPage);

        }
    }

    /**
     * @Date: 2020/10/19 15:25 xiao
     *  报表
     */
    @RabbitListener(queues = MqConstant.directQueueReport)
    public void listenerReport(DataPack dataPack) throws Exception {

        String methodName = dataPack.getMethodName();  // 接口名称
        if (StringUtils.isBlank(methodName)) {
            return;
        }
        log.info("methodName-->{}", methodName);
        if (methodName.equals(MethodNameOfMQ.CREATE_ICE_BOX_ASSETS_REPORT)) { // 创建或更新冰柜资产报表
            List<Map<String ,Object>> lists= (List<Map<String, Object>>) dataPack.getObj();
            iceBoxAssetsReportService.createIceBoxAssetsReport(lists);

        }
    }
}
