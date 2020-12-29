package com.szeastroc.icebox.newprocess.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.exception.NormalOptionException;
import com.szeastroc.common.utils.ExecutorServiceFactory;
import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.icebox.constant.IceBoxConstant;
import com.szeastroc.icebox.enums.ExamineStatusEnum;
import com.szeastroc.icebox.enums.FreePayTypeEnum;
import com.szeastroc.icebox.enums.OrderStatus;
import com.szeastroc.icebox.enums.ResultEnum;
import com.szeastroc.icebox.newprocess.consumer.common.IceBoxPutReportMsg;
import com.szeastroc.icebox.newprocess.consumer.enums.OperateTypeEnum;
import com.szeastroc.icebox.newprocess.dao.*;
import com.szeastroc.icebox.newprocess.entity.*;
import com.szeastroc.icebox.newprocess.enums.IceBoxEnums;
import com.szeastroc.icebox.newprocess.enums.OldIceBoxSignNoticeStatusEnums;
import com.szeastroc.icebox.newprocess.enums.OrderSourceEnums;
import com.szeastroc.icebox.newprocess.enums.PutStatus;
import com.szeastroc.icebox.newprocess.enums.RecordStatus;
import com.szeastroc.icebox.newprocess.enums.StoreSignStatus;
import com.szeastroc.icebox.newprocess.service.IceBoxService;
import com.szeastroc.icebox.newprocess.service.IcePutOrderService;
import com.szeastroc.icebox.oldprocess.vo.ClientInfoRequest;
import com.szeastroc.icebox.oldprocess.vo.OrderPayBack;
import com.szeastroc.icebox.oldprocess.vo.OrderPayResponse;
import com.szeastroc.icebox.util.CommonUtil;
import com.szeastroc.icebox.util.wechatpay.WXPayUtil;
import com.szeastroc.icebox.util.wechatpay.WeiXinConfig;
import com.szeastroc.icebox.util.wechatpay.WeiXinService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @_(@Autowired))
public class IcePutOrderServiceImpl extends ServiceImpl<IcePutOrderDao, IcePutOrder> implements IcePutOrderService {

    private final WeiXinConfig weiXinConfig;
    private final WeiXinService weiXinService;
    private final IceBoxDao iceBoxDao;
    private final IcePutApplyDao icePutApplyDao;
    private final IcePutApplyRelateBoxDao icePutApplyRelateBoxDao;
    private final IcePutOrderDao icePutOrderDao;
    private final IceBoxExtendDao iceBoxExtendDao;
    private final PutStoreRelateModelDao putStoreRelateModelDao;
    private final ApplyRelatePutStoreModelDao applyRelatePutStoreModelDao;
    private final IceTransferRecordDao iceTransferRecordDao;
    private final OldIceBoxSignNoticeDao oldIceBoxSignNoticeDao;
    private final IceBoxPutReportDao iceBoxPutReportDao;
    private final RabbitTemplate rabbitTemplate;
    @Autowired
    private IceBoxService iceBoxService;

    @Override
    public OrderPayResponse applyPayIceBox(ClientInfoRequest clientInfoRequest) throws Exception {

        // 获取投放申请数据及对应冰柜的申请
        IceBox iceBox = iceBoxDao.selectById(clientInfoRequest.getIceChestId());
        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(clientInfoRequest.getIceChestId());
        IcePutApply icePutApply = icePutApplyDao.selectOne(Wrappers.<IcePutApply>lambdaQuery().eq(IcePutApply::getApplyNumber, iceBoxExtend.getLastApplyNumber()));
        if (icePutApply == null) {
            throw new ImproperOptionException("该冰柜不存在申请单");
        }
        IcePutApplyRelateBox icePutApplyRelateBox = icePutApplyRelateBoxDao.selectOne(Wrappers.<IcePutApplyRelateBox>lambdaQuery()
                .eq(IcePutApplyRelateBox::getApplyNumber, icePutApply.getApplyNumber())
                .eq(IcePutApplyRelateBox::getBoxId, clientInfoRequest.getIceChestId()));

        if (icePutApplyRelateBox.getFreeType().equals(FreePayTypeEnum.IS_FREE.getType())) {
//            throw new ImproperOptionException("不免押流程的申请存在免押冰柜");
            icePutApply.setStoreSignStatus(StoreSignStatus.ALREADY_SIGN.getStatus());
            icePutApplyDao.updateById(icePutApply);
            //旧冰柜更新通知状态
            if (IceBoxEnums.TypeEnum.OLD_ICE_BOX.getType().equals(iceBox.getIceBoxType())) {
                OldIceBoxSignNotice oldIceBoxSignNotice = oldIceBoxSignNoticeDao.selectOne(Wrappers.<OldIceBoxSignNotice>lambdaQuery().eq(OldIceBoxSignNotice::getIceBoxId, iceBox.getId())
                        .eq(OldIceBoxSignNotice::getPutStoreNumber, iceBox.getPutStoreNumber())
                        .eq(OldIceBoxSignNotice::getApplyNumber, icePutApplyRelateBox.getApplyNumber()));
                if (oldIceBoxSignNotice != null) {
                    oldIceBoxSignNotice.setStatus(OldIceBoxSignNoticeStatusEnums.IS_SIGNED.getStatus());
                    oldIceBoxSignNotice.setUpdateTime(new Date());
                    oldIceBoxSignNoticeDao.updateById(oldIceBoxSignNotice);
                }
                if(!IceBoxConstant.virtual_asset_id.equals(iceBox.getAssetId())){
                    iceBox.setOldAssetId(iceBox.getAssetId());
                    iceBox.setAssetId(IceBoxConstant.virtual_asset_id);
                    iceBox.setUpdatedTime(new Date());
                    iceBoxDao.updateById(iceBox);

                    iceBoxExtend.setAssetId(IceBoxConstant.virtual_asset_id);
                    iceBoxExtendDao.updateById(iceBoxExtend);
                }
            }
            OrderPayResponse payResponse = createByFree(clientInfoRequest, iceBox);
            return payResponse;
        }

        // 判断是否存在订单
        // |-> 不存在, 创建, 区分免费与非免费
        // |-> 存在, 查询订单, 看是否过期
        IcePutOrder icePutOrder = icePutOrderDao.selectOne(Wrappers.<IcePutOrder>lambdaQuery()
                .ne(IcePutOrder::getStatus, OrderStatus.IS_CANCEL)
                .eq(IcePutOrder::getApplyNumber, icePutApply.getApplyNumber())
                .eq(IcePutOrder::getChestId, clientInfoRequest.getIceChestId()));

        if (Objects.isNull(icePutOrder)) {
            icePutOrder = createByUnFree(clientInfoRequest, iceBox, icePutApply.getApplyNumber());
        }
        // 查询订单是否已超时
        if (assertOrderInfoTimeOut(icePutOrder.getCreatedTime())) {
            icePutOrder = closeWechatWithTimeoutAndCreateNewOrder(clientInfoRequest, icePutOrder, iceBox, icePutApply.getApplyNumber());
        }

        //属于自己, 返回订单信息, 重新调起旧订单
        Map<String, String> datas = new HashMap<>();
        if (OrderSourceEnums.OTOC.getType().equals(clientInfoRequest.getOrderSource())) {
            datas.put("appId", weiXinConfig.getAppId());
        } else {
            datas.put("appId", weiXinConfig.getDmsappId());
        }
//        datas.put("appId", weiXinConfig.getAppId());
        datas.put("timeStamp", String.valueOf(System.currentTimeMillis()));
        datas.put("nonceStr", WXPayUtil.generateNonceStr());
        datas.put("package", "prepay_id=" + icePutOrder.getPrayId());
        datas.put("signType", "MD5");
        String sign = WXPayUtil.generateSignature(datas, weiXinConfig.getSecret());
        OrderPayResponse orderPayResponse = new OrderPayResponse(FreePayTypeEnum.UN_FREE.getType(), datas.get("appId"),
                datas.get("timeStamp"), datas.get("nonceStr"), datas.get("package"), datas.get("signType"), sign, icePutOrder.getOrderNum());
        return orderPayResponse;
    }

    private IcePutOrder createByUnFree(ClientInfoRequest clientInfoRequest, IceBox iceBox, String applyNumber) {
        /**
         * 创建订单信息
         */
        String orderNum = CommonUtil.generateOrderNumber();
        //调用统一下单接口
        String prepayId = weiXinService.createWeiXinPay(clientInfoRequest, iceBox.getDepositMoney(), orderNum, clientInfoRequest.getOpenid());
        //创建订单
        IcePutOrder icePutOrder = new IcePutOrder();
        icePutOrder.setChestId(iceBox.getId());
        icePutOrder.setApplyNumber(applyNumber);
        icePutOrder.setOrderNum(orderNum);
        icePutOrder.setOpenid(clientInfoRequest.getOpenid());
        icePutOrder.setTotalMoney(iceBox.getDepositMoney());
        icePutOrder.setPayMoney(iceBox.getDepositMoney());
        icePutOrder.setPrayId(prepayId);
        icePutOrder.setStatus(OrderStatus.IS_PAY_ING.getStatus());
        icePutOrder.setOrderSource(clientInfoRequest.getOrderSource());
        icePutOrder.setCreatedBy(0);
        icePutOrder.setCreatedTime(new Date());
        icePutOrder.setUpdatedBy(0);
        icePutOrder.setUpdatedTime(icePutOrder.getCreatedTime());
        icePutOrderDao.insert(icePutOrder);
        return icePutOrder;
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

    private IcePutOrder closeWechatWithTimeoutAndCreateNewOrder(ClientInfoRequest clientInfoRequest, IcePutOrder icePutOrder, IceBox iceBox, String applyNumber) throws Exception {
        // 超时关闭订单
        weiXinService.closeWeiXinPay(icePutOrder.getOrderNum());
        icePutOrder.setStatus(OrderStatus.IS_CANCEL.getStatus());
        icePutOrderDao.updateById(icePutOrder);
        // 创建新订单
        return createByUnFree(clientInfoRequest, iceBox, applyNumber);
    }

    @Override
    public OrderPayResponse createByFree(ClientInfoRequest clientInfoRequest, IceBox iceBox) throws ImproperOptionException {
        //修改冰柜信息的投放状态
        iceBox.setPutStatus(PutStatus.FINISH_PUT.getStatus());
        iceBoxDao.updateById(iceBox);
        //todo 这里冰柜改为已投放
        LambdaQueryWrapper<PutStoreRelateModel> wrapper = Wrappers.<PutStoreRelateModel>lambdaQuery();
        wrapper.eq(PutStoreRelateModel::getPutStoreNumber, iceBox.getPutStoreNumber());
        wrapper.eq(PutStoreRelateModel::getSupplierId, iceBox.getSupplierId());
        wrapper.eq(PutStoreRelateModel::getModelId, iceBox.getModelId());
        wrapper.eq(PutStoreRelateModel::getPutStatus, PutStatus.DO_PUT.getStatus());
        wrapper.eq(PutStoreRelateModel::getExamineStatus, ExamineStatusEnum.IS_PASS.getStatus());
        List<PutStoreRelateModel> relateModelList = putStoreRelateModelDao.selectList(wrapper);
        if (CollectionUtil.isNotEmpty(relateModelList)) {
            for (PutStoreRelateModel relateModel : relateModelList) {
                ApplyRelatePutStoreModel applyRelatePutStoreModel = applyRelatePutStoreModelDao.selectOne(Wrappers.<ApplyRelatePutStoreModel>lambdaQuery().eq(ApplyRelatePutStoreModel::getStoreRelateModelId, relateModel.getId()));
                log.info("处理不需要审批的冰柜信息,applyRelatePutStoreModel---》【{}】", JSON.toJSONString(applyRelatePutStoreModel));
                if (applyRelatePutStoreModel != null && FreePayTypeEnum.IS_FREE.getType().equals(applyRelatePutStoreModel.getFreeType())) {
                    relateModel.setPutStatus(PutStatus.FINISH_PUT.getStatus());
                    relateModel.setUpdateTime(new Date());
                    putStoreRelateModelDao.updateById(relateModel);
                    IceTransferRecord transferRecord = iceTransferRecordDao.selectOne(Wrappers.<IceTransferRecord>lambdaQuery().eq(IceTransferRecord::getBoxId, iceBox.getId()).eq(IceTransferRecord::getApplyNumber, applyRelatePutStoreModel.getApplyNumber()));
                    if (transferRecord != null) {
                        transferRecord.setRecordStatus(RecordStatus.SEND_ING.getStatus());
                        transferRecord.setUpdateTime(new Date());
                        iceTransferRecordDao.updateById(transferRecord);
                    }
                    //旧冰柜更新通知状态
                    if (IceBoxEnums.TypeEnum.OLD_ICE_BOX.getType().equals(iceBox.getIceBoxType())) {
                        OldIceBoxSignNotice oldIceBoxSignNotice = oldIceBoxSignNoticeDao.selectOne(Wrappers.<OldIceBoxSignNotice>lambdaQuery().eq(OldIceBoxSignNotice::getIceBoxId, iceBox.getId())
                                .eq(OldIceBoxSignNotice::getPutStoreNumber, iceBox.getPutStoreNumber())
                                .eq(OldIceBoxSignNotice::getApplyNumber, applyRelatePutStoreModel.getApplyNumber()));
                        if (oldIceBoxSignNotice != null) {
                            oldIceBoxSignNotice.setStatus(OldIceBoxSignNoticeStatusEnums.IS_SIGNED.getStatus());
                            oldIceBoxSignNotice.setUpdateTime(new Date());
                            oldIceBoxSignNoticeDao.updateById(oldIceBoxSignNotice);
                        }
                        if(!IceBoxConstant.virtual_asset_id.equals(iceBox.getAssetId())){
                            iceBox.setOldAssetId(iceBox.getAssetId());
                            iceBox.setAssetId(IceBoxConstant.virtual_asset_id);
                            iceBox.setUpdatedTime(new Date());
                            iceBoxDao.updateById(iceBox);

                            IceBoxExtend iceBoxExtend = new IceBoxExtend();
                            iceBoxExtend.setId(iceBox.getId());
                            iceBoxExtend.setAssetId(IceBoxConstant.virtual_asset_id);
                            iceBoxExtendDao.updateById(iceBoxExtend);
                        }

                    }

                    IceBoxPutReportMsg reportMsg = new IceBoxPutReportMsg();
                    reportMsg.setIceBoxId(iceBox.getId());
                    reportMsg.setIceBoxAssetId(iceBox.getAssetId());
                    reportMsg.setApplyNumber(applyRelatePutStoreModel.getApplyNumber());
                    reportMsg.setPutStatus(PutStatus.FINISH_PUT.getStatus());
                    IceBoxPutReport putReport = iceBoxPutReportDao.selectOne(Wrappers.<IceBoxPutReport>lambdaQuery().eq(IceBoxPutReport::getIceBoxId, reportMsg.getIceBoxId())
                            .eq(IceBoxPutReport::getApplyNumber, reportMsg.getApplyNumber())
                            .eq(IceBoxPutReport::getPutStatus, PutStatus.DO_PUT.getStatus()).last("limit 1"));
                    if(putReport != null){
                        putReport.setPutStatus(reportMsg.getPutStatus());
                        iceBoxPutReportDao.updateById(putReport);
                    }
                    //发送mq消息,同步申请数据到报表
//                    CompletableFuture.runAsync(() -> {
//                        IceBoxPutReportMsg report = new IceBoxPutReportMsg();
//                        report.setIceBoxId(iceBox.getId());
//                        report.setIceBoxAssetId(iceBox.getAssetId());
//                        report.setApplyNumber(applyRelatePutStoreModel.getApplyNumber());
//                        report.setPutStatus(PutStatus.FINISH_PUT.getStatus());
//                        report.setOperateType(OperateTypeEnum.UPDATE.getType());
//                        rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.iceboxReportKey, report);
//                    }, ExecutorServiceFactory.getInstance());
                    break;
                }
            }
        }
        OrderPayResponse orderPayResponse = new OrderPayResponse(FreePayTypeEnum.IS_FREE.getType());

        JSONObject jsonObject = iceBoxService.setAssetReportJson(iceBox,"createByFree");
        rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.ICEBOX_ASSETS_REPORT_ROUTING_KEY, jsonObject.toString());
        return orderPayResponse;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    @Override
    public JSONObject notifyOrderInfo(OrderPayBack orderPayBack) {
        //根据订单号查询订单
        IcePutOrder icePutOrder = icePutOrderDao.selectOne(Wrappers.<IcePutOrder>lambdaQuery().eq(IcePutOrder::getOrderNum, orderPayBack.getOutTradeNo()));
        if (Objects.isNull(icePutOrder)) {
            log.info("异常:订单成功回调,丢失订单数据 -> {}", JSON.toJSONString(orderPayBack));
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }
        //判断是否订单完成, 完成则无需修改
        if (icePutOrder.getStatus().equals(OrderStatus.IS_FINISH.getStatus())) {
            return null;
        }

        //查询对应冰柜信息
        IceBox iceBox = iceBoxDao.selectById(icePutOrder.getChestId());
        if (Objects.isNull(iceBox)) {
            log.info("异常:订单成功回调,丢失冰柜信息-> {}", JSON.toJSONString(icePutOrder));
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }
        updateInfoWhenFinishPay(orderPayBack, icePutOrder, iceBox);
        JSONObject jsonObject = iceBoxService.setAssetReportJson(iceBox,"notifyOrderInfo");
        return jsonObject;
    }

    private void updateInfoWhenFinishPay(OrderPayBack orderPayBack, IcePutOrder icePutOrder, IceBox iceBox) {

        //修改订单
        icePutOrder.setStatus(OrderStatus.IS_FINISH.getStatus());
        icePutOrder.setTransactionId(orderPayBack.getTransactionId());
        DateTimeFormatter format = DateTimeFormat.forPattern("yyyyMMddHHmmss");
        icePutOrder.setPayTime(DateTime.parse(orderPayBack.getTimeEnd(), format).toDate());
        MathContext mc = new MathContext(2, RoundingMode.HALF_DOWN);
        icePutOrder.setPayMoney(new BigDecimal(orderPayBack.getTotalFee()).divide(new BigDecimal(100), mc));
        icePutOrderDao.updateById(icePutOrder);

        //修改投放记录, 申请投放记录现在是汇总信息
        //查询对应冰柜投放记录信息
        IcePutApply icePutApply = icePutApplyDao.selectOne(Wrappers.<IcePutApply>lambdaQuery().eq(IcePutApply::getApplyNumber, icePutOrder.getApplyNumber()));
        if (icePutApply == null) {
            log.info("异常:订单成功回调,丢失冰柜投放记录信息-> {}", JSON.toJSONString(icePutOrder));
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }
        icePutApply.setStoreSignStatus(StoreSignStatus.ALREADY_SIGN.getStatus());
        icePutApplyDao.updateById(icePutApply);

        //修改冰柜投放信息
        iceBox.setPutStatus(PutStatus.FINISH_PUT.getStatus());
        iceBoxDao.update(null,Wrappers.<IceBox>lambdaUpdate().set(IceBox::getPutStatus,PutStatus.FINISH_PUT.getStatus()).eq(IceBox::getId,iceBox.getId()));
        LambdaQueryWrapper<PutStoreRelateModel> wrapper = Wrappers.<PutStoreRelateModel>lambdaQuery();
        wrapper.eq(PutStoreRelateModel::getPutStoreNumber, iceBox.getPutStoreNumber());
        wrapper.eq(PutStoreRelateModel::getSupplierId, iceBox.getSupplierId());
        wrapper.eq(PutStoreRelateModel::getPutStatus, PutStatus.DO_PUT.getStatus());
        wrapper.eq(PutStoreRelateModel::getExamineStatus, ExamineStatusEnum.IS_PASS.getStatus());
        List<PutStoreRelateModel> relateModelList = putStoreRelateModelDao.selectList(wrapper);
        if (CollectionUtil.isNotEmpty(relateModelList)) {
            PutStoreRelateModel relateModel = relateModelList.get(0);
            relateModel.setPutStatus(PutStatus.FINISH_PUT.getStatus());
            relateModel.setUpdateTime(new Date());
            putStoreRelateModelDao.updateById(relateModel);
        }
//        iceBoxDao.updateById(iceBox);
        //todo 这里冰柜改为已投放
        //旧冰柜更新通知状态
        if (IceBoxEnums.TypeEnum.OLD_ICE_BOX.getType().equals(iceBox.getIceBoxType())) {
            OldIceBoxSignNotice oldIceBoxSignNotice = oldIceBoxSignNoticeDao.selectOne(Wrappers.<OldIceBoxSignNotice>lambdaQuery().eq(OldIceBoxSignNotice::getIceBoxId, iceBox.getId())
                    .eq(OldIceBoxSignNotice::getPutStoreNumber, iceBox.getPutStoreNumber())
                    .eq(OldIceBoxSignNotice::getApplyNumber, icePutOrder.getApplyNumber()));
            if (oldIceBoxSignNotice != null) {
                oldIceBoxSignNotice.setStatus(OldIceBoxSignNoticeStatusEnums.IS_SIGNED.getStatus());
                oldIceBoxSignNotice.setUpdateTime(new Date());
                oldIceBoxSignNoticeDao.updateById(oldIceBoxSignNotice);
            }
            if(!IceBoxConstant.virtual_asset_id.equals(iceBox.getAssetId())){
                iceBox.setOldAssetId(iceBox.getAssetId());
                iceBox.setAssetId(IceBoxConstant.virtual_asset_id);
                iceBox.setUpdatedTime(new Date());
                iceBoxDao.updateById(iceBox);

                IceBoxExtend iceBoxExtend = new IceBoxExtend();
                iceBoxExtend.setId(iceBox.getId());
                iceBoxExtend.setAssetId(IceBoxConstant.virtual_asset_id);
                iceBoxExtendDao.updateById(iceBoxExtend);
            }

        }

        IceBoxPutReportMsg reportMsg = new IceBoxPutReportMsg();
        reportMsg.setIceBoxId(iceBox.getId());
        reportMsg.setIceBoxAssetId(iceBox.getAssetId());
        reportMsg.setApplyNumber(icePutOrder.getApplyNumber());
        reportMsg.setPutStatus(PutStatus.FINISH_PUT.getStatus());
        IceBoxPutReport putReport = iceBoxPutReportDao.selectOne(Wrappers.<IceBoxPutReport>lambdaQuery().eq(IceBoxPutReport::getIceBoxId, reportMsg.getIceBoxId())
                .eq(IceBoxPutReport::getApplyNumber, reportMsg.getApplyNumber())
                .eq(IceBoxPutReport::getPutStatus, PutStatus.DO_PUT.getStatus()).last("limit 1"));
        if(putReport != null){
            putReport.setPutStatus(reportMsg.getPutStatus());
            iceBoxPutReportDao.updateById(putReport);
        }
        //发送mq消息,同步申请数据到报表
//        CompletableFuture.runAsync(() -> {
//            IceBoxPutReportMsg report = new IceBoxPutReportMsg();
//            report.setIceBoxId(iceBox.getId());
//            report.setIceBoxAssetId(iceBox.getAssetId());
//            report.setApplyNumber(icePutOrder.getApplyNumber());
//            report.setPutStatus(PutStatus.FINISH_PUT.getStatus());
//            report.setOperateType(OperateTypeEnum.UPDATE.getType());
//            rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.iceboxReportKey, report);
//        }, ExecutorServiceFactory.getInstance());

        IceTransferRecord transferRecord = iceTransferRecordDao.selectOne(Wrappers.<IceTransferRecord>lambdaQuery().eq(IceTransferRecord::getBoxId, iceBox.getId()).eq(IceTransferRecord::getApplyNumber, icePutApply.getApplyNumber()));
        if (transferRecord != null) {
            transferRecord.setRecordStatus(RecordStatus.SEND_ING.getStatus());
            transferRecord.setUpdateTime(new Date());
            iceTransferRecordDao.updateById(transferRecord);
        }
    }

    @Override
    public Boolean getPayStatus(String orderNumber) throws Exception {
        boolean flag = false;
        //查询数据库中对应订单状态
        IcePutOrder icePutOrder = icePutOrderDao.selectOne(Wrappers.<IcePutOrder>lambdaQuery().eq(IcePutOrder::getOrderNum, orderNumber));
        if (Objects.isNull(icePutOrder)) {
            log.info("异常:主动查询订单状态,丢失订单数据 -> {}", JSON.toJSONString(orderNumber));
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }

        //查询对应冰柜信息
        IceBox iceBox = iceBoxDao.selectById(icePutOrder.getChestId());
        if (Objects.isNull(iceBox)) {
            log.info("异常:主动查询订单状态,丢失冰柜信息-> {}", JSON.toJSONString(icePutOrder));
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }
        //查询对应冰柜投放记录信息
        IcePutApply icePutApply = icePutApplyDao.selectOne(Wrappers.<IcePutApply>lambdaQuery().eq(IcePutApply::getApplyNumber, icePutOrder.getApplyNumber()));
        if (Objects.isNull(icePutApply)) {
            log.info("异常:订单成功回调,丢失冰柜投放记录信息-> {}", JSON.toJSONString(icePutOrder));
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }

        //如果订单已完成, 则直接返回完成
        if (icePutOrder.getStatus().equals(OrderStatus.IS_FINISH.getStatus())) {
            return true;
        }

        //如果订单已取消, 抛出异常
        if (icePutOrder.getStatus().equals(OrderStatus.IS_CANCEL.getStatus())) {
            throw new NormalOptionException(ResultEnum.ORDER_IS_CANCEL_AND_RETRY_NEW_ORDER.getCode(), ResultEnum.ORDER_IS_CANCEL_AND_RETRY_NEW_ORDER.getMessage());
        }

        //如果显示未支付, 调用接口查询订单状态
        Map<String, String> data = new HashMap<String, String>();
        data.put("appid", weiXinConfig.getAppId());
        data.put("mch_id", weiXinConfig.getMchId());
        data.put("out_trade_no", icePutOrder.getOrderNum());
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
            if (resultMap.get("trade_state") != null && resultMap.get("trade_state").equals("SUCCESS") && resultMap.get("result_code").equals("SUCCESS")) {
                orderPayBack.setOpenid(resultMap.get("openid"));
                orderPayBack.setTransactionId(resultMap.get("transaction_id"));
                orderPayBack.setTimeEnd(resultMap.get("time_end"));
                orderPayBack.setTradeStateDesc(resultMap.get("trade_state_desc"));
                orderPayBack.setTotalFee(resultMap.get("total_fee"));
            }
        }

        log.info("回调数据 -> {}", JSON.toJSONString(orderPayBack));

        //修改订单
        if ("SUCCESS".equals(orderPayBack.getReturnCode()) && "SUCCESS".equals(orderPayBack.getTradeState()) && "SUCCESS".equals(orderPayBack.getResultCode())) {
            icePutOrder.setStatus(OrderStatus.IS_FINISH.getStatus());
            flag = true;
        }
        icePutOrder.setTransactionId(orderPayBack.getTransactionId());
        DateTimeFormatter format = DateTimeFormat.forPattern("yyyyMMddHHmmss");
        if (orderPayBack.getTimeEnd() != null) {
            icePutOrder.setPayTime(DateTime.parse(orderPayBack.getTimeEnd(), format).toDate());
        }
        if (orderPayBack.getTotalFee() != null) {
            MathContext mc = new MathContext(2, RoundingMode.HALF_DOWN);
            icePutOrder.setPayMoney(new BigDecimal(orderPayBack.getTotalFee()).divide(new BigDecimal(100), mc));
        }
        icePutOrder.setTradeState(orderPayBack.getTradeState());
        icePutOrder.setTradeStateDesc(orderPayBack.getTradeStateDesc());

        icePutOrderDao.updateById(icePutOrder);

        if (flag) {
            //修改冰柜信息的投放状态
            iceBox.setPutStatus(PutStatus.FINISH_PUT.getStatus());
            iceBoxDao.updateById(iceBox);
            //todo 这里冰柜改为已投放

            ApplyRelatePutStoreModel applyRelatePutStoreModel = applyRelatePutStoreModelDao.selectOne(Wrappers.<ApplyRelatePutStoreModel>lambdaQuery().eq(ApplyRelatePutStoreModel::getApplyNumber, icePutOrder.getApplyNumber()));
            if (applyRelatePutStoreModel != null) {
                PutStoreRelateModel relateModel = putStoreRelateModelDao.selectById(applyRelatePutStoreModel.getStoreRelateModelId());
                if (relateModel != null) {
                    relateModel.setPutStatus(PutStatus.FINISH_PUT.getStatus());
                    relateModel.setUpdateTime(new Date());
                    putStoreRelateModelDao.updateById(relateModel);
                }
            }

            icePutApply.setStoreSignStatus(StoreSignStatus.ALREADY_SIGN.getStatus());
            icePutApply.setUpdateTime(new Date());
            icePutApplyDao.updateById(icePutApply);

            IceTransferRecord transferRecord = iceTransferRecordDao.selectOne(Wrappers.<IceTransferRecord>lambdaQuery().eq(IceTransferRecord::getBoxId, iceBox.getId()).eq(IceTransferRecord::getApplyNumber, icePutOrder.getApplyNumber()));
            if (transferRecord != null) {
                transferRecord.setRecordStatus(RecordStatus.SEND_ING.getStatus());
                transferRecord.setUpdateTime(new Date());
                iceTransferRecordDao.updateById(transferRecord);
            }

            //旧冰柜更新通知状态
            if (IceBoxEnums.TypeEnum.OLD_ICE_BOX.getType().equals(iceBox.getIceBoxType())) {
                OldIceBoxSignNotice oldIceBoxSignNotice = oldIceBoxSignNoticeDao.selectOne(Wrappers.<OldIceBoxSignNotice>lambdaQuery().eq(OldIceBoxSignNotice::getIceBoxId, iceBox.getId())
                        .eq(OldIceBoxSignNotice::getPutStoreNumber, iceBox.getPutStoreNumber())
                        .eq(OldIceBoxSignNotice::getApplyNumber, icePutOrder.getApplyNumber()));
                if (oldIceBoxSignNotice != null) {
                    oldIceBoxSignNotice.setStatus(OldIceBoxSignNoticeStatusEnums.IS_SIGNED.getStatus());
                    oldIceBoxSignNotice.setUpdateTime(new Date());
                    oldIceBoxSignNoticeDao.updateById(oldIceBoxSignNotice);
                }
                if(!IceBoxConstant.virtual_asset_id.equals(iceBox.getAssetId())){
                    iceBox.setOldAssetId(iceBox.getAssetId());
                    iceBox.setAssetId(IceBoxConstant.virtual_asset_id);
                    iceBox.setUpdatedTime(new Date());
                    iceBoxDao.updateById(iceBox);

                    IceBoxExtend iceBoxExtend = new IceBoxExtend();
                    iceBoxExtend.setId(iceBox.getId());
                    iceBoxExtend.setAssetId(IceBoxConstant.virtual_asset_id);
                    iceBoxExtendDao.updateById(iceBoxExtend);
                }

            }

            IceBoxPutReportMsg reportMsg = new IceBoxPutReportMsg();
            reportMsg.setIceBoxId(iceBox.getId());
            reportMsg.setIceBoxAssetId(iceBox.getAssetId());
            reportMsg.setPutStatus(PutStatus.FINISH_PUT.getStatus());
            reportMsg.setApplyNumber(icePutOrder.getApplyNumber());
            IceBoxPutReport putReport = iceBoxPutReportDao.selectOne(Wrappers.<IceBoxPutReport>lambdaQuery().eq(IceBoxPutReport::getIceBoxId, reportMsg.getIceBoxId())
                    .eq(IceBoxPutReport::getApplyNumber, reportMsg.getApplyNumber())
                    .eq(IceBoxPutReport::getPutStatus, PutStatus.DO_PUT.getStatus()).last("limit 1"));
            if(putReport != null){
                putReport.setPutStatus(reportMsg.getPutStatus());
                iceBoxPutReportDao.updateById(putReport);
            }
            //发送mq消息,同步申请数据到报表
//            CompletableFuture.runAsync(() -> {
//                IceBoxPutReportMsg report = new IceBoxPutReportMsg();
//                report.setIceBoxId(iceBox.getId());
//                report.setIceBoxAssetId(iceBox.getAssetId());
//                report.setPutStatus(PutStatus.FINISH_PUT.getStatus());
//                report.setApplyNumber(icePutOrder.getApplyNumber());
//                report.setOperateType(OperateTypeEnum.UPDATE.getType());
//                rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.iceboxReportKey, report);
//            }, ExecutorServiceFactory.getInstance());

//            LambdaQueryWrapper<PutStoreRelateModel> wrapper = Wrappers.<PutStoreRelateModel>lambdaQuery();
//            wrapper.eq(PutStoreRelateModel::getPutStoreNumber, iceBox.getPutStoreNumber());
//            wrapper.eq(PutStoreRelateModel::getSupplierId, iceBox.getSupplierId());
//            wrapper.eq(PutStoreRelateModel::getPutStatus, PutStatus.DO_PUT.getStatus());
//            wrapper.eq(PutStoreRelateModel::getExamineStatus, ExamineStatusEnum.IS_PASS.getStatus());
//            List<PutStoreRelateModel> relateModelList = putStoreRelateModelDao.selectList(wrapper);
//            if(CollectionUtil.isNotEmpty(relateModelList)){
//                PutStoreRelateModel relateModel = relateModelList.get(0);
//                relateModel.setPutStatus( PutStatus.FINISH_PUT.getStatus());
//                relateModel.setUpdateTime(new Date());
//                putStoreRelateModelDao.updateById(relateModel);
//            }

            // 新的 冰柜状态/投放状态
            JSONObject jsonObject = iceBoxService.setAssetReportJson(iceBox,"getPayStatus");
            rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.ICEBOX_ASSETS_REPORT_ROUTING_KEY, jsonObject.toString());
        }

        return flag;
    }
}
