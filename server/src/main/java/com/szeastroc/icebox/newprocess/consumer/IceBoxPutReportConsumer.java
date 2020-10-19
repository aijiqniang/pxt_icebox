package com.szeastroc.icebox.newprocess.consumer;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.common.utils.ImageUploadUtil;
import com.szeastroc.commondb.config.annotation.RoutingDataSource;
import com.szeastroc.commondb.config.mybatis.Datasources;
import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.icebox.enums.FreePayTypeEnum;
import com.szeastroc.icebox.newprocess.consumer.common.IceBoxPutReportMsg;
import com.szeastroc.icebox.newprocess.consumer.enums.OperateTypeEnum;
import com.szeastroc.icebox.newprocess.consumer.utils.PoiUtil;
import com.szeastroc.icebox.newprocess.dao.ExportRecordsDao;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.entity.IceBoxPutReport;
import com.szeastroc.icebox.newprocess.enums.PutStatus;
import com.szeastroc.icebox.newprocess.enums.SupplierTypeEnum;
import com.szeastroc.icebox.newprocess.service.IceBoxPutReportService;
import com.szeastroc.icebox.newprocess.service.IceBoxService;
import com.szeastroc.icebox.newprocess.vo.IceBoxPutReportExcelVo;
import com.szeastroc.user.common.session.UserManageVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class IceBoxPutReportConsumer {

    @Autowired
    private IceBoxPutReportService iceBoxPutReportService;
    @Autowired
    private ImageUploadUtil imageUploadUtil;
    @Autowired
    private ExportRecordsDao exportRecordsDao;
//    @RabbitHandler
    @RabbitListener(queues = MqConstant.iceboxReportQueue)
    public void task(IceBoxPutReportMsg reportMsg) throws Exception {
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

    private void selectReport(IceBoxPutReportMsg reportMsg) throws Exception {
        LambdaQueryWrapper<IceBoxPutReport> wrapper = fillWrapper(reportMsg);
        log.info("fxbill task... [{}]", JSON.toJSONString(reportMsg));
        long start = System.currentTimeMillis();
        Integer count = exportRecordsDao.selectByExportCount(wrapper); // 得到当前条件下的总量
        log.warn("当前检索条件下的分销订单总数据量为 [{}], 统计总量耗时 [{}],操作人[{}]", count, System.currentTimeMillis() - start,reportMsg.getOperateName());
        // 列
        String[] columnName = {"事业部","大区","服务处", "订单编号", "客户编号", "客户名称", "客户类型","客户等级", "供货商类型", "供货商编号","供货商名称", "业务人员","业务人员部门","录入日期", "订单日期", "订单类型", "产品编号",
                "产品名称", "单价", "订单数量（箱）", "营收标箱（箱）", "赠送数量（箱）", "发货日期", "发货数量（箱）", "发货营收标箱（箱）", "搭赠数量发货（箱）","收货日期", "状态", "单据来源", "备注"};
        // 先写入本地文件
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String tmpPath = String.format("%s.xlsx", System.currentTimeMillis());
        PoiUtil.exportReportExcelToLocalPath(count, columnName, tmpPath, imageUploadUtil, exportRecordsDao, reportMsg.getSerialNum(),
                (wb,eachSheet, startRowCount, endRowCount, currentPage, pageSize) -> {
                    List<IceBoxPutReportExcelVo> excelVoList = new ArrayList<>();
                    Page<IceBoxPutReport> page = new Page<>();
                    page.setCurrent(currentPage);
                    page.setSize(pageSize);
                    IPage<IceBoxPutReport> putReportIPage = iceBoxPutReportService.page(page,wrapper);
                    List<IceBoxPutReport> billInfos = putReportIPage.getRecords();
                    if (CollectionUtil.isNotEmpty(billInfos)) {
                        for(IceBoxPutReport report:billInfos){
                            IceBoxPutReportExcelVo excelVo = new IceBoxPutReportExcelVo();
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
                            excelVo.setFreeType(FreePayTypeEnum.getDesc(report.getFreeType()));

                            excelVo.setPutStatus("投放中");
                            if(PutStatus.FINISH_PUT.getStatus().equals(report.getPutStatus())){
                                excelVo.setPutStatus("已投放");
                            }
                            if(PutStatus.NO_PASS.getStatus().equals(report.getPutStatus())){
                                excelVo.setPutStatus("已驳回");
                            }
                            excelVoList.add(excelVo);
                        }
                        excelVoList = excelVoList.stream().sorted(Comparator.comparing(IceBoxPutReportExcelVo::getApplyNumber)).collect(Collectors.toList());
                        if(CollectionUtil.isNotEmpty(excelVoList)){
                            log.warn("当前检索条件下的分销订单导出总数据量为 [{}],操作人[{}]", excelVoList.size(),reportMsg.getOperateName());
                            for (int i = startRowCount; i <= endRowCount; i++) {
                                SXSSFRow eachDataRow = eachSheet.createRow(i);
                                if ((i - startRowCount) < excelVoList.size()) {
                                    IceBoxPutReportExcelVo excelVo = excelVoList.get(i - startRowCount);
                                    eachDataRow.createCell(0).setCellValue(excelVo.getHeadquartersDeptName());
                                    eachDataRow.createCell(1).setCellValue(excelVo.getBusinessDeptName());
                                    eachDataRow.createCell(2).setCellValue(excelVo.getRegionDeptName());
                                    eachDataRow.createCell(3).setCellValue(excelVo.getServiceDeptName());
                                    eachDataRow.createCell(4).setCellValue(excelVo.getGroupDeptName());
                                    eachDataRow.createCell(5).setCellValue(excelVo.getApplyNumber());
                                    eachDataRow.createCell(6).setCellValue(excelVo.getSupplierNumber());
                                    eachDataRow.createCell(7).setCellValue(excelVo.getSupplierName());
                                    eachDataRow.createCell(8).setCellValue(excelVo.getSubmitterName());
                                    eachDataRow.createCell(9).setCellValue(excelVo.getSubmitTime());
                                    eachDataRow.createCell(10).setCellValue(excelVo.getPutCustomerNumber());
                                    eachDataRow.createCell(11).setCellValue(excelVo.getPutCustomerName());
                                    eachDataRow.createCell(12).setCellValue(excelVo.getPutCustomerType());
                                    eachDataRow.createCell(13).setCellValue(excelVo.getIceBoxModelName());
                                    eachDataRow.createCell(14).setCellValue(excelVo.getIceBoxAssetId());
                                    eachDataRow.createCell(15).setCellValue(excelVo.getApplyCount());
                                    eachDataRow.createCell(16).setCellValue(excelVo.getDepositMoney()+"");
                                    eachDataRow.createCell(17).setCellValue(excelVo.getExamineUserName());
                                    eachDataRow.createCell(18).setCellValue(excelVo.getExamineTime());
                                    eachDataRow.createCell(19).setCellValue(excelVo.getPutStatus());
                                }
                            }
                        }
                    }
                });
    }

    private void updateReport(IceBoxPutReportMsg reportMsg) {
        if(StringUtils.isNotEmpty(reportMsg.getIceBoxAssetId())){
            if(PutStatus.FINISH_PUT.getStatus().equals(reportMsg.getPutStatus())){
                IceBoxPutReport putReport = iceBoxPutReportService.getOne(Wrappers.<IceBoxPutReport>lambdaQuery().eq(IceBoxPutReport::getIceBoxAssetId, reportMsg.getIceBoxAssetId())
                        .eq(IceBoxPutReport::getApplyNumber, reportMsg.getApplyNumber())
                        .eq(IceBoxPutReport::getPutStatus, PutStatus.DO_PUT.getStatus()));
                if(putReport != null){
                    putReport.setPutStatus(reportMsg.getPutStatus());
                    iceBoxPutReportService.updateById(putReport);
                }
            }else {
                IceBoxPutReport report = iceBoxPutReportService.getOne(Wrappers.<IceBoxPutReport>lambdaQuery().eq(IceBoxPutReport::getApplyNumber, reportMsg.getApplyNumber())
                        .eq(IceBoxPutReport::getIceBoxModelId, reportMsg.getIceBoxModelId())
                        .eq(IceBoxPutReport::getSupplierId, reportMsg.getSupplierId())
                        .eq(IceBoxPutReport::getPutStatus, PutStatus.DO_PUT.getStatus()));
                if(report != null){
                    report.setIceBoxAssetId(reportMsg.getIceBoxAssetId());
                    iceBoxPutReportService.updateById(report);
                }
            }
        }else {
            List<IceBoxPutReport> reportList = iceBoxPutReportService.list(Wrappers.<IceBoxPutReport>lambdaQuery().eq(IceBoxPutReport::getApplyNumber, reportMsg.getApplyNumber()));
            if(CollectionUtil.isNotEmpty(reportList)){
                for (IceBoxPutReport putReport:reportList){
                    IceBoxPutReport report = new IceBoxPutReport();
                    BeanUtils.copyProperties(reportMsg,report);
                    report.setId(putReport.getId());
                    iceBoxPutReportService.updateById(report);
                }
            }
        }
    }

    private void saveReport(IceBoxPutReportMsg reportMsg) {
        IceBoxPutReport report = new IceBoxPutReport();
        BeanUtils.copyProperties(reportMsg,report);
        iceBoxPutReportService.save(report);
    }

    private LambdaQueryWrapper<IceBoxPutReport> fillWrapper(IceBoxPutReportMsg reportMsg) {
        LambdaQueryWrapper<IceBoxPutReport> wrapper = Wrappers.<IceBoxPutReport>lambdaQuery();
        if(reportMsg.getGroupDeptId() != null){
            wrapper.eq(IceBoxPutReport::getGroupDeptId,reportMsg.getGroupDeptId());
        }
        if(reportMsg.getServiceDeptId() != null){
            wrapper.eq(IceBoxPutReport::getServiceDeptId,reportMsg.getServiceDeptId());
        }
        if(reportMsg.getRegionDeptId() != null){
            wrapper.eq(IceBoxPutReport::getRegionDeptId,reportMsg.getRegionDeptId());
        }
        if(reportMsg.getBusinessDeptId() != null){
            wrapper.eq(IceBoxPutReport::getBusinessDeptId,reportMsg.getBusinessDeptId());
        }
        if(reportMsg.getHeadquartersDeptId() != null){
            wrapper.eq(IceBoxPutReport::getHeadquartersDeptId,reportMsg.getHeadquartersDeptId());
        }
        if(org.apache.commons.lang3.StringUtils.isNotEmpty(reportMsg.getApplyNumber())){
            wrapper.eq(IceBoxPutReport::getApplyNumber,reportMsg.getApplyNumber());
        }
        if(org.apache.commons.lang3.StringUtils.isNotEmpty(reportMsg.getSupplierName())){
            wrapper.and(x -> x.like(IceBoxPutReport::getSupplierName,reportMsg.getSupplierName()).or().like(IceBoxPutReport::getSupplierNumber,reportMsg.getSupplierNumber()));
        }
        if(reportMsg.getSubmitterId() != null){
            wrapper.eq(IceBoxPutReport::getSubmitterId,reportMsg.getSubmitterId());
        }
        if(reportMsg.getSubmitTime() != null){
            wrapper.ge(IceBoxPutReport::getSubmitTime,reportMsg.getSubmitTime());
        }
        if(reportMsg.getSubmitEndTime() != null){
            wrapper.le(IceBoxPutReport::getSubmitTime,reportMsg.getSubmitEndTime());
        }
        if(reportMsg.getPutCustomerName() != null){
            wrapper.and(x -> x.like(IceBoxPutReport::getPutCustomerName,reportMsg.getPutCustomerName()).or().like(IceBoxPutReport::getPutCustomerNumber,reportMsg.getPutCustomerNumber()));
        }
        if(reportMsg.getPutCustomerType() != null){
            wrapper.eq(IceBoxPutReport::getPutCustomerType,reportMsg.getPutCustomerType());
        }
        if(org.apache.commons.lang3.StringUtils.isNotEmpty(reportMsg.getIceBoxAssetId())){
            wrapper.eq(IceBoxPutReport::getIceBoxAssetId,reportMsg.getIceBoxAssetId());
        }
        return wrapper;
    }

}