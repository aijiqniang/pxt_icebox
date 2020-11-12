package com.szeastroc.icebox.oldprocess.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Maps;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.entity.customer.vo.MemberInfoVo;
import com.szeastroc.common.entity.customer.vo.SimpleStoreVo;
import com.szeastroc.common.entity.customer.vo.StoreInfoDtoVo;
import com.szeastroc.common.entity.customer.vo.SubordinateInfoVo;
import com.szeastroc.common.entity.user.vo.DeptNameRequest;
import com.szeastroc.common.entity.user.vo.SessionDeptInfoVo;
import com.szeastroc.common.enums.CommonStatus;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.exception.NormalOptionException;
import com.szeastroc.common.feign.customer.FeignStoreClient;
import com.szeastroc.common.feign.customer.FeignStoreRelateMemberClient;
import com.szeastroc.common.feign.customer.FeignSupplierClient;
import com.szeastroc.common.feign.user.FeignCacheClient;
import com.szeastroc.common.feign.user.FeignDeptClient;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.common.utils.Streams;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.enums.*;
import com.szeastroc.icebox.newprocess.dao.IceBoxDao;
import com.szeastroc.icebox.newprocess.dao.IcePutApplyDao;
import com.szeastroc.icebox.newprocess.dao.IcePutApplyRelateBoxDao;
import com.szeastroc.icebox.newprocess.dao.IcePutOrderDao;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.entity.IcePutApply;
import com.szeastroc.icebox.newprocess.entity.IcePutApplyRelateBox;
import com.szeastroc.icebox.newprocess.entity.IcePutOrder;
import com.szeastroc.icebox.newprocess.enums.StoreSignStatus;
import com.szeastroc.icebox.oldprocess.dao.*;
import com.szeastroc.icebox.oldprocess.entity.*;
import com.szeastroc.icebox.oldprocess.service.IceChestPutRecordService;
import com.szeastroc.icebox.oldprocess.vo.ClientInfoRequest;
import com.szeastroc.icebox.oldprocess.vo.IceDepositResponse;
import com.szeastroc.icebox.oldprocess.vo.OrderPayResponse;
import com.szeastroc.icebox.oldprocess.vo.query.IceDepositPage;
import com.szeastroc.icebox.oldprocess.vo.report.IceDepositReport;
import com.szeastroc.icebox.util.CommonUtil;
import com.szeastroc.icebox.util.wechatpay.WXPayUtil;
import com.szeastroc.icebox.util.wechatpay.WeiXinConfig;
import com.szeastroc.icebox.util.wechatpay.WeiXinService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
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


    @Autowired
    private IceBoxDao iceBoxDao;
    @Autowired
    private IcePutApplyDao icePutApplyDao;
    @Autowired
    private IcePutApplyRelateBoxDao icePutApplyRelateBoxDao;
    @Autowired
    private IcePutOrderDao icePutOrderDao;
    @Autowired
    private FeignStoreClient feignStoreClient;
    @Autowired
    private FeignSupplierClient feignSupplierClient;
    @Autowired
    private FeignCacheClient feignCacheClient;
    @Autowired
    private FeignStoreRelateMemberClient feignStoreRelateMemberClient;
    @Autowired
    private FeignDeptClient feignDeptClient;

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
        wrapper.eq(IceChestPutRecord::getRecordStatus, RecordStatus.RECEIVE_FINISH.getStatus());

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

    @Override
    public IPage<IceDepositResponse> queryIceDepositsForPut(IceDepositPage iceDepositPage) {
        LambdaQueryWrapper<IcePutApply> wrapper = getIcePutApplyWrapper(iceDepositPage);

        List<IcePutApply> icePutApplys = icePutApplyDao.selectList(wrapper);

        if(CollectionUtil.isEmpty(icePutApplys)){
            return new Page<>(iceDepositPage.getCurrent(), iceDepositPage.getSize(), iceDepositPage.getTotal());
        }
        Set<String> applyNumbers = icePutApplys.stream().map(x -> x.getApplyNumber()).collect(Collectors.toSet());
        IPage<IcePutApplyRelateBox> relateBoxIPage = icePutApplyRelateBoxDao.selectPage(iceDepositPage, Wrappers.<IcePutApplyRelateBox>lambdaQuery().in(IcePutApplyRelateBox::getApplyNumber, applyNumbers).eq(IcePutApplyRelateBox::getFreeType,FreePayTypeEnum.UN_FREE.getType()));

        Map<String, IcePutApply> putApplyMap = icePutApplys.stream().collect(Collectors.toMap(IcePutApply::getApplyNumber, x -> x));
        Set<String> storeNumbers = new HashSet<>();
        Set<String> applyNumberSet = new HashSet<>();
        for (IcePutApplyRelateBox relateBox : relateBoxIPage.getRecords()) {
            // 投放客户信息
            IcePutApply icePutApply = putApplyMap.get(relateBox.getApplyNumber());
            if(icePutApply != null){
                storeNumbers.add(icePutApply.getPutStoreNumber());
            }
            // 冰柜信息
            applyNumberSet.add(relateBox.getApplyNumber());

        }
        if(CollectionUtils.isNotEmpty(relateBoxIPage.getRecords())){
            //投放的门店信息
            List<SimpleStoreVo> storeInfoDtoVos = FeignResponseUtil.getFeignData(feignStoreClient.getSimpleStoreByNumbers(new ArrayList<>(storeNumbers)));
            if(CollectionUtil.isEmpty(storeInfoDtoVos)){
                storeInfoDtoVos = new ArrayList<>();
            }

            //投放的配送商信息
            List<SubordinateInfoVo> subordinateInfoVos = FeignResponseUtil.getFeignData(feignSupplierClient.readByNumbers(new ArrayList<>(storeNumbers)));
            if(CollectionUtil.isNotEmpty(subordinateInfoVos)){
                for(SubordinateInfoVo infoVo:subordinateInfoVos){
                    SimpleStoreVo simpleStoreVo = new SimpleStoreVo();
                    simpleStoreVo.setStoreNumber(infoVo.getNumber());
                    simpleStoreVo.setStoreName(infoVo.getName());
                    simpleStoreVo.setAddress(infoVo.getAddress());
                    simpleStoreVo.setLinkman(infoVo.getLinkman());
                    simpleStoreVo.setLinkmanMobile(infoVo.getLinkmanMobile());
                    storeInfoDtoVos.add(simpleStoreVo);
                }
            }
            //冰柜信息
            List<IceBox> iceBoxes = new ArrayList<>();
            List<IcePutApplyRelateBox> icePutApplyRelateBoxes = icePutApplyRelateBoxDao.selectList(Wrappers.<IcePutApplyRelateBox>lambdaQuery().in(IcePutApplyRelateBox::getApplyNumber, applyNumbers));
            if(CollectionUtil.isNotEmpty(icePutApplyRelateBoxes)){
                Set<Integer> iceBoxIds = icePutApplyRelateBoxes.stream().map(x -> x.getBoxId()).collect(Collectors.toSet());
                iceBoxes = iceBoxDao.selectBatchIds(iceBoxIds);
            }
            //支付信息
            List<IcePutOrder> icePutOrders = icePutOrderDao.selectList(Wrappers.<IcePutOrder>lambdaQuery().in(IcePutOrder::getApplyNumber, applyNumbers).eq(IcePutOrder::getStatus,OrderStatus.IS_FINISH.getStatus()));
            return getNewIceDepositResponseIPage(relateBoxIPage, storeInfoDtoVos, iceBoxes, icePutOrders,putApplyMap);

        }
        return new Page<>(iceDepositPage.getCurrent(), iceDepositPage.getSize(), iceDepositPage.getTotal());
    }

    private LambdaQueryWrapper<IcePutApply> getIcePutApplyWrapper(IceDepositPage iceDepositPage) {
        LambdaQueryWrapper<IcePutApply> wrapper = Wrappers.<IcePutApply>lambdaQuery();
        wrapper.eq(IcePutApply::getStoreSignStatus, StoreSignStatus.ALREADY_SIGN.getStatus());
        if(StringUtils.isNotEmpty(iceDepositPage.getClientNumber())){
            wrapper.like(IcePutApply::getPutStoreNumber, iceDepositPage.getClientNumber());
        }
        if(StringUtils.isNotEmpty(iceDepositPage.getClientName())){
            List<StoreInfoDtoVo> storeInfoDtoVos = FeignResponseUtil.getFeignData(feignStoreClient.getByName(iceDepositPage.getClientName()));
            if(CollectionUtil.isNotEmpty(storeInfoDtoVos)){
                List<String> storeNumbers = storeInfoDtoVos.stream().map(x -> x.getStoreNumber()).collect(Collectors.toList());
                wrapper.in(IcePutApply::getPutStoreNumber, storeNumbers);
            }else {
                wrapper.eq(IcePutApply::getPutStoreNumber, "");
            }
        }
        if(StringUtils.isNotEmpty(iceDepositPage.getContactMobile())){
            List<StoreInfoDtoVo> storeInfoDtoVos = FeignResponseUtil.getFeignData(feignStoreClient.getByMobile(iceDepositPage.getContactMobile()));
            if(CollectionUtil.isNotEmpty(storeInfoDtoVos)){
                List<String> storeNumbers = storeInfoDtoVos.stream().map(x -> x.getStoreNumber()).collect(Collectors.toList());
                wrapper.in(IcePutApply::getPutStoreNumber, storeNumbers);
            }else {
                wrapper.eq(IcePutApply::getPutStoreNumber, "");
            }
        }
        if(StringUtils.isNotEmpty(iceDepositPage.getChestModel())){

            List<IceBox> iceBoxes = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().like(IceBox::getModelName,iceDepositPage.getChestModel()));
            if(CollectionUtil.isNotEmpty(iceBoxes)){
                List<String> storeNumbers = iceBoxes.stream().map(x -> x.getPutStoreNumber()).collect(Collectors.toList());
                wrapper.in(IcePutApply::getPutStoreNumber, storeNumbers);
            }else {
                wrapper.eq(IcePutApply::getPutStoreNumber, "");
            }

        }
        if(StringUtils.isNotEmpty(iceDepositPage.getAssetId())){
            List<IceBox> iceBoxes = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().like(IceBox::getAssetId,iceDepositPage.getAssetId()));
            iceBoxes = Streams.toStream(iceBoxes).filter(x -> StringUtils.isNotEmpty(x.getPutStoreNumber())).collect(Collectors.toList());
            if(CollectionUtil.isNotEmpty(iceBoxes)){
                List<String> storeNumbers = iceBoxes.stream().map(x -> x.getPutStoreNumber()).collect(Collectors.toList());
                wrapper.in(IcePutApply::getPutStoreNumber, storeNumbers);
            }else {
                wrapper.eq(IcePutApply::getPutStoreNumber, "");
            }

        }

        if(StringUtils.isNotEmpty(iceDepositPage.getPayStartTime()) && StringUtils.isNotEmpty(iceDepositPage.getPayEndTime()) ){
            List<IcePutOrder> icePutOrders = icePutOrderDao.selectList(Wrappers.<IcePutOrder>lambdaQuery().ge(IcePutOrder::getPayTime, iceDepositPage.getPayStartTime()).le(IcePutOrder::getPayTime, iceDepositPage.getPayEndTime()));
            if(CollectionUtil.isNotEmpty(icePutOrders)){
                List<String> applyNumbers = icePutOrders.stream().map(x -> x.getApplyNumber()).collect(Collectors.toList());
                wrapper.in(IcePutApply::getApplyNumber, applyNumbers);
            }else {
                wrapper.eq(IcePutApply::getApplyNumber, "");
            }
        }
        if(iceDepositPage.getMarketAreaId() != null){
            DeptNameRequest deptNameRequest = new DeptNameRequest();
            deptNameRequest.setParentIds(iceDepositPage.getMarketAreaId()+"");
            List<SessionDeptInfoVo> deptInfoVos = FeignResponseUtil.getFeignData(feignDeptClient.findDeptInfoListByParentId(deptNameRequest));
            if(CollectionUtil.isEmpty(deptInfoVos)){
                wrapper.eq(IcePutApply::getApplyNumber, "");
            }else {
                List<Integer> marketAreaIds = deptInfoVos.stream().map(x -> x.getId()).collect(Collectors.toList());
                List<IceBox> iceBoxs = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().in(IceBox::getDeptId, marketAreaIds));
                if(CollectionUtil.isNotEmpty(iceBoxs)){
                    List<Integer> iceBoxIds = iceBoxs.stream().map(x -> x.getId()).collect(Collectors.toList());
                    List<IcePutOrder> icePutOrders = icePutOrderDao.selectList(Wrappers.<IcePutOrder>lambdaQuery().in(IcePutOrder::getChestId, iceBoxIds).eq(IcePutOrder::getStatus,OrderStatus.IS_FINISH.getStatus()));
                    if(CollectionUtil.isNotEmpty(icePutOrders)){
                        List<String> applyNumbers = icePutOrders.stream().map(x -> x.getApplyNumber()).collect(Collectors.toList());
                        wrapper.in(IcePutApply::getApplyNumber, applyNumbers);
                    }else {
                        wrapper.eq(IcePutApply::getApplyNumber, "");
                    }
                }else {
                    wrapper.eq(IcePutApply::getApplyNumber, "");
                }
            }
        }
        return wrapper;
    }

    @Override
    public List<IceDepositReport> exportDepositsForPut(IceDepositPage iceDepositPage) {
        List<IceDepositReport> iceDepositReports = new ArrayList<>();
        LambdaQueryWrapper<IcePutApply> wrapper = getIcePutApplyWrapper(iceDepositPage);

        List<IcePutApply> icePutApplys = icePutApplyDao.selectList(wrapper);

        if(CollectionUtil.isEmpty(icePutApplys)){
            return iceDepositReports;
        }
        Set<String> applyNumbers = icePutApplys.stream().map(x -> x.getApplyNumber()).collect(Collectors.toSet());
        List<IcePutApplyRelateBox> relateBoxs = icePutApplyRelateBoxDao.selectList(Wrappers.<IcePutApplyRelateBox>lambdaQuery().in(IcePutApplyRelateBox::getApplyNumber, applyNumbers));

        Map<String, IcePutApply> putApplyMap = icePutApplys.stream().collect(Collectors.toMap(IcePutApply::getApplyNumber, x -> x));
        Set<String> storeNumbers = new HashSet<>();
        Set<String> applyNumberSet = new HashSet<>();
        for (IcePutApplyRelateBox relateBox : relateBoxs) {
            // 投放客户信息
            IcePutApply icePutApply = putApplyMap.get(relateBox.getApplyNumber());
            if(icePutApply != null){
                storeNumbers.add(icePutApply.getPutStoreNumber());
            }
            // 冰柜信息
            applyNumberSet.add(relateBox.getApplyNumber());

        }
        if(CollectionUtils.isNotEmpty(relateBoxs)){
            //投放的门店信息
            List<SimpleStoreVo> storeInfoDtoVos = FeignResponseUtil.getFeignData(feignStoreClient.getSimpleStoreByNumbers(new ArrayList<>(storeNumbers)));
            //冰柜信息
            List<IceBox> iceBoxes = new ArrayList<>();
            List<IcePutApplyRelateBox> icePutApplyRelateBoxes = icePutApplyRelateBoxDao.selectList(Wrappers.<IcePutApplyRelateBox>lambdaQuery().in(IcePutApplyRelateBox::getApplyNumber, applyNumbers));
            if(CollectionUtil.isNotEmpty(icePutApplyRelateBoxes)){
                Set<Integer> iceBoxIds = icePutApplyRelateBoxes.stream().map(x -> x.getBoxId()).collect(Collectors.toSet());
                iceBoxes = iceBoxDao.selectBatchIds(iceBoxIds);
            }
            //支付信息
            List<IcePutOrder> icePutOrders = icePutOrderDao.selectList(Wrappers.<IcePutOrder>lambdaQuery().in(IcePutOrder::getApplyNumber, applyNumbers));
            return getIceDepositResponseList(relateBoxs, storeInfoDtoVos, iceBoxes, icePutOrders,putApplyMap);

        }
        return iceDepositReports;
    }

    private List<IceDepositReport> getIceDepositResponseList(List<IcePutApplyRelateBox> relateBoxs, List<SimpleStoreVo> storeInfoDtoVos, List<IceBox> iceBoxes, List<IcePutOrder> icePutOrders, Map<String, IcePutApply> putApplyMap) {
        Map<String, SimpleStoreVo> storeVoMap = Streams.toStream(storeInfoDtoVos).collect(Collectors.toMap(SimpleStoreVo::getStoreNumber, x -> x));
        Map<Integer, IceBox> iceBoxMap = Streams.toStream(iceBoxes).collect(Collectors.toMap(IceBox::getId,x->x));
        Map<String, IcePutOrder> putOrderMap = Streams.toStream(icePutOrders).collect(Collectors.toMap(IcePutOrder::getApplyNumber, x -> x));
        List<IceDepositResponse> iceDepositResponses = getIceDepositResponses(putApplyMap, storeVoMap, iceBoxMap, putOrderMap, relateBoxs);
        List<IceDepositReport> reportList = new ArrayList<>();
        if(CollectionUtil.isEmpty(iceDepositResponses)){
           return reportList;
        }
        for(IceDepositResponse response:iceDepositResponses){
            IceDepositReport iceDepositReport = new IceDepositReport();
            BeanUtils.copyProperties(response, iceDepositReport);
            iceDepositReport.setPayTimeStr(new DateTime(response.getPayTime()).toString("YYYY-MM-dd HH:mm"));
            reportList.add(iceDepositReport);
        }
        return reportList;
    }

    private IPage<IceDepositResponse> getNewIceDepositResponseIPage(IPage<IcePutApplyRelateBox> applyRelateBoxIPage, List<SimpleStoreVo> storeInfoDtoVos, List<IceBox> iceBoxes, List<IcePutOrder> icePutOrders, Map<String, IcePutApply> putApplyMap) {
        Map<String, SimpleStoreVo> storeVoMap = Streams.toStream(storeInfoDtoVos).collect(Collectors.toMap(SimpleStoreVo::getStoreNumber, x -> x));
        Map<Integer, IceBox> iceBoxMap = Streams.toStream(iceBoxes).collect(Collectors.toMap(IceBox::getId,x->x));
        Map<String, IcePutOrder> putOrderMap = Streams.toStream(icePutOrders).collect(Collectors.toMap(IcePutOrder::getApplyNumber, x -> x));
        List<IcePutApplyRelateBox> records = applyRelateBoxIPage.getRecords();
        List<IceDepositResponse> iceDepositResponses = getIceDepositResponses(putApplyMap, storeVoMap, iceBoxMap, putOrderMap, records);
        IPage<IceDepositResponse> page = new Page<>();
        page.setCurrent(applyRelateBoxIPage.getCurrent());
        page.setSize(applyRelateBoxIPage.getSize());
        page.setTotal(applyRelateBoxIPage.getTotal());
        page.setRecords(iceDepositResponses);
        return page;
    }

    private List<IceDepositResponse> getIceDepositResponses(Map<String, IcePutApply> putApplyMap, Map<String, SimpleStoreVo> storeVoMap, Map<Integer, IceBox> iceBoxMap, Map<String, IcePutOrder> putOrderMap, List<IcePutApplyRelateBox> records) {
        List<IceDepositResponse> iceDepositResponses = new ArrayList<>();
        for(IcePutApplyRelateBox relateBox:records){
            IceDepositResponse iceDepositResponse = new IceDepositResponse();
            // 投放客户信息
            IcePutApply icePutApply = putApplyMap.get(relateBox.getApplyNumber());
            if(icePutApply == null){
                continue;
            }
            SimpleStoreVo simpleStoreVo = storeVoMap.get(icePutApply.getPutStoreNumber());
            if(simpleStoreVo == null){
                continue;
            }
            iceDepositResponse.setClientNumber(simpleStoreVo.getStoreNumber());
            iceDepositResponse.setClientName(simpleStoreVo.getStoreName());
            MemberInfoVo memberInfoVo = FeignResponseUtil.getFeignData(feignStoreRelateMemberClient.getMemberByStoreNumber(simpleStoreVo.getStoreNumber()));
            if(memberInfoVo != null){
                iceDepositResponse.setContactName(memberInfoVo.getName());
                iceDepositResponse.setContactMobile(memberInfoVo.getMobile());
            }else {
                iceDepositResponse.setContactName(simpleStoreVo.getLinkman());
                iceDepositResponse.setContactMobile(simpleStoreVo.getLinkmanMobile());
            }

            iceDepositResponse.setClientPlace(simpleStoreVo.getAddress());
            IceBox iceBox = iceBoxMap.get(relateBox.getBoxId());
            if(iceBox == null){
                continue;
            }
            iceDepositResponse.setChestModel(iceBox.getModelName());
            iceDepositResponse.setChestName(iceBox.getChestName());
            iceDepositResponse.setAssetId(iceBox.getAssetId());
            iceDepositResponse.setChestMoney(iceBox.getChestMoney().toPlainString());
            String marketAreaName = FeignResponseUtil.getFeignData(feignCacheClient.getForMarketAreaName(iceBox.getDeptId()));
            iceDepositResponse.setMarketAreaName(marketAreaName);
            // 初始化时 采用免押时的数据
            iceDepositResponse.setPayMoney("0");
            iceDepositResponse.setPayTime(icePutApply.getCreatedTime().getTime());
            iceDepositResponse.setOrderNum("");
            if(relateBox.getFreeType().equals(FreePayTypeEnum.UN_FREE.getType())) {
                IcePutOrder putOrder = putOrderMap.get(relateBox.getApplyNumber());
                if(putOrder == null){
                    continue;
                }
                iceDepositResponse.setPayMoney(putOrder.getPayMoney().toPlainString());
                iceDepositResponse.setPayTime(putOrder.getPayTime().getTime());
                iceDepositResponse.setOrderNum(putOrder.getOrderNum());
            }
            iceDepositResponses.add(iceDepositResponse);
        }
        return iceDepositResponses;
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
            OrderInfo orderInfo = new OrderInfo();
            if(iceChestPutRecord.getFreePayType().equals(FreePayTypeEnum.UN_FREE.getType())){ // 含有免押处理
                orderInfo = orderInfos.stream().filter(x -> x.getChestPutRecordId().equals(orderForeignKeyMap.get(iceChestPutRecord.getId()))).findFirst().get();
            }
            // 服务处信息
            MarketArea marketArea = marketAreas.stream().filter(x -> x.getId().equals(iceChestInfo.getMarketAreaId())).findFirst().get();
            return buildIceDepositResponse(iceChestPutRecord, clientInfo, iceChestInfo, orderInfo, marketArea);
        });
    }

    private IceDepositResponse buildIceDepositResponse(IceChestPutRecord iceChestPutRecord, ClientInfo clientInfo, IceChestInfo iceChestInfo, OrderInfo orderInfo, MarketArea marketArea) {
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

        // 初始化时 采用免押时的数据
        iceDepositResponse.setPayMoney("0");
        iceDepositResponse.setPayTime(iceChestPutRecord.getCreateTime().getTime());
        iceDepositResponse.setOrderNum("");
        if(iceChestPutRecord.getFreePayType().equals(FreePayTypeEnum.UN_FREE.getType())) {
            iceDepositResponse.setPayMoney(orderInfo.getPayMoney().toPlainString());
            iceDepositResponse.setPayTime(orderInfo.getPayTime().getTime());
            iceDepositResponse.setOrderNum(orderInfo.getOrderNum());
        }

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
        String prepayId = weiXinService.createWeiXinPay(clientInfoRequest, iceChestInfo.getDepositMoney(), orderNum, clientInfoRequest.getOpenid());
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
        String prepayId = weiXinService.createWeiXinPay(clientInfoRequest, iceChestInfo.getDepositMoney(), orderNum, clientInfoRequest.getOpenid());
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
