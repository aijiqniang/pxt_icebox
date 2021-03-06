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

    private final String group = "????????????";
    private final String service = "???????????????";
    private final String serviceOther = "??????????????????";
    private final String divion = "????????????";
    private final String divionOther = "???????????????";
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
    @Autowired
    private  IceBackOrderServiceImpl iceBackOrderServiceImpl;


    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    @Override
    public void takeBackOrder(Integer iceBoxId,String returnRemark) {
        // ???????????????????????????
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
            // ????????????????????????
            String selectApplyNumber = iceBackApply.getApplyNumber();
            IceBackApply selectIceBackApply = iceBackApplyDao.selectOne(Wrappers.<IceBackApply>lambdaQuery().eq(IceBackApply::getApplyNumber, selectApplyNumber));

            Integer examineStatus = selectIceBackApply.getExamineStatus();

            if (examineStatus.equals(ExamineStatusEnum.IS_PASS.getStatus()) || examineStatus.equals(ExamineStatusEnum.UN_PASS.getStatus())) {
                log.info("?????????????????????????????????????????????????????????,??????id-->[{}]", iceBoxId);
                // ????????????
                iceBackOrderServiceImpl.doBack(iceBoxId,returnRemark);
            } else {

                log.info("???????????????????????????????????????????????????????????????,??????id-->[{}]", iceBoxId);
                throw new NormalOptionException(ResultEnum.ICE_BOX_IS_REFUNDING.getCode(), ResultEnum.ICE_BOX_IS_REFUNDING.getMessage());
            }
        } else {
            // ??????????????????????????????
            iceBackOrderServiceImpl.doBack(iceBoxId,returnRemark);
        }


    }

    /**
    * ????????????????????????
    * */
    @Override
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void doBackOrder(SimpleIceBoxDetailVo simpleIceBoxDetailVo) {
        // ???????????????????????????
        validateTakeBack(simpleIceBoxDetailVo.getIceBoxId());
        //???????????????
        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(simpleIceBoxDetailVo.getIceBoxId());
        //??????????????????????????????
        IcePutApplyRelateBox icePutApplyRelateBox = icePutApplyRelateBoxDao.selectOne(Wrappers.<IcePutApplyRelateBox>lambdaQuery().eq(IcePutApplyRelateBox::getApplyNumber, iceBoxExtend.getLastApplyNumber()).eq(IcePutApplyRelateBox::getBoxId, simpleIceBoxDetailVo.getIceBoxId()));
        Integer putApplyRelateBoxId = icePutApplyRelateBox.getId();


        IceBackApply iceBackApply = iceBackApplyDao.selectOne(Wrappers.<IceBackApply>lambdaQuery().eq(IceBackApply::getOldPutId, putApplyRelateBoxId).ne(IceBackApply::getExamineStatus, 3));

        if (iceBackApply != null) {
            // ????????????????????????
            String selectApplyNumber = iceBackApply.getApplyNumber();
            IceBackApply selectIceBackApply = iceBackApplyDao.selectOne(Wrappers.<IceBackApply>lambdaQuery().eq(IceBackApply::getApplyNumber, selectApplyNumber));

            Integer examineStatus = selectIceBackApply.getExamineStatus();
            //??????????????????????????????????????????????????? ??????????????????
            if (examineStatus.equals(ExamineStatusEnum.IS_PASS.getStatus()) || examineStatus.equals(ExamineStatusEnum.UN_PASS.getStatus())) {
                log.info("?????????????????????????????????????????????????????????,??????id-->[{}]", simpleIceBoxDetailVo.getIceBoxId());
                // ????????????
                iceBackOrderServiceImpl.doBackNew(simpleIceBoxDetailVo);
            } else {
                //?????????????????????????????????????????????????????????
                log.info("???????????????????????????????????????????????????????????????,??????id-->[{}]", simpleIceBoxDetailVo);
                throw new NormalOptionException(ResultEnum.ICE_BOX_IS_REFUNDING.getCode(), ResultEnum.ICE_BOX_IS_REFUNDING.getMessage());
            }
        } else {
            // ??????????????????????????????
            iceBackOrderServiceImpl.doBackNew(simpleIceBoxDetailVo);
        }
        this.doRefundNew(simpleIceBoxDetailVo);
    }

    /**
     * ???????????????????????? ???????????? ??????????????????
     * */
    @Override
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void confirm(String applyNumber) {
        JSONObject jsonObject = doTransfer(applyNumber);
        if (jsonObject != null) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    // ??????mq??????
                    rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.ICEBOX_ASSETS_REPORT_ROUTING_KEY, jsonObject.toString());
                }
            });
        }
    }


    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void doBackNew(SimpleIceBoxDetailVo simpleIceBoxDetailVo) {
        // ????????????
        String applyNumber = "BAC" + IdUtil.simpleUUID().substring(0, 29);

        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(simpleIceBoxDetailVo.getIceBoxId());
        IceBox iceBox = iceBoxDao.selectById(simpleIceBoxDetailVo.getIceBoxId());
        String putStoreNumber = iceBox.getPutStoreNumber();

        // ????????????????????????????????????????????? ??????id ??????id ????????????
        IcePutApplyRelateBox icePutApplyRelateBox = icePutApplyRelateBoxDao.selectOne(Wrappers.<IcePutApplyRelateBox>lambdaQuery()
                .eq(IcePutApplyRelateBox::getApplyNumber, iceBoxExtend.getLastApplyNumber())
                .eq(IcePutApplyRelateBox::getBoxId, simpleIceBoxDetailVo.getIceBoxId()));
        //???????????? ????????????  ???????????? 1-????????????2-??????
        Integer freeType = 2;
        //??????????????????????????????t_ice_back_apply_relate_box
        IceBackApplyRelateBox iceBackApplyRelateBox = IceBackApplyRelateBox.builder()
                .applyNumber(applyNumber)
                .freeType(freeType)
                .boxId(simpleIceBoxDetailVo.getIceBoxId())
                .modelId(iceBox.getModelId())
                .build();

        //???????????????????????????????????????????????????t_ice_put_apply_relate_box?????????id??????????????????????????????t_ice_back_apply??????????????????????????????
        IceBackApply iceBackApply = IceBackApply.builder()
                .applyNumber(applyNumber)
                .backStoreNumber(putStoreNumber)
                .oldPutId(icePutApplyRelateBox.getId())
                .backReason(simpleIceBoxDetailVo.getReturnReason())
                .backRemark(simpleIceBoxDetailVo.getReturnRemark())
                .isLogistics(simpleIceBoxDetailVo.getIsLogistics())
                .userId(simpleIceBoxDetailVo.getUserId())
                .build();

        //????????????????????????????????????
        this.generateBackReport(iceBox, applyNumber, putStoreNumber,freeType,simpleIceBoxDetailVo.getReturnRemark(),simpleIceBoxDetailVo.getReturnReason());
        iceBackApplyRelateBoxDao.insert(iceBackApplyRelateBox);
        iceBackApplyDao.insert(iceBackApply);

        //??????????????????????????????
        if (icePutApplyRelateBox.getFreeType().equals(FreePayTypeEnum.UN_FREE.getType())) {
            // ?????????
            IcePutOrder icePutOrder = icePutOrderDao.selectOne(Wrappers.<IcePutOrder>lambdaQuery()
                    .eq(IcePutOrder::getApplyNumber, iceBoxExtend.getLastApplyNumber())
                    .eq(IcePutOrder::getChestId, simpleIceBoxDetailVo.getIceBoxId())
                    .eq(IcePutOrder::getStatus, OrderStatus.IS_FINISH.getStatus()));
            if (icePutOrder != null) {
                IceBackOrder iceBackOrder = IceBackOrder.builder()
                        .boxId(simpleIceBoxDetailVo.getIceBoxId())
                        .applyNumber(applyNumber)
                        .openid(icePutOrder.getOpenid())
                        .putOrderId(icePutOrder.getId())
                        .partnerTradeNo(icePutOrder.getOrderNum())
                        .build();
                iceBackOrderDao.insert(iceBackOrder);
            }
        }
    }

    /**
     * ???????????? :???????????????????????????????????? ????????????????????????????????????
     * @param simpleIceBoxDetailVo
     */

    @Transactional(rollbackFor = Exception.class, value = "transactionManager")
    public void doRefundNew(SimpleIceBoxDetailVo simpleIceBoxDetailVo) {

        // ???????????????
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
        //        ????????????????????????
        List<Integer> userIds = new ArrayList<>();
        //?????????????????????????????????id
        Integer examineUserId = FeignResponseUtil.getFeignData(feignIceBoxExamineUserClient.getExamineUserIdByDeptId(userDeptId));

        for (Integer key : sessionUserInfoMap.keySet()) {
            SessionUserInfoVo sessionUserInfoVo = sessionUserInfoMap.get(key);
            if (sessionUserInfoVo != null && sessionUserInfoVo.getId().equals(simpleUserInfoVo.getId())) {
                continue;
            }
            if (sessionUserInfoVo != null && userIds.contains(sessionUserInfoVo.getId())) {
                continue;
            }

            if (sessionUserInfoVo != null && DeptTypeEnum.GROUP.getType().equals(sessionUserInfoVo.getDeptType())) {
                userIds.add(sessionUserInfoVo.getId());
                continue;
            }
            //??????id????????????????????????????????????
            if (sessionUserInfoVo != null && DeptTypeEnum.SERVICE.getType().equals(sessionUserInfoVo.getDeptType())) {
                userIds.add(sessionUserInfoVo.getId());
                if (null != examineUserId && !userIds.contains(examineUserId)) {
                    userIds.add(examineUserId);
                }
                continue;
            }
            //???????????????????????? ?????????????????????
            if (sessionUserInfoVo != null && DeptTypeEnum.LARGE_AREA.getType().equals(sessionUserInfoVo.getDeptType())) {
                if (null != examineUserId && !userIds.contains(examineUserId)) {
                    userIds.add(examineUserId);
                }
                userIds.add(sessionUserInfoVo.getId());
                break;
            }
        }
        if (CollectionUtil.isEmpty(userIds)) {
            throw new NormalOptionException(Constants.API_CODE_FAIL, "??????????????????????????????????????????");
        }
        //
        if (null != examineUserId && !userIds.contains(examineUserId)) {
            userIds.add(examineUserId);
        }

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

        //??????????????? ????????????
        Integer backType = simpleIceBoxDetailVo.getBackType();
        //????????????????????????????????????
        IcePutOrder icePutOrder = icePutOrderDao.selectOne(Wrappers.<IcePutOrder>lambdaQuery()
                .eq(IcePutOrder::getApplyNumber, simpleIceBoxDetailVo.getLastPutNumber())
                .eq(IcePutOrder::getChestId, simpleIceBoxDetailVo.getId())
                .eq(IcePutOrder::getStatus, OrderStatus.IS_FINISH.getStatus()));

        // ??????????????????  ??????????????????????????????????????????
        if (icePutOrder != null) {
            IceBackOrder iceBackOrder = iceBackOrderDao.selectOne(Wrappers.<IceBackOrder>lambdaQuery().eq(IceBackOrder::getBoxId, simpleIceBoxDetailVo.getId()).eq(IceBackOrder::getApplyNumber, applyNumber));
            iceBackOrder.setAmount(backType.equals(BackType.BACK_MONEY.getType()) ? icePutOrder.getPayMoney() : BigDecimal.ZERO);
            if(Objects.nonNull(backApplyReport)){
                backApplyReport.setDepositMoney(backType.equals(BackType.BACK_MONEY.getType()) ? icePutOrder.getPayMoney() : BigDecimal.ZERO);
            }
            iceBackOrderDao.updateById(iceBackOrder);
        }
        iceBackApplyRelateBox.setBackSupplierId(simpleIceBoxDetailVo.getNewSupplierId());
        //????????????  ?????????????????????2???????????????
        iceBackApplyRelateBox.setBackType(backType);

        iceBackApplyRelateBoxDao.updateById(iceBackApplyRelateBox);
        //???????????? ??????????????? ???????????????id
        Integer examineId = FeignResponseUtil.getFeignData(feignOutExamineClient.createIceBoxRefund(sessionExamineVo));
        /*// ????????????
        DateTime date = new DateTime();
        String prefix = date.toString("yyyyMMddHHmmss");
        String assetId = iceBoxExtend.getAssetId();
        String relateCode = prefix + "_" + assetId;
        NoticeBacklogRequestVo noticeBacklogRequestVo = NoticeBacklogRequestVo.builder()
                .backlogName(NoticeTypeEnum.ICEBOX_REFUND_CONFIRM.getDesc())
                .noticeTypeEnum(NoticeTypeEnum.ICEBOX_REFUND_CONFIRM)
                .relateCode(relateCode)
                .sendUserId(simpleIceBoxDetailVo.getUserId()) //
                .build();
        // ????????????
        feignOutBacklogClient.createNoticeBacklog(noticeBacklogRequestVo);*/

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
    public void doRefund(SimpleIceBoxDetailVo simpleIceBoxDetailVo) {

        // ????????????
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


        // ???????????????
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
        //        ????????????????????????
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
            throw new NormalOptionException(Constants.API_CODE_FAIL, "??????????????????????????????????????????");
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

        // ??????????????????
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
            // ?????????
            IceBackApply iceBackApply = new IceBackApply();
            iceBackApply.setExamineStatus(ExamineStatusEnum.IS_DEFAULT.getStatus());
            iceBackApplyDao.update(iceBackApply, Wrappers.<IceBackApply>lambdaQuery().eq(IceBackApply::getApplyNumber, applyNumber));
            if(Objects.nonNull(backApplyReport)){
                backApplyReport.setExamineStatus(ExamineStatusEnum.IS_DEFAULT.getStatus());
            }
        } else if (status == 1) {
            //??????
//            JSONObject jsonObject = doTransfer(applyNumber);
            IceBackApply iceBackApply = iceBackApplyDao.selectOne(Wrappers.<IceBackApply>lambdaQuery().eq(IceBackApply::getApplyNumber, applyNumber));
            iceBackApply.setExamineStatus(ExamineStatusEnum.IS_PASS.getStatus());
            if(Objects.nonNull(backApplyReport)){
                backApplyReport.setExamineStatus(ExamineStatusEnum.IS_PASS.getStatus());
            }
            iceBackApplyDao.updateById(iceBackApply);
            CompletableFuture.runAsync(() ->
                    feignCusLabelClient.manualExpired(9999, iceBackApply.getBackStoreNumber()), ExecutorServiceFactory.getInstance());

            /*if (jsonObject != null) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCommit() {
                        // ??????mq??????
                        rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.ICEBOX_ASSETS_REPORT_ROUTING_KEY, jsonObject.toString());
                    }
                });
            }*/
        } else if (status == 2) {
            // ??????
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

        // //  2020/4/28  ????????????

        IceBackApplyRelateBox iceBackApplyRelateBox = iceBackApplyRelateBoxDao.selectOne(Wrappers.<IceBackApplyRelateBox>lambdaQuery().eq(IceBackApplyRelateBox::getBoxId, iceBoxId).orderByDesc(IceBackApplyRelateBox::getCreateTime).last("limit 1"));

        if (iceBackApplyRelateBox == null) {
            result = "??????";
        } else {
            String applyNumber = iceBackApplyRelateBox.getApplyNumber();
            IceBackApply iceBackApply = iceBackApplyDao.selectOne(Wrappers.<IceBackApply>lambdaQuery().eq(IceBackApply::getApplyNumber, applyNumber));
            Integer examineStatus = iceBackApply.getExamineStatus();
            switch (examineStatus) {
                case 0:
                    result = "?????????";
                    break;
                case 1:
                    result = "?????????";
                    break;
                case 2:
                    result = "????????????";
                    break;
                case 3:
                    result = "????????????";
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
        // ?????????????????????0???
        iceBackOrderWrapper.ne(IceBackOrder::getAmount, 0);

        // ????????????
        String payEndTime = iceDepositPage.getPayEndTime();
        String payStartTime = iceDepositPage.getPayStartTime();

        wrapper.eq(IceBackApply::getExamineStatus, ExamineStatusEnum.IS_PASS.getStatus());


        if (StringUtils.isNotBlank(payStartTime)) {
            wrapper.ge(IceBackApply::getUpdatedTime, payStartTime);
        }

        if (StringUtils.isNotBlank(payEndTime)) {
            wrapper.le(IceBackApply::getUpdatedTime, payEndTime);
        }

        // ????????????
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
        // ????????????
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

        // ???session ?????????????????????
        UserManageVo userManageVo = FeignResponseUtil.getFeignData(feignUserClient.getSessionUserInfo());
        Integer userId = userManageVo.getSessionUserInfoVo().getId();
        String userName = userManageVo.getSessionUserInfoVo().getRealname();
        // ???????????????????????????
        String key = "ice_refund_export_excel_" + userId;
        String value = jedisClient.get(key);
//        if (StringUtils.isNotBlank(value)) {
//            throw new NormalOptionException(Constants.API_CODE_FAIL, "???????????????-?????????????????????????????????????????????????????????(??????3??????)...");
//        }
        jedisClient.setnx(key, userId.toString(), 180);
        // ??????????????????????????????  exportRecordId
        Integer exportRecordId = FeignResponseUtil.getFeignData(feignExportRecordsClient.createExportRecords(userId, userName, JSON.toJSONString(iceDepositPage), "??????????????????????????????"));
        iceDepositPage.setExportRecordId(exportRecordId);
        // ??????????????????
        DataPack dataPack = new DataPack(); // ?????????
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
            // ?????????????????????0???
            iceBackOrderWrapper.ne(IceBackOrder::getAmount, 0);

            // ????????????
            String payEndTime = iceDepositPage.getPayEndTime();
            String payStartTime = iceDepositPage.getPayStartTime();

            wrapper.eq(IceBackApply::getExamineStatus, ExamineStatusEnum.IS_PASS.getStatus());


            if (StringUtils.isNotBlank(payStartTime)) {
                wrapper.ge(IceBackApply::getUpdatedTime, payStartTime);
            }

            if (StringUtils.isNotBlank(payEndTime)) {
                wrapper.le(IceBackApply::getUpdatedTime, payEndTime);
            }

            // ????????????
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
            // ????????????
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
                String fileName = "???????????????????????????";
                String titleName = "???????????????????????????";
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
     * takeBackIceChest?????????????????????
     *
     * @param iceBoxId
     * @throws ImproperOptionException
     */
    private void validateTakeBack(Integer iceBoxId) throws ImproperOptionException, NormalOptionException {

        IceBox iceBox = iceBoxDao.selectById(iceBoxId);
        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(iceBoxId);

        // ??????: ??????????????????
        if (Objects.isNull(iceBox) || Objects.isNull(iceBoxExtend)) {
            throw new NormalOptionException(ResultEnum.CANNOT_FIND_ICE_BOX.getCode(), ResultEnum.CANNOT_FIND_ICE_BOX.getMessage());
        }
        // ?????????????????????????????????
        if (IceBoxConstant.virtual_asset_id.equals(iceBox.getAssetId())){
            throw new NormalOptionException(ResultEnum.CANNOT_REFUND_ICE_BOX.getCode(), ResultEnum.CANNOT_REFUND_ICE_BOX.getMessage());
        }
        // ?????????????????????????????????
        Integer unfinishOrderCount = iceRepairOrderService.getUnfinishOrderCount(iceBox.getId());
        if(unfinishOrderCount>0){
            throw new NormalOptionException(ResultEnum.HAVE_REPAIR_ORDER.getCode(), ResultEnum.HAVE_REPAIR_ORDER.getMessage());
        }

        IcePutApply icePutApply = icePutApplyDao.selectOne(Wrappers.<IcePutApply>lambdaQuery()
                .eq(IcePutApply::getApplyNumber, iceBoxExtend.getLastApplyNumber()).eq(IcePutApply::getPutStoreNumber, iceBox.getPutStoreNumber()));
        // ??????: ??????????????????
        if (Objects.isNull(icePutApply)) {
            throw new NormalOptionException(ResultEnum.CANNOT_FIND_ICE_BOX_APPLY.getCode(), ResultEnum.CANNOT_FIND_ICE_BOX_APPLY.getMessage());
        }

        IcePutApplyRelateBox icePutApplyRelateBox = icePutApplyRelateBoxDao.selectOne(Wrappers.<IcePutApplyRelateBox>lambdaQuery()
                .eq(IcePutApplyRelateBox::getApplyNumber, iceBoxExtend.getLastApplyNumber())
                .eq(IcePutApplyRelateBox::getBoxId, iceBoxId));

        // 2020/11/17  ????????????????????????????????????????????????????????????????????????
//        IcePutPactRecord icePutPactRecord = icePutPactRecordDao.selectOne(Wrappers.<IcePutPactRecord>lambdaQuery()
//                .eq(IcePutPactRecord::getApplyNumber, iceBoxExtend.getLastApplyNumber())
//                .eq(IcePutPactRecord::getBoxId, iceBoxId).eq(IcePutPactRecord::getStoreNumber, iceBox.getPutStoreNumber()));
//
//        // ??????: ????????????
//        if (icePutPactRecord == null) {
//            throw new NormalOptionException(ResultEnum.CANNOT_FIND_ICE_PUT_PACT_RECORD.getCode(), ResultEnum.CANNOT_FIND_ICE_PUT_PACT_RECORD.getMessage());
//        }

        // ???????????????????????? //??????????????????????????????????????????????????????
//        if (icePutPactRecord.getPutExpireTime().getTime() > new Date().getTime()) {
//            throw new NormalOptionException(ResultEnum.TAKE_BAKE_ERR_WITH_EXPIRE_TIME.getCode(), ResultEnum.TAKE_BAKE_ERR_WITH_EXPIRE_TIME.getMessage());
//        }

        // ?????????, ???????????????, ????????????
        if (FreePayTypeEnum.IS_FREE.getType() == icePutApplyRelateBox.getFreeType()) {
            return;
        }

        IcePutOrder icePutOrder = icePutOrderDao.selectOne(Wrappers.<IcePutOrder>lambdaQuery()
                .eq(IcePutOrder::getApplyNumber, icePutApply.getApplyNumber())
                .eq(IcePutOrder::getChestId, iceBoxId)
                .eq(IcePutOrder::getStatus, OrderStatus.IS_FINISH.getStatus()));
        /**
         * ??????: ?????????
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

        // ??????????????????
        iceTransferRecordDao.insert(iceTransferRecord);

        // ??????????????????
//        iceBoxExtend.setLastPutTime(date);
//        iceBoxExtend.setLastPutId(iceTransferRecord.getId());
//        iceBoxExtend.setLastApplyNumber(applyNumber);
//        iceBoxExtendDao.updateById(iceBoxExtend);
        iceBoxExtendDao.update(null, Wrappers.<IceBoxExtend>lambdaUpdate()
                .eq(IceBoxExtend::getId, iceBoxId)
                .set(IceBoxExtend::getLastPutId, 0)
                .set(IceBoxExtend::getLastApplyNumber, null));

        // ????????????????????????
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

        //  2021/5/12 ??????dms??????
        iceBoxRelateDmsDao.insert(iceBoxRelateDms);
        params.put("type",SendDmsIceboxTypeEnum.BACK_CONFIRM.getCode()+"");
        params.put("relateCode",iceBoxRelateDms.getId()+"");
        CompletableFuture.runAsync(()->SendRequestUtils.sendPostRequest(dmsUrlConfig.getToDmsUrl()+"/drpOpen/pxtAndIceBox/pxtToDmsIceBoxMsg",params), ExecutorServiceFactory.getInstance());


        // ??????????????????
        iceBox.setPutStatus(PutStatus.NO_PUT.getStatus());
        iceBox.setPutStoreNumber("0");
        iceBox.setSupplierId(iceBackApplyRelateBox.getBackSupplierId());
        iceBox.setResponseMan(null);
        iceBox.setResponseManId(0);
        iceBoxDao.updateById(iceBox);
        JSONObject jsonObject = iceBoxService.setAssetReportJson(iceBox,"doTransfer");
//         ?????????, ???????????????, ????????????
        if (FreePayTypeEnum.IS_FREE.getType().equals(icePutApplyRelateBox.getFreeType())) {
            return jsonObject;
        }

        // ?????????????????????????????????????????????
        if (BackType.BACK_WITHOUT_MONEY.getType() == iceBackApplyRelateBox.getBackType()) {
            return jsonObject;
        }
        IcePutOrder icePutOrder = icePutOrderDao.selectOne(Wrappers.<IcePutOrder>lambdaQuery()
                .eq(IcePutOrder::getApplyNumber, icePutApply.getApplyNumber())
                .eq(IcePutOrder::getChestId, iceBoxId)
                .eq(IcePutOrder::getStatus, OrderStatus.IS_FINISH.getStatus()));

        /**
         * ??????????????????
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

        log.info("????????????????????????-->[{}]", JSON.toJSONString(transferRequest, true));
        TransferReponse transferReponse = FeignResponseUtil.getFeignData(feignTransferClient.transfer(transferRequest));

        log.info("???????????????????????????-->[{}]", JSON.toJSONString(transferReponse, true));

        return jsonObject;
    }


    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void doBack(Integer iceBoxId,String returnRemark) {
        // ????????????
        String applyNumber = "BAC" + IdUtil.simpleUUID().substring(0, 29);

        // ????????????
        DateTime date = new DateTime();
        String prefix = date.toString("yyyyMMddHHmmss");
//        String blockName = "?????????????????????";
        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(iceBoxId);
        IceBox iceBox = iceBoxDao.selectById(iceBoxId);
        String putStoreNumber = iceBox.getPutStoreNumber();

        // ???????????????????????????
        Integer userId = FeignResponseUtil.getFeignData(feignStoreClient.getMainSaleManId(putStoreNumber));


        if (null == userId) {
            // ??????????????????????????????
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
                .backRemark(returnRemark)
                .build();

        this.generateBackReport(iceBox, applyNumber, putStoreNumber,icePutApplyRelateBox.getFreeType(),returnRemark,null);
        iceBackApplyRelateBoxDao.insert(iceBackApplyRelateBox);
        iceBackApplyDao.insert(iceBackApply);

        // ????????????
        feignOutBacklogClient.createNoticeBacklog(noticeBacklogRequestVo);

        if (icePutApplyRelateBox.getFreeType().equals(FreePayTypeEnum.UN_FREE.getType())) {
            // ?????????
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
    public IceBackApplyReport generateBackReport(IceBox iceBox, String applyNumber, String putStoreNumber, Integer freeType,String returnRemark,String returnReason){
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
        /*if(!DeptTypeEnum.BUSINESS_UNIT.getType().equals(business.getDeptType())){
            business = null;
            headquarters = deptMap.get(4);
        }*/
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
                .backReason(returnReason).backRemark(returnRemark)
                .build();
        iceBackApplyReportService.save(report);
        return report;
    }


    private Map<String, String> separateMarketAreaName(String marketAreaName) {
        String newMarketAreaName = "";
        String[] splits = marketAreaName.split("/");
        List<String> stringList = Arrays.asList(splits);
        int index = 0;
        if (stringList.contains("????????????")) {
            index = stringList.indexOf("????????????");
        } else if (stringList.contains("???????????????")) {
            index = stringList.indexOf("???????????????");
        } else if (stringList.contains("???????????????")) {
            index = stringList.indexOf("???????????????");
        } else if (stringList.contains("???????????????")) {
            index = stringList.indexOf("???????????????");
        } else if (stringList.contains("???????????????")) {
            index = stringList.indexOf("???????????????");
        } else if (stringList.contains("???????????????")) {
            index = stringList.indexOf("???????????????");
        } else if (stringList.contains("??????????????????")) {
            index = stringList.indexOf("??????????????????");
        } else if (stringList.contains("???????????????")) {
            index = stringList.indexOf("???????????????");
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
        if (newStringList.contains("????????????")) {
            int newIndex = newStringList.indexOf("????????????");
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

