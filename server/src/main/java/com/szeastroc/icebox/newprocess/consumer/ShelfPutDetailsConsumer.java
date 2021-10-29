package com.szeastroc.icebox.newprocess.consumer;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.szeastroc.common.feign.visit.FeignExportRecordsClient;
import com.szeastroc.common.utils.ImageUploadUtil;
import com.szeastroc.commondb.config.annotation.RoutingDataSource;
import com.szeastroc.commondb.config.mybatis.Datasources;
import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.icebox.newprocess.consumer.common.ShelfPutDetailsMsg;
import com.szeastroc.icebox.newprocess.consumer.utils.PoiUtil;
import com.szeastroc.icebox.newprocess.dao.DisplayShelfDao;
import com.szeastroc.icebox.newprocess.entity.DisplayShelf;
import com.szeastroc.icebox.newprocess.entity.DisplayShelfInspectReport;
import com.szeastroc.icebox.newprocess.enums.SupplierTypeEnum;
import com.szeastroc.icebox.newprocess.service.DisplayShelfService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class ShelfPutDetailsConsumer {
    @Autowired
    private DisplayShelfDao displayShelfDao;
    @Autowired
    private ImageUploadUtil imageUploadUtil;
    @Autowired
    private DisplayShelfService displayShelfService;
    @Autowired
    private FeignExportRecordsClient feignExportRecordsClient;


    @RabbitListener(queues = MqConstant.SHELF_PUT_DETAILS_Q, containerFactory = "shelfPutDetailsContainer")
    public void task(ShelfPutDetailsMsg shelfPutDetailsMsg) throws Exception {
        log.info("ShelfPutDetailsConsumer task start... 导出序列号:[{}]", shelfPutDetailsMsg.getRecordsId());
        long start = System.currentTimeMillis();
        Integer count = displayShelfService.selectByExportCount(shelfPutDetailsMsg.getShelfLambdaQueryWrapper()); // 得到当前条件下的总量
        log.warn("当前检索条件下的总数据量为 [{}], 统计总量耗时 [{}]", count, System.currentTimeMillis()-start);
        // 列
        String[] columnName = {"本部", "事业部", "大区", "服务处", "货架类型", "尺寸", "数量", "投放数量"};
        // 先写入本地文件
        String tmpPath = String.format("%s.xlsx", System.currentTimeMillis());
        PoiUtil.exportReportExcelToLocalPath(count, columnName, tmpPath, imageUploadUtil, feignExportRecordsClient, shelfPutDetailsMsg.getRecordsId(),
                (wb, eachSheet, startRowCount, endRowCount, currentPage, pageSize) -> {
                    List<DisplayShelf> displayShelfList = shelfPutDetailsMsg.getDisplayShelfList();
//                    List<DisplayShelf> reports = displayShelfIPage.getRecords();
                    if (CollectionUtil.isNotEmpty(displayShelfList)) {
                        for (int i = startRowCount; i <= endRowCount; i++) {
                            SXSSFRow eachDataRow = eachSheet.createRow(i);
                            if ((i - startRowCount) < displayShelfList.size()) {
                                DisplayShelf report = displayShelfList.get(i - startRowCount);
                                eachDataRow.createCell(0).setCellValue(report.getHeadquartersDeptName());
                                eachDataRow.createCell(1).setCellValue(report.getBusinessDeptName());
                                eachDataRow.createCell(2).setCellValue(report.getRegionDeptName());
                                eachDataRow.createCell(3).setCellValue(report.getServiceDeptName());
                                eachDataRow.createCell(4).setCellValue(report.getName());
                                eachDataRow.createCell(5).setCellValue(report.getSize());
                                eachDataRow.createCell(6).setCellValue(report.getCount());
                                eachDataRow.createCell(7).setCellValue(report.getPutCount());

                            }
                        }
                    }
                });
    }
}


