package com.szeastroc.icebox.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.exception.NormalOptionException;
import com.szeastroc.icebox.dao.IceChestInfoDao;
import com.szeastroc.icebox.dao.IceChestPutRecordDao;
import com.szeastroc.icebox.dao.OrderInfoDao;
import com.szeastroc.icebox.dao.PactRecordDao;
import com.szeastroc.icebox.entity.IceChestInfo;
import com.szeastroc.icebox.entity.IceChestPutRecord;
import com.szeastroc.icebox.entity.OrderInfo;
import com.szeastroc.icebox.entity.PactRecord;
import com.szeastroc.icebox.enums.OrderStatus;
import com.szeastroc.icebox.enums.PutStatus;
import com.szeastroc.icebox.enums.RecordStatus;
import com.szeastroc.icebox.enums.ResultEnum;
import com.szeastroc.icebox.service.OrderInfoService;
import com.szeastroc.icebox.util.CommonUtil;
import com.szeastroc.icebox.util.wechatpay.WXPayUtil;
import com.szeastroc.icebox.util.wechatpay.WeiXinConfig;
import com.szeastroc.icebox.util.wechatpay.WeiXinService;
import com.szeastroc.icebox.vo.OrderPayBack;
import com.szeastroc.icebox.vo.OrderPayResponse;
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

    @Transactional(value = "assetsTransactionManager")
    @Override
    public OrderPayResponse createPayInfo(String ip, String openid, Integer iceChestId, Integer chestPutRecordId) throws Exception {

        IceChestInfo iceChestInfo = iceChestInfoDao.selectById(iceChestId);
        if(iceChestInfo == null){
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }

        String orderNum = CommonUtil.generateOrderNumber();
        //调用统一下单接口
        String prepayId = weiXinService.createWeiXinPay(ip, iceChestInfo.getDepositMoney(), orderNum, openid);
        //创建订单
        OrderInfo orderInfo = new OrderInfo(iceChestInfo.getId(), chestPutRecordId, orderNum, openid, iceChestInfo.getDepositMoney(), prepayId);
        orderInfoDao.insert(orderInfo);

        Map<String, String> datas = new HashMap<>();
        datas.put("appId", weiXinConfig.getAppId());
        datas.put("timeStamp", String.valueOf(System.currentTimeMillis()));
        datas.put("nonceStr", WXPayUtil.generateNonceStr());
        datas.put("package", "prepay_id=" + orderInfo.getPrayId());
        datas.put("signType", "MD5");
        String sign = WXPayUtil.generateSignature(datas, weiXinConfig.getSecret());

        OrderPayResponse orderPayResponse = new OrderPayResponse(datas.get("appId"),
                datas.get("timeStamp"), datas.get("nonceStr"), datas.get("package"), datas.get("signType"), sign, orderNum);
        return orderPayResponse;
    }

    @Transactional(value = "assetsTransactionManager")
    @Override
    public boolean getPayStatus(String orderNum) throws Exception {

        boolean flag = false;

        //查询数据库中对应订单状态
        OrderInfo orderInfo = orderInfoDao.selectOne(Wrappers.<OrderInfo>lambdaQuery().eq(OrderInfo::getOrderNum, orderNum));
        if(orderInfo == null){
            log.error("异常:主动查询订单状态,丢失订单数据 -> {}", JSON.toJSONString(orderNum));
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }

        //查询对应冰柜信息
        IceChestInfo iceChestInfo = iceChestInfoDao.selectById(orderInfo.getChestId());
        if(iceChestInfo == null){
            log.error("异常:主动查询订单状态,丢失冰柜信息-> {}", JSON.toJSONString(orderInfo));
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }
        //查询对应冰柜投放记录信息
        IceChestPutRecord iceChestPutRecord = iceChestPutRecordDao.selectById(orderInfo.getChestPutRecordId());
        if(iceChestPutRecord == null){
            log.error("异常:订单成功回调,丢失冰柜投放记录信息-> {}", JSON.toJSONString(orderInfo));
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }

        //如果订单已完成, 则直接返回完成
        if(orderInfo.getStatus().equals(OrderStatus.IS_FINISH.getStatus())){
            return true;
        }

        //如果订单已取消, 抛出异常
        if(orderInfo.getStatus().equals(OrderStatus.IS_CANCEL.getStatus())){
            throw new NormalOptionException(ResultEnum.ORDER_IS_CANCEL_AND_RETRY_NEW_ORDER.getCode(), ResultEnum.ORDER_IS_CANCEL_AND_RETRY_NEW_ORDER.getMessage());
        }

        //如果显示未支付, 调用接口查询订单状态
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

        log.info("回调数据 -> {}", JSON.toJSONString(orderPayBack));

        //修改订单
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
            //修改投放信息状态
            iceChestPutRecord.setRecordStatus(RecordStatus.RECEIVE_FINISH.getStatus());
            iceChestPutRecordDao.updateById(iceChestPutRecord);
            //修改冰柜信息的投放状态
            iceChestInfo.setPutStatus(PutStatus.IS_PUT.getStatus());
            iceChestInfo.setClientId(iceChestPutRecord.getReceiveClientId());
            iceChestInfo.setLastPutId(iceChestPutRecord.getId());
            iceChestInfo.setLastPutTime(iceChestPutRecord.getCreateTime());
            iceChestInfoDao.updateById(iceChestInfo);
            //修改电子协议, 关联投放id及投放时间
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

    @Transactional(value = "assetsTransactionManager")
    @Override
    public void notifyOrderInfo(OrderPayBack orderPayBack) throws ImproperOptionException {
        //根据订单号查询订单
        OrderInfo orderInfo = orderInfoDao.selectOne(Wrappers.<OrderInfo>lambdaQuery().eq(OrderInfo::getOrderNum, orderPayBack.getOutTradeNo()));
        if(orderInfo == null){
            log.error("异常:订单成功回调,丢失订单数据 -> {}", JSON.toJSONString(orderPayBack));
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }
        //判断是否订单完成, 完成则无需修改
        if(orderInfo.getStatus().equals(OrderStatus.IS_FINISH.getStatus())){
            return;
        }

        //查询对应冰柜信息
        IceChestInfo iceChestInfo = iceChestInfoDao.selectById(orderInfo.getChestId());
        if(iceChestInfo == null){
            log.error("异常:订单成功回调,丢失冰柜信息-> {}", JSON.toJSONString(orderInfo));
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }
        //查询对应冰柜投放记录信息
        IceChestPutRecord iceChestPutRecord = iceChestPutRecordDao.selectById(orderInfo.getChestPutRecordId());
        if(iceChestPutRecord == null){
            log.error("异常:订单成功回调,丢失冰柜投放记录信息-> {}", JSON.toJSONString(orderInfo));
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }

        //修改订单
        orderInfo.setStatus(OrderStatus.IS_FINISH.getStatus());
        orderInfo.setTransactionId(orderPayBack.getTransactionId());
        DateTimeFormatter format = DateTimeFormat.forPattern("yyyyMMddHHmmss");
        orderInfo.setPayTime(DateTime.parse(orderPayBack.getTimeEnd(), format).toDate());
        MathContext mc = new MathContext(2, RoundingMode.HALF_DOWN);
        orderInfo.setPayMoney(new BigDecimal(orderPayBack.getTotalFee()).divide(new BigDecimal(100), mc));
        orderInfoDao.updateById(orderInfo);

        //修改投放记录
        iceChestPutRecord.setRecordStatus(RecordStatus.RECEIVE_FINISH.getStatus());
        iceChestPutRecordDao.updateById(iceChestPutRecord);

        //修改冰柜投放信息
        iceChestInfo.setPutStatus(PutStatus.IS_PUT.getStatus());
        iceChestInfo.setClientId(iceChestPutRecord.getReceiveClientId());
        iceChestInfo.setLastPutId(iceChestPutRecord.getId());
        iceChestInfo.setLastPutTime(iceChestPutRecord.getCreateTime());
        iceChestInfoDao.updateById(iceChestInfo);

        //修改电子协议, 关联投放id及投放时间
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
