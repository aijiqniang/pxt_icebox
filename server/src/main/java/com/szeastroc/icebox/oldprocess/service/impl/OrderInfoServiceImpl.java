package com.szeastroc.icebox.oldprocess.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.exception.NormalOptionException;
import com.szeastroc.icebox.oldprocess.dao.IceChestInfoDao;
import com.szeastroc.icebox.oldprocess.dao.IceChestPutRecordDao;
import com.szeastroc.icebox.oldprocess.dao.OrderInfoDao;
import com.szeastroc.icebox.oldprocess.dao.PactRecordDao;
import com.szeastroc.icebox.oldprocess.entity.IceChestInfo;
import com.szeastroc.icebox.oldprocess.entity.IceChestPutRecord;
import com.szeastroc.icebox.oldprocess.entity.OrderInfo;
import com.szeastroc.icebox.oldprocess.entity.PactRecord;
import com.szeastroc.icebox.enums.OrderStatus;
import com.szeastroc.icebox.enums.PutStatus;
import com.szeastroc.icebox.enums.RecordStatus;
import com.szeastroc.icebox.enums.ResultEnum;
import com.szeastroc.icebox.oldprocess.service.OrderInfoService;
import com.szeastroc.icebox.oldprocess.vo.ClientInfoRequest;
import com.szeastroc.icebox.util.CommonUtil;
import com.szeastroc.icebox.util.wechatpay.WXPayUtil;
import com.szeastroc.icebox.util.wechatpay.WeiXinConfig;
import com.szeastroc.icebox.util.wechatpay.WeiXinService;
import com.szeastroc.icebox.oldprocess.vo.OrderPayBack;
import com.szeastroc.icebox.oldprocess.vo.OrderPayResponse;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Tulane
 * 2019/5/23
 */
@Slf4j
@Service
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoDao, OrderInfo> implements OrderInfoService {

    @Autowired
    private WeiXinService weiXinService;

    @Autowired
    private WeiXinConfig weiXinConfig;

    @Autowired
    private OrderInfoDao orderInfoDao;
    @Autowired
    private IceChestInfoDao iceChestInfoDao;
    @Autowired
    private IceChestPutRecordDao iceChestPutRecordDao;
    @Autowired
    private PactRecordDao pactRecordDao;

    @Transactional(value = "transactionManager")
    @Override
    public OrderPayResponse createPayInfo(String ip, String openid, Integer iceChestId, Integer chestPutRecordId) throws Exception {

        IceChestInfo iceChestInfo = iceChestInfoDao.selectById(iceChestId);
        if(iceChestInfo == null){
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }

        String orderNum = CommonUtil.generateOrderNumber();
        //????????????????????????
        ClientInfoRequest clientInfoRequest = new ClientInfoRequest();
        clientInfoRequest.setIp(ip);
        String prepayId = weiXinService.createWeiXinPay(clientInfoRequest, iceChestInfo.getDepositMoney(), orderNum, openid);
        //????????????
        OrderInfo orderInfo = new OrderInfo(iceChestInfo.getId(), chestPutRecordId, orderNum, openid, iceChestInfo.getDepositMoney(), prepayId);
        orderInfoDao.insert(orderInfo);

        Map<String, String> datas = new HashMap<>();
        datas.put("appId", weiXinConfig.getAppId());
        datas.put("timeStamp", String.valueOf(System.currentTimeMillis()));
        datas.put("nonceStr", WXPayUtil.generateNonceStr());
        datas.put("package", "prepay_id=" + orderInfo.getPrayId());
        datas.put("signType", "MD5");
        String sign = WXPayUtil.generateSignature(datas, weiXinConfig.getSecret());

        OrderPayResponse orderPayResponse = new OrderPayResponse(iceChestInfo.getFreePayType(), datas.get("appId"),
                datas.get("timeStamp"), datas.get("nonceStr"), datas.get("package"), datas.get("signType"), sign, orderNum);
        return orderPayResponse;
    }

    @Transactional(value = "transactionManager")
    @Override
    public boolean getPayStatus(String orderNum) throws Exception {

        boolean flag = false;

        //????????????????????????????????????
        OrderInfo orderInfo = orderInfoDao.selectOne(Wrappers.<OrderInfo>lambdaQuery().eq(OrderInfo::getOrderNum, orderNum));
        if(orderInfo == null){
            log.info("??????:????????????????????????,?????????????????? -> {}", JSON.toJSONString(orderNum));
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }

        //????????????????????????
        IceChestInfo iceChestInfo = iceChestInfoDao.selectById(orderInfo.getChestId());
        if(iceChestInfo == null){
            log.info("??????:????????????????????????,??????????????????-> {}", JSON.toJSONString(orderInfo));
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }
        //????????????????????????????????????
        IceChestPutRecord iceChestPutRecord = iceChestPutRecordDao.selectById(orderInfo.getChestPutRecordId());
        if(iceChestPutRecord == null){
            log.info("??????:??????????????????,??????????????????????????????-> {}", JSON.toJSONString(orderInfo));
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }

        //?????????????????????, ?????????????????????
        if(orderInfo.getStatus().equals(OrderStatus.IS_FINISH.getStatus())){
            return true;
        }

        //?????????????????????, ????????????
        if(orderInfo.getStatus().equals(OrderStatus.IS_CANCEL.getStatus())){
            throw new NormalOptionException(ResultEnum.ORDER_IS_CANCEL_AND_RETRY_NEW_ORDER.getCode(), ResultEnum.ORDER_IS_CANCEL_AND_RETRY_NEW_ORDER.getMessage());
        }

        //?????????????????????, ??????????????????????????????
        Map<String, String> data = new HashMap<String, String>();
        data.put("appid", weiXinConfig.getAppId());
        data.put("mch_id", weiXinConfig.getMchId());
        data.put("out_trade_no", orderInfo.getOrderNum());
        data.put("nonce_str", WXPayUtil.generateNonceStr());
        String xml = WXPayUtil.generateSignedXml(data, weiXinConfig.getSecret());
        String result = weiXinService.requestOnce("https://api.mch.weixin.qq.com/pay/orderquery", xml);

        OrderPayBack orderPayBack = new OrderPayBack();

        Map<String, String> resultMap = WXPayUtil.xmlToMap(result);
        orderPayBack.setReturnCode(resultMap.get("return_code"));
        orderPayBack.setReturnMsg(resultMap.get("return_msg"));

        if (resultMap.get("return_code").equals("SUCCESS")) {
            orderPayBack.setTradeState(resultMap.get("trade_state"));
            orderPayBack.setResultCode(resultMap.get("result_code"));
            orderPayBack.setOutTradeNo(resultMap.get("out_trade_no"));
            if (resultMap.get("trade_state").equals("SUCCESS") && resultMap.get("result_code").equals("SUCCESS")) {
                orderPayBack.setOpenid(resultMap.get("openid"));
                orderPayBack.setTransactionId(resultMap.get("transaction_id"));
                orderPayBack.setTimeEnd(resultMap.get("time_end"));
                orderPayBack.setTradeStateDesc(resultMap.get("trade_state_desc"));
                orderPayBack.setTotalFee(resultMap.get("total_fee"));
            }
        }

        log.info("???????????? -> {}", JSON.toJSONString(orderPayBack));

        //????????????
        if("SUCCESS".equals(orderPayBack.getReturnCode()) && "SUCCESS".equals(orderPayBack.getTradeState()) && "SUCCESS".equals(orderPayBack.getResultCode())) {
            orderInfo.setStatus(OrderStatus.IS_FINISH.getStatus());
            flag = true;
        }
        orderInfo.setTransactionId(orderPayBack.getTransactionId());
        DateTimeFormatter format = DateTimeFormat.forPattern("yyyyMMddHHmmss");
        if(orderPayBack.getTimeEnd() != null) {
            orderInfo.setPayTime(DateTime.parse(orderPayBack.getTimeEnd(), format).toDate());
        }
        if(orderPayBack.getTotalFee() != null) {
            MathContext mc = new MathContext(2, RoundingMode.HALF_DOWN);
            orderInfo.setPayMoney(new BigDecimal(orderPayBack.getTotalFee()).divide(new BigDecimal(100), mc));
        }
        orderInfo.setTradeState(orderPayBack.getTradeState());
        orderInfo.setTradeStateDesc(orderPayBack.getTradeStateDesc());

        orderInfoDao.updateById(orderInfo);

        if(flag){
            //????????????????????????
            iceChestPutRecord.setRecordStatus(RecordStatus.RECEIVE_FINISH.getStatus());
            iceChestPutRecordDao.updateById(iceChestPutRecord);
            //?????????????????????????????????
            iceChestInfo.setPutStatus(PutStatus.IS_PUT.getStatus());
            iceChestInfo.setClientId(iceChestPutRecord.getReceiveClientId());
            iceChestInfo.setLastPutId(iceChestPutRecord.getId());
            iceChestInfo.setLastPutTime(iceChestPutRecord.getCreateTime());
            iceChestInfoDao.updateById(iceChestInfo);
            //??????????????????, ????????????id???????????????
            PactRecord pactRecord = pactRecordDao.selectOne(Wrappers.<PactRecord>lambdaQuery()
                    .eq(PactRecord::getClientId, iceChestPutRecord.getReceiveClientId())
                    .eq(PactRecord::getChestId, iceChestInfo.getId()));
            if(pactRecord == null){
                throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
            }
            pactRecord.setPutId(iceChestPutRecord.getId());
            pactRecord.setPutTime(iceChestPutRecord.getCreateTime());
            DateTime startTime = new DateTime(pactRecord.getPutTime());
            DateTime endTime = startTime.plusYears(1);
            pactRecord.setPutExpireTime(endTime.toDate());
            pactRecordDao.updateById(pactRecord);
        }

        return flag;
    }

    @Transactional(value = "transactionManager")
    @Override
    public void notifyOrderInfo(OrderPayBack orderPayBack) throws ImproperOptionException {
        //???????????????????????????
        OrderInfo orderInfo = orderInfoDao.selectOne(Wrappers.<OrderInfo>lambdaQuery().eq(OrderInfo::getOrderNum, orderPayBack.getOutTradeNo()));
        if(orderInfo == null){
            log.info("??????:??????????????????,?????????????????? -> {}", JSON.toJSONString(orderPayBack));
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }
        //????????????????????????, ?????????????????????
        if(orderInfo.getStatus().equals(OrderStatus.IS_FINISH.getStatus())){
            return;
        }

        //????????????????????????
        IceChestInfo iceChestInfo = iceChestInfoDao.selectById(orderInfo.getChestId());
        if(iceChestInfo == null){
            log.info("??????:??????????????????,??????????????????-> {}", JSON.toJSONString(orderInfo));
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }
        //????????????????????????????????????
        IceChestPutRecord iceChestPutRecord = iceChestPutRecordDao.selectById(orderInfo.getChestPutRecordId());
        if(iceChestPutRecord == null){
            log.info("??????:??????????????????,??????????????????????????????-> {}", JSON.toJSONString(orderInfo));
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }

        //????????????
        orderInfo.setStatus(OrderStatus.IS_FINISH.getStatus());
        orderInfo.setTransactionId(orderPayBack.getTransactionId());
        DateTimeFormatter format = DateTimeFormat.forPattern("yyyyMMddHHmmss");
        orderInfo.setPayTime(DateTime.parse(orderPayBack.getTimeEnd(), format).toDate());
        MathContext mc = new MathContext(2, RoundingMode.HALF_DOWN);
        orderInfo.setPayMoney(new BigDecimal(orderPayBack.getTotalFee()).divide(new BigDecimal(100), mc));
        orderInfoDao.updateById(orderInfo);

        //??????????????????
        iceChestPutRecord.setRecordStatus(RecordStatus.RECEIVE_FINISH.getStatus());
        iceChestPutRecordDao.updateById(iceChestPutRecord);

        //????????????????????????
        iceChestInfo.setPutStatus(PutStatus.IS_PUT.getStatus());
        iceChestInfo.setClientId(iceChestPutRecord.getReceiveClientId());
        iceChestInfo.setLastPutId(iceChestPutRecord.getId());
        iceChestInfo.setLastPutTime(iceChestPutRecord.getCreateTime());
        iceChestInfoDao.updateById(iceChestInfo);

        //??????????????????, ????????????id???????????????
        PactRecord pactRecord = pactRecordDao.selectOne(Wrappers.<PactRecord>lambdaQuery()
                .eq(PactRecord::getClientId, iceChestPutRecord.getReceiveClientId())
                .eq(PactRecord::getChestId, iceChestInfo.getId()));
        if(pactRecord == null){
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }
        pactRecord.setPutId(iceChestPutRecord.getId());
        pactRecord.setPutTime(iceChestPutRecord.getCreateTime());
        DateTime startTime = new DateTime(pactRecord.getPutTime());
        DateTime endTime = startTime.plusYears(1);
        pactRecord.setPutExpireTime(endTime.toDate());
        pactRecordDao.updateById(pactRecord);
    }
}
