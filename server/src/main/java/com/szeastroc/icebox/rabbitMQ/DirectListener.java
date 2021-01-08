package com.szeastroc.icebox.rabbitMQ;

import com.alibaba.fastjson.JSON;
import com.szeastroc.common.entity.visit.ExportRecordsVo;
import com.szeastroc.common.feign.visit.FeignExportRecordsClient;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.icebox.newprocess.service.IceBackOrderService;
import com.szeastroc.icebox.newprocess.service.IceBoxChangeHistoryService;
import com.szeastroc.icebox.newprocess.service.IceBoxService;
import com.szeastroc.icebox.newprocess.service.IceBoxTransferHistoryService;
import com.szeastroc.icebox.newprocess.vo.request.IceBoxPage;
import com.szeastroc.icebox.newprocess.vo.request.IceTransferRecordPage;
import com.szeastroc.icebox.oldprocess.vo.query.IceDepositPage;
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
    private final IceBackOrderService iceBackOrderService;
    private final IceBoxTransferHistoryService iceBoxTransferHistoryService;
    private final FeignExportRecordsClient feignExportRecordsClient;
    private final IceBoxChangeHistoryService iceBoxChangeHistoryService;


    @RabbitListener(queues = MqConstant.directQueue, containerFactory = "iceExportExcelContainer")
    public void listener(DataPack dataPack) throws Exception {

        String methodName = dataPack.getMethodName();  // 接口名称
        if (StringUtils.isBlank(methodName)) {
            return;
        }
        log.info("methodName-->{}", methodName);
       if (methodName.equals(MethodNameOfMQ.EXPORT_ICE_REFUND)) {
            IceDepositPage iceDepositPage = (IceDepositPage) dataPack.getObj();
            iceBackOrderService.exportRefundTransfer(iceDepositPage);
        } else if (methodName.equals(MethodNameOfMQ.EXPORT_ICE_TRANSFER)) {
            IceTransferRecordPage iceTransferRecordPage = (IceTransferRecordPage) dataPack.getObj();
            iceBoxTransferHistoryService.exportTransferHistory(iceTransferRecordPage);
        }
    }

    @RabbitListener(queues = MqConstant.EXPORT_EXCEL_QUEUE, containerFactory = "iceExportExcelContainer")
    public void listenerOne(Integer exportRecordId) throws Exception {
        String param = getIceBoxPage(exportRecordId);
        if (param == null) return;
        IceBoxPage iceBoxPage = JSON.parseObject(param, IceBoxPage.class);
        iceBoxPage.setExportRecordId(exportRecordId);
        iceBoxService.exportExcel(iceBoxPage);
    }

    @RabbitListener(queues = MqConstant.EXPORT_CHANGE_RECORD_QUEUE, containerFactory = "iceExportExcelContainer")
    public void listenerTwo(Integer exportRecordId) throws Exception {
        String param = getIceBoxPage(exportRecordId);
        if (param == null) return;
        IceBoxPage iceBoxPage = JSON.parseObject(param, IceBoxPage.class);
        iceBoxPage.setExportRecordId(exportRecordId);
        iceBoxChangeHistoryService.exportChangeRecord(iceBoxPage);
    }

    private String getIceBoxPage(Integer exportRecordId) {
        if (exportRecordId == null) {
            return null;
        }
        ExportRecordsVo recordsVo = FeignResponseUtil.getFeignData(feignExportRecordsClient.readId(exportRecordId));
        if (recordsVo == null) {
            return null;
        }
        String param = recordsVo.getParam();
        if (StringUtils.isBlank(param)) {
            return null;
        }
        return param;
    }
}
