package com.szeastroc.icebox.newprocess.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.entity.customer.vo.*;
import com.szeastroc.common.entity.icebox.vo.IceBoxRequest;
import com.szeastroc.common.entity.transfer.enums.ResourceTypeEnum;
import com.szeastroc.common.entity.transfer.enums.WechatPayTypeEnum;
import com.szeastroc.common.entity.transfer.request.TransferRequest;
import com.szeastroc.common.entity.transfer.response.TransferReponse;
import com.szeastroc.common.entity.user.session.UserManageVo;
import com.szeastroc.common.entity.user.vo.SessionDeptInfoVo;
import com.szeastroc.common.entity.user.vo.SessionUserInfoVo;
import com.szeastroc.common.entity.user.vo.SimpleUserInfoVo;
import com.szeastroc.common.entity.visit.NoticeBacklogRequestVo;
import com.szeastroc.common.entity.visit.SessionExamineCreateVo;
import com.szeastroc.common.entity.visit.SessionExamineVo;
import com.szeastroc.common.entity.visit.SessionIceBoxRefundModel;
import com.szeastroc.common.entity.visit.enums.NoticeTypeEnum;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.exception.NormalOptionException;
import com.szeastroc.common.feign.customer.FeignCusLabelClient;
import com.szeastroc.common.feign.customer.FeignDistrictClient;
import com.szeastroc.common.feign.customer.FeignStoreClient;
import com.szeastroc.common.feign.customer.FeignStoreRelateMemberClient;
import com.szeastroc.common.feign.customer.FeignSupplierClient;
import com.szeastroc.common.feign.transfer.FeignTransferClient;
import com.szeastroc.common.feign.user.FeignCacheClient;
import com.szeastroc.common.feign.user.FeignDeptClient;
import com.szeastroc.common.feign.user.FeignIceBoxExamineUserClient;
import com.szeastroc.common.feign.user.FeignUserClient;
import com.szeastroc.common.feign.visit.FeignExportRecordsClient;
import com.szeastroc.common.feign.visit.FeignOutBacklogClient;
import com.szeastroc.common.feign.visit.FeignOutExamineClient;
import com.szeastroc.common.utils.ExecutorServiceFactory;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.commondb.config.redis.JedisClient;
import com.szeastroc.icebox.config.DmsUrlConfig;
import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.icebox.config.XcxConfig;
import com.szeastroc.icebox.constant.IceBoxConstant;
import com.szeastroc.icebox.enums.*;
import com.szeastroc.common.entity.icebox.vo.IceInspectionReportMsg;
import com.szeastroc.icebox.enums.PutStatus;
import com.szeastroc.icebox.enums.ResultEnum;
import com.szeastroc.icebox.newprocess.dao.*;
import com.szeastroc.icebox.newprocess.entity.*;
import com.szeastroc.icebox.newprocess.enums.*;
import com.szeastroc.icebox.newprocess.enums.ServiceType;
import com.szeastroc.icebox.newprocess.service.IceBackApplyReportService;
import com.szeastroc.icebox.newprocess.service.IceBackOrderService;
import com.szeastroc.icebox.newprocess.service.IceBoxService;
import com.szeastroc.icebox.newprocess.service.IceRepairOrderService;
import com.szeastroc.icebox.newprocess.vo.SimpleIceBoxDetailVo;
import com.szeastroc.icebox.oldprocess.dao.WechatTransferOrderDao;
import com.szeastroc.icebox.oldprocess.vo.IceDepositResponse;
import com.szeastroc.icebox.oldprocess.vo.query.IceDepositPage;
import com.szeastroc.icebox.rabbitMQ.DataPack;
import com.szeastroc.icebox.rabbitMQ.DirectProducer;
import com.szeastroc.icebox.rabbitMQ.MethodNameOfMQ;
import com.szeastroc.icebox.util.NewExcelUtil;
import com.szeastroc.icebox.util.SendRequestUtils;
import com.szeastroc.icebox.util.wechatpay.WeiXinConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @_(@Autowired))
public class IceBackOrderServiceImpl extends ServiceImpl<IceBackOrderDao, IceBackOrder> implements IceBackOrderService {

    private final IceBoxDao iceBoxDao;
    private final IceBoxExtendDao iceBoxExtendDao;
    private final IcePutApplyDao icePutApplyDao;
    private final IcePutOrderDao icePutOrderDao;
    private final IcePutPactRecordDao icePutPactRecordDao;
    private final IcePutApplyRelateBoxDao icePutApplyRelateBoxDao;
    private final FeignOutBacklogClient feignOutBacklogClient;
    private final IceBackApplyRelateBoxDao iceBackApplyRelateBoxDao;
    private final IceBackOrderDao iceBackOrderDao;
    private final IceBackApplyDao iceBackApplyDao;
    private final FeignStoreClient feignStoreClient;
    private final FeignDeptClient feignDeptClient;
    private final FeignUserClient feignUserClient;
    private final FeignOutExamineClient feignOutExamineClient;
    private final WechatTransferOrderDao wechatTransferOrderDao;
    private final XcxConfig xcxConfig;
    private final FeignTransferClient feignTransferClient;
    private final FeignSupplierClient feignSupplierClient;
    private final IceModelDao iceModelDao;
    private final IceTransferRecordDao iceTransferRecordDao;
    private final FeignCusLabelClient feignCusLabelClient;
    private final PutStoreRelateModelDao putStoreRelateModelDao;
    private final ApplyRelatePutStoreModelDao applyRelatePutStoreModelDao;
    private final WeiXinConfig weiXinConfig;
    private final RabbitTemplate rabbitTemplate;
    private final IceBoxService iceBoxService;

    private final String group = "销售组长";
    private final String service = "服务处经理";
    private final String serviceOther = "服务处副经理";
    private final String divion = "大区总监";
    private final String divionOther = "大区副总监";
    private final FeignExportRecordsClient feignExportRecordsClient;
    private final JedisClient jedisClient;

    private final DirectProducer directProducer;
    private final FeignCacheClient feignCacheClient;
    private final IceBackApplyReportService iceBackApplyReportService;
    private final FeignStoreRelateMemberClient feignStoreRelateMemberClient;
    private final FeignDistrictClient feignDistrictClient;
    private final FeignIceBoxExamineUserClient feignIceBoxExamineUserClient;

    private final IceRepairOrderService iceRepairOrderService;
    private final IceBoxRelateDmsDao iceBoxRelateDmsDao;

    private final DmsUrlConfig dmsUrlConfig;


    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    @Override
    public void takeBackOrder(Integer iceBoxId) {
        // 校验是否可申请退还
        validateTakeBack(iceBoxId);


//        IceBackOrder selectIceBackOrder = iceBackOrderDao.selectOne(Wrappers.<IceBackOrder>lambdaQuery()
//                .eq(IceBackOrder::getBoxId, iceBoxId)
//                .orderByDesc(IceBackOrder::getCreatedTime)
//                .last("limit 1"));

        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(iceBoxId);
//        IcePutApply icePutApply = icePutApplyDao.selectOne(Wrappers.<IcePutApply>lambdaQuery().eq(IcePutApply::getApplyNumber, iceBoxExtend.getLastApplyNumber()));
//        Integer icePutApplyId = icePutApply.getId();


        IcePutApplyRelateBox icePutApplyRelateBox = icePutApplyRelateBoxDao.selectOne(Wrappers.<IcePutApplyRelateBox>lambdaQuery().eq(IcePutApplyRelateBox::getApplyNumber, iceBoxExtend.getLastApplyNumber()).eq(IcePutApplyRelateBox::getBoxId, iceBoxId));
        Integer putApplyRelateBoxId = icePutApplyRelateBox.getId();


        IceBackApply iceBackApply = iceBackApplyDao.selectOne(Wrappers.<IceBackApply>lambdaQuery().eq(IceBackApply::getOldPutId, putApplyRelateBoxId).ne(IceBackApply::getExamineStatus, 3));


//        selectIceBackOrder
        if (iceBackApply != null) {
            // 该冰柜存在过退还
            String selectApplyNumber = iceBackApply.getApplyNumber();
            IceBackApply selectIceBackApply = iceBackApplyDao.selectOne(Wrappers.<IceBackApply>lambdaQuery().eq(IceBackApply::getApplyNumber, selectApplyNumber));

            Integer examineStatus = selectIceBackApply.getExamineStatus();

            if (examineStatus.equals(ExamineStatusEnum.IS_PASS.getStatus()) || examineStatus.equals(ExamineStatusEnum.UN_PASS.getStatus())) {
                log.info("该冰柜最后一次申请退还已经通过或者驳回,冰柜id-->[{}]", iceBoxId);
                // 退还编号
                doBack(iceBoxId);
            } else {

                log.info("该冰柜最后一次申请退还已经未通过或者未驳回,冰柜id-->[{}]", iceBoxId);
                throw new NormalOptionException(ResultEnum.ICE_BOX_IS_REFUNDING.getCode(), ResultEnum.ICE_BOX_IS_REFUNDING.getMessage());
            }
        } else {
            // 该冰柜第一次进行退还
            doBack(iceBoxId);
        }


    }

    @Override
    @Transactional(rollbackFor = Exception.class, value = "transactionManager")
    public void doRefund(SimpleIceBoxDetailVo simpleIceBoxDetailVo) {

        // 退还编号
//        String applyNumber = "BAC" + IdUtil.simpleUUID().substring(0, 29);
//        Integer iceBoxId = simpleIceBoxDetailVo.getId();
//
//        IcePutOrder icePutOrder = icePutOrderDao.selectOne(Wrappers.<IcePutOrder>lambdaQuery()
//                .eq(IcePutOrder::getApplyNumber, simpleIceBoxDetailVo.getLastPutNumber())
//                .eq(IcePutOrder::getChestId, iceBoxId)
//                .eq(IcePutOrder::getStatus,OrderStatus.IS_FINISH.getStatus()));
//
//
//        IcePutApplyRelateBox icePutApplyRelateBox = icePutApplyRelateBoxDao.selectOne(Wrappers.<IcePutApplyRelateBox>lambdaQuery()
//                .eq(IcePutApplyRelateBox::getApplyNumber, simpleIceBoxDetailVo.getLastPutNumber())
//                .eq(IcePutApplyRelateBox::getBoxId, iceBoxId));
//
//        Integer backType = simpleIceBoxDetailVo.getBackType();
//
//        IceBackOrder iceBackOrder = IceBackOrder.builder()
//                .boxId(iceBoxId)
//                .amount(backType.equals(BackType.BACK_MONEY.getType()) ? icePutOrder.getPayMoney() : BigDecimal.ZERO)
//                .applyNumber(applyNumber)
//                .openid(icePutOrder.getOpenid())
//                .putOrderId(icePutOrder.getId())
//                .partnerTradeNo(icePutOrder.getOrderNum())
//                .build();
//
//
//        IceBackApplyRelateBox iceBackApplyRelateBox = IceBackApplyRelateBox.builder()
//                .applyNumber(applyNumber)
//                .backSupplierId(simpleIceBoxDetailVo.getSupplierId())
//                .backType(backType)
//                .freeType(icePutApplyRelateBox.getFreeType())
//                .boxId(iceBoxId)
//                .modelId(simpleIceBoxDetailVo.getChestModelId())
//                .build();


        // 创建审批流
        IceBackApplyRelateBox iceBackApplyRelateBox = iceBackApplyRelateBoxDao.selectOne(Wrappers.<IceBackApplyRelateBox>lambdaQuery()
                .eq(IceBackApplyRelateBox::getBoxId, simpleIceBoxDetailVo.getId())
                .orderByDesc(IceBackApplyRelateBox::getCreateTime)
                .last("limit 1"));
        String applyNumber = iceBackApplyRelateBox.getApplyNumber();
        IceBackApply iceBackApply = iceBackApplyDao.selectOne(Wrappers.<IceBackApply>lambdaQuery().eq(IceBackApply::getApplyNumber, applyNumber));
        IceBackApplyReport backApplyReport = iceBackApplyReportService.getOne(Wrappers.<IceBackApplyReport>lambdaQuery().eq(IceBackApplyReport::getApplyNumber, applyNumber));

        SimpleUserInfoVo simpleUserInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findSimpleUserById(simpleIceBoxDetailVo.getUserId()));
        Integer userDeptId = simpleUserInfoVo.getSimpleDeptInfoVos().get(0).getId();
        Map<Integer, SessionUserInfoVo> sessionUserInfoMap = FeignResponseUtil.getFeignData(feignDeptClient.findLevelLeaderByDeptId(userDeptId));
        //        获取上级部门领导
        List<Integer> userIds = new ArrayList<>();
//        SessionUserInfoVo userInfoVo1 = sessionUserInfoMap.get(1);
//        SessionUserInfoVo userInfoVo2 = sessionUserInfoMap.get(2);
//        SessionUserInfoVo userInfoVo3 = sessionUserInfoMap.get(3);


        Integer examineUserId = FeignResponseUtil.getFeignData(feignIceBoxExamineUserClient.getExamineUserIdByDeptId(userDeptId));

        for (Integer key : sessionUserInfoMap.keySet()) {
            SessionUserInfoVo sessionUserInfoVo = sessionUserInfoMap.get(key);
            if (sessionUserInfoVo != null && sessionUserInfoVo.getId().equals(simpleUserInfoVo.getId())) {
                continue;
            }
            if (sessionUserInfoVo != null && userIds.contains(sessionUserInfoVo.getId())) {
                continue;
            }
//            if (sessionUserInfoVo != null && (group.equals(sessionUserInfoVo.getOfficeName()))) {
//                userIds.add(sessionUserInfoVo.getId());
//                continue;
//            }
//            if (sessionUserInfoVo != null && (service.equals(sessionUserInfoVo.getOfficeName()) || serviceOther.equals(sessionUserInfoVo.getOfficeName()))) {
//                userIds.add(sessionUserInfoVo.getId());
//                continue;
//            }
//            if (sessionUserInfoVo != null && (divion.equals(sessionUserInfoVo.getOfficeName()) || divionOther.equals(sessionUserInfoVo.getOfficeName()))) {
//                userIds.add(sessionUserInfoVo.getId());
//                break;
//            }
            if (sessionUserInfoVo != null && DeptTypeEnum.GROUP.getType().equals(sessionUserInfoVo.getDeptType())) {
                userIds.add(sessionUserInfoVo.getId());
                continue;
            }

            if (sessionUserInfoVo != null && DeptTypeEnum.SERVICE.getType().equals(sessionUserInfoVo.getDeptType())) {
                userIds.add(sessionUserInfoVo.getId());
                if (null != examineUserId && !userIds.contains(examineUserId)) {
                    userIds.add(examineUserId);
                }
                continue;
            }
            if (sessionUserInfoVo != null && DeptTypeEnum.LARGE_AREA.getType().equals(sessionUserInfoVo.getDeptType())) {
                if (null != examineUserId && !userIds.contains(examineUserId)) {
                    userIds.add(examineUserId);
                }
                userIds.add(sessionUserInfoVo.getId());
                break;
            }
        }


        if (CollectionUtil.isEmpty(userIds)) {
            throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败，找不到上级审批人！");
        }

        if (null != examineUserId && !userIds.contains(examineUserId)) {
            userIds.add(examineUserId);
        }

//        List<Integer> userIds = Arrays.asList(5941, 2103);
        SessionExamineVo sessionExamineVo = new SessionExamineVo();
        SessionIceBoxRefundModel sessionIceBoxRefundModel = new SessionIceBoxRefundModel();

        BeanUtils.copyProperties(simpleIceBoxDetailVo, sessionIceBoxRefundModel);

        SessionExamineCreateVo sessionExamineCreateVo = SessionExamineCreateVo.builder()
                .code(applyNumber)
                .relateCode(applyNumber)
                .createBy(simpleIceBoxDetailVo.getUserId())
                .userIds(userIds)
                .build();

        sessionExamineVo.setSessionExamineCreateVo(sessionExamineCreateVo);
        sessionExamineVo.setSessionIceBoxRefundModel(sessionIceBoxRefundModel);


        Integer backType = simpleIceBoxDetailVo.getBackType();
        IcePutOrder icePutOrder = icePutOrderDao.selectOne(Wrappers.<IcePutOrder>lambdaQuery()
                .eq(IcePutOrder::getApplyNumber, simpleIceBoxDetailVo.getLastPutNumber())
                .eq(IcePutOrder::getChestId, simpleIceBoxDetailVo.getId())
                .eq(IcePutOrder::getStatus, OrderStatus.IS_FINISH.getStatus()));

        // 更新退还数据
        if (icePutOrder != null) {
            IceBackOrder iceBackOrder = iceBackOrderDao.selectOne(Wrappers.<IceBackOrder>lambdaQuery().eq(IceBackOrder::getBoxId, simpleIceBoxDetailVo.getId()).eq(IceBackOrder::getApplyNumber, applyNumber));
            iceBackOrder.setAmount(backType.equals(BackType.BACK_MONEY.getType()) ? icePutOrder.getPayMoney() : BigDecimal.ZERO);
            if(Objects.nonNull(backApplyReport)){
                backApplyReport.setDepositMoney(backType.equals(BackType.BACK_MONEY.getType()) ? icePutOrder.getPayMoney() : BigDecimal.ZERO);
            }
            iceBackOrderDao.updateById(iceBackOrder);
        }
        iceBackApplyRelateBox.setBackSupplierId(simpleIceBoxDetailVo.getNewSupplierId());
        iceBackApplyRelateBox.setBackType(backType);

        iceBackApplyRelateBoxDao.updateById(iceBackApplyRelateBox);

        Integer examineId = FeignResponseUtil.getFeignData(feignOutExamineClient.createIceBoxRefund(sessionExamineVo));
        iceBackApply.setUserId(simpleIceBoxDetailVo.getUserId());
        iceBackApply.setCreatedBy(simpleIceBoxDetailVo.getUserId());
        iceBackApply.setExamineId(examineId);
        iceBackApply.setExamineStatus(ExamineStatusEnum.IS_DEFAULT.getStatus());
        if(Objects.nonNull(backApplyReport)){
            backApplyReport.setExamineId(examineId);
            backApplyReport.setExamineStatus(ExamineStatusEnum.IS_DEFAULT.getStatus());
            Integer checkPersonId = userIds.get(0);
            SimpleUserInfoVo checkPerson = FeignResponseUtil.getFeignData(feignUserClient.findUserById(checkPersonId));
            backApplyReport.setCheckPerson(checkPerson.getRealname());
            backApplyReport.setCheckPersonId(checkPersonId);
            backApplyReport.setCheckOfficeName(checkPerson.getPosion());
            SimpleUserInfoVo submitter = FeignResponseUtil.getFeignData(feignUserClient.findUserById(simpleIceBoxDetailVo.getUserId()));
            backApplyReport.setSubmitterName(submitter.getRealname());
            backApplyReport.setSubmitterMobile(submitter.getMobile());
            backApplyReport.setSubmitterId(simpleIceBoxDetailVo.getUserId());
            SubordinateInfoVo supplier = FeignResponseUtil.getFeignData(feignSupplierClient.readId(simpleIceBoxDetailVo.getNewSupplierId()));
            backApplyReport.setDealerName(supplier.getName());
            backApplyReport.setDealerNumber(supplier.getNumber());
            iceBackApplyReportService.updateById(backApplyReport);
        }
        iceBackApplyDao.updateById(iceBackApply);

    }

    @Override
    @Transactional(rollbackFor = Exception.class, value = "transactionManager")
    public void updateExamineStatus(IceBoxRequest iceBoxRequest) {

        Integer status = iceBoxRequest.getStatus();
        String applyNumber = iceBoxRequest.getApplyNumber();
        Integer checkPersonId = iceBoxRequest.getUpdateBy();
        SimpleUserInfoVo checkPerson = FeignResponseUtil.getFeignData(feignUserClient.findUserById(checkPersonId));
        IceBackApplyReport backApplyReport = iceBackApplyReportService.getOne(Wrappers.<IceBackApplyReport>lambdaQuery().eq(IceBackApplyReport::getApplyNumber, applyNumber));
        if (status == 0) {
            // 审批中
            IceBackApply iceBackApply = new IceBackApply();
            iceBackApply.setExamineStatus(ExamineStatusEnum.IS_DEFAULT.getStatus());
            iceBackApplyDao.update(iceBackApply, Wrappers.<IceBackApply>lambdaQuery().eq(IceBackApply::getApplyNumber, applyNumber));
            if(Objects.nonNull(backApplyReport)){
                backApplyReport.setExamineStatus(ExamineStatusEnum.IS_DEFAULT.getStatus());
            }
        } else if (status == 1) {
            //批准
            JSONObject jsonObject = doTransfer(applyNumber);
            IceBackApply iceBackApply = iceBackApplyDao.selectOne(Wrappers.<IceBackApply>lambdaQuery().eq(IceBackApply::getApplyNumber, applyNumber));
            iceBackApply.setExamineStatus(ExamineStatusEnum.IS_PASS.getStatus());
            if(Objects.nonNull(backApplyReport)){
                backApplyReport.setExamineStatus(ExamineStatusEnum.IS_PASS.getStatus());
            }
            iceBackApplyDao.updateById(iceBackApply);
            CompletableFuture.runAsync(() ->
                    feignCusLabelClient.manualExpired(9999, iceBackApply.getBackStoreNumber()), ExecutorServiceFactory.getInstance());

            if (jsonObject != null) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCommit() {
                        // 发送mq消息
                        rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.ICEBOX_ASSETS_REPORT_ROUTING_KEY, jsonObject.toString());
                    }
                });
            }
        } else if (status == 2) {
            // 驳回
            IceBackApply iceBackApply = new IceBackApply();
            iceBackApply.setExamineStatus(ExamineStatusEnum.UN_PASS.getStatus());
            if(Objects.nonNull(backApplyReport)){
                backApplyReport.setExamineStatus(ExamineStatusEnum.UN_PASS.getStatus());
            }
            iceBackApplyDao.update(iceBackApply, Wrappers.<IceBackApply>lambdaQuery().eq(IceBackApply::getApplyNumber, applyNumber));
        }
        if(Objects.nonNull(backApplyReport)){
            backApplyReport.setCheckPersonId(checkPersonId);
            backApplyReport.setCheckPerson(checkPerson.getRealname());
            backApplyReport.setCheckOfficeName(checkPerson.getPosion());
            backApplyReport.setCheckDate(new Date());
            backApplyReport.setReason(iceBoxRequest.getReason());
            iceBackApplyReportService.updateById(backApplyReport);
        }
    }

    @Override
    public String checkBackIceBox(Integer iceBoxId) {

        String result = "";

        // //  2020/4/28  需要倒序

        IceBackApplyRelateBox iceBackApplyRelateBox = iceBackApplyRelateBoxDao.selectOne(Wrappers.<IceBackApplyRelateBox>lambdaQuery().eq(IceBackApplyRelateBox::getBoxId, iceBoxId).orderByDesc(IceBackApplyRelateBox::getCreateTime).last("limit 1"));

        if (iceBackApplyRelateBox == null) {
            result = "退还";
        } else {
            String applyNumber = iceBackApplyRelateBox.getApplyNumber();
            IceBackApply iceBackApply = iceBackApplyDao.selectOne(Wrappers.<IceBackApply>lambdaQuery().eq(IceBackApply::getApplyNumber, applyNumber));
            Integer examineStatus = iceBackApply.getExamineStatus();
            switch (examineStatus) {
                case 0:
                    result = "未审核";
                    break;
                case 1:
                    result = "审核中";
                    break;
                case 2:
                    result = "审核通过";
                    break;
                case 3:
                    result = "审核驳回";
                    break;
            }
        }
        return result;
    }

    @Override
    public IPage<IceDepositResponse> findRefundTransferByPage(IceDepositPage iceDepositPage) {

        IPage<IceDepositResponse> page = new Page<>();

        LambdaQueryWrapper<IceBackApply> wrapper = Wrappers.<IceBackApply>lambdaQuery();
        LambdaQueryWrapper<IceBox> iceBoxWrapper = Wrappers.<IceBox>lambdaQuery();
        LambdaQueryWrapper<IceBackOrder> iceBackOrderWrapper = Wrappers.<IceBackOrder>lambdaQuery();
        // 筛选退化数量为0的
        iceBackOrderWrapper.ne(IceBackOrder::getAmount, 0);

        // 主表条件
        String payEndTime = iceDepositPage.getPayEndTime();
        String payStartTime = iceDepositPage.getPayStartTime();

        wrapper.eq(IceBackApply::getExamineStatus, ExamineStatusEnum.IS_PASS.getStatus());


        if (StringUtils.isNotBlank(payStartTime)) {
            wrapper.ge(IceBackApply::getUpdatedTime, payStartTime);
        }

        if (StringUtils.isNotBlank(payEndTime)) {
            wrapper.le(IceBackApply::getUpdatedTime, payEndTime);
        }

        // 副表条件
        String assetId = iceDepositPage.getAssetId();
        String chestModel = iceDepositPage.getChestModel();
        String clientName = iceDepositPage.getClientName();
        String clientNumber = iceDepositPage.getClientNumber();
        String contactMobile = iceDepositPage.getContactMobile();
        Integer marketAreaId = iceDepositPage.getMarketAreaId();

        if (StringUtils.isNotBlank(clientNumber)) {
            wrapper.eq(IceBackApply::getBackStoreNumber, clientNumber);
        }
        List<String> storeNumberList = new ArrayList<>();
        if (StringUtils.isNotBlank(clientName)) {
            List<StoreInfoDtoVo> storeInfoDtoVos = FeignResponseUtil.getFeignData(feignStoreClient.getByName(clientName));
            if (CollectionUtil.isNotEmpty(storeInfoDtoVos)) {
                storeNumberList.addAll(storeInfoDtoVos.stream().map(StoreInfoDtoVo::getStoreNumber).collect(Collectors.toList()));
            }
            List<SimpleSupplierInfoVo> simpleSupplierInfoVos = FeignResponseUtil.getFeignData(feignSupplierClient.readLikeName(clientName));

            if (CollectionUtil.isNotEmpty(simpleSupplierInfoVos)) {
                storeNumberList.addAll(simpleSupplierInfoVos.stream().map(SimpleSupplierInfoVo::getNumber).collect(Collectors.toList()));
            }
        }

        if (StringUtils.isNotBlank(contactMobile)) {
            List<StoreInfoDtoVo> storeInfoDtoVos = FeignResponseUtil.getFeignData(feignStoreClient.getByMobile(contactMobile));
            if (CollectionUtil.isNotEmpty(storeInfoDtoVos)) {
                storeNumberList.addAll(storeInfoDtoVos.stream().map(StoreInfoDtoVo::getStoreNumber).collect(Collectors.toList()));
            }
        }

        if (CollectionUtil.isNotEmpty(storeNumberList)) {
            wrapper.in(IceBackApply::getBackStoreNumber, storeNumberList);
        }


        if (StringUtils.isNotBlank(assetId)) {
            iceBoxWrapper.eq(IceBox::getAssetId, assetId);
        }

        if (StringUtils.isNotBlank(chestModel)) {
            iceBoxWrapper.eq(IceBox::getModelName, chestModel);
        }


        if (marketAreaId != null) {
            iceBoxWrapper.eq(IceBox::getDeptId, marketAreaId);
        }

        List<IceBox> iceBoxes = new ArrayList<>();
        // 查询副表
        if (StringUtils.isNotBlank(assetId) || StringUtils.isNotBlank(chestModel) || marketAreaId != null) {
            iceBoxes = iceBoxDao.selectList(iceBoxWrapper);
            if (CollectionUtil.isNotEmpty(iceBoxes)) {
                List<Integer> collect = iceBoxes.stream().map(IceBox::getId).collect(Collectors.toList());
                iceBackOrderWrapper.in(IceBackOrder::getBoxId, collect);
            } else {
                return page;
            }
        }


        List<IceBackApply> iceBackApplyList = iceBackApplyDao.selectList(wrapper);

        if (CollectionUtil.isNotEmpty(iceBackApplyList)) {
            List<String> collect = iceBackApplyList.stream().map(IceBackApply::getApplyNumber).collect(Collectors.toList());
            iceBackOrderWrapper.in(IceBackOrder::getApplyNumber, collect);
        } else {
            if (StringUtils.isNotBlank(payStartTime) || StringUtils.isNotBlank(payEndTime) || StringUtils.isNotBlank(clientNumber) || CollectionUtil.isNotEmpty(storeNumberList)) {
                return page;
            }
        }

        IPage<IceBackOrder> iPage = iceBackOrderDao.selectPage(iceDepositPage, iceBackOrderWrapper);
        page = iPage.convert(iceBackOrder -> {
            IceDepositResponse iceDepositResponse = new IceDepositResponse();
            String applyNumber = iceBackOrder.getApplyNumber();
            IceBackApply iceBackApply = iceBackApplyDao.selectOne(Wrappers.<IceBackApply>lambdaQuery().eq(IceBackApply::getApplyNumber, applyNumber));
            String backStoreNumber = iceBackApply.getBackStoreNumber();
            Integer boxId = iceBackOrder.getBoxId();
            IceBox iceBox = iceBoxDao.selectById(boxId);
            Map<String, SessionStoreInfoVo> storeInfoVoMap = FeignResponseUtil.getFeignData(feignStoreClient.getSessionStoreInfoVo((Collections.singletonList(backStoreNumber))));
            SessionStoreInfoVo sessionStoreInfoVo = storeInfoVoMap.get(backStoreNumber);
            if (null != sessionStoreInfoVo && StringUtils.isNotBlank(sessionStoreInfoVo.getStoreNumber())) {
                iceDepositResponse.setClientNumber(backStoreNumber);
                iceDepositResponse.setClientName(sessionStoreInfoVo.getStoreName());
                iceDepositResponse.setContactName(sessionStoreInfoVo.getMemberName());
                iceDepositResponse.setContactMobile(sessionStoreInfoVo.getMemberMobile());
                iceDepositResponse.setClientPlace(sessionStoreInfoVo.getParserAddress());
            } else {
                SubordinateInfoVo subordinateInfoVo = FeignResponseUtil.getFeignData(feignSupplierClient.findByNumber(backStoreNumber));
                if (null != subordinateInfoVo && StringUtils.isNotBlank(subordinateInfoVo.getNumber())) {
                    iceDepositResponse.setClientNumber(backStoreNumber);
                    iceDepositResponse.setClientName(subordinateInfoVo.getName());
                    iceDepositResponse.setContactName(subordinateInfoVo.getLinkman());
                    iceDepositResponse.setContactMobile(subordinateInfoVo.getLinkmanMobile());
                    iceDepositResponse.setClientPlace(subordinateInfoVo.getAddress());
                }
            }
            String marketAreaName = FeignResponseUtil.getFeignData(feignCacheClient.getForMarketAreaName(iceBox.getDeptId()));
            iceDepositResponse.setMarketAreaName(marketAreaName);
            iceDepositResponse.setChestModel(iceBox.getModelName());
            iceDepositResponse.setChestName(iceBox.getChestName());
            iceDepositResponse.setAssetId(iceBox.getAssetId());
            iceDepositResponse.setPayMoney("-" + iceBackOrder.getAmount().toString());
            iceDepositResponse.setPayTime(iceBackApply.getUpdatedTime().getTime());
            iceDepositResponse.setChestMoney(iceBox.getChestMoney().toString());
            return iceDepositResponse;
        });
        return page;
    }

    @Override
    public void exportRefundTransferByMq(IceDepositPage iceDepositPage) {

        // 从session 中获取用户信息
        UserManageVo userManageVo = FeignResponseUtil.getFeignData(feignUserClient.getSessionUserInfo());
        Integer userId = userManageVo.getSessionUserInfoVo().getId();
        String userName = userManageVo.getSessionUserInfoVo().getRealname();
        // 控制导出的请求频率
        String key = "ice_refund_export_excel_" + userId;
        String value = jedisClient.get(key);
//        if (StringUtils.isNotBlank(value)) {
//            throw new NormalOptionException(Constants.API_CODE_FAIL, "请到“首页-下载任务”中查看导出结果，请勿频繁操作(间隔3分钟)...");
//        }
        jedisClient.setnx(key, userId.toString(), 180);
        // 塞入数据到下载列表中  exportRecordId
        Integer exportRecordId = FeignResponseUtil.getFeignData(feignExportRecordsClient.createExportRecords(userId, userName, JSON.toJSONString(iceDepositPage), "冰柜押金退还明细导出"));
        iceDepositPage.setExportRecordId(exportRecordId);
        // 塞入部门集合
        DataPack dataPack = new DataPack(); // 数据包
        dataPack.setMethodName(MethodNameOfMQ.EXPORT_ICE_REFUND);
        dataPack.setObj(iceDepositPage);
        directProducer.sendMsg(MqConstant.directRoutingKey, dataPack);
    }

    @Override
    public void exportRefundTransfer(IceDepositPage iceDepositPage) {
        try {
            LambdaQueryWrapper<IceBackApply> wrapper = Wrappers.<IceBackApply>lambdaQuery();
            LambdaQueryWrapper<IceBox> iceBoxWrapper = Wrappers.<IceBox>lambdaQuery();
            LambdaQueryWrapper<IceBackOrder> iceBackOrderWrapper = Wrappers.<IceBackOrder>lambdaQuery();
            // 筛选退化数量为0的
            iceBackOrderWrapper.ne(IceBackOrder::getAmount, 0);

            // 主表条件
            String payEndTime = iceDepositPage.getPayEndTime();
            String payStartTime = iceDepositPage.getPayStartTime();

            wrapper.eq(IceBackApply::getExamineStatus, ExamineStatusEnum.IS_PASS.getStatus());


            if (StringUtils.isNotBlank(payStartTime)) {
                wrapper.ge(IceBackApply::getUpdatedTime, payStartTime);
            }

            if (StringUtils.isNotBlank(payEndTime)) {
                wrapper.le(IceBackApply::getUpdatedTime, payEndTime);
            }

            // 副表条件
            String assetId = iceDepositPage.getAssetId();
            String chestModel = iceDepositPage.getChestModel();
            String clientName = iceDepositPage.getClientName();
            String clientNumber = iceDepositPage.getClientNumber();
            String contactMobile = iceDepositPage.getContactMobile();
            Integer marketAreaId = iceDepositPage.getMarketAreaId();

            if (StringUtils.isNotBlank(clientNumber)) {
                wrapper.eq(IceBackApply::getBackStoreNumber, clientNumber);
            }
            List<String> storeNumberList = new ArrayList<>();
            if (StringUtils.isNotBlank(clientName)) {
                List<StoreInfoDtoVo> storeInfoDtoVos = FeignResponseUtil.getFeignData(feignStoreClient.getByName(clientName));
                if (CollectionUtil.isNotEmpty(storeInfoDtoVos)) {
                    storeNumberList.addAll(storeInfoDtoVos.stream().map(StoreInfoDtoVo::getStoreNumber).collect(Collectors.toList()));
                }
                List<SimpleSupplierInfoVo> simpleSupplierInfoVos = FeignResponseUtil.getFeignData(feignSupplierClient.readLikeName(clientName));

                if (CollectionUtil.isNotEmpty(simpleSupplierInfoVos)) {
                    storeNumberList.addAll(simpleSupplierInfoVos.stream().map(SimpleSupplierInfoVo::getNumber).collect(Collectors.toList()));
                }
            }

            if (StringUtils.isNotBlank(contactMobile)) {
                List<StoreInfoDtoVo> storeInfoDtoVos = FeignResponseUtil.getFeignData(feignStoreClient.getByMobile(contactMobile));
                if (CollectionUtil.isNotEmpty(storeInfoDtoVos)) {
                    storeNumberList.addAll(storeInfoDtoVos.stream().map(StoreInfoDtoVo::getStoreNumber).collect(Collectors.toList()));
                }
            }

            if (CollectionUtil.isNotEmpty(storeNumberList)) {
                wrapper.in(IceBackApply::getBackStoreNumber, storeNumberList);
            }


            if (StringUtils.isNotBlank(assetId)) {
                iceBoxWrapper.eq(IceBox::getAssetId, assetId);
            }

            if (StringUtils.isNotBlank(chestModel)) {
                iceBoxWrapper.eq(IceBox::getModelName, chestModel);
            }


            if (marketAreaId != null) {
                iceBoxWrapper.eq(IceBox::getDeptId, marketAreaId);
            }

            List<IceBox> iceBoxes = new ArrayList<>();
            // 查询副表
            if (StringUtils.isNotBlank(assetId) || StringUtils.isNotBlank(chestModel) || marketAreaId != null) {
                iceBoxes = iceBoxDao.selectList(iceBoxWrapper);
                if (CollectionUtil.isNotEmpty(iceBoxes)) {
                    List<Integer> collect = iceBoxes.stream().map(IceBox::getId).collect(Collectors.toList());
                    iceBackOrderWrapper.in(IceBackOrder::getBoxId, collect);
                } else {
                    return;
                }
            }

            List<IceBackApply> iceBackApplyList = iceBackApplyDao.selectList(wrapper);

            if (CollectionUtil.isNotEmpty(iceBackApplyList)) {
                List<String> collect = iceBackApplyList.stream().map(IceBackApply::getApplyNumber).collect(Collectors.toList());
                iceBackOrderWrapper.in(IceBackOrder::getApplyNumber, collect);
            } else {
                if (StringUtils.isNotBlank(payStartTime) || StringUtils.isNotBlank(payEndTime) || StringUtils.isNotBlank(clientNumber) || CollectionUtil.isNotEmpty(storeNumberList)) {
                    return;
                }
            }
            List<IceBackOrder> iceBackOrderList = iceBackOrderDao.selectList(iceBackOrderWrapper);
            if (CollectionUtil.isNotEmpty(iceBackOrderList)) {
                String fileName = "冰柜押金退还明细表";
                String titleName = "冰柜押金退还明细表";
                List<IceDepositResponse> iceDepositResponseList = new ArrayList<>();
                iceBackOrderList.forEach(iceBackOrder -> {
                    IceDepositResponse iceDepositResponse = new IceDepositResponse();
                    String applyNumber = iceBackOrder.getApplyNumber();
                    IceBackApply iceBackApply = iceBackApplyDao.selectOne(Wrappers.<IceBackApply>lambdaQuery().eq(IceBackApply::getApplyNumber, applyNumber));
                    String backStoreNumber = iceBackApply.getBackStoreNumber();
                    Integer boxId = iceBackOrder.getBoxId();
                    IceBox iceBox = iceBoxDao.selectById(boxId);
                    Map<String, SessionStoreInfoVo> storeInfoVoMap = FeignResponseUtil.getFeignData(feignStoreClient.getSessionStoreInfoVo((Collections.singletonList(backStoreNumber))));
                    SessionStoreInfoVo sessionStoreInfoVo = storeInfoVoMap.get(backStoreNumber);
                    if (null != sessionStoreInfoVo && StringUtils.isNotBlank(sessionStoreInfoVo.getStoreNumber())) {
                        iceDepositResponse.setClientNumber(backStoreNumber);
                        iceDepositResponse.setClientName(sessionStoreInfoVo.getStoreName());
                        iceDepositResponse.setContactName(sessionStoreInfoVo.getMemberName());
                        iceDepositResponse.setContactMobile(sessionStoreInfoVo.getMemberMobile());
                        iceDepositResponse.setClientPlace(sessionStoreInfoVo.getParserAddress());
                    } else {
                        SubordinateInfoVo subordinateInfoVo = FeignResponseUtil.getFeignData(feignSupplierClient.findByNumber(backStoreNumber));
                        if (null != subordinateInfoVo && StringUtils.isNotBlank(subordinateInfoVo.getNumber())) {
                            iceDepositResponse.setClientNumber(backStoreNumber);
                            iceDepositResponse.setClientName(subordinateInfoVo.getName());
                            iceDepositResponse.setContactName(subordinateInfoVo.getLinkman());
                            iceDepositResponse.setContactMobile(subordinateInfoVo.getLinkmanMobile());
                            iceDepositResponse.setClientPlace(subordinateInfoVo.getAddress());
                        }
                    }
                    SessionDeptInfoVo sessionDeptInfoVo = FeignResponseUtil.getFeignData(feignDeptClient.findSessionDeptById(iceBox.getDeptId()));
                    String marketAreaName = FeignResponseUtil.getFeignData(feignCacheClient.getForMarketAreaName(iceBox.getDeptId()));
                    Map<String, String> map = separateMarketAreaName(marketAreaName);

                    iceDepositResponse.setDivision(map.get("division"));
                    iceDepositResponse.setRegion(map.get("region"));
                    iceDepositResponse.setService(map.get("service"));

                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    iceDepositResponse.setMarketAreaName(sessionDeptInfoVo.getName());
                    iceDepositResponse.setChestModel(iceBox.getModelName());
                    iceDepositResponse.setChestName(iceBox.getChestName());
                    iceDepositResponse.setAssetId(iceBox.getAssetId());
                    iceDepositResponse.setPayMoney("-" + iceBackOrder.getAmount().toString());
                    iceDepositResponse.setPayTimeStr(formatter.format(iceBackApply.getUpdatedTime()));
                    iceDepositResponse.setChestMoney(iceBox.getChestMoney().toString());
                    iceDepositResponseList.add(iceDepositResponse);
                });
                NewExcelUtil<IceDepositResponse> newExcelUtil = new NewExcelUtil<>();
                newExcelUtil.asyncExportExcelOther(fileName, titleName, iceDepositResponseList, iceDepositPage.getExportRecordId());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * takeBackIceChest注入对象及校验
     *
     * @param iceBoxId
     * @throws ImproperOptionException
     */
    private void validateTakeBack(Integer iceBoxId) throws ImproperOptionException, NormalOptionException {

        IceBox iceBox = iceBoxDao.selectById(iceBoxId);
        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(iceBoxId);

        // 校验: 冰柜表中数据
        if (Objects.isNull(iceBox) || Objects.isNull(iceBoxExtend)) {
            throw new NormalOptionException(ResultEnum.CANNOT_FIND_ICE_BOX.getCode(), ResultEnum.CANNOT_FIND_ICE_BOX.getMessage());
        }
        // 特殊编号冰柜不支持退还
        if (IceBoxConstant.virtual_asset_id.equals(iceBox.getAssetId())){
            throw new NormalOptionException(ResultEnum.CANNOT_REFUND_ICE_BOX.getCode(), ResultEnum.CANNOT_REFUND_ICE_BOX.getMessage());
        }
        // 报修中的冰柜不支持退还
        Integer unfinishOrderCount = iceRepairOrderService.getUnfinishOrderCount(iceBox.getId());
        if(unfinishOrderCount>0){
            throw new NormalOptionException(ResultEnum.HAVE_REPAIR_ORDER.getCode(), ResultEnum.HAVE_REPAIR_ORDER.getMessage());
        }

        IcePutApply icePutApply = icePutApplyDao.selectOne(Wrappers.<IcePutApply>lambdaQuery()
                .eq(IcePutApply::getApplyNumber, iceBoxExtend.getLastApplyNumber()).eq(IcePutApply::getPutStoreNumber, iceBox.getPutStoreNumber()));
        // 校验: 投放表中数据
        if (Objects.isNull(icePutApply)) {
            throw new NormalOptionException(ResultEnum.CANNOT_FIND_ICE_BOX_APPLY.getCode(), ResultEnum.CANNOT_FIND_ICE_BOX_APPLY.getMessage());
        }

        IcePutApplyRelateBox icePutApplyRelateBox = icePutApplyRelateBoxDao.selectOne(Wrappers.<IcePutApplyRelateBox>lambdaQuery()
                .eq(IcePutApplyRelateBox::getApplyNumber, iceBoxExtend.getLastApplyNumber())
                .eq(IcePutApplyRelateBox::getBoxId, iceBoxId));

        // 2020/11/17  投放新规则，可以不签收直接投放，过滤电子协议校验
//        IcePutPactRecord icePutPactRecord = icePutPactRecordDao.selectOne(Wrappers.<IcePutPactRecord>lambdaQuery()
//                .eq(IcePutPactRecord::getApplyNumber, iceBoxExtend.getLastApplyNumber())
//                .eq(IcePutPactRecord::getBoxId, iceBoxId).eq(IcePutPactRecord::getStoreNumber, iceBox.getPutStoreNumber()));
//
//        // 校验: 电子协议
//        if (icePutPactRecord == null) {
//            throw new NormalOptionException(ResultEnum.CANNOT_FIND_ICE_PUT_PACT_RECORD.getCode(), ResultEnum.CANNOT_FIND_ICE_PUT_PACT_RECORD.getMessage());
//        }

        // 校验退还到期时间 //注释掉了这段代码，即使未到期也能退还
//        if (icePutPactRecord.getPutExpireTime().getTime() > new Date().getTime()) {
//            throw new NormalOptionException(ResultEnum.TAKE_BAKE_ERR_WITH_EXPIRE_TIME.getCode(), ResultEnum.TAKE_BAKE_ERR_WITH_EXPIRE_TIME.getMessage());
//        }

        // 免押时, 不校验订单, 直接跳过
        if (FreePayTypeEnum.IS_FREE.getType() == icePutApplyRelateBox.getFreeType()) {
            return;
        }

        IcePutOrder icePutOrder = icePutOrderDao.selectOne(Wrappers.<IcePutOrder>lambdaQuery()
                .eq(IcePutOrder::getApplyNumber, icePutApply.getApplyNumber())
                .eq(IcePutOrder::getChestId, iceBoxId)
                .eq(IcePutOrder::getStatus, OrderStatus.IS_FINISH.getStatus()));
        /**
         * 校验: 订单号
         */
        if (Objects.isNull(icePutOrder)) {
            throw new NormalOptionException(ResultEnum.CANNOT_FIND_ICE_PUT_ORDER.getCode(), ResultEnum.CANNOT_FIND_ICE_PUT_ORDER.getMessage());
        }
        if (!icePutOrder.getStatus().equals(OrderStatus.IS_FINISH.getStatus())) {
            throw new NormalOptionException(ResultEnum.PUT_ORDER_IS_NOT_FINISH.getCode(), ResultEnum.PUT_ORDER_IS_NOT_FINISH.getMessage());
        }
    }

    private JSONObject doTransfer(String applyNumber) {

        IceBoxRelateDms iceBoxRelateDms = new IceBoxRelateDms();
        Map params = new HashMap();
        IceBackApplyRelateBox iceBackApplyRelateBox = iceBackApplyRelateBoxDao.selectOne(Wrappers.<IceBackApplyRelateBox>lambdaQuery().eq(IceBackApplyRelateBox::getApplyNumber, applyNumber));
        IceBackApply iceBackApply = iceBackApplyDao.selectOne(Wrappers.<IceBackApply>lambdaQuery().eq(IceBackApply::getApplyNumber, applyNumber));

        Integer iceBoxId = iceBackApplyRelateBox.getBoxId();
        IceBox iceBox = iceBoxDao.selectById(iceBoxId);
        String storeNumber = iceBox.getPutStoreNumber();
        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(iceBoxId);
        IcePutApply icePutApply = icePutApplyDao.selectOne(Wrappers.<IcePutApply>lambdaQuery().eq(IcePutApply::getApplyNumber, iceBoxExtend.getLastApplyNumber()));

        IcePutApplyRelateBox icePutApplyRelateBox = icePutApplyRelateBoxDao.selectOne(Wrappers.<IcePutApplyRelateBox>lambdaQuery()
                .eq(IcePutApplyRelateBox::getApplyNumber, iceBoxExtend.getLastApplyNumber())
                .eq(IcePutApplyRelateBox::getBoxId, iceBoxId));

        Date date = new Date();
        IceTransferRecord iceTransferRecord = IceTransferRecord.builder()
                .applyNumber(applyNumber)
                .serviceType(ServiceType.IS_RETURN.getType())
                .boxId(iceBoxId)
                .supplierId(iceBackApplyRelateBox.getBackSupplierId())
                .storeNumber(iceBackApply.getBackStoreNumber())
                .applyUserId(iceBackApply.getUserId())
                .applyTime(date)
                .receiveTime(date)
                .recordStatus(com.szeastroc.icebox.newprocess.enums.RecordStatus.SEND_ING.getStatus())
                .build();

        IceBackOrder iceBackOrder = iceBackOrderDao.selectOne(Wrappers.<IceBackOrder>lambdaQuery().eq(IceBackOrder::getApplyNumber, applyNumber));

        if (iceBackOrder != null) {
            iceTransferRecord.setTransferMoney(iceBackOrder.getAmount());
        }
        iceBoxRelateDms.setIceBoxId(iceBoxId);
        iceBoxRelateDms.setIceBoxType(iceBox.getIceBoxType());
        iceBoxRelateDms.setIceBoxAssetId(iceBox.getAssetId());
        iceBoxRelateDms.setSupplierId(iceBox.getSupplierId());
        iceBoxRelateDms.setModelId(iceBox.getModelId());
        iceBoxRelateDms.setRelateNumber(applyNumber);
        iceBoxRelateDms.setType(2);
        iceBoxRelateDms.setPutStoreNumber(icePutApply.getPutStoreNumber());
        iceBoxRelateDms.setBackstatus(IceBackStatusEnum.IS_ACEPTD.getType());

        // 插入交易记录
        iceTransferRecordDao.insert(iceTransferRecord);

        // 更新冰柜状态
//        iceBoxExtend.setLastPutTime(date);
//        iceBoxExtend.setLastPutId(iceTransferRecord.getId());
//        iceBoxExtend.setLastApplyNumber(applyNumber);
//        iceBoxExtendDao.updateById(iceBoxExtend);
        iceBoxExtendDao.update(null, Wrappers.<IceBoxExtend>lambdaUpdate()
                .eq(IceBoxExtend::getId, iceBoxId)
                .set(IceBoxExtend::getLastPutId, 0)
                .set(IceBoxExtend::getLastApplyNumber, null));

        // 变更当前型号状态
        List<ApplyRelatePutStoreModel> applyRelatePutStoreModelList = applyRelatePutStoreModelDao.selectList(Wrappers.<ApplyRelatePutStoreModel>lambdaQuery()
                .eq(ApplyRelatePutStoreModel::getApplyNumber, iceBoxExtend.getLastApplyNumber())
                .eq(ApplyRelatePutStoreModel::getFreeType, icePutApplyRelateBox.getFreeType()));
        if (CollectionUtil.isNotEmpty(applyRelatePutStoreModelList)) {
            Integer modelId = iceBox.getModelId();
            for (ApplyRelatePutStoreModel applyRelatePutStoreModel : applyRelatePutStoreModelList) {
                Integer storeRelateModelId = applyRelatePutStoreModel.getStoreRelateModelId();
                PutStoreRelateModel putStoreRelateModel = putStoreRelateModelDao.selectOne(Wrappers.<PutStoreRelateModel>lambdaQuery()
                        .eq(PutStoreRelateModel::getId, storeRelateModelId)
                        .eq(PutStoreRelateModel::getModelId, modelId)
                        .eq(PutStoreRelateModel::getPutStatus, com.szeastroc.icebox.newprocess.enums.PutStatus.FINISH_PUT.getStatus()));
                if (null != putStoreRelateModel) {
                    putStoreRelateModelDao.update(putStoreRelateModel, Wrappers.<PutStoreRelateModel>lambdaUpdate()
                            .set(PutStoreRelateModel::getPutStatus, com.szeastroc.icebox.newprocess.enums.PutStatus.NO_PUT.getStatus())
                            .set(PutStoreRelateModel::getUpdateTime, new Date())
                            .eq(PutStoreRelateModel::getId, storeRelateModelId));
                    iceBoxRelateDms.setPutStoreRelateModelId(putStoreRelateModel.getId());
                    if(putStoreRelateModel.getSupplierId() != null && putStoreRelateModel.getSupplierId() > 0){
                        SupplierInfo supplierInfo = FeignResponseUtil.getFeignData(feignSupplierClient.findInfoById(putStoreRelateModel.getSupplierId()));
                        params.put("pxtNumber",supplierInfo.getNumber());
                    }
                    break;
                }
            }
        }

        //  2021/5/12 加入dms通知
        iceBoxRelateDmsDao.insert(iceBoxRelateDms);
        params.put("type",SendDmsIceboxTypeEnum.BACK_CONFIRM.getCode()+"");
        params.put("relateCode",iceBoxRelateDms.getId()+"");
        CompletableFuture.runAsync(()->SendRequestUtils.sendPostRequest(dmsUrlConfig.getToDmsUrl()+"/drpOpen/pxtAndIceBox/pxtToDmsIceBoxMsg",params), ExecutorServiceFactory.getInstance());


        // 更新冰柜状态
        iceBox.setPutStatus(PutStatus.NO_PUT.getStatus());
        iceBox.setPutStoreNumber("0");
        iceBox.setSupplierId(iceBackApplyRelateBox.getBackSupplierId());
        iceBox.setResponseMan(null);
        iceBox.setResponseManId(0);
        iceBoxDao.updateById(iceBox);
        JSONObject jsonObject = iceBoxService.setAssetReportJson(iceBox,"doTransfer");
//         免押时, 不校验订单, 直接跳过
        if (FreePayTypeEnum.IS_FREE.getType().equals(icePutApplyRelateBox.getFreeType())) {
            return jsonObject;
        }

        // 非免押，但是不退押金，直接跳过
        if (BackType.BACK_WITHOUT_MONEY.getType() == iceBackApplyRelateBox.getBackType()) {
            return jsonObject;
        }
        IcePutOrder icePutOrder = icePutOrderDao.selectOne(Wrappers.<IcePutOrder>lambdaQuery()
                .eq(IcePutOrder::getApplyNumber, icePutApply.getApplyNumber())
                .eq(IcePutOrder::getChestId, iceBoxId)
                .eq(IcePutOrder::getStatus, OrderStatus.IS_FINISH.getStatus()));

        /**
         * 调用转账服务
         */
        TransferRequest transferRequest = TransferRequest.builder()
                .resourceType(ResourceTypeEnum.FROM_ICEBOX.getType())
                .resourceKey(String.valueOf(icePutOrder.getId()))
//                .wxappid(xcxConfig.getAppid())
                .openid(icePutOrder.getOpenid())
//                .paymentAmount(orderInfo.getPayMoney().multiply(new BigDecimal(100)))
                .paymentAmount(icePutOrder.getPayMoney())
                .wechatPayType(WechatPayTypeEnum.FOR_TRANSFER.getType())
//                .mchType(xcxConfig.getMchType())
                .build();

        if (icePutOrder.getOrderSource().equals(OrderSourceEnums.OTOC.getType())) {
            transferRequest.setWxappid(xcxConfig.getAppid());
            transferRequest.setMchType(xcxConfig.getMchType());
        } else if (icePutOrder.getOrderSource().equals(OrderSourceEnums.DMS.getType())) {
            transferRequest.setWxappid(xcxConfig.getDmsAppId());
            transferRequest.setMchType(xcxConfig.getDmsMchType());
        }

        log.info("转帐服务请求数据-->[{}]", JSON.toJSONString(transferRequest, true));
        TransferReponse transferReponse = FeignResponseUtil.getFeignData(feignTransferClient.transfer(transferRequest));

        log.info("转账服务返回的数据-->[{}]", JSON.toJSONString(transferReponse, true));

        return jsonObject;
    }


    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void doBack(Integer iceBoxId) {
        // 退还编号
        String applyNumber = "BAC" + IdUtil.simpleUUID().substring(0, 29);

        // 创建通知
        DateTime date = new DateTime();
        String prefix = date.toString("yyyyMMddHHmmss");
//        String blockName = "冰柜退还确认单";
        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(iceBoxId);
        IceBox iceBox = iceBoxDao.selectById(iceBoxId);
        String putStoreNumber = iceBox.getPutStoreNumber();

        // 查询门店的主业务员
        Integer userId = FeignResponseUtil.getFeignData(feignStoreClient.getMainSaleManId(putStoreNumber));


        if (null == userId) {
            // 查询配送上的主业务员
            userId = FeignResponseUtil.getFeignData(feignSupplierClient.getMainSaleManId(putStoreNumber));
        }

        if (null == userId) {
            throw new NormalOptionException(ResultEnum.CANNOT_FIND_MAIN_SALESMAN.getCode(), ResultEnum.CANNOT_FIND_MAIN_SALESMAN.getMessage());
        }


        String assetId = iceBoxExtend.getAssetId();
        String relateCode = prefix + "_" + assetId;
        NoticeBacklogRequestVo noticeBacklogRequestVo = NoticeBacklogRequestVo.builder()
                .backlogName(NoticeTypeEnum.ICEBOX_REFUND_CONFIRM.getDesc())
                .noticeTypeEnum(NoticeTypeEnum.ICEBOX_REFUND_CONFIRM)
                .relateCode(relateCode)
                .sendUserId(userId) //
                .build();
        // 创建通知
        feignOutBacklogClient.createNoticeBacklog(noticeBacklogRequestVo);

        IcePutApplyRelateBox icePutApplyRelateBox = icePutApplyRelateBoxDao.selectOne(Wrappers.<IcePutApplyRelateBox>lambdaQuery()
                .eq(IcePutApplyRelateBox::getApplyNumber, iceBoxExtend.getLastApplyNumber())
                .eq(IcePutApplyRelateBox::getBoxId, iceBoxId));


        IceBackApplyRelateBox iceBackApplyRelateBox = IceBackApplyRelateBox.builder()
                .applyNumber(applyNumber)
                .freeType(icePutApplyRelateBox.getFreeType())
                .boxId(iceBoxId)
                .modelId(iceBox.getModelId())
                .build();

//        IcePutApply icePutApply = icePutApplyDao.selectOne(Wrappers.<IcePutApply>lambdaQuery().eq(IcePutApply::getApplyNumber, iceBoxExtend.getLastApplyNumber()));


        IceBackApply iceBackApply = IceBackApply.builder()
                .applyNumber(applyNumber)
                .backStoreNumber(putStoreNumber)
                .oldPutId(icePutApplyRelateBox.getId())
                .userId(userId)
                .build();

        this.generateBackReport(iceBox, applyNumber, putStoreNumber,icePutApplyRelateBox.getFreeType());
        iceBackApplyRelateBoxDao.insert(iceBackApplyRelateBox);
        iceBackApplyDao.insert(iceBackApply);

        if (icePutApplyRelateBox.getFreeType().equals(FreePayTypeEnum.UN_FREE.getType())) {
            // 非免押
            IcePutOrder icePutOrder = icePutOrderDao.selectOne(Wrappers.<IcePutOrder>lambdaQuery()
                    .eq(IcePutOrder::getApplyNumber, iceBoxExtend.getLastApplyNumber())
                    .eq(IcePutOrder::getChestId, iceBoxId)
                    .eq(IcePutOrder::getStatus, OrderStatus.IS_FINISH.getStatus()));
            if (icePutOrder != null) {
                IceBackOrder iceBackOrder = IceBackOrder.builder()
                        .boxId(iceBoxId)
                        .applyNumber(applyNumber)
                        .openid(icePutOrder.getOpenid())
                        .putOrderId(icePutOrder.getId())
                        .partnerTradeNo(icePutOrder.getOrderNum())
                        .build();
                iceBackOrderDao.insert(iceBackOrder);
            }
        }
    }

    @Override
    public IceBackApplyReport generateBackReport(IceBox iceBox, String applyNumber, String putStoreNumber, Integer freeType){
        Map<Integer, SessionDeptInfoVo> deptMap = FeignResponseUtil.getFeignData(feignCacheClient.getFiveLevelDept(iceBox.getDeptId()));
        Integer groupId = null;
        String groupName = null;
        Integer serviceId = null;
        String serviceName = null;
        Integer regionId = null;
        String regionName = null;
        Integer businessId = null;
        String businessName = null;
        Integer headquartersId = null;
        String headquartersName = null;
        SessionDeptInfoVo group = deptMap.get(1);
        if(Objects.nonNull(group)){
            groupId = group.getId();
            groupName = group.getName();
        }
        SessionDeptInfoVo service = deptMap.get(2);
        if(Objects.nonNull(service)){
            serviceId = service.getId();
            serviceName = service.getName();
        }
        SessionDeptInfoVo region = deptMap.get(3);
        if(Objects.nonNull(region)){
            regionId = region.getId();
            regionName = region.getName();
        }
        SessionDeptInfoVo business = deptMap.get(4);
        SessionDeptInfoVo headquarters = deptMap.get(5);
        if(!DeptTypeEnum.BUSINESS_UNIT.getType().equals(business.getDeptType())){
            business = null;
            headquarters = deptMap.get(4);
        }
        if(Objects.nonNull(business)){
            businessId = business.getId();
            businessName = business.getName();
        }

        if(Objects.nonNull(headquarters)){
            headquartersId = headquarters.getId();
            headquartersName = headquarters.getName();
        }
        String customerName;
        Integer customerType;
        String customerAddress;
        SupplierInfoSessionVo supplierInfoSessionVo = FeignResponseUtil.getFeignData(feignSupplierClient.getSuppliserInfoByNumber(putStoreNumber));
        String linkMan;
        String linkMobile;
        String province=null;
        String city=null;
        String area=null;
        String provinceCode = null;
        String cityCode = null;
        String districtCode = null;
        if(Objects.nonNull(supplierInfoSessionVo)){
            customerName = supplierInfoSessionVo.getName();
            customerType = supplierInfoSessionVo.getSupplierType();
            customerAddress = supplierInfoSessionVo.getAddress();
            linkMan = supplierInfoSessionVo.getLinkMan();
            linkMobile = supplierInfoSessionVo.getLinkManMobile();
            provinceCode = supplierInfoSessionVo.getProvinceCode();
            cityCode = supplierInfoSessionVo.getCityCode();
            districtCode = supplierInfoSessionVo.getRegionCode();
        }else{
            StoreInfoDtoVo store = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(putStoreNumber));
            customerName = store.getStoreName();
            customerAddress = store.getAddress();
            customerType = 5;
            MemberInfoVo member = FeignResponseUtil.getFeignData(feignStoreRelateMemberClient.getShopKeeper(putStoreNumber));
            linkMan = member.getName();
            linkMobile = member.getMobile();
            provinceCode = store.getProvinceCode();
            cityCode = store.getCityCode();
            districtCode = store.getDistrictCode();
        }
        if(StringUtils.isNotBlank(provinceCode)){
            BaseDistrictVO provinceDistrict = FeignResponseUtil.getFeignData(feignDistrictClient.getByCode(provinceCode));
            if(Objects.nonNull(provinceDistrict)){
                province= provinceDistrict.getName();
            }
        }
        if(StringUtils.isNotBlank(cityCode)){
            BaseDistrictVO cityDistrict = FeignResponseUtil.getFeignData(feignDistrictClient.getByCode(cityCode));
            if(Objects.nonNull(cityDistrict)){
                city= cityDistrict.getName();
            }
        }
        if(StringUtils.isNotBlank(districtCode)){
            BaseDistrictVO areaDistrict = FeignResponseUtil.getFeignData(feignDistrictClient.getByCode(districtCode));
            if(Objects.nonNull(areaDistrict)){
                area= areaDistrict.getName();
            }
        }

        IceBackApplyReport report = IceBackApplyReport.builder()
                .applyNumber(applyNumber)
                .customerName(customerName).customerNumber(putStoreNumber).customerType(customerType).customerAddress(customerAddress)
                .linkMan(linkMan).linkMobile(linkMobile)
                .assetId(iceBox.getAssetId())
                .boxId(iceBox.getId()).freeType(freeType)
                .modelId(iceBox.getModelId()).modelName(iceBox.getModelName())
                .groupDeptId(groupId).groupDeptName(groupName)
                .serviceDeptId(serviceId).serviceDeptName(serviceName)
                .businessDeptId(businessId).businessDeptName(businessName)
                .regionDeptId(regionId).regionDeptName(regionName)
                .headquartersDeptId(headquartersId).headquartersDeptName(headquartersName)
                .backDate(new Date()).province(province).city(city).area(area)
                .build();
        iceBackApplyReportService.save(report);
        return report;
    }


    private Map<String, String> separateMarketAreaName(String marketAreaName) {
        String newMarketAreaName = "";
        String[] splits = marketAreaName.split("/");
        List<String> stringList = Arrays.asList(splits);
        int index = 0;
        if (stringList.contains("北方大区")) {
            index = stringList.indexOf("北方大区");
        } else if (stringList.contains("广东事业部")) {
            index = stringList.indexOf("广东事业部");
        } else if (stringList.contains("广西事业部")) {
            index = stringList.indexOf("广西事业部");
        } else if (stringList.contains("华北事业部")) {
            index = stringList.indexOf("华北事业部");
        } else if (stringList.contains("华中事业部")) {
            index = stringList.indexOf("华中事业部");
        } else if (stringList.contains("西南事业部")) {
            index = stringList.indexOf("西南事业部");
        } else if (stringList.contains("全国直营本部")) {
            index = stringList.indexOf("全国直营本部");
        } else if (stringList.contains("测试事业部")) {
            index = stringList.indexOf("测试事业部");
        }

        for (int i = index; i < stringList.size(); i++) {
            if (i == (stringList.size() - 1)) {
                newMarketAreaName = newMarketAreaName + stringList.get(i);
            } else {
                newMarketAreaName = newMarketAreaName + stringList.get(i) + "/";
            }
        }

        String division = "";
        String region = "";
        String service = "";

        String[] strings = newMarketAreaName.split("/");
        List<String> newStringList = Arrays.asList(strings);
        if (newStringList.contains("北方大区")) {
            int newIndex = newStringList.indexOf("北方大区");
            division = strings[newIndex];

            if ((newIndex + 1) <= strings.length - 1) {
                service = strings[newIndex + 1];
            }
        } else {
            if (0 <= strings.length - 1) {
                division = strings[0];
            }
            if (1 <= strings.length - 1) {
                region = strings[1];
            }
            if (2 <= strings.length - 1) {
                service = strings[2];
            }
        }
        Map<String, String> map = new HashMap<>();

        map.put("division", division);
        map.put("region", region);
        map.put("service", service);

        return map;
    }

}

