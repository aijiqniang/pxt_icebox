package com.szeastroc.icebox.newprocess.consumer;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.szeastroc.common.utils.ImageUploadUtil;
import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.icebox.newprocess.consumer.common.IceBoxExamineExceptionReportMsg;
import com.szeastroc.icebox.newprocess.consumer.enums.OperateTypeEnum;
import com.szeastroc.icebox.newprocess.consumer.utils.PoiUtil;
import com.szeastroc.icebox.newprocess.entity.IceBoxExamineExceptionReport;
import com.szeastroc.icebox.newprocess.enums.ExamineExceptionStatusEnums;
import com.szeastroc.icebox.newprocess.enums.IceBoxEnums;
import com.szeastroc.icebox.newprocess.enums.SupplierTypeEnum;
import com.szeastroc.icebox.newprocess.service.IceBoxExamineExceptionReportService;
import com.szeastroc.icebox.newprocess.vo.IceBoxExamineExceptionReportExcelVo;
import com.szeastroc.visit.client.FeignExportRecordsClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
public class IceBoxExamineExceptionReportConsumer {

    @Autowired
    private IceBoxExamineExceptionReportService iceBoxExamineExceptionReportService;
    @Autowired
    private ImageUploadUtil imageUploadUtil;
    @Autowired
    private FeignExportRecordsClient feignExportRecordsClient;

//    @RabbitHandler
    @RabbitListener(queues = MqConstant.iceboxExceptionReportQueue)
    public void task(IceBoxExamineExceptionReportMsg reportMsg) throws Exception {
        if(OperateTypeEnum.INSERT.getType().equals(reportMsg.getOperateType())){
            saveReport(reportMsg);
        }
        if(OperateTypeEnum.UPDATE.getType().equals(reportMsg.getOperateType())){
            updateReport(reportMsg);
        }
        if(OperateTypeEnum.SELECT.getType().equals(reportMsg.getOperateType())){
            selectReport(reportMsg);
        }
    }

    private void selectReport(IceBoxExamineExceptionReportMsg reportMsg) throws Exception {
        LambdaQueryWrapper<IceBoxExamineExceptionReport> wrapper = fillWrapper(reportMsg);
        log.info("fxbill task... [{}]", JSON.toJSONString(reportMsg));
        long start = System.currentTimeMillis();
        Integer count = iceBoxExamineExceptionReportService.selectByExportCount(wrapper); // 得到当前条件下的总量
        log.warn("当前检索条件下的分销订单总数据量为 [{}], 统计总量耗时 [{}],操作人[{}]", count, System.currentTimeMillis() - start,reportMsg.getOperateName());
        // 列
        String[] columnName = {"事业部","大区","服务处","所属经销商编号", "所属经销商名称", "投放客户编号", "投放客户名称","投放客户类型","冰柜编号", "冰柜型号","押金金额","提报类型","提交人","提交日期",  "审核人员",
                "审核日期", "状态", "提报时间","提报单号", };
        // 先写入本地文件
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String tmpPath = String.format("%s.xlsx", System.currentTimeMillis());
        PoiUtil.exportReportExcelToLocalPath(count, columnName, tmpPath, imageUploadUtil, feignExportRecordsClient, reportMsg.getRecordsId(),
                (wb,eachSheet, startRowCount, endRowCount, currentPage, pageSize) -> {
                    List<IceBoxExamineExceptionReportExcelVo> excelVoList = new ArrayList<>();
                    Page<IceBoxExamineExceptionReport> page = new Page<>();
                    page.setCurrent(currentPage);
                    page.setSize(pageSize);
                    IPage<IceBoxExamineExceptionReport> putReportIPage = iceBoxExamineExceptionReportService.page(page,wrapper);
                    List<IceBoxExamineExceptionReport> billInfos = putReportIPage.getRecords();
                    if (CollectionUtil.isNotEmpty(billInfos)) {
                        for(IceBoxExamineExceptionReport report:billInfos){
                            IceBoxExamineExceptionReportExcelVo excelVo = new IceBoxExamineExceptionReportExcelVo();
                            BeanUtils.copyProperties(report,excelVo);

                            if(report.getSubmitTime() != null){
                                excelVo.setSubmitTime(dateFormat.format(report.getSubmitTime()));
                            }
                            if(report.getExamineTime() != null){
                                excelVo.setExamineTime(dateFormat.format(report.getExamineTime()));
                            }
                            if(SupplierTypeEnum.IS_STORE.getType().equals(report.getPutCustomerType())){
                                excelVo.setPutCustomerType(SupplierTypeEnum.IS_STORE.getDesc());
                            }
                            if(SupplierTypeEnum.IS_POSTMAN.getType().equals(report.getPutCustomerType())){
                                excelVo.setPutCustomerType(SupplierTypeEnum.IS_POSTMAN.getDesc());
                            }
                            if(SupplierTypeEnum.IS_WHOLESALER.getType().equals(report.getPutCustomerType())){
                                excelVo.setPutCustomerType(SupplierTypeEnum.IS_WHOLESALER.getDesc());
                            }
                            excelVo.setToOaType(IceBoxEnums.StatusEnum.getDesc(report.getToOaType()));
                            excelVo.setStatus(ExamineExceptionStatusEnums.getDesc(report.getStatus()));
                            excelVoList.add(excelVo);
                        }
//                        excelVoList = excelVoList.stream().sorted(Comparator.comparing(IceBoxExamineExceptionReportExcelVo::)).collect(Collectors.toList());
                        if(CollectionUtil.isNotEmpty(excelVoList)){
                            log.warn("当前检索条件下的分销订单导出总数据量为 [{}],操作人[{}]", excelVoList.size(),reportMsg.getOperateName());
                            for (int i = startRowCount; i <= endRowCount; i++) {
                                SXSSFRow eachDataRow = eachSheet.createRow(i);
                                if ((i - startRowCount) < excelVoList.size()) {
                                    IceBoxExamineExceptionReportExcelVo excelVo = excelVoList.get(i - startRowCount);
                                    eachDataRow.createCell(0).setCellValue(excelVo.getBusinessDeptName());
                                    eachDataRow.createCell(1).setCellValue(excelVo.getRegionDeptName());
                                    eachDataRow.createCell(2).setCellValue(excelVo.getServiceDeptName());
                                    eachDataRow.createCell(3).setCellValue(excelVo.getSupplierNumber());
                                    eachDataRow.createCell(4).setCellValue(excelVo.getSupplierName());
                                    eachDataRow.createCell(5).setCellValue(excelVo.getPutCustomerNumber());
                                    eachDataRow.createCell(6).setCellValue(excelVo.getPutCustomerName());
                                    eachDataRow.createCell(7).setCellValue(excelVo.getPutCustomerType());
                                    eachDataRow.createCell(8).setCellValue(excelVo.getIceBoxAssetId());
                                    eachDataRow.createCell(9).setCellValue(excelVo.getIceBoxModelName());
                                    eachDataRow.createCell(10).setCellValue(excelVo.getDepositMoney()+"");
                                    eachDataRow.createCell(11).setCellValue(excelVo.getToOaType());
                                    eachDataRow.createCell(12).setCellValue(excelVo.getSubmitterName());
                                    eachDataRow.createCell(13).setCellValue(excelVo.getSubmitTime());
                                    eachDataRow.createCell(14).setCellValue(excelVo.getExamineUserName());
                                    eachDataRow.createCell(15).setCellValue(excelVo.getExamineTime());
                                    eachDataRow.createCell(16).setCellValue(excelVo.getStatus());
                                    eachDataRow.createCell(17).setCellValue(excelVo.getToOaTime());
                                    eachDataRow.createCell(18).setCellValue(excelVo.getToOaNumber());
                                }
                            }
                        }
                    }
                });
    }

    private void updateReport(IceBoxExamineExceptionReportMsg reportMsg) {
        IceBoxExamineExceptionReport isExsit = iceBoxExamineExceptionReportService.getOne(Wrappers.<IceBoxExamineExceptionReport>lambdaQuery().eq(IceBoxExamineExceptionReport::getExamineNumber, reportMsg.getExamineNumber()));
        isExsit.setStatus(reportMsg.getStatus());
        if(reportMsg.getExamineUserId() != null){
            isExsit.setExamineUserId(reportMsg.getExamineUserId());
        }

        if(StringUtils.isNotEmpty(reportMsg.getExamineUserName())){
            isExsit.setExamineUserName(reportMsg.getExamineUserName());
        }

        if(reportMsg.getExamineTime() != null){
            isExsit.setExamineTime(reportMsg.getExamineTime());
        }

        iceBoxExamineExceptionReportService.updateById(isExsit);
    }

    private void saveReport(IceBoxExamineExceptionReportMsg reportMsg) {
        IceBoxExamineExceptionReport report = new IceBoxExamineExceptionReport();
        BeanUtils.copyProperties(reportMsg,report);
        iceBoxExamineExceptionReportService.save(report);
    }

    private LambdaQueryWrapper<IceBoxExamineExceptionReport> fillWrapper(IceBoxExamineExceptionReportMsg reportMsg) {
        LambdaQueryWrapper<IceBoxExamineExceptionReport> wrapper = Wrappers.<IceBoxExamineExceptionReport>lambdaQuery();
        if(reportMsg.getGroupDeptId() != null){
            wrapper.eq(IceBoxExamineExceptionReport::getGroupDeptId,reportMsg.getGroupDeptId());
        }
        if(reportMsg.getServiceDeptId() != null){
            wrapper.eq(IceBoxExamineExceptionReport::getServiceDeptId,reportMsg.getServiceDeptId());
        }
        if(reportMsg.getRegionDeptId() != null){
            wrapper.eq(IceBoxExamineExceptionReport::getRegionDeptId,reportMsg.getRegionDeptId());
        }
        if(reportMsg.getBusinessDeptId() != null){
            wrapper.eq(IceBoxExamineExceptionReport::getBusinessDeptId,reportMsg.getBusinessDeptId());
        }
        if(reportMsg.getHeadquartersDeptId() != null){
            wrapper.eq(IceBoxExamineExceptionReport::getHeadquartersDeptId,reportMsg.getHeadquartersDeptId());
        }
        if(StringUtils.isNotEmpty(reportMsg.getSupplierName())){
            wrapper.and(x -> x.like(IceBoxExamineExceptionReport::getSupplierName,reportMsg.getSupplierName()).or().like(IceBoxExamineExceptionReport::getSupplierNumber,reportMsg.getSupplierNumber()));
        }
        if(reportMsg.getSubmitterId() != null){
            wrapper.eq(IceBoxExamineExceptionReport::getSubmitterId,reportMsg.getSubmitterId());
        }
        if(reportMsg.getSubmitTime() != null){
            wrapper.ge(IceBoxExamineExceptionReport::getSubmitTime,reportMsg.getSubmitTime());
        }
        if(reportMsg.getSubmitEndTime() != null){
            wrapper.le(IceBoxExamineExceptionReport::getSubmitTime,reportMsg.getSubmitEndTime());
        }
        if(reportMsg.getPutCustomerName() != null){
            wrapper.and(x -> x.like(IceBoxExamineExceptionReport::getPutCustomerName,reportMsg.getPutCustomerName()).or().like(IceBoxExamineExceptionReport::getPutCustomerNumber,reportMsg.getPutCustomerNumber()));
        }
        if(reportMsg.getPutCustomerType() != null){
            wrapper.eq(IceBoxExamineExceptionReport::getPutCustomerType,reportMsg.getPutCustomerType());
        }
        if(org.apache.commons.lang3.StringUtils.isNotEmpty(reportMsg.getIceBoxAssetId())){
            wrapper.eq(IceBoxExamineExceptionReport::getIceBoxAssetId,reportMsg.getIceBoxAssetId());
        }
        return wrapper;
    }

}