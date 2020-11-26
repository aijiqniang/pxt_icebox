package com.szeastroc.icebox.newprocess.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.enums.CommonStatus;
import com.szeastroc.common.utils.ExecutorServiceFactory;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.commondb.config.redis.JedisClient;
import com.szeastroc.customer.client.FeignStoreClient;
import com.szeastroc.customer.client.FeignSupplierClient;
import com.szeastroc.customer.common.vo.StoreInfoDtoVo;
import com.szeastroc.customer.common.vo.SubordinateInfoVo;
import com.szeastroc.customer.common.vo.SupplierInfoVo;
import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.icebox.constant.RedisConstant;
import com.szeastroc.icebox.newprocess.consumer.common.IceBoxPutReportMsg;
import com.szeastroc.icebox.newprocess.consumer.enums.OperateTypeEnum;
import com.szeastroc.icebox.newprocess.dao.*;
import com.szeastroc.icebox.newprocess.entity.*;
import com.szeastroc.icebox.newprocess.enums.PutStatus;
import com.szeastroc.icebox.newprocess.enums.SupplierTypeEnum;
import com.szeastroc.icebox.newprocess.service.IceBoxPutReportService;
import com.szeastroc.user.client.FeignCacheClient;
import com.szeastroc.user.client.FeignUserClient;
import com.szeastroc.user.common.session.UserManageVo;
import com.szeastroc.user.common.vo.SessionDeptInfoVo;
import com.szeastroc.user.common.vo.SessionUserInfoVo;
import com.szeastroc.user.common.vo.SimpleUserInfoVo;
import com.szeastroc.visit.client.FeignExamineClient;
import com.szeastroc.visit.client.FeignExportRecordsClient;
import com.szeastroc.visit.common.SessionExamineVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class IceBoxPutReportServiceImpl extends ServiceImpl<IceBoxPutReportDao, IceBoxPutReport> implements IceBoxPutReportService {

    @Autowired
    private IceBoxPutReportDao iceBoxPutReportDao;
    @Autowired
    private IceBoxDao iceBoxDao;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private FeignUserClient feignUserClient;
    @Autowired
    private FeignCacheClient feignCacheClient;
    @Autowired
    private JedisClient jedis;
    @Autowired
    private FeignExportRecordsClient feignExportRecordsClient;
    @Autowired
    private PutStoreRelateModelDao putStoreRelateModelDao;
    @Autowired
    private ApplyRelatePutStoreModelDao applyRelatePutStoreModelDao;
    @Autowired
    private IcePutApplyRelateBoxDao icePutApplyRelateBoxDao;
    @Autowired
    private FeignSupplierClient feignSupplierClient;
    @Autowired
    private FeignStoreClient feignStoreClient;
    @Autowired
    private FeignExamineClient feignExamineClient ;

    @Override
    public IPage<IceBoxPutReport> findByPage(IceBoxPutReportMsg reportMsg) {
        LambdaQueryWrapper<IceBoxPutReport> wrapper = fillWrapper(reportMsg);
        IPage<IceBoxPutReport> page = iceBoxPutReportDao.selectPage(reportMsg, wrapper);
        return page;
    }

    @Override
    public CommonResponse<IceBoxPutReport> sendExportMsg(IceBoxPutReportMsg reportMsg) {
        // 获取当前用户相关信息
        UserManageVo userManageVo = FeignResponseUtil.getFeignData(feignUserClient.getSessionUserInfo());
        String key = String.format("%s%s", RedisConstant.ICE_BOX_PUT_REPORT_EXPORT_KEY, userManageVo.getSessionUserInfoVo().getId());
//        if (null != jedis.get(key)) {
//            return new CommonResponse<>(Constants.API_CODE_FAIL, "请求导出操作频繁，请稍候操作");
//        }
        LambdaQueryWrapper<IceBoxPutReport> wrapper = fillWrapper(reportMsg);
        Integer count = Optional.ofNullable(iceBoxPutReportDao.selectByExportCount(wrapper)).orElse(0);
        if (0 == count) {
            return new CommonResponse<>(Constants.API_CODE_FAIL, "暂无可下载数据");
        }
        // 生成下载任务
        Integer recordsId = FeignResponseUtil.getFeignData(feignExportRecordsClient.createExportRecords(userManageVo.getSessionUserInfoVo().getId(),
                userManageVo.getSessionUserInfoVo().getRealname(), JSON.toJSONString(reportMsg), "冰柜投放信息-导出"));

        //发送mq消息,同步申请数据到报表
        CompletableFuture.runAsync(() -> {
            reportMsg.setOperateType(OperateTypeEnum.SELECT.getType());
            reportMsg.setRecordsId(recordsId);
            reportMsg.setOperateName(userManageVo.getSessionUserInfoVo().getRealname());
            rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.iceboxReportKey, reportMsg);
        }, ExecutorServiceFactory.getInstance());
        // 三分钟间隔
        jedis.set(key, "ex", 300, TimeUnit.SECONDS);

        return new CommonResponse<>(Constants.API_CODE_SUCCESS,null);
    }

    @Override
    public Integer selectByExportCount(LambdaQueryWrapper<IceBoxPutReport> wrapper) {
        return iceBoxPutReportDao.selectByExportCount(wrapper);
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

    @Override
    public void dealHistoryData() {
        List<PutStoreRelateModel> relateModelList = putStoreRelateModelDao.selectList(Wrappers.<PutStoreRelateModel>lambdaQuery().last("limit 100"));
        if(CollectionUtil.isEmpty(relateModelList)){
            return;
        }
        for(PutStoreRelateModel relateModel:relateModelList){
            ApplyRelatePutStoreModel applyRelatePutStoreModel = applyRelatePutStoreModelDao.selectOne(Wrappers.<ApplyRelatePutStoreModel>lambdaQuery().eq(ApplyRelatePutStoreModel::getStoreRelateModelId, relateModel.getId()));
            if(applyRelatePutStoreModel == null){
                continue;
            }
            IceBox iceBox = iceBoxDao.selectOne(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getModelId, relateModel.getModelId()).eq(IceBox::getSupplierId, relateModel.getSupplierId()).last("limit 1"));
            if(iceBox == null){
                continue;
            }
            IceBoxPutReport report = new IceBoxPutReport();
            Map<Integer, SessionDeptInfoVo> deptInfoVoMap = FeignResponseUtil.getFeignData(feignCacheClient.getFiveLevelDept(iceBox.getDeptId()));
            SessionDeptInfoVo group = deptInfoVoMap.get(1);
            if (group != null) {
                report.setGroupDeptId(group.getId());
                report.setGroupDeptName(group.getName());
            }

            SessionDeptInfoVo service = deptInfoVoMap.get(2);
            if (service != null) {
                report.setServiceDeptId(service.getId());
                report.setServiceDeptName(service.getName());
            }

            SessionDeptInfoVo region = deptInfoVoMap.get(3);
            if (region != null) {
                report.setRegionDeptId(region.getId());
                report.setRegionDeptName(region.getName());
            }

            SessionDeptInfoVo business = deptInfoVoMap.get(4);
            if (business != null) {
                report.setBusinessDeptId(business.getId());
                report.setBusinessDeptName(business.getName());
            }

            SessionDeptInfoVo headquarters = deptInfoVoMap.get(5);
            if (headquarters != null) {
                report.setHeadquartersDeptId(headquarters.getId());
                report.setHeadquartersDeptName(headquarters.getName());
            }

            report.setApplyNumber(applyRelatePutStoreModel.getApplyNumber());
            report.setDepositMoney(iceBox.getDepositMoney());
            report.setFreeType(applyRelatePutStoreModel.getFreeType());
            report.setIceBoxModelId(iceBox.getModelId());
            report.setIceBoxModelName(iceBox.getModelName());
            SubordinateInfoVo supplier = FeignResponseUtil.getFeignData(feignSupplierClient.findSupplierBySupplierId(relateModel.getSupplierId()));
            report.setSupplierId(relateModel.getSupplierId());
            if (supplier != null) {
                report.setSupplierNumber(supplier.getNumber());
                report.setSupplierName(supplier.getName());
            }
            report.setPutCustomerNumber(relateModel.getPutStoreNumber());


            report.setPutCustomerType(SupplierTypeEnum.IS_STORE.getType());
            if(StringUtils.isNotEmpty(relateModel.getPutStoreNumber()) && !relateModel.getPutStoreNumber().startsWith("C0")){
                SubordinateInfoVo putSupplier = FeignResponseUtil.getFeignData(feignSupplierClient.findSupplierBySupplierId(relateModel.getSupplierId()));
                if(putSupplier != null){
                    report.setPutCustomerType(putSupplier.getSupplierType());
                    report.setPutCustomerName(putSupplier.getName());
                }
            }else {
                StoreInfoDtoVo putStore = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(relateModel.getPutStoreNumber()));
                if(putStore != null){
                    report.setPutCustomerName(putStore.getStoreName());
                }
            }

            report.setPutStatus(relateModel.getPutStatus());
            report.setExamineUserId(relateModel.getUpdateBy());
            report.setExamineUserName(relateModel.getUpdateByName());
            report.setExamineTime(relateModel.getUpdateTime());
            SimpleUserInfoVo userInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findUserById(relateModel.getCreateBy()));
            report.setSubmitterId(relateModel.getCreateBy());
            if (userInfoVo != null) {
                report.setSubmitterName(userInfoVo.getRealname());
            }
            report.setSubmitTime(new Date());
            if(PutStatus.FINISH_PUT.getStatus().equals(relateModel.getPutStatus())){
                List<IcePutApplyRelateBox> icePutApplyRelateBoxes = icePutApplyRelateBoxDao.selectList(Wrappers.<IcePutApplyRelateBox>lambdaQuery().eq(IcePutApplyRelateBox::getApplyNumber, applyRelatePutStoreModel.getApplyNumber()));
                if(CollectionUtil.isNotEmpty(icePutApplyRelateBoxes)){
                    for(IcePutApplyRelateBox relateBox:icePutApplyRelateBoxes){
                        IceBox box = iceBoxDao.selectById(relateBox.getBoxId());
                        if(box == null){
                            continue;
                        }
                        IceBoxPutReport putReport = iceBoxPutReportDao.selectOne(Wrappers.<IceBoxPutReport>lambdaQuery().eq(IceBoxPutReport::getIceBoxAssetId, box.getAssetId())
                                .eq(IceBoxPutReport::getApplyNumber, relateBox.getApplyNumber()));
                        if(putReport == null){
                            report.setIceBoxAssetId(box.getAssetId());
                            break;
                        }
                    }
                }
            }
            IceBoxPutReport putReport = iceBoxPutReportDao.selectOne(Wrappers.<IceBoxPutReport>lambdaQuery().eq(IceBoxPutReport::getIceBoxAssetId, report.getIceBoxAssetId())
                    .eq(IceBoxPutReport::getApplyNumber, report.getApplyNumber()));
            if(putReport == null){
                iceBoxPutReportDao.insert(report);
            }
        }
    }

    @Override
    public void syncPutDataToReport(List<Integer> ids) {
        List<PutStoreRelateModel> relateModelList = new ArrayList<>();
        if(CollectionUtil.isNotEmpty(ids)){
            List<PutStoreRelateModel> valids = putStoreRelateModelDao.selectList(Wrappers.<PutStoreRelateModel>lambdaQuery().ne(PutStoreRelateModel::getPutStatus,PutStatus.NO_PUT.getStatus()).eq(PutStoreRelateModel::getStatus, CommonStatus.VALID.getStatus()).in(PutStoreRelateModel::getId, ids));
            List<PutStoreRelateModel> inValids = putStoreRelateModelDao.selectList(Wrappers.<PutStoreRelateModel>lambdaQuery().eq(PutStoreRelateModel::getStatus, CommonStatus.INVALID.getStatus()).in(PutStoreRelateModel::getId, ids));
            if(CollectionUtil.isNotEmpty(valids)){
                relateModelList.addAll(valids);
            }
            if(CollectionUtil.isNotEmpty(inValids)){
                relateModelList.addAll(inValids);
            }
        }else {
            List<PutStoreRelateModel> valids = putStoreRelateModelDao.selectList(Wrappers.<PutStoreRelateModel>lambdaQuery().ne(PutStoreRelateModel::getPutStatus,PutStatus.NO_PUT.getStatus()).eq(PutStoreRelateModel::getStatus, CommonStatus.VALID.getStatus()));
            List<PutStoreRelateModel> inValids = putStoreRelateModelDao.selectList(Wrappers.<PutStoreRelateModel>lambdaQuery().eq(PutStoreRelateModel::getStatus, CommonStatus.INVALID.getStatus()));
            if(CollectionUtil.isNotEmpty(valids)){
                relateModelList.addAll(valids);
            }
            if(CollectionUtil.isNotEmpty(inValids)){
                relateModelList.addAll(inValids);
            }
        }
        if(CollectionUtil.isEmpty(relateModelList)){
            return;
        }
        for(PutStoreRelateModel relateModel:relateModelList){
            IceBoxPutReport putReport = new IceBoxPutReport();
            ApplyRelatePutStoreModel storeModel = applyRelatePutStoreModelDao.selectOne(Wrappers.<ApplyRelatePutStoreModel>lambdaQuery().eq(ApplyRelatePutStoreModel::getStoreRelateModelId, relateModel.getId()));
            if(storeModel == null){
                continue;
            }
            putReport.setApplyNumber(storeModel.getApplyNumber());
            putReport.setPutStatus(relateModel.getPutStatus());
            if(CommonStatus.INVALID.getStatus().equals(relateModel.getStatus())){
                putReport.setPutStatus(PutStatus.IS_CANCEL.getStatus());
            }
            if(PutStatus.FINISH_PUT.getStatus().equals(relateModel.getPutStatus())){
                List<IcePutApplyRelateBox> icePutApplyRelateBoxes = icePutApplyRelateBoxDao.selectList(Wrappers.<IcePutApplyRelateBox>lambdaQuery().eq(IcePutApplyRelateBox::getApplyNumber, storeModel.getApplyNumber()));
                if(CollectionUtil.isNotEmpty(icePutApplyRelateBoxes)){
                    List<Integer> iceboxIds = icePutApplyRelateBoxes.stream().map(x -> x.getBoxId()).collect(Collectors.toList());
                    List<IceBox> iceBoxList = iceBoxDao.selectBatchIds(iceboxIds);
                    if(CollectionUtil.isNotEmpty(iceBoxList)){
                        for(IceBox iceBox:iceBoxList){
                            if(PutStatus.FINISH_PUT.getStatus().equals(iceBox.getPutStatus()) && iceBox.getModelId().equals(relateModel.getModelId())
                                    && iceBox.getSupplierId().equals(relateModel.getSupplierId())){
                                IceBoxPutReport isExistPutReport = iceBoxPutReportDao.selectOne(Wrappers.<IceBoxPutReport>lambdaQuery().eq(IceBoxPutReport::getIceBoxAssetId, iceBox.getAssetId())
                                        .eq(IceBoxPutReport::getApplyNumber, storeModel.getApplyNumber()));
                                if(isExistPutReport == null){
                                    putReport.setIceBoxAssetId(iceBox.getAssetId());
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            putReport.setSubmitterId(relateModel.getCreateBy());
            putReport.setSubmitTime(relateModel.getCreateTime());
            if(relateModel.getCreateBy() != null){
                SessionUserInfoVo userInfoVo = FeignResponseUtil.getFeignData(feignCacheClient.getForUserInfoVo(relateModel.getCreateBy()));
                if(userInfoVo != null){
                    putReport.setSubmitterName(userInfoVo.getRealname());
                }
            }
            putReport.setPutCustomerNumber(relateModel.getPutStoreNumber());
            Integer headquartersDeptId = null;

            String headquartersDeptName = null;

            Integer businessDeptId = null;

            String businessDeptName = null;

            Integer regionDeptId = null;

            String regionDeptName = null;

            Integer serviceDeptId = null;

            String serviceDeptName = null;

            Integer groupDeptId = null;

            String groupDeptName = null;

            if(relateModel.getPutStoreNumber().startsWith("C0")){
                putReport.setPutCustomerType(SupplierTypeEnum.IS_STORE.getType());
                StoreInfoDtoVo storeInfo = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(relateModel.getPutStoreNumber()));
                if(storeInfo != null){
                    putReport.setPutCustomerName(storeInfo.getStoreName());
                    headquartersDeptId = storeInfo.getHeadquartersDeptId();
                    headquartersDeptName = storeInfo.getHeadquartersDeptName();
                    businessDeptId = storeInfo.getBusinessDeptId();
                    businessDeptName = storeInfo.getBusinessDeptName();
                    regionDeptId = storeInfo.getRegionDeptId();
                    regionDeptName = storeInfo.getRegionDeptName();
                    serviceDeptId = storeInfo.getServiceDeptId();
                    serviceDeptName = storeInfo.getServiceDeptName();
                    groupDeptId = storeInfo.getGroupDeptId();
                    groupDeptName = storeInfo.getGroupDeptName();
                }
            }else {
                SupplierInfoVo supplierInfo = FeignResponseUtil.getFeignData(feignSupplierClient.getSuppliserInfoByNumber(relateModel.getPutStoreNumber()));
                if(supplierInfo != null){
                    putReport.setPutCustomerName(supplierInfo.getName());
                    putReport.setPutCustomerType(supplierInfo.getSupplierType());
                    headquartersDeptId = supplierInfo.getHeadquartersDeptId();
                    headquartersDeptName = supplierInfo.getHeadquartersDeptName();
                    businessDeptId = supplierInfo.getBusinessDeptId();
                    businessDeptName = supplierInfo.getBusinessDeptName();
                    regionDeptId = supplierInfo.getRegionDeptId();
                    regionDeptName = supplierInfo.getRegionDeptName();
                    serviceDeptId = supplierInfo.getServiceDeptId();
                    serviceDeptName = supplierInfo.getServiceDeptName();
                    groupDeptId = supplierInfo.getGroupDeptId();
                    groupDeptName = supplierInfo.getGroupDeptName();
                }
            }
            putReport.setHeadquartersDeptId(headquartersDeptId);
            putReport.setHeadquartersDeptName(headquartersDeptName);
            putReport.setBusinessDeptId(businessDeptId);
            putReport.setBusinessDeptName(businessDeptName);
            putReport.setRegionDeptId(regionDeptId);
            putReport.setRegionDeptName(regionDeptName);
            putReport.setServiceDeptId(serviceDeptId);
            putReport.setServiceDeptName(serviceDeptName);
            putReport.setGroupDeptId(groupDeptId);
            putReport.setGroupDeptName(groupDeptName);

            putReport.setSupplierId(relateModel.getSupplierId());
            SubordinateInfoVo supplier = FeignResponseUtil.getFeignData(feignSupplierClient.findSupplierBySupplierId(relateModel.getSupplierId()));
            if(supplier != null){
                putReport.setSupplierNumber(supplier.getNumber());
                putReport.setSupplierName(supplier.getName());
            }
            putReport.setFreeType(storeModel.getFreeType());
            putReport.setIceBoxModelId(relateModel.getModelId());
            IceBox iceBox = iceBoxDao.selectOne(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getModelId, relateModel.getModelId()).eq(IceBox::getSupplierId, relateModel.getSupplierId()).last("limit 1"));
            if(iceBox != null){
                putReport.setIceBoxModelName(iceBox.getModelName());
                putReport.setDepositMoney(iceBox.getDepositMoney());
            }
            List<SessionExamineVo.VisitExamineNodeVo> visitExamineNodeVos = FeignResponseUtil.getFeignData(feignExamineClient.getExamineNodesByRelateCode(storeModel.getApplyNumber()));
            if(CollectionUtil.isNotEmpty(visitExamineNodeVos)){
                for(SessionExamineVo.VisitExamineNodeVo examineNodeVo:visitExamineNodeVos){
                    if(examineNodeVo.getExamineStatus().equals(1)){
                        putReport.setExamineUserId(examineNodeVo.getUserId());
                        SimpleUserInfoVo userInfo = FeignResponseUtil.getFeignData(feignUserClient.findSimpleUserById(examineNodeVo.getUserId()));
                        if(userInfo != null){
                            putReport.setExamineUserName(userInfo.getRealname());
                        }
                        putReport.setExamineTime(examineNodeVo.getUpdateTime());
                    }
                }
            }
            iceBoxPutReportDao.insert(putReport);
        }
    }
}

