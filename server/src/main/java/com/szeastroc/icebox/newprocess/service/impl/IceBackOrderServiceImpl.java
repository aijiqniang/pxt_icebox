package com.szeastroc.icebox.newprocess.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.exception.NormalOptionException;
import com.szeastroc.common.utils.ExecutorServiceFactory;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.customer.client.FeignCusLabelClient;
import com.szeastroc.customer.client.FeignStoreClient;
import com.szeastroc.customer.client.FeignSupplierClient;
import com.szeastroc.customer.common.vo.SessionStoreInfoVo;
import com.szeastroc.customer.common.vo.StoreInfoDtoVo;
import com.szeastroc.icebox.config.XcxConfig;
import com.szeastroc.icebox.enums.*;
import com.szeastroc.icebox.newprocess.dao.*;
import com.szeastroc.icebox.newprocess.entity.*;
import com.szeastroc.icebox.newprocess.enums.BackType;
import com.szeastroc.icebox.newprocess.enums.ServiceType;
import com.szeastroc.icebox.newprocess.service.IceBackOrderService;
import com.szeastroc.icebox.newprocess.vo.SimpleIceBoxDetailVo;
import com.szeastroc.icebox.oldprocess.dao.WechatTransferOrderDao;
import com.szeastroc.icebox.oldprocess.vo.IceDepositResponse;
import com.szeastroc.icebox.oldprocess.vo.query.IceDepositPage;
import com.szeastroc.icebox.vo.IceBoxRequest;
import com.szeastroc.transfer.client.FeignTransferClient;
import com.szeastroc.transfer.common.enums.ResourceTypeEnum;
import com.szeastroc.transfer.common.enums.WechatPayTypeEnum;
import com.szeastroc.transfer.common.request.TransferRequest;
import com.szeastroc.transfer.common.response.TransferReponse;
import com.szeastroc.user.client.FeignDeptClient;
import com.szeastroc.user.client.FeignUserClient;
import com.szeastroc.user.common.vo.SessionUserInfoVo;
import com.szeastroc.user.common.vo.SimpleUserInfoVo;
import com.szeastroc.visit.client.FeignOutBacklogClient;
import com.szeastroc.visit.client.FeignOutExamineClient;
import com.szeastroc.visit.common.NoticeBacklogRequestVo;
import com.szeastroc.visit.common.SessionExamineCreateVo;
import com.szeastroc.visit.common.SessionExamineVo;
import com.szeastroc.visit.common.SessionIceBoxRefundModel;
import com.szeastroc.visit.common.enums.NoticeTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    private final String group = "销售组长";
    private final String service = "服务处经理";
    private final String serviceOther = "服务处副经理";
    private final String divion = "大区总监";
    private final String divionOther = "大区副总监";


    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    @Override
    public void takeBackOrder(Integer iceBoxId) {
        // 校验是否可申请退还
        validateTakeBack(iceBoxId);

        // TODO 由崔梦阳实现退还逻辑

//        IceBackOrder selectIceBackOrder = iceBackOrderDao.selectOne(Wrappers.<IceBackOrder>lambdaQuery()
//                .eq(IceBackOrder::getBoxId, iceBoxId)
//                .orderByDesc(IceBackOrder::getCreatedTime)
//                .last("limit 1"));

        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(iceBoxId);
//        IcePutApply icePutApply = icePutApplyDao.selectOne(Wrappers.<IcePutApply>lambdaQuery().eq(IcePutApply::getApplyNumber, iceBoxExtend.getLastApplyNumber()));
//        Integer icePutApplyId = icePutApply.getId();


        IcePutApplyRelateBox icePutApplyRelateBox = icePutApplyRelateBoxDao.selectOne(Wrappers.<IcePutApplyRelateBox>lambdaQuery().eq(IcePutApplyRelateBox::getApplyNumber, iceBoxExtend.getLastApplyNumber()).eq(IcePutApplyRelateBox::getBoxId, iceBoxId));
        Integer putApplyRelateBoxId = icePutApplyRelateBox.getId();


        IceBackApply iceBackApply = iceBackApplyDao.selectOne(Wrappers.<IceBackApply>lambdaQuery().eq(IceBackApply::getOldPutId, putApplyRelateBoxId));


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

        SimpleUserInfoVo simpleUserInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findSimpleUserById(simpleIceBoxDetailVo.getUserId()));
        Map<Integer, SessionUserInfoVo> sessionUserInfoMap = FeignResponseUtil.getFeignData(feignDeptClient.findLevelLeaderByDeptId(simpleUserInfoVo.getSimpleDeptInfoVos().get(0).getId()));
        //        获取上级部门领导
        List<Integer> userIds = new ArrayList<>();
//        SessionUserInfoVo userInfoVo1 = sessionUserInfoMap.get(1);
//        SessionUserInfoVo userInfoVo2 = sessionUserInfoMap.get(2);
//        SessionUserInfoVo userInfoVo3 = sessionUserInfoMap.get(3);


        for (Integer key : sessionUserInfoMap.keySet()) {
            SessionUserInfoVo sessionUserInfoVo = sessionUserInfoMap.get(key);
            if (sessionUserInfoVo != null && sessionUserInfoVo.getId().equals(simpleUserInfoVo.getId())) {
                continue;
            }
            if (userIds.contains(sessionUserInfoVo.getId())) {
                continue;
            }
            if (sessionUserInfoVo != null && (group.equals(sessionUserInfoVo.getOfficeName()))) {
                userIds.add(sessionUserInfoVo.getId());
                continue;
            }
            if (sessionUserInfoVo != null && (service.equals(sessionUserInfoVo.getOfficeName()) || serviceOther.equals(sessionUserInfoVo.getOfficeName()))) {
                userIds.add(sessionUserInfoVo.getId());
                continue;
            }
            if (sessionUserInfoVo != null && (divion.equals(sessionUserInfoVo.getOfficeName()) || divionOther.equals(sessionUserInfoVo.getOfficeName()))) {
                userIds.add(sessionUserInfoVo.getId());
                break;
            }
        }


        if (CollectionUtil.isEmpty(userIds)) {
            throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败，找不到上级审批人！");
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
        iceBackApplyDao.updateById(iceBackApply);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, value = "transactionManager")
    public void updateExamineStatus(IceBoxRequest iceBoxRequest) {

        Integer status = iceBoxRequest.getStatus();
        String applyNumber = iceBoxRequest.getApplyNumber();

        if (status == 0) {
            // 审批中
            IceBackApply iceBackApply = new IceBackApply();
            iceBackApply.setExamineStatus(ExamineStatusEnum.IS_DEFAULT.getStatus());
            iceBackApplyDao.update(iceBackApply, Wrappers.<IceBackApply>lambdaQuery().eq(IceBackApply::getApplyNumber, applyNumber));

        } else if (status == 1) {
            //批准
            doTransfer(applyNumber);
            IceBackApply iceBackApply = iceBackApplyDao.selectOne(Wrappers.<IceBackApply>lambdaQuery().eq(IceBackApply::getApplyNumber, applyNumber));
            iceBackApply.setExamineStatus(ExamineStatusEnum.IS_PASS.getStatus());
            iceBackApplyDao.updateById(iceBackApply);
            CompletableFuture.runAsync(() ->
                    feignCusLabelClient.manualExpired(9999, iceBackApply.getBackStoreNumber()), ExecutorServiceFactory.getInstance());
        } else if (status == 2) {
            // 驳回
            IceBackApply iceBackApply = new IceBackApply();
            iceBackApply.setExamineStatus(ExamineStatusEnum.UN_PASS.getStatus());
            iceBackApplyDao.update(iceBackApply, Wrappers.<IceBackApply>lambdaQuery().eq(IceBackApply::getApplyNumber, applyNumber));
        }
    }

    @Override
    public String checkBackIceBox(Integer iceBoxId) {

        String result = "";

        // // TODO: 2020/4/28  需要倒序

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

        // 主表条件
        String payEndTime = iceDepositPage.getPayEndTime();
        String payStartTime = iceDepositPage.getPayStartTime();

        LambdaQueryWrapper<IceBackOrder> wrapper = Wrappers.<IceBackOrder>lambdaQuery();

        if (StringUtils.isNotBlank(payStartTime)) {
            wrapper.ge(IceBackOrder::getUpdatedTime, payStartTime);
        }

        if (StringUtils.isNotBlank(payEndTime)) {
            wrapper.le(IceBackOrder::getUpdatedTime, payEndTime);
        }

        // 副表条件
        String assetId = iceDepositPage.getAssetId();
        String chestModel = iceDepositPage.getChestModel();
        String clientName = iceDepositPage.getClientName();
        String clientNumber = iceDepositPage.getClientNumber();
        String contactMobile = iceDepositPage.getContactMobile();
        Integer marketAreaId = iceDepositPage.getMarketAreaId();

        LambdaQueryWrapper<IceBox> iceBoxWrapper = Wrappers.<IceBox>lambdaQuery();
        LambdaQueryWrapper<IceBoxExtend> iceBoxExtendWrapper = Wrappers.<IceBoxExtend>lambdaQuery();
        LambdaQueryWrapper<IceBackApply> iceBackApplyWrapper = Wrappers.<IceBackApply>lambdaQuery();


        if (StringUtils.isNotBlank(clientNumber)) {
            iceBackApplyWrapper.eq(IceBackApply::getBackStoreNumber, clientNumber);
        }

        if (StringUtils.isNotBlank(clientName)) {
            List<StoreInfoDtoVo> storeInfoDtoVos = FeignResponseUtil.getFeignData(feignStoreClient.getByName(clientName));

            List<String> storeNumberList = storeInfoDtoVos.stream().map(StoreInfoDtoVo::getStoreNumber).collect(Collectors.toList());
            iceBackApplyWrapper.in(IceBackApply::getBackStoreNumber, storeNumberList);
        }

        if (StringUtils.isNotBlank(contactMobile)) {
            List<StoreInfoDtoVo> storeInfoDtoVos = FeignResponseUtil.getFeignData(feignStoreClient.getByMobile(contactMobile));
            List<String> storeNumberList = storeInfoDtoVos.stream().map(StoreInfoDtoVo::getStoreNumber).collect(Collectors.toList());
            iceBackApplyWrapper.in(IceBackApply::getBackStoreNumber, storeNumberList);
        }
        if (StringUtils.isNotBlank(chestModel)) {

            List<IceModel> iceModels = iceModelDao.selectList(Wrappers.<IceModel>lambdaQuery().like(IceModel::getChestName, chestModel));
            List<Integer> iceModelIds = iceModels.stream().map(IceModel::getId).collect(Collectors.toList());

            iceBoxWrapper.in(IceBox::getModelId, iceModelIds);
        }

        if (StringUtils.isNotBlank(assetId)) {
            iceBoxExtendWrapper.like(IceBoxExtend::getAssetId, assetId);
        }

        if (marketAreaId != null) {
            iceBoxWrapper.eq(IceBox::getDeptId, marketAreaId);
        }


        // 取 iceBoxId 的交集

        List<IceBox> iceBoxes = iceBoxDao.selectList(iceBoxWrapper);
        List<IceBoxExtend> iceBoxExtends = iceBoxExtendDao.selectList(iceBoxExtendWrapper);
        List<IceBackApply> iceBackApplies = iceBackApplyDao.selectList(iceBackApplyWrapper);

        List<Integer> list = new ArrayList<>();

        if (CollectionUtil.isNotEmpty(iceBoxes)) {


        }
        if (CollectionUtil.isNotEmpty(iceBoxExtends)) {


        }
        if (CollectionUtil.isNotEmpty(iceBackApplies)) {


        }


        IPage<IceBackOrder> iPage = iceBackOrderDao.selectPage(iceDepositPage, wrapper);


        return null;
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

        IcePutApply icePutApply = icePutApplyDao.selectOne(Wrappers.<IcePutApply>lambdaQuery().eq(IcePutApply::getApplyNumber, iceBoxExtend.getLastApplyNumber()));
        // 校验: 投放表中数据
        if (Objects.isNull(icePutApply)) {
            throw new NormalOptionException(ResultEnum.CANNOT_FIND_ICE_BOX_APPLY.getCode(), ResultEnum.CANNOT_FIND_ICE_BOX_APPLY.getMessage());
        }

        IcePutApplyRelateBox icePutApplyRelateBox = icePutApplyRelateBoxDao.selectOne(Wrappers.<IcePutApplyRelateBox>lambdaQuery()
                .eq(IcePutApplyRelateBox::getApplyNumber, iceBoxExtend.getLastApplyNumber())
                .eq(IcePutApplyRelateBox::getBoxId, iceBoxId));

        IcePutPactRecord icePutPactRecord = icePutPactRecordDao.selectOne(Wrappers.<IcePutPactRecord>lambdaQuery()
                .eq(IcePutPactRecord::getApplyNumber, iceBoxExtend.getLastApplyNumber())
                .eq(IcePutPactRecord::getBoxId, iceBoxId));

        // 校验: 电子协议
        if (icePutPactRecord == null) {
            throw new NormalOptionException(ResultEnum.CANNOT_FIND_ICE_PUT_PACT_RECORD.getCode(), ResultEnum.CANNOT_FIND_ICE_PUT_PACT_RECORD.getMessage());
        }

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

    private void doTransfer(String applyNumber) {

        IceBackApplyRelateBox iceBackApplyRelateBox = iceBackApplyRelateBoxDao.selectOne(Wrappers.<IceBackApplyRelateBox>lambdaQuery().eq(IceBackApplyRelateBox::getApplyNumber, applyNumber));

        IceBackApply iceBackApply = iceBackApplyDao.selectOne(Wrappers.<IceBackApply>lambdaQuery().eq(IceBackApply::getApplyNumber, applyNumber));

        Integer iceBoxId = iceBackApplyRelateBox.getBoxId();
        IceBox iceBox = iceBoxDao.selectById(iceBoxId);
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


        // 插入交易记录
        iceTransferRecordDao.insert(iceTransferRecord);

        // 更新冰柜状态
//        iceBoxExtend.setLastPutTime(date);
//        iceBoxExtend.setLastPutId(iceTransferRecord.getId());
//        iceBoxExtend.setLastApplyNumber(applyNumber);
//        iceBoxExtendDao.updateById(iceBoxExtend);

        // 更新冰柜状态
        iceBox.setPutStatus(PutStatus.NO_PUT.getStatus());
        iceBox.setPutStoreNumber("0");
        iceBox.setSupplierId(iceBackApplyRelateBox.getBackSupplierId());
        iceBoxDao.updateById(iceBox);
//         免押时, 不校验订单, 直接跳过
        if (FreePayTypeEnum.IS_FREE.getType().equals(icePutApplyRelateBox.getFreeType())) {
            return;
        }


        // 非免押，但是不退押金，直接跳过
        if (BackType.BACK_WITHOUT_MONEY.getType() == iceBackApplyRelateBox.getBackType()) {
            return;
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
                .wxappid(xcxConfig.getAppid())
                .openid(icePutOrder.getOpenid())
//                .paymentAmount(orderInfo.getPayMoney().multiply(new BigDecimal(100)))
                .paymentAmount(icePutOrder.getPayMoney())
                .wechatPayType(WechatPayTypeEnum.FOR_TRANSFER.getType())
                .mchType(xcxConfig.getMchType())
                .build();

        TransferReponse transferReponse = FeignResponseUtil.getFeignData(feignTransferClient.transfer(transferRequest));

        log.info("转账服务返回的数据-->[{}]", JSON.toJSONString(transferReponse, true));
        // 修改冰柜状态
    }


    private void doBack(Integer iceBoxId) {
        // 退还编号
        String applyNumber = "BAC" + IdUtil.simpleUUID().substring(0, 29);

        // 创建通知
        DateTime date = new DateTime();
        String prefix = date.toString("yyyyMMdd");
//        String blockName = "冰柜退还确认单";
        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(iceBoxId);
        IceBox iceBox = iceBoxDao.selectById(iceBoxId);
        String putStoreNumber = iceBox.getPutStoreNumber();

        Map<String, SessionStoreInfoVo> map = FeignResponseUtil.getFeignData(feignStoreClient.getSessionStoreInfoVo(Collections.singletonList(putStoreNumber)));

        SessionStoreInfoVo sessionStoreInfoVo = map.get(putStoreNumber);

        Integer userId = sessionStoreInfoVo.getUserId();

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
                .build();

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

}

