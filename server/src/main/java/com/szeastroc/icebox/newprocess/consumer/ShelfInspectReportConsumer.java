package com.szeastroc.icebox.newprocess.consumer;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.szeastroc.common.feign.visit.FeignExportRecordsClient;
import com.szeastroc.common.utils.ImageUploadUtil;
import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.icebox.newprocess.consumer.common.ShelfInspectReportMsg;
import com.szeastroc.icebox.newprocess.consumer.utils.PoiUtil;
import com.szeastroc.icebox.newprocess.entity.DisplayShelfInspectReport;
import com.szeastroc.icebox.newprocess.service.DisplayShelfInspectReportService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.List;

@Slf4j
@Component
public class ShelfInspectReportConsumer {

    @Autowired
    private DisplayShelfInspectReportService shelfInspectReportService;
    @Autowired
    private ImageUploadUtil imageUploadUtil;
    @Autowired
    private FeignExportRecordsClient feignExportRecordsClient;
    @RabbitListener(queues = MqConstant.shelfInspectReportQueue)
    public void task(ShelfInspectReportMsg reportMsg) throws Exception {
        selectReport(reportMsg);
    }

    private void selectReport(ShelfInspectReportMsg reportMsg) throws Exception {
        LambdaQueryWrapper<DisplayShelfInspectReport> wrapper = shelfInspectReportService.fillWrapper(reportMsg);
        Integer count = shelfInspectReportService.selectByExportCount(wrapper); // 得到当前条件下的总量
        // 列
        String[] columnName = {"事业部", "大区", "服务处", "门店编号","门店名称", "货架",  "投放数量", "巡检状态", "异常数量", "巡检人员", "巡检日期",
                "现场照片", "巡检职务", "巡检备注"};
        // 先写入本地文件
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String tmpPath = String.format("%s.xlsx", System.currentTimeMillis());
        PoiUtil.exportReportExcelToLocalPath(count, columnName, tmpPath, imageUploadUtil, feignExportRecordsClient, reportMsg.getRecordsId(),
                (wb, eachSheet, startRowCount, endRowCount, currentPage, pageSize) -> {
                    Page<DisplayShelfInspectReport> page = new Page<>();
                    page.setCurrent(currentPage);
                    page.setSize(pageSize);
                    IPage<DisplayShelfInspectReport> reportPage = shelfInspectReportService.page(page, wrapper);
                    List<DisplayShelfInspectReport> reports = reportPage.getRecords();
                    if (CollectionUtil.isNotEmpty(reports)) {
                        for (int i = startRowCount; i <= endRowCount; i++) {
                            SXSSFRow eachDataRow = eachSheet.createRow(i);
                            if ((i - startRowCount) < reports.size()) {
                                DisplayShelfInspectReport report = reports.get(i - startRowCount);
                                eachDataRow.createCell(0).setCellValue(report.getBusinessDeptName());
                                eachDataRow.createCell(1).setCellValue(report.getRegionDeptName());
                                eachDataRow.createCell(2).setCellValue(report.getServiceDeptName());
                                eachDataRow.createCell(3).setCellValue(report.getPutCustomerNumber());
                                eachDataRow.createCell(4).setCellValue(report.getPutCustomerName());
                                eachDataRow.createCell(5).setCellValue(report.getName() + "(" + report.getSize() + ")");
                                eachDataRow.createCell(6).setCellValue(report.getPutCount());
                                eachDataRow.createCell(7).setCellValue(report.getInspectStatus());
                                eachDataRow.createCell(8).setCellValue(report.getUnusualNumber());
                                eachDataRow.createCell(9).setCellValue(report.getSubmitterName());
                                eachDataRow.createCell(10).setCellValue(dateFormat.format(report.getSubmitTime()));
                                eachDataRow.createCell(11).setCellValue(report.getImageUrl());
                                eachDataRow.createCell(12).setCellValue(report.getSubmitterPosition());
                                eachDataRow.createCell(13).setCellValue(report.getInspectRemark());
                            }
                        }
                    }
                });
    }

}