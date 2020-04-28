package com.szeastroc.icebox.newprocess.service.impl;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.exception.NormalOptionException;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.customer.client.FeignStoreClient;
import com.szeastroc.customer.client.FeignSupplierClient;
import com.szeastroc.customer.common.vo.SessionStoreInfoVo;
import com.szeastroc.icebox.config.XcxConfig;
import com.szeastroc.icebox.enums.ExamineStatusEnum;
import com.szeastroc.icebox.enums.FreePayTypeEnum;
import com.szeastroc.icebox.enums.OrderStatus;
import com.szeastroc.icebox.enums.ResultEnum;
import com.szeastroc.icebox.newprocess.dao.*;
import com.szeastroc.icebox.newprocess.entity.*;
import com.szeastroc.icebox.newprocess.enums.BackType;
import com.szeastroc.icebox.newprocess.enums.ServiceType;
import com.szeastroc.icebox.newprocess.service.IceBackOrderService;
import com.szeastroc.icebox.newprocess.vo.SimpleIceBoxDetailVo;
import com.szeastroc.icebox.oldprocess.dao.WechatTransferOrderDao;
import com.szeastroc.icebox.oldprocess.entity.IceEventRecord;
import com.szeastroc.icebox.oldprocess.entity.WechatTransferOrder;
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
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

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


    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    @Override
    public void takeBackOrder(Integer iceBoxId) {
        // 校验是否可申请退还
        validateTakeBack(iceBoxId);

        // TODO 由崔梦阳实现退还逻辑

        IceBackOrder selectIceBackOrder = iceBackOrderDao.selectOne(Wrappers.<IceBackOrder>lambdaQuery()
                .eq(IceBackOrder::getBoxId, iceBoxId)
                .orderByDesc(IceBackOrder::getCreatedTime)
                .last("limit 1"));

        if(selectIceBackOrder != null) {
            // 该冰柜存在过退还
            String selectApplyNumber = selectIceBackOrder.getApplyNumber();
            IceBackApply selectIceBackApply = iceBackApplyDao.selectOne(Wrappers.<IceBackApply>lambdaQuery().eq(IceBackApply::getApplyNumber, selectApplyNumber));

            Integer examineStatus = selectIceBackApply.getExamineStatus();

            if (examineStatus.equals(ExamineStatusEnum.IS_PASS.getStatus()) || examineStatus.equals(ExamineStatusEnum.UN_PASS.getStatus())) {
                log.info("该冰柜最后一次申请退还已经通过或者驳回,冰柜id-->[{}]", iceBoxId);
                // 退还编号
                doBack(iceBoxId);
            } else {

                log.info("该冰柜最后一次申请退还已经未通过或者未驳回,冰柜id-->[{}]", iceBoxId);
                throw new NormalOptionException(ResultEnum.ICE_BOX_IS_REFUNDING.getCode(),ResultEnum.ICE_BOX_IS_REFUNDING.getMessage());
            }
        }else {
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
//                .eq(IcePutOrder::getChestId, iceBoxId));
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
        IceBackOrder iceBackOrder = iceBackOrderDao.selectOne(Wrappers.<IceBackOrder>lambdaQuery().eq(IceBackOrder::getBoxId, simpleIceBoxDetailVo.getId()).orderByDesc(IceBackOrder::getCreatedTime).last("limit 1"));
        String applyNumber = iceBackOrder.getApplyNumber();
        IceBackApply iceBackApply = iceBackApplyDao.selectOne(Wrappers.<IceBackApply>lambdaQuery().eq(IceBackApply::getApplyNumber, applyNumber));
        IceBackApplyRelateBox iceBackApplyRelateBox = iceBackApplyRelateBoxDao.selectOne(Wrappers.<IceBackApplyRelateBox>lambdaQuery().eq(IceBackApplyRelateBox::getApplyNumber, applyNumber));


        SimpleUserInfoVo simpleUserInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findSimpleUserById(simpleIceBoxDetailVo.getUserId()));
        Map<Integer, SessionUserInfoVo> sessionUserInfoMap = FeignResponseUtil.getFeignData(feignDeptClient.findLevelLeaderByDeptId(simpleUserInfoVo.getSimpleDeptInfoVos().get(0).getId()));
        //        获取上级部门领导
//        List<Integer> userIds = new ArrayList<Integer>();
//        SessionUserInfoVo userInfoVo1 = sessionUserInfoMap.get(1);
//        SessionUserInfoVo userInfoVo2 = sessionUserInfoMap.get(2);
//        SessionUserInfoVo userInfoVo3 = sessionUserInfoMap.get(2);
//        if (userInfoVo1 == null || userInfoVo2 == null || userInfoVo3 == null) {
//            throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败，找不到上级审批人！");
//        }
//        userIds.add(userInfoVo1.getId());
//        userIds.add(userInfoVo2.getId());
//        userIds.add(userInfoVo3.getId());

        List<Integer> userIds = Arrays.asList(5941, 2103);
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


        Integer examineId = FeignResponseUtil.getFeignData(feignOutExamineClient.iceBoxRefund(sessionExamineVo));
        Integer backType = simpleIceBoxDetailVo.getBackType();
        IcePutOrder icePutOrder = icePutOrderDao.selectOne(Wrappers.<IcePutOrder>lambdaQuery()
                .eq(IcePutOrder::getApplyNumber, simpleIceBoxDetailVo.getLastPutNumber())
                .eq(IcePutOrder::getChestId, simpleIceBoxDetailVo.getId()));
        // 更新退还数据
        iceBackOrder.setAmount(backType.equals(BackType.BACK_MONEY.getType()) ? icePutOrder.getPayMoney() : BigDecimal.ZERO);
        iceBackApplyRelateBox.setBackSupplierId(simpleIceBoxDetailVo.getNewSupplierId());
        iceBackApplyRelateBox.setBackType(backType);

        iceBackApply.setUserId(simpleIceBoxDetailVo.getUserId());
        iceBackApply.setCreatedBy(simpleIceBoxDetailVo.getUserId());
        iceBackApply.setExamineId(examineId);
        iceBackApply.setExamineStatus(ExamineStatusEnum.IS_DEFAULT.getStatus());

        iceBackOrderDao.updateById(iceBackOrder);

        iceBackApplyRelateBoxDao.updateById(iceBackApplyRelateBox);

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
            IceBackApply iceBackApply = new IceBackApply();
            iceBackApply.setExamineStatus(ExamineStatusEnum.IS_PASS.getStatus());
            iceBackApplyDao.update(iceBackApply, Wrappers.<IceBackApply>lambdaQuery().eq(IceBackApply::getApplyNumber, applyNumber));
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

        IceBackApplyRelateBox iceBackApplyRelateBox = iceBackApplyRelateBoxDao.selectOne(Wrappers.<IceBackApplyRelateBox>lambdaQuery().eq(IceBackApplyRelateBox::getBoxId, iceBoxId).last("limit 1"));

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
            throw new NormalOptionException(ResultEnum.CANNOT_FIND_ICE_BOX.getCode(),ResultEnum.CANNOT_FIND_ICE_BOX.getMessage());
        }

        IcePutApply icePutApply = icePutApplyDao.selectOne(Wrappers.<IcePutApply>lambdaQuery().eq(IcePutApply::getApplyNumber, iceBoxExtend.getLastApplyNumber()));
        // 校验: 投放表中数据
        if (Objects.isNull(icePutApply)) {
            throw new NormalOptionException(ResultEnum.CANNOT_FIND_ICE_BOX_APPLY.getCode(),ResultEnum.CANNOT_FIND_ICE_BOX_APPLY.getMessage());
        }

        IcePutApplyRelateBox icePutApplyRelateBox = icePutApplyRelateBoxDao.selectOne(Wrappers.<IcePutApplyRelateBox>lambdaQuery()
                .eq(IcePutApplyRelateBox::getApplyNumber, iceBoxExtend.getLastApplyNumber())
                .eq(IcePutApplyRelateBox::getBoxId, iceBoxId));

        IcePutPactRecord icePutPactRecord = icePutPactRecordDao.selectOne(Wrappers.<IcePutPactRecord>lambdaQuery()
                .eq(IcePutPactRecord::getApplyNumber, iceBoxExtend.getLastApplyNumber())
                .eq(IcePutPactRecord::getBoxId, iceBoxId));

        // 校验: 电子协议
        if (icePutPactRecord == null) {
            throw new NormalOptionException(ResultEnum.CANNOT_FIND_ICE_PUT_PACT_RECORD.getCode(),ResultEnum.CANNOT_FIND_ICE_PUT_PACT_RECORD.getMessage());
        }

        // 校验退还到期时间
        if (icePutPactRecord.getPutExpireTime().getTime() > new Date().getTime()) {
            throw new NormalOptionException(ResultEnum.TAKE_BAKE_ERR_WITH_EXPIRE_TIME.getCode(), ResultEnum.TAKE_BAKE_ERR_WITH_EXPIRE_TIME.getMessage());
        }

        // 免押时, 不校验订单, 直接跳过
        if (FreePayTypeEnum.IS_FREE.getType() == icePutApplyRelateBox.getFreeType()) {
            return;
        }

        IcePutOrder icePutOrder = icePutOrderDao.selectOne(Wrappers.<IcePutOrder>lambdaQuery()
                .eq(IcePutOrder::getApplyNumber, icePutApply.getApplyNumber())
                .eq(IcePutOrder::getChestId, iceBoxId));
        /**
         * 校验: 订单号
         */
        if (Objects.isNull(icePutOrder)) {
            throw new NormalOptionException(ResultEnum.CANNOT_FIND_ICE_PUT_ORDER.getCode(),ResultEnum.CANNOT_FIND_ICE_PUT_ORDER.getMessage());
        }
        if (!icePutOrder.getStatus().equals(OrderStatus.IS_FINISH.getStatus())) {
            throw new NormalOptionException(ResultEnum.PUT_ORDER_IS_NOT_FINISH.getCode(),ResultEnum.PUT_ORDER_IS_NOT_FINISH.getMessage());
        }
    }

    private void doTransfer(String applyNumber) {

        IceBackApplyRelateBox iceBackApplyRelateBox = iceBackApplyRelateBoxDao.selectOne(Wrappers.<IceBackApplyRelateBox>lambdaQuery().eq(IceBackApplyRelateBox::getApplyNumber, applyNumber));

        IceBackApply iceBackApply = iceBackApplyDao.selectOne(Wrappers.<IceBackApply>lambdaQuery().eq(IceBackApply::getApplyNumber, applyNumber));


        IceBackOrder iceBackOrder = iceBackOrderDao.selectOne(Wrappers.<IceBackOrder>lambdaQuery().eq(IceBackOrder::getApplyNumber, applyNumber));


        Integer iceBoxId = iceBackApplyRelateBox.getBoxId();
        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(iceBoxId);

        IcePutApply icePutApply = icePutApplyDao.selectOne(Wrappers.<IcePutApply>lambdaQuery().eq(IcePutApply::getApplyNumber, iceBoxExtend.getLastApplyNumber()));


        IcePutApplyRelateBox icePutApplyRelateBox = icePutApplyRelateBoxDao.selectOne(Wrappers.<IcePutApplyRelateBox>lambdaQuery()
                .eq(IcePutApplyRelateBox::getApplyNumber, iceBoxExtend.getLastApplyNumber())
                .eq(IcePutApplyRelateBox::getBoxId, iceBoxId));

        IcePutPactRecord icePutPactRecord = icePutPactRecordDao.selectOne(Wrappers.<IcePutPactRecord>lambdaQuery()
                .eq(IcePutPactRecord::getApplyNumber, iceBoxExtend.getLastApplyNumber())
                .eq(IcePutPactRecord::getBoxId, iceBoxId));


        IceTransferRecord iceTransferRecord = IceTransferRecord.builder()
                .applyNumber(applyNumber)
                .serviceType(ServiceType.IS_RETURN.getType())
                .boxId(iceBoxId)
                .supplierId(iceBackApplyRelateBox.getBackSupplierId())
                .storeNumber(iceBackApply.getBackStoreNumber())
                .transferMoney(iceBackOrder.getAmount())
                .applyUserId(iceBackApply.getUserId())
                .build();


        // 插入交易记录
        iceTransferRecordDao.insert(iceTransferRecord);

        // 更新冰柜状态
        iceBoxExtend.setLastPutTime(new Date());
        iceBoxExtend.setLastPutId(iceTransferRecord.getId());
        iceBoxExtend.setLastApplyNumber(applyNumber);
        iceBoxExtendDao.updateById(iceBoxExtend);

//         免押时, 不校验订单, 直接跳过
        if (FreePayTypeEnum.IS_FREE.getType() == icePutApplyRelateBox.getFreeType()) {
            return;
        }

        IcePutOrder icePutOrder = icePutOrderDao.selectOne(Wrappers.<IcePutOrder>lambdaQuery()
                .eq(IcePutOrder::getApplyNumber, icePutApply.getApplyNumber())
                .eq(IcePutOrder::getChestId, iceBoxId));

        WechatTransferOrder wechatTransferOrder = new WechatTransferOrder(String.valueOf(icePutOrder.getId()), iceBoxId,
                icePutPactRecord.getId(), icePutOrder.getId(), icePutOrder.getOpenid(), icePutOrder.getPayMoney());

        log.info("wechatTransferOrder存入数据库 -> [{}]", JSON.toJSONString(wechatTransferOrder));
        wechatTransferOrderDao.insert(wechatTransferOrder);

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
                .sendUserId(5941) //
                .build();
        // 创建通知
        feignOutBacklogClient.createNoticeBacklog(noticeBacklogRequestVo);

        IcePutOrder icePutOrder = icePutOrderDao.selectOne(Wrappers.<IcePutOrder>lambdaQuery()
                .eq(IcePutOrder::getApplyNumber, iceBoxExtend.getLastApplyNumber())
                .eq(IcePutOrder::getChestId, iceBoxId));

        IcePutApplyRelateBox icePutApplyRelateBox = icePutApplyRelateBoxDao.selectOne(Wrappers.<IcePutApplyRelateBox>lambdaQuery()
                .eq(IcePutApplyRelateBox::getApplyNumber, iceBoxExtend.getLastApplyNumber())
                .eq(IcePutApplyRelateBox::getBoxId, iceBoxId));

        IceBackOrder iceBackOrder = IceBackOrder.builder()
                .boxId(iceBoxId)
                .applyNumber(applyNumber)
                .openid(icePutOrder.getOpenid())
                .putOrderId(icePutOrder.getId())
                .partnerTradeNo(icePutOrder.getOrderNum())
                .build();

        IceBackApplyRelateBox iceBackApplyRelateBox = IceBackApplyRelateBox.builder()
                .applyNumber(applyNumber)
                .freeType(icePutApplyRelateBox.getFreeType())
                .boxId(iceBoxId)
                .modelId(iceBox.getModelId())
                .build();

        IceBackApply iceBackApply = IceBackApply.builder()
                .applyNumber(applyNumber)
                .backStoreNumber(putStoreNumber)
                .build();

        iceBackOrderDao.insert(iceBackOrder);
        iceBackApplyRelateBoxDao.insert(iceBackApplyRelateBox);
        iceBackApplyDao.insert(iceBackApply);
    }

}

