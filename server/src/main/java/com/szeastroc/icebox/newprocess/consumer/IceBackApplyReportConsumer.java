package com.szeastroc.icebox.newprocess.consumer;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.szeastroc.common.feign.user.FeignUserClient;
import com.szeastroc.common.feign.visit.FeignExportRecordsClient;
import com.szeastroc.common.utils.ImageUploadUtil;
import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.icebox.enums.FreePayTypeEnum;
import com.szeastroc.icebox.newprocess.consumer.common.IceBackApplyReportMsg;
import com.szeastroc.icebox.newprocess.consumer.utils.PoiUtil;
import com.szeastroc.icebox.newprocess.entity.IceBackApplyReport;
import com.szeastroc.icebox.newprocess.enums.IceBackStatusEnum;
import com.szeastroc.icebox.newprocess.enums.PutStatus;
import com.szeastroc.icebox.newprocess.enums.SupplierTypeEnum;
import com.szeastroc.icebox.newprocess.service.IceBackApplyReportService;
import com.szeastroc.icebox.newprocess.vo.IceBackApplyReportExcelVo;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class IceBackApplyReportConsumer {

    @Autowired
    private IceBackApplyReportService iceBackApplyReportService;
    @Autowired
    private ImageUploadUtil imageUploadUtil;
    @Autowired
    private FeignExportRecordsClient feignExportRecordsClient;

    //    @RabbitHandler
    @RabbitListener(queues = MqConstant.iceBackApplyReportQueue)
    public void task(IceBackApplyReportMsg reportMsg) throws Exception {
        selectReport(reportMsg);
    }

    private void selectReport(IceBackApplyReportMsg reportMsg) throws Exception {
        LambdaQueryWrapper<IceBackApplyReport> wrapper = iceBackApplyReportService.fillWrapper(reportMsg);
        Integer count = iceBackApplyReportService.selectByExportCount(wrapper); // 得到当前条件下的总量
        // 列
        String[] columnName = {"事业部", "大区", "服务处", "流程编号", "所属经销商编号", "所属经销商名称", "退还客户编号", "退还客户名称", "退还客户类型", "退还日期", "冰柜型号", "冰柜编号", "是否免押"
                , "押金金额", "审核人员", "审核人职务", "审核日期", "退还状态"};
        // 先写入本地文件
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String tmpPath = String.format("%s.xlsx", System.currentTimeMillis());
        PoiUtil.exportReportExcelToLocalPath(count, columnName, tmpPath, imageUploadUtil, feignExportRecordsClient, reportMsg.getRecordsId(),
                (wb, eachSheet, startRowCount, endRowCount, currentPage, pageSize) -> {
                    Page<IceBackApplyReport> page = new Page<>();
                    page.setCurrent(currentPage);
                    page.setSize(pageSize);
                    IPage<IceBackApplyReport> reportPage = iceBackApplyReportService.page(page, wrapper);
                    List<IceBackApplyReport> reports = reportPage.getRecords();
                    if (CollectionUtil.isNotEmpty(reports)) {
                        for (int i = startRowCount; i <= endRowCount; i++) {
                            SXSSFRow eachDataRow = eachSheet.createRow(i);
                            if ((i - startRowCount) < reports.size()) {
                                IceBackApplyReport report = reports.get(i - startRowCount);
                                eachDataRow.createCell(0).setCellValue(report.getBusinessDeptName());
                                eachDataRow.createCell(1).setCellValue(report.getRegionDeptName());
                                eachDataRow.createCell(2).setCellValue(report.getServiceDeptName());
                                eachDataRow.createCell(3).setCellValue(report.getApplyNumber());
                                eachDataRow.createCell(4).setCellValue(report.getDealerNumber());
                                eachDataRow.createCell(5).setCellValue(report.getDealerName());
                                eachDataRow.createCell(6).setCellValue(report.getBackCustomerNumber());
                                eachDataRow.createCell(7).setCellValue(report.getBackCustomerName());
                                eachDataRow.createCell(8).setCellValue(SupplierTypeEnum.getDesc(report.getBackCustomerType()));
                                eachDataRow.createCell(9).setCellValue(dateFormat.format(report.getBackDate()));
                                eachDataRow.createCell(10).setCellValue(report.getModelName());
                                eachDataRow.createCell(11).setCellValue(report.getAssetId());
                                eachDataRow.createCell(12).setCellValue(1==report.getFreeType()?"否":"是");
                                eachDataRow.createCell(13).setCellValue(report.getDepositMoney() + "");
                                eachDataRow.createCell(14).setCellValue(report.getCheckPerson());
                                eachDataRow.createCell(15).setCellValue(report.getCheckOfficeName());
                                eachDataRow.createCell(16).setCellValue(dateFormat.format(report.getCheckDate()));
                                eachDataRow.createCell(17).setCellValue(IceBackStatusEnum.getDesc(report.getExamineStatus()));
                            }
                        }
                    }
                });
    }

}