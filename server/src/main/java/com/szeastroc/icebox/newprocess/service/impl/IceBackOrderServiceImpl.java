package com.szeastroc.icebox.newprocess.service.impl;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.exception.NormalOptionException;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.customer.client.FeignStoreClient;
import com.szeastroc.customer.common.vo.SessionStoreInfoVo;
import com.szeastroc.icebox.config.XcxConfig;
import com.szeastroc.icebox.enums.FreePayTypeEnum;
import com.szeastroc.icebox.enums.OrderStatus;
import com.szeastroc.icebox.enums.ResultEnum;
import com.szeastroc.icebox.newprocess.dao.*;
import com.szeastroc.icebox.newprocess.entity.*;
import com.szeastroc.icebox.newprocess.enums.BackType;
import com.szeastroc.icebox.newprocess.service.IceBackOrderService;
import com.szeastroc.icebox.newprocess.vo.SimpleIceBoxDetailVo;
import com.szeastroc.icebox.newprocess.vo.request.IceBoxPage;
import com.szeastroc.icebox.oldprocess.dao.WechatTransferOrderDao;
import com.szeastroc.icebox.oldprocess.entity.WechatTransferOrder;
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

import javax.annotation.Resource;
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

    @Autowired
    private FeignOutBacklogClient feignOutBacklogClient;
    @Resource
    private IceBackApplyRelateBoxDao iceBackApplyRelateBoxDao;

    @Resource
    private IceBackOrderDao iceBackOrderDao;
    @Resource
    private IceBackApplyDao iceBackApplyDao;

    @Autowired
    private FeignStoreClient feignStoreClient;

    @Autowired
    private FeignDeptClient feignDeptClient;
    @Autowired
    private FeignUserClient feignUserClient;
    @Autowired
    private FeignOutExamineClient feignOutExamineClient;
    @Autowired
    private WechatTransferOrderDao wechatTransferOrderDao;
    @Autowired
    private XcxConfig xcxConfig;
    @Autowired
    private FeignTransferClient feignTransferClient;

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    @Override
    public void takeBackOrder(Integer iceBoxId) {
        // 校验
        validateTakeBack(iceBoxId);

        // TODO 由崔梦阳实现退还逻辑

        // 创建通知
        DateTime date = new DateTime();
        String prefix = date.toString("yyyyMMdd");
        String blockName = "冰柜退押确认";
        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(iceBoxId);
        IceBox iceBox = iceBoxDao.selectById(iceBoxId);
        String putStoreNumber = iceBox.getPutStoreNumber();

        Map<String, SessionStoreInfoVo> map = FeignResponseUtil.getFeignData(feignStoreClient.getSessionStoreInfoVo(Collections.singletonList(putStoreNumber)));

        SessionStoreInfoVo sessionStoreInfoVo = map.get(putStoreNumber);

        Integer userId = sessionStoreInfoVo.getUserId();

        String assetId = iceBoxExtend.getAssetId();
        String relateCode = prefix + "_" + assetId;
        NoticeBacklogRequestVo noticeBacklogRequestVo = NoticeBacklogRequestVo.builder()
                .backlogName(blockName)
                .noticeTypeEnum(NoticeTypeEnum.ICEBOX_REFUND_CONFIRM)
                .relateCode(relateCode)
                .sendUserId(5941)
                .build();
        feignOutBacklogClient.createNoticeBacklog(noticeBacklogRequestVo);

    }

    @Override
    @Transactional(rollbackFor = Exception.class, value = "transactionManager")
    public void doRefund(SimpleIceBoxDetailVo simpleIceBoxDetailVo) {

        // 退还编号
        String applyNumber = "BAC" + IdUtil.simpleUUID().substring(0, 29);
        Integer iceBoxId = simpleIceBoxDetailVo.getId();

        IcePutOrder icePutOrder = icePutOrderDao.selectOne(Wrappers.<IcePutOrder>lambdaQuery()
                .eq(IcePutOrder::getApplyNumber, simpleIceBoxDetailVo.getLastPutNumber())
                .eq(IcePutOrder::getChestId, iceBoxId));


        IcePutApplyRelateBox icePutApplyRelateBox = icePutApplyRelateBoxDao.selectOne(Wrappers.<IcePutApplyRelateBox>lambdaQuery()
                .eq(IcePutApplyRelateBox::getApplyNumber, simpleIceBoxDetailVo.getLastPutNumber())
                .eq(IcePutApplyRelateBox::getBoxId, iceBoxId));

        Integer backType = simpleIceBoxDetailVo.getBackType();

        IceBackOrder iceBackOrder = IceBackOrder.builder()
                .boxId(iceBoxId)
                .amount(backType.equals(BackType.BACK_MONEY.getType()) ? icePutOrder.getPayMoney() : BigDecimal.ZERO)
                .applyNumber(icePutOrder.getApplyNumber())
                .openid(icePutOrder.getOpenid())
                .putOrderId(icePutOrder.getId())
                .partnerTradeNo(icePutOrder.getOrderNum())
                .build();


        IceBackApplyRelateBox iceBackApplyRelateBox = IceBackApplyRelateBox.builder()
                .applyNumber(applyNumber)
                .backSupplierId(simpleIceBoxDetailVo.getSupplierId())
                .backType(backType)
                .freeType(icePutApplyRelateBox.getFreeType())
                .boxId(iceBoxId)
                .modelId(simpleIceBoxDetailVo.getChestModelId())
                .build();


        // 创建审批流

        SimpleUserInfoVo simpleUserInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findSimpleUserById(simpleIceBoxDetailVo.getUserId()));
        Map<Integer, SessionUserInfoVo> sessionUserInfoMap = FeignResponseUtil.getFeignData(feignDeptClient.findLevelLeaderByDeptId(simpleUserInfoVo.getSimpleDeptInfoVos().get(0).getId()));
//        List<Integer> userIds = new ArrayList<Integer>();
//        获取上级部门领导
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

        feignOutExamineClient.iceBoxRefund(sessionExamineVo);


        IceBackApply iceBackApply = IceBackApply.builder()
                .applyNumber(applyNumber)
                .backStoreNumber(simpleIceBoxDetailVo.getPutStoreNumber())
                .userId(simpleIceBoxDetailVo.getUserId())
                .createdBy(simpleIceBoxDetailVo.getUserId())
                .build();


        iceBackOrderDao.insert(iceBackOrder);

        iceBackApplyRelateBoxDao.insert(iceBackApplyRelateBox);

        iceBackApplyDao.insert(iceBackApply);


    }

    @Override
    public void doTransfer(Integer iceBoxId) {
//        IceBox iceBox = iceBoxDao.selectById(iceBoxId);
//        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(iceBoxId);
//
//        IcePutApply icePutApply = icePutApplyDao.selectOne(Wrappers.<IcePutApply>lambdaQuery().eq(IcePutApply::getApplyNumber, iceBoxExtend.getLastApplyNumber()));
//
//
//        IcePutApplyRelateBox icePutApplyRelateBox = icePutApplyRelateBoxDao.selectOne(Wrappers.<IcePutApplyRelateBox>lambdaQuery()
//                .eq(IcePutApplyRelateBox::getApplyNumber, iceBoxExtend.getLastApplyNumber())
//                .eq(IcePutApplyRelateBox::getBoxId, iceBoxId));
//
//        IcePutPactRecord icePutPactRecord = icePutPactRecordDao.selectOne(Wrappers.<IcePutPactRecord>lambdaQuery()
//                .eq(IcePutPactRecord::getApplyNumber, iceBoxExtend.getLastApplyNumber())
//                .eq(IcePutPactRecord::getBoxId, iceBoxId));

        // 免押时, 不校验订单, 直接跳过
//        if (FreePayTypeEnum.IS_FREE.getType() == icePutApplyRelateBox.getFreeType()) {
//            return;
//        }
//
//        IcePutOrder icePutOrder = icePutOrderDao.selectOne(Wrappers.<IcePutOrder>lambdaQuery()
//                .eq(IcePutOrder::getApplyNumber, icePutApply.getApplyNumber())
//                .eq(IcePutOrder::getChestId, iceBoxId));
//
//        WechatTransferOrder wechatTransferOrder = new WechatTransferOrder(String.valueOf(icePutOrder.getId()), iceBoxId,
//                icePutPactRecord.getId(), icePutOrder.getId(), icePutOrder.getOpenid(), icePutOrder.getPayMoney());
//
//        log.info("wechatTransferOrder存入数据库 -> [{}]", JSON.toJSONString(wechatTransferOrder));
//        wechatTransferOrderDao.insert(wechatTransferOrder);
//
//        /**
//         * 调用转账服务
//         */
//        TransferRequest transferRequest = TransferRequest.builder()
//                .resourceType(ResourceTypeEnum.FROM_ICEBOX.getType())
//                .resourceKey(String.valueOf(icePutOrder.getId()))
//                .wxappid(xcxConfig.getAppid())
//                .openid(icePutOrder.getOpenid())
////                .paymentAmount(orderInfo.getPayMoney().multiply(new BigDecimal(100)))
//                .paymentAmount(icePutOrder.getPayMoney())
//                .wechatPayType(WechatPayTypeEnum.FOR_TRANSFER.getType())
//                .mchType(xcxConfig.getMchType())
//                .build();
//
//        TransferReponse transferReponse = FeignResponseUtil.getFeignData(feignTransferClient.transfer(transferRequest));

        // 修改冰柜状态
    }

    @Override
    public IPage findPage(IceBoxPage iceBoxPage) {
        Integer deptId = iceBoxPage.getDeptId(); // 营销区域id
        if(deptId!=null){

        }

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
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }

        IcePutApply icePutApply = icePutApplyDao.selectOne(Wrappers.<IcePutApply>lambdaQuery().eq(IcePutApply::getApplyNumber, iceBoxExtend.getLastApplyNumber()));
        // 校验: 投放表中数据
        if (Objects.isNull(icePutApply)) {
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }

        IcePutApplyRelateBox icePutApplyRelateBox = icePutApplyRelateBoxDao.selectOne(Wrappers.<IcePutApplyRelateBox>lambdaQuery()
                .eq(IcePutApplyRelateBox::getApplyNumber, iceBoxExtend.getLastApplyNumber())
                .eq(IcePutApplyRelateBox::getBoxId, iceBoxId));

        IcePutPactRecord icePutPactRecord = icePutPactRecordDao.selectOne(Wrappers.<IcePutPactRecord>lambdaQuery()
                .eq(IcePutPactRecord::getApplyNumber, iceBoxExtend.getLastApplyNumber())
                .eq(IcePutPactRecord::getBoxId, iceBoxId));

        // 校验: 电子协议
        if (icePutPactRecord == null) {
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD + ": 未找到对应的电子协议");
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
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }
        if (!icePutOrder.getStatus().equals(OrderStatus.IS_FINISH.getStatus())) {
            throw new ImproperOptionException(Constants.ErrorMsg.RECORD_DATA_ERROR + ": 订单未完成");
        }
    }
}

