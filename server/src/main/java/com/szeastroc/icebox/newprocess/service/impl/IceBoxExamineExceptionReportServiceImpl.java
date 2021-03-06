package com.szeastroc.icebox.newprocess.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.entity.customer.vo.StoreInfoDtoVo;
import com.szeastroc.common.entity.user.session.UserManageVo;
import com.szeastroc.common.entity.user.vo.SimpleUserInfoVo;
import com.szeastroc.common.feign.customer.FeignStoreClient;
import com.szeastroc.common.feign.user.FeignUserClient;
import com.szeastroc.common.feign.visit.FeignExportRecordsClient;
import com.szeastroc.common.utils.ExecutorServiceFactory;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.commondb.config.redis.JedisClient;
import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.icebox.constant.RedisConstant;
import com.szeastroc.icebox.newprocess.consumer.common.IceBoxExamineExceptionReportMsg;
import com.szeastroc.icebox.newprocess.consumer.enums.OperateTypeEnum;
import com.szeastroc.icebox.newprocess.dao.IceBoxDao;
import com.szeastroc.icebox.newprocess.dao.IceBoxExamineExceptionReportDao;
import com.szeastroc.icebox.newprocess.dao.IceExamineDao;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.entity.IceBoxExamineExceptionReport;
import com.szeastroc.icebox.newprocess.entity.IceExamine;
import com.szeastroc.icebox.newprocess.enums.IceBoxEnums;
import com.szeastroc.icebox.newprocess.enums.IceBoxReprotTypeEnum;
import com.szeastroc.icebox.newprocess.enums.PutStatus;
import com.szeastroc.icebox.newprocess.service.IceBoxExamineExceptionReportService;
import com.szeastroc.icebox.newprocess.vo.IceBoxExamineVo;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class IceBoxExamineExceptionReportServiceImpl extends ServiceImpl<IceBoxExamineExceptionReportDao, IceBoxExamineExceptionReport> implements IceBoxExamineExceptionReportService {

    @Autowired
    private IceBoxExamineExceptionReportDao iceBoxExamineExceptionReportDao;
    @Autowired
    private IceBoxDao iceBoxDao;
    @Autowired
    private IceExamineDao iceExamineDao;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private FeignUserClient feignUserClient;
    @Autowired
    private JedisClient jedis;
    @Autowired
    private FeignExportRecordsClient feignExportRecordsClient;
    @Autowired
    private FeignStoreClient feignStoreClient;

    @Override
    public IPage<IceBoxExamineExceptionReport> findByPage(IceBoxExamineExceptionReportMsg reportMsg) {
        LambdaQueryWrapper<IceBoxExamineExceptionReport> wrapper = fillWrapper(reportMsg);
        IPage<IceBoxExamineExceptionReport> page = iceBoxExamineExceptionReportDao.selectPage(reportMsg, wrapper);
        page.convert(iceBoxExamineExceptionReport -> {
            if(iceBoxExamineExceptionReport != null && StringUtils.isNotEmpty(iceBoxExamineExceptionReport.getPutCustomerNumber())){
                StoreInfoDtoVo storeInfoDtoVo = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(iceBoxExamineExceptionReport.getPutCustomerNumber()));
                if(storeInfoDtoVo != null && storeInfoDtoVo.getMerchantNumber() != null){
                    iceBoxExamineExceptionReport.setMerchantNumber(storeInfoDtoVo.getMerchantNumber());
                }
            }
            return iceBoxExamineExceptionReport;
        });
        return page;
    }

    @Override
    public CommonResponse<IceBoxExamineExceptionReport> sendExportMsg(IceBoxExamineExceptionReportMsg reportMsg) {
        // ??????????????????????????????
        UserManageVo userManageVo = FeignResponseUtil.getFeignData(feignUserClient.getSessionUserInfo());
        String key = String.format("%s%s", RedisConstant.ICE_BOX_EXCEPTION_REPORT_EXPORT_KEY, userManageVo.getSessionUserInfoVo().getId());
        if (null != jedis.get(key)) {
            return new CommonResponse<>(Constants.API_CODE_FAIL, "??????????????????????????????????????????");
        }
        LambdaQueryWrapper<IceBoxExamineExceptionReport> wrapper = fillWrapper(reportMsg);
        Integer count = Optional.ofNullable(iceBoxExamineExceptionReportDao.selectByExportCount(wrapper)).orElse(0);
        if (0 == count) {
            return new CommonResponse<>(Constants.API_CODE_FAIL, "?????????????????????");
        }
        // ??????????????????
        Integer recordsId = FeignResponseUtil.getFeignData(feignExportRecordsClient.createExportRecords(userManageVo.getSessionUserInfoVo().getId(),
                userManageVo.getSessionUserInfoVo().getRealname(), JSON.toJSONString(reportMsg), "????????????????????????-??????"));

        //??????mq??????,???????????????????????????
        CompletableFuture.runAsync(() -> {
            reportMsg.setOperateType(OperateTypeEnum.SELECT.getType());
            reportMsg.setRecordsId(recordsId);
            reportMsg.setOperateName(userManageVo.getSessionUserInfoVo().getRealname());
            reportMsg.setReportType(IceBoxReprotTypeEnum.EXCEPTION.getType());
            rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.iceboxExceptionReportKey, reportMsg);
        }, ExecutorServiceFactory.getInstance());
        // ???????????????
        jedis.set(key, "ex", 300, TimeUnit.SECONDS);

        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }

    @Override
    public Integer selectByExportCount(LambdaQueryWrapper<IceBoxExamineExceptionReport> wrapper) {
        return iceBoxExamineExceptionReportDao.selectByExportCount(wrapper);
    }

    private LambdaQueryWrapper<IceBoxExamineExceptionReport> fillWrapper(IceBoxExamineExceptionReportMsg reportMsg) {
        LambdaQueryWrapper<IceBoxExamineExceptionReport> wrapper = Wrappers.<IceBoxExamineExceptionReport>lambdaQuery();
        wrapper.ne(IceBoxExamineExceptionReport::getToOaType,IceBoxEnums.StatusEnum.NORMAL.getType());
        if (reportMsg.getGroupDeptId() != null) {
            wrapper.eq(IceBoxExamineExceptionReport::getGroupDeptId, reportMsg.getGroupDeptId());
        }
        if (reportMsg.getServiceDeptId() != null) {
            wrapper.eq(IceBoxExamineExceptionReport::getServiceDeptId, reportMsg.getServiceDeptId());
        }
        if (reportMsg.getRegionDeptId() != null) {
            wrapper.eq(IceBoxExamineExceptionReport::getRegionDeptId, reportMsg.getRegionDeptId());
        }
        if (reportMsg.getBusinessDeptId() != null) {
            wrapper.eq(IceBoxExamineExceptionReport::getBusinessDeptId, reportMsg.getBusinessDeptId());
        }
        if (reportMsg.getHeadquartersDeptId() != null) {
            wrapper.eq(IceBoxExamineExceptionReport::getHeadquartersDeptId, reportMsg.getHeadquartersDeptId());
        }
        if (StringUtils.isNotEmpty(reportMsg.getExamineNumber())) {
            wrapper.like(IceBoxExamineExceptionReport::getExamineNumber, reportMsg.getExamineNumber());
        }
        if (StringUtils.isNotEmpty(reportMsg.getSupplierName())) {
            wrapper.like(IceBoxExamineExceptionReport::getSupplierName, reportMsg.getSupplierName());
        }
        if (StringUtils.isNotEmpty(reportMsg.getSupplierNumber())) {
            wrapper.like(IceBoxExamineExceptionReport::getSupplierNumber, reportMsg.getSupplierNumber());
        }
        if (StringUtils.isNotEmpty(reportMsg.getSubmitterName())) {
            List<Integer> userIds = FeignResponseUtil.getFeignData(feignUserClient.findUserIdsByUserName(reportMsg.getSubmitterName()));
            if (CollectionUtil.isNotEmpty(userIds)) {
                wrapper.in(IceBoxExamineExceptionReport::getSubmitterId, userIds);
            } else {
                wrapper.eq(IceBoxExamineExceptionReport::getSubmitterId, "");
            }

        }
        if (reportMsg.getSubmitTime() != null) {
            wrapper.ge(IceBoxExamineExceptionReport::getSubmitTime, reportMsg.getSubmitTime());
        }
        if (reportMsg.getSubmitEndTime() != null) {
            wrapper.le(IceBoxExamineExceptionReport::getSubmitTime, reportMsg.getSubmitEndTime());
        }
        if (reportMsg.getToOaTime() != null) {
            wrapper.ge(IceBoxExamineExceptionReport::getToOaTime, reportMsg.getToOaTime());
        }
        if (reportMsg.getToOaEndTime() != null) {
            wrapper.le(IceBoxExamineExceptionReport::getToOaTime, reportMsg.getToOaEndTime());
        }
        if (reportMsg.getPutCustomerName() != null) {
            wrapper.like(IceBoxExamineExceptionReport::getPutCustomerName, reportMsg.getPutCustomerName());
        }
        if (reportMsg.getPutCustomerNumber() != null) {
            wrapper.like(IceBoxExamineExceptionReport::getPutCustomerNumber, reportMsg.getPutCustomerNumber());
        }
        if (reportMsg.getPutCustomerType() != null) {
            wrapper.eq(IceBoxExamineExceptionReport::getPutCustomerType, reportMsg.getPutCustomerType());
        }
        if (StringUtils.isNotEmpty(reportMsg.getIceBoxAssetId())) {
            wrapper.eq(IceBoxExamineExceptionReport::getIceBoxAssetId, reportMsg.getIceBoxAssetId());
        }
        if (reportMsg.getStatus() != null) {
            wrapper.eq(IceBoxExamineExceptionReport::getStatus, reportMsg.getStatus());
        }
        if(reportMsg.getToOaType() != null){
            wrapper.eq(IceBoxExamineExceptionReport::getToOaType,reportMsg.getToOaType());
        }
        if(StringUtils.isNotEmpty(reportMsg.getToOaNumber())){
            wrapper.eq(IceBoxExamineExceptionReport::getToOaNumber,reportMsg.getToOaNumber());
        }

        return wrapper;
    }

    @Override
    public IPage<IceBoxExamineVo> findIceExamineByPage(IceBoxExamineExceptionReportMsg reportMsg) {
        LambdaQueryWrapper<IceBoxExamineExceptionReport> wrapper = fillExamineWrapper(reportMsg);
        IPage<IceBoxExamineExceptionReport> page = iceBoxExamineExceptionReportDao.selectPage(reportMsg, wrapper);
        IPage<IceBoxExamineVo> examineVoIPage = page.convert(report -> {
            IceBoxExamineVo examineVo = new IceBoxExamineVo();
            BeanUtils.copyProperties(report, examineVo);
            IceExamine iceExamine = iceExamineDao.selectOne(Wrappers.<IceExamine>lambdaQuery().eq(IceExamine::getExamineNumber, report.getExamineNumber()));
            if (iceExamine != null) {
//                String displayImage = StringUtils.isEmpty(iceExamine.getDisplayImage())?"":iceExamine.getDisplayImage().replace("http","https");
//                String exteriorImage = StringUtils.isEmpty(iceExamine.getExteriorImage())?"":iceExamine.getExteriorImage().replace("http","https");
                examineVo.setDisplayImage(iceExamine.getDisplayImage());
                examineVo.setExteriorImage(iceExamine.getExteriorImage());
                examineVo.setAssetImage(iceExamine.getAssetImage());
                examineVo.setExaminMsg(iceExamine.getExaminMsg());
                examineVo.setStatusStr(IceBoxEnums.StatusEnum.getDesc(iceExamine.getIceStatus()));
                if(StringUtils.isNotEmpty(examineVo.getPutCustomerNumber())){
                    StoreInfoDtoVo storeInfoDtoVo = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(examineVo.getPutCustomerNumber()));
                    if(storeInfoDtoVo != null && StringUtils.isNotEmpty(storeInfoDtoVo.getMerchantNumber())){
                        examineVo.setMerchantNumber(storeInfoDtoVo.getMerchantNumber());
                    }
                }
            }
//            IceBox iceBox = iceBoxDao.selectOne(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getAssetId, report.getIceBoxAssetId()));
//            if(iceBox != null){
//                examineVo.setStatusStr(IceBoxEnums.StatusEnum.getDesc(iceBox.getStatus()));
//            }
            return examineVo;
        });
        return examineVoIPage;
    }

    @Override
    public CommonResponse<IceBoxExamineExceptionReport> sendIceExamineExportMsg(IceBoxExamineExceptionReportMsg reportMsg) {
        // ??????????????????????????????
        UserManageVo userManageVo = FeignResponseUtil.getFeignData(feignUserClient.getSessionUserInfo());
        String key = String.format("%s%s", RedisConstant.ICE_BOX_EXAMINE_REPORT_EXPORT_KEY, userManageVo.getSessionUserInfoVo().getId());
        if (null != jedis.get(key)) {
            return new CommonResponse<>(Constants.API_CODE_FAIL, "??????????????????????????????????????????");
        }
        LambdaQueryWrapper<IceBoxExamineExceptionReport> wrapper = fillExamineWrapper(reportMsg);
        Integer count = Optional.ofNullable(iceBoxExamineExceptionReportDao.selectByExportCount(wrapper)).orElse(0);
        if (0 == count) {
            return new CommonResponse<>(Constants.API_CODE_FAIL, "?????????????????????");
        }
        // ??????????????????
        Integer recordsId = FeignResponseUtil.getFeignData(feignExportRecordsClient.createExportRecords(userManageVo.getSessionUserInfoVo().getId(),
                userManageVo.getSessionUserInfoVo().getRealname(), JSON.toJSONString(reportMsg), "??????????????????-??????"));

        //??????mq??????,???????????????????????????
        CompletableFuture.runAsync(() -> {
            reportMsg.setOperateType(OperateTypeEnum.SELECT.getType());
            reportMsg.setRecordsId(recordsId);
            reportMsg.setOperateName(userManageVo.getSessionUserInfoVo().getRealname());
            reportMsg.setReportType(IceBoxReprotTypeEnum.EXAMINE.getType());
            rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.iceboxExceptionReportKey, reportMsg);
        }, ExecutorServiceFactory.getInstance());
        // ???????????????
        jedis.set(key, "ex", 300, TimeUnit.SECONDS);

        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }

    @Override
    public void updateExamineUserOfficeName() {
        List<IceBoxExamineExceptionReport> exceptionReportList = iceBoxExamineExceptionReportDao.selectList(Wrappers.<IceBoxExamineExceptionReport>lambdaQuery()
                .isNotNull(IceBoxExamineExceptionReport::getExamineUserId)
                .isNull(IceBoxExamineExceptionReport::getExamineUserOfficeName)
        );
        if (CollectionUtils.isEmpty(exceptionReportList)) {
            return;
        }
        for (IceBoxExamineExceptionReport exceptionReport:exceptionReportList){
            SimpleUserInfoVo userInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findUserById(exceptionReport.getExamineUserId()));
            if(userInfoVo==null){
                continue;
            }
            exceptionReport.setExamineUserOfficeName(userInfoVo.getPosion());
            iceBoxExamineExceptionReportDao.updateById(exceptionReport);
        }
    }

    @Override
    public void updateIceboxImportTime() {
        List<IceBoxExamineExceptionReport> iceBoxExamineExceptionReports = iceBoxExamineExceptionReportDao.selectList(Wrappers.<IceBoxExamineExceptionReport>lambdaQuery());
        if(iceBoxExamineExceptionReports.size()>0){
            for(IceBoxExamineExceptionReport report : iceBoxExamineExceptionReports){
                if(report.getIceBoxImportTime() == null){
                    IceBox iceBox = iceBoxDao.selectOne(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getAssetId, report.getIceBoxAssetId()).last("limit 1"));
                    if(iceBox != null && iceBox.getCreatedTime() !=null){
                        report.setIceBoxImportTime(iceBox.getCreatedTime());
                        iceBoxExamineExceptionReportDao.updateById(report);
                    }else{
                        IceBox oldIceBox = iceBoxDao.selectOne(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getOldAssetId, report.getIceBoxAssetId()).last("limit 1"));
                        if(oldIceBox != null && oldIceBox.getCreatedTime() !=null){
                            report.setIceBoxImportTime(oldIceBox.getCreatedTime());
                            iceBoxExamineExceptionReportDao.updateById(report);
                        }
                    }
                }
            }
        }
    }

    private LambdaQueryWrapper<IceBoxExamineExceptionReport> fillExamineWrapper(IceBoxExamineExceptionReportMsg reportMsg) {
        LambdaQueryWrapper<IceBoxExamineExceptionReport> wrapper = Wrappers.<IceBoxExamineExceptionReport>lambdaQuery();
        if (reportMsg.getGroupDeptId() != null) {
            wrapper.eq(IceBoxExamineExceptionReport::getGroupDeptId, reportMsg.getGroupDeptId());
        }
        if (reportMsg.getServiceDeptId() != null) {
            wrapper.eq(IceBoxExamineExceptionReport::getServiceDeptId, reportMsg.getServiceDeptId());
        }
        if (reportMsg.getRegionDeptId() != null) {
            wrapper.eq(IceBoxExamineExceptionReport::getRegionDeptId, reportMsg.getRegionDeptId());
        }
        if (reportMsg.getBusinessDeptId() != null) {
            wrapper.eq(IceBoxExamineExceptionReport::getBusinessDeptId, reportMsg.getBusinessDeptId());
        }
        if (reportMsg.getHeadquartersDeptId() != null) {
            wrapper.eq(IceBoxExamineExceptionReport::getHeadquartersDeptId, reportMsg.getHeadquartersDeptId());
        }
        if (StringUtils.isNotEmpty(reportMsg.getSupplierName())) {
            wrapper.like(IceBoxExamineExceptionReport::getSupplierName, reportMsg.getSupplierName());
        }
        if (StringUtils.isNotEmpty(reportMsg.getSupplierNumber())) {
            wrapper.like(IceBoxExamineExceptionReport::getSupplierNumber, reportMsg.getSupplierNumber());
        }
        if (StringUtils.isNotEmpty(reportMsg.getSubmitterName())) {
            List<Integer> userIds = FeignResponseUtil.getFeignData(feignUserClient.findUserIdsByUserName(reportMsg.getSubmitterName()));
            if (CollectionUtil.isNotEmpty(userIds)) {
                wrapper.in(IceBoxExamineExceptionReport::getSubmitterId, userIds);
            } else {
                wrapper.eq(IceBoxExamineExceptionReport::getSubmitterId, "");
            }
        }
        if (reportMsg.getSubmitTime() != null) {
            wrapper.ge(IceBoxExamineExceptionReport::getSubmitTime, reportMsg.getSubmitTime());
        }
        if (reportMsg.getSubmitEndTime() != null) {
            wrapper.le(IceBoxExamineExceptionReport::getSubmitTime, reportMsg.getSubmitEndTime());
        }
        if (reportMsg.getPutCustomerName() != null) {
            wrapper.like(IceBoxExamineExceptionReport::getPutCustomerName, reportMsg.getPutCustomerName());
        }
        if (reportMsg.getPutCustomerNumber() != null) {
            wrapper.like(IceBoxExamineExceptionReport::getPutCustomerNumber, reportMsg.getPutCustomerNumber());
        }

        if (StringUtils.isNotEmpty(reportMsg.getIceBoxAssetId())) {
            wrapper.eq(IceBoxExamineExceptionReport::getIceBoxAssetId, reportMsg.getIceBoxAssetId());
        }
        if (reportMsg.getStatus() != null) {
            List<IceBox> iceBoxList = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getStatus, reportMsg.getStatus()));
            if (CollectionUtil.isNotEmpty(iceBoxList)) {
                List<String> assetIds = iceBoxList.stream().map(x -> x.getAssetId()).collect(Collectors.toList());
                wrapper.in(IceBoxExamineExceptionReport::getIceBoxAssetId, assetIds);
            } else {
                wrapper.eq(IceBoxExamineExceptionReport::getIceBoxAssetId, "");
            }

        }
        if(reportMsg.getIceBoxImportStartTime() != null){
            wrapper.ge(IceBoxExamineExceptionReport::getIceBoxImportTime,reportMsg.getIceBoxImportStartTime());
        }
        if(reportMsg.getIceBoxImportEndTime() != null){
            wrapper.le(IceBoxExamineExceptionReport::getIceBoxImportTime,reportMsg.getIceBoxImportEndTime());
        }
        if(StringUtils.isNotEmpty(reportMsg.getIceBoxAssetId())){
            wrapper.eq(IceBoxExamineExceptionReport::getIceBoxAssetId,reportMsg.getIceBoxAssetId());
        }
        return wrapper;
    }
}

