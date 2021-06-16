package com.szeastroc.icebox.newprocess.consumer;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.szeastroc.common.feign.visit.FeignExportRecordsClient;
import com.szeastroc.common.utils.ImageUploadUtil;
import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.icebox.newprocess.consumer.common.ShelfPutReportMsg;
import com.szeastroc.icebox.newprocess.consumer.utils.PoiUtil;
import com.szeastroc.icebox.newprocess.entity.DisplayShelfPutReport;
import com.szeastroc.icebox.newprocess.enums.PutStatus;
import com.szeastroc.icebox.newprocess.enums.SupplierTypeEnum;
import com.szeastroc.icebox.newprocess.service.DisplayShelfPutReportService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class ShelfPutReportConsumer {

    @Autowired
    private DisplayShelfPutReportService shelfPutReportService;
    @Autowired
    private ImageUploadUtil imageUploadUtil;
    @Autowired
    private FeignExportRecordsClient feignExportRecordsClient;

    @RabbitListener(queues = MqConstant.shelfPutReportQueue)
    public void task(ShelfPutReportMsg reportMsg) throws Exception {
        selectReport(reportMsg);
    }

    private void selectReport(ShelfPutReportMsg reportMsg) throws Exception {
        LambdaQueryWrapper<DisplayShelfPutReport> wrapper = shelfPutReportService.fillWrapper(reportMsg);
        Integer count = shelfPutReportService.selectByExportCount(wrapper); // 得到当前条件下的总量
        // 列
        String[] columnName = {"事业部", "大区", "服务处", "流程编号", "所属经销商编号", "所属经销商名称", "投放客户编号", "投放客户名称", "投放客户类型","客户联系人","联系人电话","省","市","区县",
                "投放客户地址", "投放日期",  "审核人员", "审核人职务", "审核日期","业务员","业务员电话", "投放状态","审批备注","商户编号"};
        // 先写入本地文件
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String tmpPath = String.format("%s.xlsx", System.currentTimeMillis());
        PoiUtil.exportReportExcelToLocalPath(count, columnName, tmpPath, imageUploadUtil, feignExportRecordsClient, reportMsg.getRecordsId(),
                (wb, eachSheet, startRowCount, endRowCount, currentPage, pageSize) -> {
                    Page<DisplayShelfPutReport> page = new Page<>();
                    page.setCurrent(currentPage);
                    page.setSize(pageSize);
                    IPage<DisplayShelfPutReport> reportPage = shelfPutReportService.page(page, wrapper);
                    List<DisplayShelfPutReport> reports = reportPage.getRecords();
                    if (CollectionUtil.isNotEmpty(reports)) {
                        for (int i = startRowCount; i <= endRowCount; i++) {
                            SXSSFRow eachDataRow = eachSheet.createRow(i);
                            if ((i - startRowCount) < reports.size()) {
                                DisplayShelfPutReport report = reports.get(i - startRowCount);
                                eachDataRow.createCell(0).setCellValue(report.getBusinessDeptName());
                                eachDataRow.createCell(1).setCellValue(report.getRegionDeptName());
                                eachDataRow.createCell(2).setCellValue(report.getServiceDeptName());
                                eachDataRow.createCell(3).setCellValue(report.getApplyNumber());
                                eachDataRow.createCell(4).setCellValue(report.getSupplierNumber());
                                eachDataRow.createCell(5).setCellValue(report.getSupplierName());
                                eachDataRow.createCell(6).setCellValue(report.getPutCustomerNumber());
                                eachDataRow.createCell(7).setCellValue(report.getPutCustomerName());
                                eachDataRow.createCell(8).setCellValue(SupplierTypeEnum.getDesc(report.getPutCustomerType()));
                                eachDataRow.createCell(9).setCellValue(report.getLinkmanName());
                                eachDataRow.createCell(10).setCellValue(report.getLinkmanMobile());
                                eachDataRow.createCell(11).setCellValue(report.getProvinceName());
                                eachDataRow.createCell(12).setCellValue(report.getCityName());
                                eachDataRow.createCell(13).setCellValue(report.getDistrictName());
                                eachDataRow.createCell(14).setCellValue(report.getCustomerAddress());
                                eachDataRow.createCell(15).setCellValue(Objects.nonNull(report.getCreateTime())?dateFormat.format(report.getCreateTime()):"");
                                eachDataRow.createCell(16).setCellValue(report.getExamineUserName());
                                eachDataRow.createCell(17).setCellValue(report.getExamineUserPosion());
                                eachDataRow.createCell(18).setCellValue(Objects.nonNull(report.getExamineTime())?dateFormat.format(report.getExamineTime()):"");
                                eachDataRow.createCell(19).setCellValue(report.getSubmitterName());
                                eachDataRow.createCell(20).setCellValue(report.getSubmitterMobile());
                                eachDataRow.createCell(21).setCellValue(PutStatus.convertEnum(report.getPutStatus()).getDesc());
                                eachDataRow.createCell(22).setCellValue(report.getExamineRemark());
                                eachDataRow.createCell(23).setCellValue(report.getShNumber());

                            }
                        }
                    }
                });
    }

}