package com.szeastroc.icebox.newprocess.consumer;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.szeastroc.common.entity.customer.vo.MemberInfoVo;
import com.szeastroc.common.entity.customer.vo.StoreInfoDtoVo;
import com.szeastroc.common.entity.customer.vo.SupplierInfoSessionVo;
import com.szeastroc.common.entity.user.vo.AddressVo;
import com.szeastroc.common.entity.user.vo.SimpleUserInfoVo;
import com.szeastroc.common.feign.customer.FeignStoreClient;
import com.szeastroc.common.feign.customer.FeignStoreRelateMemberClient;
import com.szeastroc.common.feign.customer.FeignSupplierClient;
import com.szeastroc.common.feign.user.FeignUserClient;
import com.szeastroc.common.feign.user.FeignXcxBaseClient;
import com.szeastroc.common.feign.visit.FeignExportRecordsClient;
import com.szeastroc.common.feign.visit.FeignIceboxQueryClient;
import com.szeastroc.common.redis.impl.UserRedisServiceImpl;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.common.utils.ImageUploadUtil;
import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.icebox.enums.FreePayTypeEnum;
import com.szeastroc.icebox.newprocess.consumer.common.IceBoxPutReportMsg;
import com.szeastroc.icebox.newprocess.consumer.enums.OperateTypeEnum;
import com.szeastroc.icebox.newprocess.consumer.utils.PoiUtil;
import com.szeastroc.icebox.newprocess.dao.IcePutApplyDao;
import com.szeastroc.icebox.newprocess.dao.IceTransferRecordDao;
import com.szeastroc.icebox.newprocess.entity.IceBoxPutReport;
import com.szeastroc.icebox.newprocess.entity.IcePutApply;
import com.szeastroc.icebox.newprocess.entity.IceTransferRecord;
import com.szeastroc.icebox.newprocess.enums.PutStatus;
import com.szeastroc.icebox.newprocess.enums.StoreSignStatus;
import com.szeastroc.icebox.newprocess.enums.SupplierTypeEnum;
import com.szeastroc.icebox.newprocess.enums.VisitCycleEnum;
import com.szeastroc.icebox.newprocess.service.IceBoxPutReportService;
import com.szeastroc.icebox.newprocess.service.IceTransferRecordService;
import com.szeastroc.icebox.newprocess.vo.IceBoxPutReportExcelVo;
import com.szeastroc.icebox.util.JudgeCustomerUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class IceBoxPutReportConsumer {

    @Autowired
    private IceBoxPutReportService iceBoxPutReportService;
    @Autowired
    private ImageUploadUtil imageUploadUtil;
    @Autowired
    private FeignExportRecordsClient feignExportRecordsClient;
    @Autowired
    private FeignUserClient feignUserClient;
    @Autowired
    private IcePutApplyDao icePutApplyDao;
    @Autowired
    private FeignStoreClient feignStoreClient;
    @Autowired
    private FeignSupplierClient feignSupplierClient;
    @Autowired
    private FeignIceboxQueryClient feignIceboxQueryClient;
    @Autowired
    private IceTransferRecordService iceTransferRecordService;
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
        Integer count = iceBoxPutReportService.selectByExportCount(wrapper); // 得到当前条件下的总量
        log.warn("当前检索条件下的分销订单总数据量为 [{}], 统计总量耗时 [{}],操作人[{}]", count, System.currentTimeMillis() - start,reportMsg.getOperateName());
        // 列
        String[] columnName = {"事业部","大区","服务处","省","市","区县", "流程编号"
                , "所属经销商编号", "所属经销商名称", "提交人","提交人电话","申请日期","签收日期"
                ,"客户等级", "投放客户编号", "投放客户名称","投放客户类型","客户地址","联系人","联系人电话","拜访频率"
                , "冰柜型号","冰柜编号", "是否免押", "押金金额","审核人员","审批人职务","审核日期","审批备注", "投放状态","投放备注"};
        // 先写入本地文件
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String tmpPath = String.format("%s.xlsx", System.currentTimeMillis());
        PoiUtil.exportReportExcelToLocalPath(count, columnName, tmpPath, imageUploadUtil, feignExportRecordsClient, reportMsg.getRecordsId(),
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
                            excelVo.setVisitTypeName(VisitCycleEnum.getDescByCode(report.getVisitType()));
                            IcePutApply icePutApply = icePutApplyDao.selectOne(Wrappers.<IcePutApply>lambdaQuery().eq(IcePutApply::getApplyNumber, report.getApplyNumber()).last("limit 1"));
                            if(icePutApply != null){
                                excelVo.setApplyPit(icePutApply.getApplyPit());
                                if(StoreSignStatus.ALREADY_SIGN.getStatus().equals(icePutApply.getStoreSignStatus())
                                        && icePutApply.getUpdateTime() != null){
                                    excelVo.setSignTime(dateFormat.format(icePutApply.getUpdateTime()));
                                }
                            }
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
                            if(PutStatus.IS_CANCEL.getStatus().equals(report.getPutStatus())){
                                excelVo.setPutStatus("已作废");
                            }
                            excelVo.setVisitTypeName(VisitCycleEnum.getDescByCode(report.getVisitType()));
                            excelVoList.add(excelVo);
                        }
                        excelVoList = excelVoList.stream().sorted(Comparator.comparing(IceBoxPutReportExcelVo::getApplyNumber)).collect(Collectors.toList());
                        if(CollectionUtil.isNotEmpty(excelVoList)){
                            log.warn("当前检索条件下的分销订单导出总数据量为 [{}],操作人[{}]", excelVoList.size(),reportMsg.getOperateName());
                            for (int i = startRowCount; i <= endRowCount; i++) {
                                SXSSFRow eachDataRow = eachSheet.createRow(i);
                                if ((i - startRowCount) < excelVoList.size()) {
                                    IceBoxPutReportExcelVo excelVo = excelVoList.get(i - startRowCount);

                                    eachDataRow.createCell(0).setCellValue(excelVo.getBusinessDeptName());
                                    eachDataRow.createCell(1).setCellValue(excelVo.getRegionDeptName());
                                    eachDataRow.createCell(2).setCellValue(excelVo.getServiceDeptName());
                                    eachDataRow.createCell(3).setCellValue(excelVo.getProvinceName());
                                    eachDataRow.createCell(4).setCellValue(excelVo.getCityName());
                                    eachDataRow.createCell(5).setCellValue(excelVo.getDistrictName());
                                    eachDataRow.createCell(6).setCellValue(excelVo.getApplyNumber());
                                    eachDataRow.createCell(7).setCellValue(excelVo.getSupplierNumber());
                                    eachDataRow.createCell(8).setCellValue(excelVo.getSupplierName());
                                    eachDataRow.createCell(9).setCellValue(excelVo.getSubmitterName());
                                    eachDataRow.createCell(10).setCellValue(excelVo.getSubmitterMobile());
                                    eachDataRow.createCell(11).setCellValue(excelVo.getSubmitTime());
                                    eachDataRow.createCell(12).setCellValue(excelVo.getSignTime());
                                    eachDataRow.createCell(13).setCellValue(excelVo.getPutCustomerLevel());
                                    eachDataRow.createCell(14).setCellValue(excelVo.getPutCustomerNumber());
                                    eachDataRow.createCell(15).setCellValue(excelVo.getPutCustomerName());
                                    eachDataRow.createCell(16).setCellValue(excelVo.getPutCustomerType());
                                    eachDataRow.createCell(17).setCellValue(excelVo.getCustomerAddress());
                                    eachDataRow.createCell(18).setCellValue(excelVo.getLinkmanName());
                                    eachDataRow.createCell(19).setCellValue(excelVo.getLinkmanMobile());
                                    eachDataRow.createCell(20).setCellValue(excelVo.getVisitTypeName());
                                    eachDataRow.createCell(21).setCellValue(excelVo.getIceBoxModelName());
                                    eachDataRow.createCell(22).setCellValue(excelVo.getIceBoxAssetId());
                                    eachDataRow.createCell(23).setCellValue(excelVo.getFreeType());
                                    eachDataRow.createCell(24).setCellValue(excelVo.getDepositMoney()+"");
                                    eachDataRow.createCell(25).setCellValue(excelVo.getExamineUserName());
                                    eachDataRow.createCell(26).setCellValue(excelVo.getExamineUserPosion());
                                    eachDataRow.createCell(27).setCellValue(excelVo.getExamineTime());
                                    eachDataRow.createCell(28).setCellValue(excelVo.getExamineRemark());
                                    eachDataRow.createCell(29).setCellValue(excelVo.getPutStatus());
                                    eachDataRow.createCell(30).setCellValue(excelVo.getApplyPit());
                                }
                            }
                        }
                    }
                });
    }

    private void updateReport(IceBoxPutReportMsg reportMsg) {
        if(StringUtils.isNotEmpty(reportMsg.getIceBoxAssetId())){
            if(PutStatus.FINISH_PUT.getStatus().equals(reportMsg.getPutStatus())){
                IceBoxPutReport putReport = iceBoxPutReportService.getOne(Wrappers.<IceBoxPutReport>lambdaQuery().eq(IceBoxPutReport::getIceBoxId, reportMsg.getIceBoxId())
                        .eq(IceBoxPutReport::getApplyNumber, reportMsg.getApplyNumber())
                        .eq(IceBoxPutReport::getPutStatus, PutStatus.DO_PUT.getStatus()).last("limit 1"));
                if(putReport != null){
                    if(Objects.nonNull(putReport.getExamineUserId())){
                        SimpleUserInfoVo exaine = FeignResponseUtil.getFeignData(feignUserClient.findUserById(putReport.getExamineUserId()));
                        if (Objects.nonNull(exaine)){
                            putReport.setExamineUserPosion(exaine.getPosion());
                        }
                    }
                    putReport.setPutStatus(reportMsg.getPutStatus());
                    iceBoxPutReportService.updateById(putReport);
                }
            }else {
                IceBoxPutReport report = iceBoxPutReportService.getOne(Wrappers.<IceBoxPutReport>lambdaQuery().eq(IceBoxPutReport::getApplyNumber, reportMsg.getApplyNumber())
                        .eq(IceBoxPutReport::getIceBoxModelId, reportMsg.getIceBoxModelId())
                        .eq(IceBoxPutReport::getSupplierId, reportMsg.getSupplierId())
                        .eq(IceBoxPutReport::getPutStatus, PutStatus.DO_PUT.getStatus()).last("limit 1"));
                if(report != null){
                    if(Objects.nonNull(report.getExamineUserId())){
                        SimpleUserInfoVo exaine = FeignResponseUtil.getFeignData(feignUserClient.findUserById(report.getExamineUserId()));
                        if (Objects.nonNull(exaine)){
                            report.setExamineUserPosion(exaine.getPosion());
                        }
                    }
                    report.setIceBoxId(reportMsg.getIceBoxId());
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
                    if(Objects.nonNull(report.getExamineUserId())){
                        SimpleUserInfoVo exaine = FeignResponseUtil.getFeignData(feignUserClient.findUserById(report.getExamineUserId()));
                        if (Objects.nonNull(exaine)){
                            report.setExamineUserPosion(exaine.getPosion());
                        }
                    }
                    iceBoxPutReportService.updateById(report);
                }
            }
        }
    }

    private void saveReport(IceBoxPutReportMsg reportMsg) {
        IceBoxPutReport report = new IceBoxPutReport();
        BeanUtils.copyProperties(reportMsg,report);
        if(StrUtil.isNotEmpty(report.getPutCustomerNumber())){
            if(JudgeCustomerUtils.isStoreType(report.getPutCustomerNumber())){
                StoreInfoDtoVo putStore = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(report.getPutCustomerNumber()));
                if(putStore != null){
                    report.setProvinceName(putStore.getProvinceName());
                    report.setCityName(putStore.getCityName());
                    report.setDistrictName(putStore.getDistrictName());
                    report.setPutCustomerName(putStore.getStoreName());
                    report.setCustomerAddress(putStore.getAddress());
                    report.setPutCustomerLevel(putStore.getStoreLevel());
                }
                String memberNumber = FeignResponseUtil.getFeignData(feignIceboxQueryClient.selectStoreKeeperNumberForReport(report.getPutCustomerNumber()));
                if(StrUtil.isNotEmpty(memberNumber)){
                    MemberInfoVo memberInfoVo = FeignResponseUtil.getFeignData(feignIceboxQueryClient.selectStoreKeeperForReport(memberNumber));
                    if(Objects.nonNull(memberInfoVo)){
                        report.setLinkmanMobile(memberInfoVo.getMobile());
                        report.setLinkmanName(memberInfoVo.getName());
                    }
                }
            }else{
                SupplierInfoSessionVo supplierInfoSessionVo = FeignResponseUtil.getFeignData(feignSupplierClient.getSuppliserInfoByNumber(report.getPutCustomerNumber()));
                if(Objects.nonNull(supplierInfoSessionVo)){
                    if (Objects.nonNull(supplierInfoSessionVo.getProvinceId())) report.setProvinceName(FeignResponseUtil.getFeignData(feignIceboxQueryClient.selectDistrictNameForReport(supplierInfoSessionVo.getProvinceId())));
                    if (Objects.nonNull(supplierInfoSessionVo.getCityId())) report.setCityName(FeignResponseUtil.getFeignData(feignIceboxQueryClient.selectDistrictNameForReport(supplierInfoSessionVo.getCityId())));
                    if (Objects.nonNull(supplierInfoSessionVo.getRegionId())) report.setDistrictName(FeignResponseUtil.getFeignData(feignIceboxQueryClient.selectDistrictNameForReport(supplierInfoSessionVo.getRegionId())));
                    report.setPutCustomerName(supplierInfoSessionVo.getName());
                    report.setCustomerAddress(supplierInfoSessionVo.getAddress());
                    report.setLinkmanMobile(supplierInfoSessionVo.getLinkManMobile());
                    report.setLinkmanName(supplierInfoSessionVo.getLinkMan());
                    report.setPutCustomerLevel(supplierInfoSessionVo.getLevel());
                }
            }
        }
        report.setVisitType(FeignResponseUtil.getFeignData(feignIceboxQueryClient.selectVisitTypeForReport(report.getPutCustomerNumber())));
        if(Objects.nonNull(report.getExamineUserId())){
            SimpleUserInfoVo exaine = FeignResponseUtil.getFeignData(feignUserClient.findUserById(report.getExamineUserId()));
            if (Objects.nonNull(exaine)){
                report.setExamineUserPosion(exaine.getPosion());
            }
        }
        if(Objects.nonNull(report.getSubmitterId())){
            SimpleUserInfoVo userInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findUserById(report.getSubmitterId()));
            if (userInfoVo != null) {
                report.setSubmitterName(userInfoVo.getRealname());
                report.setSubmitterMobile(userInfoVo.getMobile());
            }
        }
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
        if(StringUtils.isNotEmpty(reportMsg.getApplyNumber())){
            wrapper.eq(IceBoxPutReport::getApplyNumber,reportMsg.getApplyNumber());
        }
        if(StringUtils.isNotEmpty(reportMsg.getSupplierName())){
            wrapper.like(IceBoxPutReport::getSupplierName,reportMsg.getSupplierName());
        }
        if(StringUtils.isNotEmpty(reportMsg.getSupplierNumber())){
            wrapper.like(IceBoxPutReport::getSupplierNumber,reportMsg.getSupplierNumber());
        }
        if(StringUtils.isNotEmpty(reportMsg.getSubmitterName())){
            List<Integer> userIds = FeignResponseUtil.getFeignData(feignUserClient.findUserIdsByUserName(reportMsg.getSubmitterName()));
            if(CollectionUtil.isNotEmpty(userIds)){
                wrapper.in(IceBoxPutReport::getSubmitterId,userIds);
            }else {
                wrapper.eq(IceBoxPutReport::getSubmitterId,"");
            }

        }
        if(reportMsg.getSubmitTime() != null){
            wrapper.ge(IceBoxPutReport::getSubmitTime,reportMsg.getSubmitTime());
        }
        if(reportMsg.getSubmitEndTime() != null){
            wrapper.le(IceBoxPutReport::getSubmitTime,reportMsg.getSubmitEndTime());
        }
        if(reportMsg.getPutCustomerName() != null){
            wrapper.like(IceBoxPutReport::getPutCustomerName,reportMsg.getPutCustomerName());
        }
        if(reportMsg.getPutCustomerNumber() != null){
            wrapper.like(IceBoxPutReport::getPutCustomerNumber,reportMsg.getPutCustomerNumber());
        }
        if(reportMsg.getPutCustomerType() != null){
            wrapper.eq(IceBoxPutReport::getPutCustomerType,reportMsg.getPutCustomerType());
        }
        if(StringUtils.isNotEmpty(reportMsg.getIceBoxAssetId())){
            wrapper.eq(IceBoxPutReport::getIceBoxAssetId,reportMsg.getIceBoxAssetId());
        }
        if(reportMsg.getPutStatus() != null){
            if(PutStatus.DO_PUT.getStatus().equals(reportMsg.getPutStatus())){
                wrapper.and(x -> x.eq(IceBoxPutReport::getPutStatus,PutStatus.LOCK_PUT.getStatus()).or().eq(IceBoxPutReport::getPutStatus,PutStatus.DO_PUT.getStatus()));
            }else {
                wrapper.eq(IceBoxPutReport::getPutStatus,reportMsg.getPutStatus());
            }
        }
        return wrapper;
    }

}