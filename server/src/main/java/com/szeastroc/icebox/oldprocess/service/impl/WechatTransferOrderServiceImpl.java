package com.szeastroc.icebox.oldprocess.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.entity.transfer.enums.ResourceTypeEnum;
import com.szeastroc.common.entity.transfer.enums.WechatPayTypeEnum;
import com.szeastroc.common.entity.transfer.request.TransferRequest;
import com.szeastroc.common.entity.transfer.response.TransferReponse;
import com.szeastroc.common.enums.CommonStatus;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.exception.NormalOptionException;
import com.szeastroc.common.feign.transfer.FeignTransferClient;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.config.XcxConfig;
import com.szeastroc.icebox.oldprocess.dao.*;
import com.szeastroc.icebox.oldprocess.entity.*;
import com.szeastroc.icebox.enums.*;
import com.szeastroc.icebox.oldprocess.service.WechatTransferOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * Created by Tulane
 * 2019/8/19
 */
@Slf4j
@Service
public class WechatTransferOrderServiceImpl extends ServiceImpl<WechatTransferOrderDao, WechatTransferOrder> implements WechatTransferOrderService {

    @Autowired
    private XcxConfig xcxConfig;
    @Autowired
    private OrderInfoDao orderInfoDao;
    @Autowired
    private IceChestInfoDao iceChestInfoDao;
    @Autowired
    private IceChestPutRecordDao iceChestPutRecordDao;
    @Autowired
    private PactRecordDao pactRecordDao;
    @Autowired
    private WechatTransferOrderDao wechatTransferOrderDao;
    @Autowired
    private FeignTransferClient feignTransferClient;

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    @Override
    public CommonResponse<String> takeBackIceChest(Integer iceChestId, Integer clientId) throws Exception {
        PactRecord pactRecord = new PactRecord();
        IceChestInfo iceChestInfo = new IceChestInfo();
        IceChestPutRecord iceChestPutRecord = new IceChestPutRecord();
        OrderInfo orderInfo = new OrderInfo();

        /**
         * ?????????????????????
         */
        validateTakeBack(iceChestId, clientId, pactRecord, iceChestInfo, iceChestPutRecord, orderInfo);

        /**
         * ????????????: ????????????
         */
        IceChestPutRecord bakIceChestPutRecord = new IceChestPutRecord(iceChestId, null, null,
                iceChestPutRecord.getReceiveClientId(), iceChestPutRecord.getSendClientId(), orderInfo.getPayMoney(),
                RecordStatus.RECEIVE_FINISH.getStatus(), ServiceType.ENTER_WAREHOUSE.getType());

        iceChestPutRecordDao.insert(bakIceChestPutRecord);

        iceChestInfo.setPutStatus(PutStatus.NO_PUT.getStatus());
        iceChestInfo.setClientId(iceChestPutRecord.getSendClientId());
        iceChestInfo.setLastPutId(bakIceChestPutRecord.getId());
        iceChestInfo.setLastPutTime(bakIceChestPutRecord.getCreateTime());
        iceChestInfoDao.updateById(iceChestInfo);

        /**
         * ?????????????????????
         */
        if(FreePayTypeEnum.IS_FREE.getType() == iceChestPutRecord.getFreePayType()){
            return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
        }

        WechatTransferOrder wechatTransferOrder = new WechatTransferOrder(String.valueOf(orderInfo.getId()), iceChestId,
                iceChestPutRecord.getId(), orderInfo.getId(), orderInfo.getOpenid(), orderInfo.getPayMoney());

        log.info("wechatTransferOrder??????????????? -> [{}]", JSON.toJSONString(wechatTransferOrder));
        wechatTransferOrderDao.insert(wechatTransferOrder);

        /**
         * ??????????????????
         */
        TransferRequest transferRequest = TransferRequest.builder()
                .resourceType(ResourceTypeEnum.FROM_ICEBOX.getType())
                .resourceKey(String.valueOf(orderInfo.getId()))
                .wxappid(xcxConfig.getAppid())
                .openid(orderInfo.getOpenid())
//                .paymentAmount(orderInfo.getPayMoney().multiply(new BigDecimal(100)))
                .paymentAmount(orderInfo.getPayMoney())
                .wechatPayType(WechatPayTypeEnum.FOR_TRANSFER.getType())
                .mchType(xcxConfig.getMchType())
                .build();

        TransferReponse transferReponse = FeignResponseUtil.getFeignData(feignTransferClient.transfer(transferRequest));
//        String result = HttpUtils.postJson("http://139.199.154.215:9351/transfer/wx/transfer", JSON.toJSONString(transferRequest));
//        log.info("???????????????transferReponse -> [{}]", result);
//        TransferReponse transferReponse = JSON.parseObject(result, TransferReponse.class);

        /**
         * ????????????
         */
//        wechatTransferOrder.setPaymentNo(transferReponse.getOrderNumber());
//        wechatTransferOrderDao.updateById(wechatTransferOrder);

        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }

    /**
     * takeBackIceChest?????????????????????
     * @param iceChestId
     * @param clientId
     * @param pactRecord
     * @param iceChestInfo
     * @param iceChestPutRecord
     * @throws ImproperOptionException
     */
    private void validateTakeBack(Integer iceChestId, Integer clientId, PactRecord pactRecord, IceChestInfo iceChestInfo,
                                  IceChestPutRecord iceChestPutRecord, OrderInfo orderInfo) throws ImproperOptionException, NormalOptionException {

        PactRecord pactRecordTmp = pactRecordDao.selectOne(Wrappers.<PactRecord>lambdaQuery()
                .eq(PactRecord::getChestId, iceChestId)
                .eq(PactRecord::getClientId, clientId));
        /**
         * ??????: ????????????
         */
        if(pactRecordTmp == null){
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD + ": ??????????????????????????????");
        }
        if(pactRecordTmp.getPutId() == null){
            throw new ImproperOptionException(Constants.ErrorMsg.RECORD_DATA_ERROR + ": ????????????????????????????????????");
        }

        if(pactRecordTmp.getPutExpireTime().getTime() > new Date().getTime()){
            throw new NormalOptionException(ResultEnum.TAKE_BAKE_ERR_WITH_EXPIRE_TIME.getCode(), ResultEnum.TAKE_BAKE_ERR_WITH_EXPIRE_TIME.getMessage());
        }
        BeanUtil.copyProperties(pactRecordTmp, pactRecord);

        IceChestInfo iceChestInfoTmp = iceChestInfoDao.selectById(iceChestId);
        /**
         * ??????: ??????????????????
         */
        if(iceChestInfoTmp == null){
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }
        if(!iceChestInfoTmp.getClientId().equals(clientId)){
            throw new ImproperOptionException(Constants.ErrorMsg.RECORD_DATA_ERROR + "????????????????????????????????????");
        }
        BeanUtil.copyProperties(iceChestInfoTmp, iceChestInfo);

        /**
         * ??????: ????????????
         */
        List<IceChestPutRecord> iceChestPutRecords = iceChestPutRecordDao.selectList(Wrappers.<IceChestPutRecord>lambdaQuery()
                .eq(IceChestPutRecord::getStatus, CommonStatus.VALID.getStatus())
                .eq(IceChestPutRecord::getServiceType, ServiceType.IS_PUT.getType())
                .eq(IceChestPutRecord::getRecordStatus, RecordStatus.RECEIVE_FINISH.getStatus())
                .eq(IceChestPutRecord::getChestId, iceChestId)
                .eq(IceChestPutRecord::getReceiveClientId, clientId)
                .orderByDesc(IceChestPutRecord::getCreateTime));

        /**
         * ??????: ??????????????????
         */
        if(CollectionUtil.isEmpty(iceChestPutRecords)){
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }

        BeanUtil.copyProperties(iceChestPutRecords.get(0), iceChestPutRecord);

        /**
         * ??????: ???????????????????????????????????????????????????????????????
         */
        if(!iceChestPutRecord.getId().equals(pactRecord.getPutId())){
            throw new ImproperOptionException(Constants.ErrorMsg.RECORD_DATA_ERROR + ": ????????????????????????????????????????????????");
        }

        /**
         * ?????????, ???????????????, ????????????
         */
        if(FreePayTypeEnum.IS_FREE.getType() == iceChestPutRecord.getFreePayType()){
            return;
        }

        OrderInfo orderInfoTmp = orderInfoDao.selectOne(Wrappers.<OrderInfo>lambdaQuery().eq(OrderInfo::getChestPutRecordId, iceChestPutRecord.getId()));
        /**
         * ??????: ?????????
         */
        if(orderInfoTmp == null){
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }
        if(!orderInfoTmp.getStatus().equals(OrderStatus.IS_FINISH.getStatus())){
            throw new ImproperOptionException(Constants.ErrorMsg.RECORD_DATA_ERROR + ": ???????????????");
        }
        BeanUtil.copyProperties(orderInfoTmp, orderInfo);
    }
}
