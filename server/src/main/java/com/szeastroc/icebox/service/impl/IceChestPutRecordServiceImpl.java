package com.szeastroc.icebox.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Maps;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.enums.CommonStatus;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.exception.NormalOptionException;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.dao.*;
import com.szeastroc.icebox.entity.*;
import com.szeastroc.icebox.enums.*;
import com.szeastroc.icebox.service.IceChestPutRecordService;
import com.szeastroc.icebox.util.CommonUtil;
import com.szeastroc.icebox.util.wechatpay.WXPayUtil;
import com.szeastroc.icebox.util.wechatpay.WeiXinConfig;
import com.szeastroc.icebox.util.wechatpay.WeiXinService;
import com.szeastroc.icebox.vo.ClientInfoRequest;
import com.szeastroc.icebox.vo.IceDepositResponse;
import com.szeastroc.icebox.vo.OrderPayResponse;
import com.szeastroc.icebox.vo.query.IceDepositPage;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by Tulane
 * 2019/5/21
 */
@Slf4j
@Service
public class IceChestPutRecordServiceImpl extends ServiceImpl<IceChestPutRecordDao, IceChestPutRecord> implements IceChestPutRecordService {

    @Autowired
    private ClientInfoDao clientInfoDao;
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
    @Autowired
    private MarketAreaDao marketAreaDao;

    @Transactional(value = "transactionManager")
    @Override
    public CommonResponse<OrderPayResponse> applyPayIceChest(ClientInfoRequest clientInfoRequest) throws Exception {
        //查询冰柜信息
        IceChestInfo iceChestInfo = iceChestInfoDao.selectById(clientInfoRequest.getIceChestId());
        CommonUtil.assertNullObj(iceChestInfo);
        /**
         * 查询对应冰柜是否可投放
         */
        if (iceChestInfo.getPutStatus().equals(PutStatus.IS_PUT.getStatus())) {
            throw new NormalOptionException(ResultEnum.ICE_CHEST_IS_NOT_UN_PUT.getCode(), ResultEnum.ICE_CHEST_IS_NOT_UN_PUT.getMessage());
        }

        /**
         * 未投放, 查询是否有发出中的投放的信息
         */
        List<IceChestPutRecord> iceChestPutRecords = iceChestPutRecordDao.selectList(Wrappers.<IceChestPutRecord>lambdaQuery()
                .eq(IceChestPutRecord::getStatus, CommonStatus.VALID.getStatus())
                .eq(IceChestPutRecord::getChestId, iceChestInfo.getId())
                .eq(IceChestPutRecord::getServiceType, ServiceType.IS_PUT.getType())
                .eq(IceChestPutRecord::getRecordStatus, RecordStatus.SEND_ING.getStatus()));
        if (CollectionUtils.isNotEmpty(iceChestPutRecords) && iceChestPutRecords.size() > 1) {
            //数据错误: 不存在对应单个冰柜
            log.error("数据错误:冰柜投放发出记录存在多条 -> {}", JSON.toJSONString(iceChestPutRecords));
            throw new ImproperOptionException(Constants.ErrorMsg.RECORD_DATA_ERROR);
        }

        /**
         * 如果不存在正在投放的记录, 则进入创建投放记录及订单流程
         */
        //查询对应客户的鹏讯通id是否存在
        ClientInfo clientInfo = clientInfoDao.selectOne(Wrappers.<ClientInfo>lambdaQuery().eq(ClientInfo::getClientNumber, clientInfoRequest.getClientNumber()));
        if (clientInfo == null) {
            //创建新的
            clientInfo = new ClientInfo(clientInfoRequest.getClientName(), ClientType.IS_STORE.getType(), clientInfoRequest.getClientNumber(), clientInfoRequest.getClientPlace(),
                    clientInfoRequest.getClientLevel(), CommonStatus.VALID.getStatus(), clientInfoRequest.getContactName(), clientInfoRequest.getContactMobile(), Integer.valueOf(clientInfoRequest.getMarketAreaId()));
            clientInfoDao.insert(clientInfo);
        } else {
            /**
             * 如果客户存在其他拥有的冰柜, 无法继续绑定
             */
            List<IceChestInfo> oldIceChestInfos = iceChestInfoDao.selectList(Wrappers.<IceChestInfo>lambdaQuery().eq(IceChestInfo::getClientId, clientInfo.getId()));
            if (CollectionUtils.isNotEmpty(oldIceChestInfos)) {
                throw new NormalOptionException(ResultEnum.CLIENT_HAVE_ICECHEST_NOW.getCode(), ResultEnum.CLIENT_HAVE_ICECHEST_NOW.getMessage());
            }
        }

        if (CollectionUtils.isEmpty(iceChestPutRecords)) {
            OrderPayResponse orderPayResponse = createPutIceChestAndOrderInfo(clientInfoRequest, iceChestInfo, clientInfo);
            return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, orderPayResponse);
        }


        /**
         * 存在, 则查询投放信息对应订单号是否完成
         */
        IceChestPutRecord iceChestPutRecord = iceChestPutRecords.get(0);
        List<OrderInfo> orderInfos = orderInfoDao.selectList(Wrappers.<OrderInfo>lambdaQuery()
                .eq(OrderInfo::getChestPutRecordId, iceChestPutRecord.getId()));
        if (CollectionUtils.isNotEmpty(orderInfos)) {
            orderInfos = orderInfos.stream().filter(x -> !x.getStatus().equals(OrderStatus.IS_CANCEL.getStatus())).collect(Collectors.toList());
        }
        if (CollectionUtils.isEmpty(orderInfos) || orderInfos.size() > 1) {
            //数据错误: 不存在对应投放的单个订单
            log.error("数据错误:投放对应订单记录不存在或存在多条 -> 订单: {} | 投放: {}", JSON.toJSONString(orderInfos), JSON.toJSON(iceChestPutRecord));
            throw new ImproperOptionException(Constants.ErrorMsg.RECORD_DATA_ERROR);
        }
        OrderInfo orderInfo = orderInfos.get(0);

        if (orderInfo.getStatus().equals(OrderStatus.IS_FINISH.getStatus())) {

            //已完成, 则修改投放信息为已接收, 修改冰柜信息为已投放
            iceChestPutRecord.setRecordStatus(RecordStatus.RECEIVE_FINISH.getStatus());
            iceChestPutRecordDao.updateById(iceChestPutRecord);
            iceChestInfo.setPutStatus(PutStatus.IS_PUT.getStatus());
            iceChestInfoDao.updateById(iceChestInfo);

            //修改电子协议, 关联投放id及投放时间
            PactRecord pactRecord = pactRecordDao.selectOne(Wrappers.<PactRecord>lambdaQuery()
                    .eq(PactRecord::getClientId, clientInfo.getId())
                    .eq(PactRecord::getChestId, iceChestInfo.getId()));
            pactRecord.setPutId(iceChestPutRecord.getId());
            pactRecord.setPutTime(iceChestPutRecord.getCreateTime());
            pactRecordDao.updateById(pactRecord);
            return new CommonResponse<>(ResultEnum.ICE_CHEST_IS_NOT_UN_PUT.getCode(), ResultEnum.ICE_CHEST_IS_NOT_UN_PUT.getMessage());
        }

        /**
         * 订单未完成, 查询订单是否已超时
         */
        if (assertOrderInfoTimeOut(orderInfo.getCreateTime())) {
            //订单超时 调用订单超时流程
            return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, closeWechatWithTimeout(clientInfoRequest, orderInfo, iceChestInfo, clientInfo, iceChestPutRecord));
        }

        /**
         * 订单未超时, 判断订单所属的投放记录的客户人是否是自己, 不是则拒绝投放, 是则返回订单信息
         */
        if (!iceChestPutRecord.getReceiveClientId().equals(clientInfo.getId())) {
            return new CommonResponse<>(ResultEnum.ICE_CHEST_IS_HAVE_PUT_ING.getCode(), ResultEnum.ICE_CHEST_IS_HAVE_PUT_ING.getMessage());
        }
        //属于自己, 返回订单信息, 重新调起旧订单
        Map<String, String> datas = new HashMap<>();
        datas.put("appId", weiXinConfig.getAppId());
        datas.put("timeStamp", String.valueOf(System.currentTimeMillis()));
        datas.put("nonceStr", WXPayUtil.generateNonceStr());
        datas.put("package", "prepay_id=" + orderInfo.getPrayId());
        datas.put("signType", "MD5");
        String sign = WXPayUtil.generateSignature(datas, weiXinConfig.getSecret());
        OrderPayResponse orderPayResponse = new OrderPayResponse(iceChestPutRecord.getFreePayType(), datas.get("appId"),
                datas.get("timeStamp"), datas.get("nonceStr"), datas.get("package"), datas.get("signType"), sign, orderInfo.getOrderNum());
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, orderPayResponse);
    }

    @Override
    public IPage<IceDepositResponse> queryIceDeposits(IceDepositPage iceDepositPage) {
        LambdaQueryWrapper<IceChestPutRecord> wrapper = Wrappers.<IceChestPutRecord>lambdaQuery();
        wrapper.eq(IceChestPutRecord::getServiceType, ServiceType.IS_PUT.getType());

        IPage<IceChestPutRecord> iceChestPutRecordIPage = iceChestPutRecordDao.customSelectPage(iceDepositPage, wrapper, iceDepositPage);

        final Map<Integer, Integer> clientForeignKeyMap = Maps.newHashMap();
        final Map<Integer, Integer> chestForeignKeyMap = Maps.newHashMap();
        final Map<Integer, Integer> orderForeignKeyMap = Maps.newHashMap();
        for (IceChestPutRecord record : iceChestPutRecordIPage.getRecords()) {
            // 投放客户信息
            clientForeignKeyMap.put(record.getId(), record.getReceiveClientId());
            // 冰柜信息
            chestForeignKeyMap.put(record.getId(), record.getChestId());
            // 支付信息
            orderForeignKeyMap.put(record.getId(), record.getId());
        }

        // 批量查询数据库
        if(CollectionUtils.isNotEmpty(iceChestPutRecordIPage.getRecords())) {
            return getIceDepositResponseIPage(iceChestPutRecordIPage, clientForeignKeyMap, chestForeignKeyMap, orderForeignKeyMap);
        }
        return new Page<>(iceDepositPage.getCurrent(), iceDepositPage.getSize(), iceDepositPage.getTotal());
    }

    private IPage<IceDepositResponse> getIceDepositResponseIPage(IPage<IceChestPutRecord> iceChestPutRecordIPage, Map<Integer, Integer> clientForeignKeyMap, Map<Integer, Integer> chestForeignKeyMap, Map<Integer, Integer> orderForeignKeyMap) {
        List<ClientInfo> clientInfos = clientInfoDao.selectList(Wrappers.<ClientInfo>lambdaQuery().in(ClientInfo::getId, clientForeignKeyMap.entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toList())));
        List<IceChestInfo> iceChestInfos = iceChestInfoDao.selectList(Wrappers.<IceChestInfo>lambdaQuery().in(IceChestInfo::getId, chestForeignKeyMap.entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toList())));
        List<OrderInfo> orderInfos = orderInfoDao.selectList(Wrappers.<OrderInfo>lambdaQuery().in(OrderInfo::getChestPutRecordId, orderForeignKeyMap.entrySet().stream().map(Map.Entry::getValue).collect(Collectors.toList())));

        // 查询服务处
        List<MarketArea> marketAreas = marketAreaDao.selectList(Wrappers.<MarketArea>lambdaQuery().in(MarketArea::getId, iceChestInfos.stream().map(IceChestInfo::getMarketAreaId).collect(Collectors.toList())));

        return iceChestPutRecordIPage.convert(iceChestPutRecord -> {
            // 投放客户信息
            ClientInfo clientInfo = clientInfos.stream().filter(x -> x.getId().equals(clientForeignKeyMap.get(iceChestPutRecord.getId()))).findFirst().get();
            // 冰柜信息
            IceChestInfo iceChestInfo = iceChestInfos.stream().filter(x -> x.getId().equals(chestForeignKeyMap.get(iceChestPutRecord.getId()))).findFirst().get();
            // 支付信息
            OrderInfo orderInfo = orderInfos.stream().filter(x -> x.getChestPutRecordId().equals(orderForeignKeyMap.get(iceChestPutRecord.getId()))).findFirst().get();
            // 服务处信息
            MarketArea marketArea = marketAreas.stream().filter(x -> x.getId().equals(iceChestInfo.getMarketAreaId())).findFirst().get();
            return buildIceDepositResponse(clientInfo, iceChestInfo, orderInfo, marketArea);
        });
    }

    private IceDepositResponse buildIceDepositResponse(ClientInfo clientInfo, IceChestInfo iceChestInfo, OrderInfo orderInfo, MarketArea marketArea) {
        IceDepositResponse iceDepositResponse = new IceDepositResponse();
        iceDepositResponse.setClientNumber(clientInfo.getClientNumber());
        iceDepositResponse.setClientName(clientInfo.getClientName());
        iceDepositResponse.setContactName(clientInfo.getContactName());
        iceDepositResponse.setContactMobile(clientInfo.getContactMobile());
        iceDepositResponse.setClientPlace(clientInfo.getClientPlace());
        iceDepositResponse.setMarketAreaName(marketArea.getName());
        iceDepositResponse.setChestModel(iceChestInfo.getChestModel());
        iceDepositResponse.setChestName(iceChestInfo.getChestName());
        iceDepositResponse.setAssetId(iceChestInfo.getAssetId());
        // TODO BigDecimal转换
        iceDepositResponse.setPayMoney(orderInfo.getPayMoney().toPlainString());
        iceDepositResponse.setPayTime(orderInfo.getPayTime().getTime());
        iceDepositResponse.setOrderNum(orderInfo.getOrderNum());
        iceDepositResponse.setChestMoney(iceChestInfo.getChestMoney().toPlainString());
        return iceDepositResponse;
    }

    /**
     * 创建投放记录及订单流程
     *
     * @param clientInfoRequest
     * @param iceChestInfo
     * @return
     * @throws Exception
     */
    private OrderPayResponse createPutIceChestAndOrderInfo(ClientInfoRequest clientInfoRequest, IceChestInfo iceChestInfo, ClientInfo clientInfo) throws Exception {
        int freePayType = iceChestInfo.getFreePayType();
        if(freePayType == FreePayTypeEnum.UN_FREE.getType()){
            return createByUnFree(clientInfoRequest, iceChestInfo, clientInfo);
        }else{
            return createByFree(clientInfoRequest, iceChestInfo, clientInfo);
        }
    }

    private OrderPayResponse createByUnFree(ClientInfoRequest clientInfoRequest, IceChestInfo iceChestInfo, ClientInfo clientInfo) throws Exception {
        /**
         * 创建冰柜投放记录
         */
        IceChestPutRecord iceChestPutRecord = new IceChestPutRecord(Integer.parseInt(clientInfoRequest.getIceChestId()), null, null, iceChestInfo.getClientId(), clientInfo.getId(), iceChestInfo.getDepositMoney(), RecordStatus.SEND_ING.getStatus());
        iceChestPutRecordDao.insert(iceChestPutRecord);

        /**
         * 创建订单信息
         */
        String orderNum = CommonUtil.generateOrderNumber();
        //调用统一下单接口
        String prepayId = weiXinService.createWeiXinPay(clientInfoRequest.getIp(), iceChestInfo.getDepositMoney(), orderNum, clientInfoRequest.getOpenid());
        //创建订单
        OrderInfo orderInfo = new OrderInfo(iceChestInfo.getId(), iceChestPutRecord.getId(), orderNum, clientInfoRequest.getOpenid(), iceChestInfo.getDepositMoney(), prepayId);
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

    private OrderPayResponse createByFree(ClientInfoRequest clientInfoRequest, IceChestInfo iceChestInfo, ClientInfo clientInfo) throws ImproperOptionException {
        IceChestPutRecord iceChestPutRecord = new IceChestPutRecord(Integer.parseInt(clientInfoRequest.getIceChestId()), null, null, iceChestInfo.getClientId(), clientInfo.getId(), iceChestInfo.getDepositMoney(), RecordStatus.RECEIVE_FINISH.getStatus());
        iceChestPutRecord.setFreePayType(iceChestInfo.getFreePayType());
        iceChestPutRecordDao.insert(iceChestPutRecord);
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
        OrderPayResponse orderPayResponse = new OrderPayResponse(iceChestInfo.getFreePayType());
        return orderPayResponse;
    }

    private OrderPayResponse closeWechatWithTimeout(ClientInfoRequest clientInfoRequest, OrderInfo orderInfo, IceChestInfo iceChestInfo, ClientInfo clientInfo, IceChestPutRecord iceChestPutRecord) throws Exception {
        /**
         * 超时关闭订单
         */
        weiXinService.closeWeiXinPay(orderInfo.getOrderNum());
        orderInfo.setStatus(OrderStatus.IS_CANCEL.getStatus());
        orderInfoDao.updateById(orderInfo);

        /**
         * 查询投放者是否是自身 是则创建新的订单 否则关闭旧投放记录创建新的订单
         */
        String orderNum = CommonUtil.generateOrderNumber();
        //调用统一下单接口
        String prepayId = weiXinService.createWeiXinPay(clientInfoRequest.getIp(), iceChestInfo.getDepositMoney(), orderNum, clientInfoRequest.getOpenid());
        //创建订单
        OrderInfo newOrderInfo = new OrderInfo(iceChestInfo.getId(), orderNum, clientInfoRequest.getOpenid(), iceChestInfo.getDepositMoney(), prepayId);
        if (iceChestPutRecord.getReceiveClientId().equals(clientInfo.getId())) {
            /**
             * 属于自身, 订单直接关联旧投放记录
             */
            newOrderInfo.setChestPutRecordId(iceChestPutRecord.getId());
        } else {
            /**
             * 属于他人, 则关闭旧投放记录, 创建新投放, 订单关联新投放
             */
            //关闭旧投放记录
            iceChestPutRecord.setStatus(CommonStatus.INVALID.getStatus());
            iceChestPutRecordDao.updateById(iceChestPutRecord);
            //创建新投放
            IceChestPutRecord newIceChestPutRecord = new IceChestPutRecord(iceChestInfo.getId(), null, null, iceChestInfo.getClientId(), clientInfo.getId(), iceChestInfo.getDepositMoney(), RecordStatus.SEND_ING.getStatus());
            iceChestPutRecordDao.insert(newIceChestPutRecord);
            //订单关联新投放
            newOrderInfo.setChestPutRecordId(newIceChestPutRecord.getId());
        }
        orderInfoDao.insert(newOrderInfo);

        Map<String, String> datas = new HashMap<>();
        datas.put("appId", weiXinConfig.getAppId());
        datas.put("timeStamp", String.valueOf(System.currentTimeMillis()));
        datas.put("nonceStr", WXPayUtil.generateNonceStr());
        datas.put("package", "prepay_id=" + newOrderInfo.getPrayId());
        datas.put("signType", "MD5");
        String sign = WXPayUtil.generateSignature(datas, weiXinConfig.getSecret());

        OrderPayResponse orderPayResponse = new OrderPayResponse(iceChestPutRecord.getFreePayType(), datas.get("appId"),
                datas.get("timeStamp"), datas.get("nonceStr"), datas.get("package"), datas.get("signType"), sign, orderNum);
        return orderPayResponse;
    }

    /**
     * 判断订单下单时间是否超过自定义限制时间( 10分钟 )
     *
     * @param orderTime
     * @return true 超时 false 未超时
     */
    private boolean assertOrderInfoTimeOut(Date orderTime) {
        long time = new DateTime().toDate().getTime() - orderTime.getTime();
        if (time >= weiXinConfig.getOrder().getTimeout()) {
            return true;
        }
        return false;
    }
}
