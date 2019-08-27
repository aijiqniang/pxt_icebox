package com.szeastroc.icebox.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.enums.CommonStatus;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.exception.NormalOptionException;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.config.XcxConfig;
import com.szeastroc.icebox.dao.*;
import com.szeastroc.icebox.entity.*;
import com.szeastroc.icebox.enums.*;
import com.szeastroc.icebox.service.WechatTransferOrderService;
import com.szeastroc.icebox.util.wechatpay.WeiXinConfig;
import com.szeastroc.transfer.client.FeignTransferClient;
import com.szeastroc.transfer.common.enums.MchTypeEnum;
import com.szeastroc.transfer.common.enums.ResourceTypeEnum;
import com.szeastroc.transfer.common.request.TransferRequest;
import com.szeastroc.transfer.common.response.TransferReponse;
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
         * 注入对象及校验
         */
        validateTakeBack(iceChestId, clientId, pactRecord, iceChestInfo, iceChestPutRecord, orderInfo);

        /**
         * 退款流程: 数据变更
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
         * 免押则直接返回
         */
        if(FreePayTypeEnum.IS_FREE.getType() == iceChestPutRecord.getFreePayType()){
            return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
        }

        WechatTransferOrder wechatTransferOrder = new WechatTransferOrder(String.valueOf(orderInfo.getId()), iceChestId,
                iceChestPutRecord.getId(), orderInfo.getId(), orderInfo.getOpenid(), orderInfo.getPayMoney());

        log.info("wechatTransferOrder存入数据库 -> [{}]", JSON.toJSONString(wechatTransferOrder));
        wechatTransferOrderDao.insert(wechatTransferOrder);

        /**
         * 调用转账服务
         */
        TransferRequest transferRequest = TransferRequest.builder()
                .resourceType(ResourceTypeEnum.FROM_ICEBOX.getType())
                .resourceKey(String.valueOf(orderInfo.getId()))
                .wxappid(xcxConfig.getAppid())
                .openid(orderInfo.getOpenid())
                .paymentAmount(orderInfo.getPayMoney())
                .mchType(MchTypeEnum.DONG_PENG_SHANG_HU.getType())
                .build();
        TransferReponse transferReponse = FeignResponseUtil.getFeignData(feignTransferClient.transfer(transferRequest));

        /**
         * 修改状态
         */
        wechatTransferOrder.setPaymentNo(transferReponse.getOrderNumber());
        wechatTransferOrderDao.updateById(wechatTransferOrder);

        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }

    /**
     * takeBackIceChest注入对象及校验
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
         * 校验: 电子协议
         */
        if(pactRecordTmp == null){
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD + ": 未找到对应的电子协议");
        }
        if(pactRecordTmp.getPutId() == null){
            throw new ImproperOptionException(Constants.ErrorMsg.RECORD_DATA_ERROR + ": 电子协议没有关联投放记录");
        }

        if(pactRecordTmp.getPutExpireTime().getTime() > new Date().getTime()){
            throw new NormalOptionException(ResultEnum.TAKE_BAKE_ERR_WITH_EXPIRE_TIME.getCode(), ResultEnum.TAKE_BAKE_ERR_WITH_EXPIRE_TIME.getMessage());
        }
        BeanUtil.copyProperties(pactRecordTmp, pactRecord);

        IceChestInfo iceChestInfoTmp = iceChestInfoDao.selectById(iceChestId);
        /**
         * 校验: 冰柜表中数据
         */
        if(iceChestInfoTmp == null){
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }
        if(!iceChestInfoTmp.getClientId().equals(clientId)){
            throw new ImproperOptionException(Constants.ErrorMsg.RECORD_DATA_ERROR + "退回了不属于该客户的冰柜");
        }
        BeanUtil.copyProperties(iceChestInfoTmp, iceChestInfo);

        /**
         * 查询: 投放记录
         */
        List<IceChestPutRecord> iceChestPutRecords = iceChestPutRecordDao.selectList(Wrappers.<IceChestPutRecord>lambdaQuery()
                .eq(IceChestPutRecord::getStatus, CommonStatus.VALID.getStatus())
                .eq(IceChestPutRecord::getServiceType, ServiceType.IS_PUT.getType())
                .eq(IceChestPutRecord::getRecordStatus, RecordStatus.RECEIVE_FINISH.getStatus())
                .eq(IceChestPutRecord::getChestId, iceChestId)
                .eq(IceChestPutRecord::getReceiveClientId, clientId)
                .orderByDesc(IceChestPutRecord::getCreateTime));

        /**
         * 校验: 投放表中数据
         */
        if(CollectionUtil.isEmpty(iceChestPutRecords)){
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }

        BeanUtil.copyProperties(iceChestPutRecords.get(0), iceChestPutRecord);

        /**
         * 校验: 最新投放记录是否与电子协议中的投放记录一致
         */
        if(!iceChestPutRecord.getId().equals(pactRecord.getPutId())){
            throw new ImproperOptionException(Constants.ErrorMsg.RECORD_DATA_ERROR + ": 电子协议投放记录不是最新投放记录");
        }

        /**
         * 免押时, 不校验订单, 直接跳过
         */
        if(FreePayTypeEnum.IS_FREE.getType() == iceChestPutRecord.getFreePayType()){
            return;
        }

        OrderInfo orderInfoTmp = orderInfoDao.selectOne(Wrappers.<OrderInfo>lambdaQuery().eq(OrderInfo::getChestPutRecordId, iceChestPutRecord.getId()));
        /**
         * 校验: 订单号
         */
        if(orderInfoTmp == null){
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }
        if(!orderInfoTmp.getStatus().equals(OrderStatus.IS_FINISH.getStatus())){
            throw new ImproperOptionException(Constants.ErrorMsg.RECORD_DATA_ERROR + ": 订单未完成");
        }
        BeanUtil.copyProperties(orderInfoTmp, orderInfo);
    }
}
