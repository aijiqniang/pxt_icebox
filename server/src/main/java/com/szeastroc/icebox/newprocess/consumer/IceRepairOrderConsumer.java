package com.szeastroc.icebox.newprocess.consumer;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.szeastroc.common.feign.visit.FeignExportRecordsClient;
import com.szeastroc.common.utils.ImageUploadUtil;
import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.icebox.enums.IceRepairStatusEnum;
import com.szeastroc.icebox.newprocess.consumer.common.IceRepairOrderMsg;
import com.szeastroc.icebox.newprocess.consumer.utils.PoiUtil;
import com.szeastroc.icebox.newprocess.entity.IceRepairOrder;
import com.szeastroc.icebox.newprocess.service.IceRepairOrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class IceRepairOrderConsumer {

    @Autowired
    private IceRepairOrderService iceRepairOrderService;
    @Autowired
    private ImageUploadUtil imageUploadUtil;
    @Autowired
    private FeignExportRecordsClient feignExportRecordsClient;

    @RabbitListener(queues = MqConstant.iceRepairOrderQueue)
    public void task(IceRepairOrderMsg msg) throws Exception {
        selectReport(msg);
    }

    private void selectReport(IceRepairOrderMsg msg) throws Exception {
        LambdaQueryWrapper<IceRepairOrder> wrapper = iceRepairOrderService.fillWrapper(msg);
        Integer count = iceRepairOrderService.selectByExportCount(wrapper);
        // 列
        String[] columnName = {"保修工单号","事业部", "大区", "服务处", "用户姓名","门店名称","手机","行政区域","地址",
                "资产编号","产品型号","问题描述","备注","服务商代码","受理服务商","受理时间","故障原因描述","维修措施","实际服务类型","实际服务方式","中间结果描述","反馈备注","工单状态","完成状态","服务完成时间","工程师"};
        // 先写入本地文件
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String tmpPath = String.format("%s.xlsx", System.currentTimeMillis());
        PoiUtil.exportReportExcelToLocalPath(count, columnName, tmpPath, imageUploadUtil, feignExportRecordsClient, msg.getRecordsId(),
                (wb, eachSheet, startRowCount, endRowCount, currentPage, pageSize) -> {
                    Page<IceRepairOrder> page = new Page<>();
                    page.setCurrent(currentPage);
                    page.setSize(pageSize);
                    IPage<IceRepairOrder> reportPage = iceRepairOrderService.page(page, wrapper);
                    List<IceRepairOrder> reports = reportPage.getRecords();
                    if (CollectionUtil.isNotEmpty(reports)) {
                        for (int i = startRowCount; i <= endRowCount; i++) {
                            SXSSFRow eachDataRow = eachSheet.createRow(i);
                            if ((i - startRowCount) < reports.size()) {
                                IceRepairOrder report = reports.get(i - startRowCount);
                                eachDataRow.createCell(0).setCellValue(report.getOrderNumber());
                                eachDataRow.createCell(1).setCellValue(report.getBusinessDeptName());
                                eachDataRow.createCell(2).setCellValue(report.getRegionDeptName());
                                eachDataRow.createCell(3).setCellValue(report.getServiceDeptName());
                                eachDataRow.createCell(4).setCellValue(report.getLinkMan());
                                eachDataRow.createCell(5).setCellValue(report.getCustomerName());
                                eachDataRow.createCell(6).setCellValue(report.getLinkMobile());
                                eachDataRow.createCell(7).setCellValue(report.getProvince()+report.getCity()+report.getArea());
                                eachDataRow.createCell(8).setCellValue(report.getCustomerAddress());
                                eachDataRow.createCell(9).setCellValue(report.getAssetId());
                                eachDataRow.createCell(10).setCellValue(report.getModelName());
                                eachDataRow.createCell(11).setCellValue(report.getDescription());
                                eachDataRow.createCell(12).setCellValue(StringUtils.isNotBlank(report.getRemark())?"null".equals(report.getRemark().trim())?"":report.getRemark().trim():"");
                                eachDataRow.createCell(13).setCellValue(report.getServiceProviderCode());
                                eachDataRow.createCell(14).setCellValue(report.getServiceProviderName());
                                eachDataRow.createCell(15).setCellValue(Objects.nonNull(report.getAcceptTime())?dateFormat.format(report.getAcceptTime()):"");
                                eachDataRow.createCell(16).setCellValue(report.getCause());
                                eachDataRow.createCell(17).setCellValue(report.getRepairMethod());
                                eachDataRow.createCell(18).setCellValue(report.getFactServiceType());
                                eachDataRow.createCell(19).setCellValue(report.getFactServiceMethod());
                                eachDataRow.createCell(20).setCellValue(report.getResult());
                                eachDataRow.createCell(21).setCellValue(StringUtils.isNotBlank(report.getFeedback())?"null".equals(report.getFeedback().trim())?"":report.getFeedback().trim():"");
                                eachDataRow.createCell(22).setCellValue(IceRepairStatusEnum.getDesc(report.getStatus()));
                                eachDataRow.createCell(23).setCellValue(report.getFinishStatus());
                                eachDataRow.createCell(24).setCellValue(Objects.nonNull(report.getFinishTime())?dateFormat.format(report.getFinishTime()):"");
                                eachDataRow.createCell(25).setCellValue(report.getEngineer());
                            }
                        }
                    }
                });

    }

}