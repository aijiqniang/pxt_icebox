package com.szeastroc.icebox.newprocess.consumer;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.szeastroc.common.entity.customer.vo.StoreInfoDtoVo;
import com.szeastroc.common.feign.customer.FeignStoreClient;
import com.szeastroc.common.feign.visit.FeignExportRecordsClient;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.common.utils.ImageUploadUtil;
import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.icebox.newprocess.consumer.common.IceBackApplyReportMsg;
import com.szeastroc.icebox.newprocess.consumer.utils.PoiUtil;
import com.szeastroc.icebox.newprocess.entity.IceBackApplyReport;
import com.szeastroc.icebox.newprocess.enums.IceBackStatusEnum;
import com.szeastroc.icebox.newprocess.enums.SupplierTypeEnum;
import com.szeastroc.icebox.newprocess.service.IceBackApplyReportService;
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
public class IceBackApplyReportConsumer {

    @Autowired
    private IceBackApplyReportService iceBackApplyReportService;
    @Autowired
    private ImageUploadUtil imageUploadUtil;
    @Autowired
    private FeignExportRecordsClient feignExportRecordsClient;
    @Autowired
    private FeignStoreClient feignStoreClient;
    //    @RabbitHandler
    @RabbitListener(queues = MqConstant.iceBackApplyReportQueue)
    public void task(IceBackApplyReportMsg reportMsg) throws Exception {
        selectReport(reportMsg);
    }

    private void selectReport(IceBackApplyReportMsg reportMsg) throws Exception {
        LambdaQueryWrapper<IceBackApplyReport> wrapper = iceBackApplyReportService.fillWrapper(reportMsg);
        Integer count = iceBackApplyReportService.selectByExportCount(wrapper); // 得到当前条件下的总量
        // 列
        String[] columnName = {"事业部", "大区", "服务处", "流程编号", "所属经销商编号", "所属经销商名称", "退还客户编号", "退还客户名称", "退还客户类型","客户联系人","联系人电话","省","市","区县",
                "退还客户地址", "退还日期", "冰柜型号", "冰柜编号", "是否免押", "押金金额", "审核人员", "审核人职务", "审核日期","业务员","业务员电话", "退还状态","审批备注","商户编号","退还备注","退还原因"};
        // 先写入本地文件
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String tmpPath = String.format("%s.xlsx", System.currentTimeMillis());
        PoiUtil.exportReportExcelToLocalPath(count, columnName, tmpPath, imageUploadUtil, feignExportRecordsClient, reportMsg.getRecordsId(),
                (wb, eachSheet, startRowCount, endRowCount, currentPage, pageSize) -> {
                    Page<IceBackApplyReport> page = new Page<>();
                    page.setCurrent(currentPage);
                    page.setSize(pageSize);
                    IPage<IceBackApplyReport> reportPage = iceBackApplyReportService.page(page, wrapper);
                    reportPage.convert(iceBackApplyReport -> {
                        if(iceBackApplyReport != null && StringUtils.isNotEmpty(iceBackApplyReport.getCustomerNumber())){
                            StoreInfoDtoVo storeInfoDtoVo = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(iceBackApplyReport.getCustomerNumber()));
                            if(storeInfoDtoVo != null && StringUtils.isNotEmpty(storeInfoDtoVo.getMerchantNumber())){
                                iceBackApplyReport.setMerchantNumber(storeInfoDtoVo.getMerchantNumber());
                            }
                        }
                        return iceBackApplyReport;
                    });
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
                                eachDataRow.createCell(6).setCellValue(report.getCustomerNumber());
                                eachDataRow.createCell(7).setCellValue(report.getCustomerName());
                                eachDataRow.createCell(8).setCellValue(SupplierTypeEnum.getDesc(report.getCustomerType()));
                                eachDataRow.createCell(9).setCellValue(report.getLinkMan());
                                eachDataRow.createCell(10).setCellValue(report.getLinkMobile());
                                eachDataRow.createCell(11).setCellValue(report.getProvince());
                                eachDataRow.createCell(12).setCellValue(report.getCity());
                                eachDataRow.createCell(13).setCellValue(report.getArea());
                                eachDataRow.createCell(14).setCellValue(report.getCustomerAddress());
                                eachDataRow.createCell(15).setCellValue(Objects.nonNull(report.getBackDate())?dateFormat.format(report.getBackDate()):"");
                                eachDataRow.createCell(16).setCellValue(report.getModelName());
                                eachDataRow.createCell(17).setCellValue(report.getAssetId());
                                eachDataRow.createCell(18).setCellValue(1==report.getFreeType()?"否":"是");
                                eachDataRow.createCell(19).setCellValue(report.getDepositMoney() + "");
                                eachDataRow.createCell(20).setCellValue(report.getCheckPerson());
                                eachDataRow.createCell(21).setCellValue(report.getCheckOfficeName());
                                eachDataRow.createCell(22).setCellValue(Objects.nonNull(report.getCheckDate())?dateFormat.format(report.getCheckDate()):"");
                                eachDataRow.createCell(23).setCellValue(report.getSubmitterName());
                                eachDataRow.createCell(24).setCellValue(report.getSubmitterMobile());
                                eachDataRow.createCell(25).setCellValue(IceBackStatusEnum.getDesc(report.getExamineStatus()));
                                eachDataRow.createCell(26).setCellValue(report.getReason());
                                eachDataRow.createCell(27).setCellValue(report.getMerchantNumber());
                                eachDataRow.createCell(28).setCellValue(report.getBackRemark());
                                eachDataRow.createCell(29).setCellValue(report.getBackReason());

                            }
                        }
                    }
                });
    }

}