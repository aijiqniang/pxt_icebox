package com.szeastroc.icebox.newprocess.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.entity.customer.vo.StoreInfoDtoVo;
import com.szeastroc.common.entity.customer.vo.SubordinateInfoVo;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.exception.NormalOptionException;
import com.szeastroc.common.feign.customer.FeignStoreClient;
import com.szeastroc.common.feign.customer.FeignSupplierClient;
import com.szeastroc.common.feign.user.FeignUserClient;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.icebox.constant.IceBoxConstant;
import com.szeastroc.icebox.enums.*;
import com.szeastroc.icebox.newprocess.consumer.common.IceBoxPutReportMsg;
import com.szeastroc.common.entity.icebox.vo.IceInspectionReportMsg;
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
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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
    private final FeignStoreClient feignStoreClient;
    private final FeignSupplierClient feignSupplierClient;
    @Autowired
    private IceBoxService iceBoxService;

    @Override
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public OrderPayResponse applyPayIceBox(ClientInfoRequest clientInfoRequest) throws Exception {

        // ????????????????????????????????????????????????
        /*IceBox iceBox = iceBoxDao.selectById(clientInfoRequest.getIceChestId());
        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(clientInfoRequest.getIceChestId());*/

        IceBox iceBox = iceBoxDao.selectOne(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getAssetId, clientInfoRequest.getIceChestAssetId()));
        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectOne(Wrappers.<IceBoxExtend>lambdaQuery().eq(IceBoxExtend::getAssetId, clientInfoRequest.getIceChestAssetId()));

        /**
         * ????????????5.20 ?????????????????????????????????????????????????????????  ????????????IcePutApplyRelateBox??????????????????????????????  ???????????????????????????????????????
         * ?????????????????????????????????  ????????????????????????  ??????  t_apply_relate_put_store_model
         * applynumber???????????????iceboxextend??????  ?????????????????????  ????????????iceboxextend???????????????????????? ??????????????????????????????
         */
        /*IcePutApplyRelateBox icePutApplyRelateBox = icePutApplyRelateBoxDao.selectOne(Wrappers.<IcePutApplyRelateBox>lambdaQuery()
                .eq(IcePutApplyRelateBox::getApplyNumber, icePutApply.getApplyNumber())
                .eq(IcePutApplyRelateBox::getBoxId, clientInfoRequest.getIceChestId()));*/
        PutStoreRelateModel relateModel = putStoreRelateModelDao.selectOne(Wrappers.<PutStoreRelateModel>lambdaQuery().eq(PutStoreRelateModel::getPutStoreNumber, clientInfoRequest.getClientNumber()).eq(PutStoreRelateModel::getSupplierId, iceBox.getSupplierId()).eq(PutStoreRelateModel::getModelId, iceBox.getModelId()).eq(PutStoreRelateModel::getPutStatus, PutStatus.DO_PUT.getStatus()).eq(PutStoreRelateModel::getExamineStatus, ExamineStatusEnum.IS_PASS.getStatus()).eq(PutStoreRelateModel::getStatus, 1).orderByDesc(PutStoreRelateModel::getId).last("limit 1"));
        if(relateModel == null){
            throw new ImproperOptionException("????????????????????????");
        }
        ApplyRelatePutStoreModel applyRelatePutStoreModel = applyRelatePutStoreModelDao.selectOne(Wrappers.<ApplyRelatePutStoreModel>lambdaQuery().eq(ApplyRelatePutStoreModel::getStoreRelateModelId, relateModel.getId()).last("limit 1"));
        if(applyRelatePutStoreModel == null){
            throw new ImproperOptionException("????????????????????????");
        }
        IcePutApply icePutApply = icePutApplyDao.selectOne(Wrappers.<IcePutApply>lambdaQuery().eq(IcePutApply::getApplyNumber, applyRelatePutStoreModel.getApplyNumber()));
        if (icePutApply == null) {
            throw new ImproperOptionException("???????????????????????????");
        }

        if (applyRelatePutStoreModel.getFreeType().equals(FreePayTypeEnum.IS_FREE.getType())) {
//            throw new ImproperOptionException("??????????????????????????????????????????");
            icePutApply.setStoreSignStatus(StoreSignStatus.ALREADY_SIGN.getStatus());
            icePutApply.setUpdateTime(new Date());
            icePutApplyDao.updateById(icePutApply);
            //???????????????????????????
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

                    iceBoxExtend.setAssetId(IceBoxConstant.virtual_asset_id);
                    iceBoxExtendDao.updateById(iceBoxExtend);
                }
            }
            //OrderPayResponse payResponse = createByFree(clientInfoRequest, iceBox);
            OrderPayResponse payResponse = createByFreeNew(clientInfoRequest, iceBox,iceBoxExtend,icePutApply);
            return payResponse;
        }

        // ????????????????????????
        // |-> ?????????, ??????, ????????????????????????
        // |-> ??????, ????????????, ???????????????
        IcePutOrder icePutOrder = icePutOrderDao.selectOne(Wrappers.<IcePutOrder>lambdaQuery()
                .ne(IcePutOrder::getStatus, OrderStatus.IS_CANCEL)
                .eq(IcePutOrder::getApplyNumber, icePutApply.getApplyNumber())
                .eq(IcePutOrder::getChestId, iceBox.getId()));

        if (Objects.isNull(icePutOrder)) {
            icePutOrder = createByUnFree(clientInfoRequest, iceBox, icePutApply.getApplyNumber());
        }
        // ???????????????????????????
        if (assertOrderInfoTimeOut(icePutOrder.getCreatedTime())) {
            icePutOrder = closeWechatWithTimeoutAndCreateNewOrder(clientInfoRequest, icePutOrder, iceBox, icePutApply.getApplyNumber());
        }

        //????????????, ??????????????????, ?????????????????????
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
         * ??????????????????
         */
        String orderNum = CommonUtil.generateOrderNumber();
        //????????????????????????
        String prepayId = weiXinService.createWeiXinPay(clientInfoRequest, iceBox.getDepositMoney(), orderNum, clientInfoRequest.getOpenid());
        //????????????
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
     * ?????????????????????????????????????????????????????????( 10?????? )
     *
     * @param orderTime
     * @return true ?????? false ?????????
     */
    private boolean assertOrderInfoTimeOut(Date orderTime) {
        long time = new DateTime().toDate().getTime() - orderTime.getTime();
        if (time >= weiXinConfig.getOrder().getTimeout()) {
            return true;
        }
        return false;
    }

    private IcePutOrder closeWechatWithTimeoutAndCreateNewOrder(ClientInfoRequest clientInfoRequest, IcePutOrder icePutOrder, IceBox iceBox, String applyNumber) throws Exception {
        // ??????????????????
        weiXinService.closeWeiXinPay(icePutOrder.getOrderNum());
        icePutOrder.setStatus(OrderStatus.IS_CANCEL.getStatus());
        icePutOrderDao.updateById(icePutOrder);
        // ???????????????
        return createByUnFree(clientInfoRequest, iceBox, applyNumber);
    }

    /**
     * ????????????  ??????????????????????????????  ??????????????????????????????
     * @param clientInfoRequest
     * @param iceBox
     * @return
     * @throws ImproperOptionException
     */
    public OrderPayResponse createByFreeNew(ClientInfoRequest clientInfoRequest, IceBox iceBox,IceBoxExtend iceBoxExtend,IcePutApply icePutApply) throws ImproperOptionException {
        //?????????????????????????????????
        iceBox.setPutStatus(PutStatus.FINISH_PUT.getStatus());
        iceBox.setPutStoreNumber(clientInfoRequest.getClientNumber());
        //??????????????????????????????????????????
        if(iceBox.getPutStoreNumber().startsWith("C0")){
            StoreInfoDtoVo store = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(iceBox.getPutStoreNumber()));
            if(store != null){
                iceBox.setDeptId(store.getMarketArea());
            }
        }else {
            SubordinateInfoVo supplier = FeignResponseUtil.getFeignData(feignSupplierClient.findByNumber(iceBox.getPutStoreNumber()));
            if(supplier != null){
                iceBox.setDeptId(supplier.getMarketAreaId());
            }
        }


        iceBoxExtend.setLastApplyNumber(icePutApply.getApplyNumber());
        iceBoxExtend.setLastPutTime(icePutApply.getCreatedTime());
        iceBoxExtend.setLastPutId(icePutApply.getId());
        iceBoxExtendDao.updateById(iceBoxExtend);

        ApplyRelatePutStoreModel applyRelatePutStoreModel1 = applyRelatePutStoreModelDao.selectOne(Wrappers.<ApplyRelatePutStoreModel>lambdaQuery().eq(ApplyRelatePutStoreModel::getApplyNumber,icePutApply.getApplyNumber()).last("limit 1"));

        /**
         *????????????????????????????????????????????????
         */
        IcePutApplyRelateBox isExist = icePutApplyRelateBoxDao.selectOne(Wrappers.<IcePutApplyRelateBox>lambdaQuery().eq(IcePutApplyRelateBox::getApplyNumber, icePutApply.getApplyNumber()).eq(IcePutApplyRelateBox::getBoxId, iceBox.getId()));
        if (isExist == null) {
            IcePutApplyRelateBox applyRelateBox = new IcePutApplyRelateBox();
            applyRelateBox.setApplyNumber(icePutApply.getApplyNumber());
            applyRelateBox.setBoxId(iceBox.getId());
            applyRelateBox.setModelId(iceBox.getModelId());
            applyRelateBox.setFreeType(applyRelatePutStoreModel1.getFreeType());
            icePutApplyRelateBoxDao.insert(applyRelateBox);
        }

        //todo ???????????????????????????
        LambdaQueryWrapper<PutStoreRelateModel> wrapper = Wrappers.<PutStoreRelateModel>lambdaQuery();
        wrapper.eq(PutStoreRelateModel::getPutStoreNumber, iceBox.getPutStoreNumber());
        wrapper.eq(PutStoreRelateModel::getSupplierId, iceBox.getSupplierId());
        wrapper.eq(PutStoreRelateModel::getModelId, iceBox.getModelId());
        wrapper.eq(PutStoreRelateModel::getPutStatus, PutStatus.DO_PUT.getStatus());
        wrapper.eq(PutStoreRelateModel::getExamineStatus, ExamineStatusEnum.IS_PASS.getStatus());
        wrapper.orderByDesc(PutStoreRelateModel::getId);
        List<PutStoreRelateModel> relateModelList = putStoreRelateModelDao.selectList(wrapper);
        if (CollectionUtil.isNotEmpty(relateModelList)) {
            for (PutStoreRelateModel relateModel : relateModelList) {
                ApplyRelatePutStoreModel applyRelatePutStoreModel = applyRelatePutStoreModelDao.selectOne(Wrappers.<ApplyRelatePutStoreModel>lambdaQuery().eq(ApplyRelatePutStoreModel::getStoreRelateModelId, relateModel.getId()));
                log.info("????????????????????????????????????,applyRelatePutStoreModel---??????{}???", JSON.toJSONString(applyRelatePutStoreModel));
                if (applyRelatePutStoreModel != null && FreePayTypeEnum.IS_FREE.getType().equals(applyRelatePutStoreModel.getFreeType())) {
                    relateModel.setPutStatus(PutStatus.FINISH_PUT.getStatus());
                    relateModel.setUpdateTime(new Date());
                    relateModel.setSignTime(new Date());
                    putStoreRelateModelDao.updateById(relateModel);
                    /**
                     * ????????????????????????????????????  ????????????????????????????????????
                     */
                    /*IceTransferRecord transferRecord = iceTransferRecordDao.selectOne(Wrappers.<IceTransferRecord>lambdaQuery().eq(IceTransferRecord::getBoxId, iceBox.getId()).eq(IceTransferRecord::getApplyNumber, applyRelatePutStoreModel.getApplyNumber()));
                    if (transferRecord != null) {
                        transferRecord.setRecordStatus(RecordStatus.SEND_ING.getStatus());
                        transferRecord.setUpdateTime(new Date());
                        iceTransferRecordDao.updateById(transferRecord);
                    }*/
                    IceTransferRecord transferRecord = iceTransferRecordDao.selectOne(Wrappers.<IceTransferRecord>lambdaQuery().eq(IceTransferRecord::getBoxId, iceBox.getId()).eq(IceTransferRecord::getApplyNumber, icePutApply.getApplyNumber()));
                    if(transferRecord == null){
                        IceTransferRecord iceTransferRecord = IceTransferRecord.builder()
                                .applyNumber(icePutApply.getApplyNumber())
                                .applyTime(new Date())
                                .applyUserId(icePutApply.getUserId())
                                .boxId(iceBox.getId())
                                .createTime(new Date())
                                .recordStatus(RecordStatus.APPLY_ING.getStatus())
                                .serviceType(ServiceType.IS_PUT.getType())
                                .storeNumber(iceBox.getPutStoreNumber())
                                .supplierId(iceBox.getSupplierId())
                                .build();
                        iceTransferRecord.setTransferMoney(new BigDecimal(0));
                        iceTransferRecord.setRecordStatus(RecordStatus.SEND_ING.getStatus());
                        IcePutApplyRelateBox relateBox = icePutApplyRelateBoxDao.selectOne(Wrappers.<IcePutApplyRelateBox>lambdaQuery().eq(IcePutApplyRelateBox::getBoxId, iceBox.getId()).eq(IcePutApplyRelateBox::getApplyNumber, icePutApply.getApplyNumber()));
                        if (relateBox != null && FreePayTypeEnum.UN_FREE.getType().equals(relateBox.getFreeType())) {
                            iceTransferRecord.setTransferMoney(iceBox.getDepositMoney());
                        }
                        iceTransferRecordDao.insert(iceTransferRecord);
                        log.info("applyNumber-->???{}???????????????????????????",icePutApply.getApplyNumber());
                    }

                    //???????????????????????????
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

                            IceBoxExtend iceBoxExtendVo = new IceBoxExtend();
                            iceBoxExtendVo.setId(iceBox.getId());
                            iceBoxExtendVo.setAssetId(IceBoxConstant.virtual_asset_id);
                            iceBoxExtendDao.updateById(iceBoxExtend);
                        }

                    }

                    /*IceBoxPutReport putReport = iceBoxPutReportDao.selectOne(Wrappers.<IceBoxPutReport>lambdaQuery().eq(IceBoxPutReport::getIceBoxId, reportMsg.getIceBoxId())
                            .eq(IceBoxPutReport::getApplyNumber, reportMsg.getApplyNumber())
                            .eq(IceBoxPutReport::getPutStatus, PutStatus.DO_PUT.getStatus()).last("limit 1"));
                    if(putReport != null){
                        putReport.setIceBoxId(iceBox.getId());
                        putReport.setIceBoxAssetId(iceBox.getAssetId());
                        putReport.setApplyNumber(applyRelatePutStoreModel.getApplyNumber());
                        putReport.setIceBoxModelId(iceBox.getModelId());
                        putReport.setSupplierId(iceBox.getSupplierId());
                        putReport.setPutStatus(PutStatus.FINISH_PUT.getStatus());
                        putReport.setPutStatus(reportMsg.getPutStatus());
                        putReport.setSignTime(new Date());
                        iceBoxPutReportDao.updateById(putReport);
                    }*/
                    /**
                     * ????????????
                     */

                    IceBoxPutReport putReport = iceBoxPutReportDao.selectOne(Wrappers.<IceBoxPutReport>lambdaQuery().eq(IceBoxPutReport::getPutStoreModelId,relateModel.getId()));
                    if(putReport != null){
                        putReport.setIceBoxId(iceBox.getId());
                        putReport.setIceBoxAssetId(iceBox.getAssetId());
                        putReport.setSupplierId(iceBox.getSupplierId());
                        putReport.setIceBoxModelId(iceBox.getModelId());
                        putReport.setPutStatus(PutStatus.FINISH_PUT.getStatus());
                        putReport.setSignTime(new Date());
                        if(putReport.getSubmitterId() != null && StringUtils.isNotEmpty(putReport.getSubmitterName())){
                            iceBox.setResponseManId(putReport.getSubmitterId());
                            iceBox.setResponseMan(putReport.getSubmitterName());
                        }
                        iceBoxPutReportDao.updateById(putReport);
                    }


                    //??????mq??????,???????????????????????????
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
        iceBoxDao.updateById(iceBox);
        OrderPayResponse orderPayResponse = new OrderPayResponse(FreePayTypeEnum.IS_FREE.getType());

        JSONObject jsonObject = iceBoxService.setAssetReportJson(iceBox,"createByFree");

        boolean actualTransactionActive = TransactionSynchronizationManager.isActualTransactionActive();
        if(actualTransactionActive){
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.ICEBOX_ASSETS_REPORT_ROUTING_KEY, jsonObject.toString());
                    //??????????????????????????????
                    IceInspectionReportMsg reportMsg = new IceInspectionReportMsg();
                    reportMsg.setOperateType(1);
                    reportMsg.setBoxId(iceBox.getId());
                    rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.iceInspectionReportKey,reportMsg);

                }
            });
        }
        return orderPayResponse;
    }

    @Override
    public OrderPayResponse createByFree(ClientInfoRequest clientInfoRequest, IceBox iceBox) throws ImproperOptionException {
        //?????????????????????????????????
        iceBox.setPutStatus(PutStatus.FINISH_PUT.getStatus());

        //??????????????????????????????????????????
        if(iceBox.getPutStoreNumber().startsWith("C0")){
            StoreInfoDtoVo store = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(iceBox.getPutStoreNumber()));
            if(store != null){
                iceBox.setDeptId(store.getMarketArea());
            }
        }else {
            SubordinateInfoVo supplier = FeignResponseUtil.getFeignData(feignSupplierClient.findByNumber(iceBox.getPutStoreNumber()));
            if(supplier != null){
                iceBox.setDeptId(supplier.getMarketAreaId());
            }
        }
        iceBoxDao.updateById(iceBox);


        //todo ???????????????????????????
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
                log.info("????????????????????????????????????,applyRelatePutStoreModel---??????{}???", JSON.toJSONString(applyRelatePutStoreModel));
                if (applyRelatePutStoreModel != null && FreePayTypeEnum.IS_FREE.getType().equals(applyRelatePutStoreModel.getFreeType())) {
                    relateModel.setPutStatus(PutStatus.FINISH_PUT.getStatus());
                    relateModel.setUpdateTime(new Date());
                    relateModel.setSignTime(new Date());
                    putStoreRelateModelDao.updateById(relateModel);
                    IceTransferRecord transferRecord = iceTransferRecordDao.selectOne(Wrappers.<IceTransferRecord>lambdaQuery().eq(IceTransferRecord::getBoxId, iceBox.getId()).eq(IceTransferRecord::getApplyNumber, applyRelatePutStoreModel.getApplyNumber()));
                    if (transferRecord != null) {
                        transferRecord.setRecordStatus(RecordStatus.SEND_ING.getStatus());
                        transferRecord.setUpdateTime(new Date());
                        iceTransferRecordDao.updateById(transferRecord);
                    }
                    //???????????????????????????
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
                        putReport.setSignTime(new Date());
                        iceBoxPutReportDao.updateById(putReport);
                    }
                    //??????mq??????,???????????????????????????
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

        boolean actualTransactionActive = TransactionSynchronizationManager.isActualTransactionActive();
        if(actualTransactionActive){
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.ICEBOX_ASSETS_REPORT_ROUTING_KEY, jsonObject.toString());
                    //??????????????????????????????
                    IceInspectionReportMsg reportMsg = new IceInspectionReportMsg();
                    reportMsg.setOperateType(1);
                    reportMsg.setBoxId(iceBox.getId());
                    rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.iceInspectionReportKey,reportMsg);

                }
            });
        }
        return orderPayResponse;
    }

    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    @Override
    public JSONObject notifyOrderInfo(OrderPayBack orderPayBack) {
        //???????????????????????????
        IcePutOrder icePutOrder = icePutOrderDao.selectOne(Wrappers.<IcePutOrder>lambdaQuery().eq(IcePutOrder::getOrderNum, orderPayBack.getOutTradeNo()));
        if (Objects.isNull(icePutOrder)) {
            log.info("??????:??????????????????,?????????????????? -> {}", JSON.toJSONString(orderPayBack));
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }
        //????????????????????????, ?????????????????????
        if (icePutOrder.getStatus().equals(OrderStatus.IS_FINISH.getStatus())) {
            return null;
        }

        //????????????????????????
        IceBox iceBox = iceBoxDao.selectById(icePutOrder.getChestId());
        if (Objects.isNull(iceBox)) {
            log.info("??????:??????????????????,??????????????????-> {}", JSON.toJSONString(icePutOrder));
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }
        updateInfoWhenFinishPay(orderPayBack, icePutOrder, iceBox);
        JSONObject jsonObject = iceBoxService.setAssetReportJson(iceBox,"notifyOrderInfo");
        return jsonObject;
    }

    private void updateInfoWhenFinishPay(OrderPayBack orderPayBack, IcePutOrder icePutOrder, IceBox iceBox) {

        //????????????
        icePutOrder.setStatus(OrderStatus.IS_FINISH.getStatus());
        icePutOrder.setTransactionId(orderPayBack.getTransactionId());
        DateTimeFormatter format = DateTimeFormat.forPattern("yyyyMMddHHmmss");
        icePutOrder.setPayTime(DateTime.parse(orderPayBack.getTimeEnd(), format).toDate());
        MathContext mc = new MathContext(2, RoundingMode.HALF_DOWN);
        icePutOrder.setPayMoney(new BigDecimal(orderPayBack.getTotalFee()).divide(new BigDecimal(100), mc));
        icePutOrderDao.updateById(icePutOrder);

        //??????????????????, ???????????????????????????????????????
        //????????????????????????????????????
        IcePutApply icePutApply = icePutApplyDao.selectOne(Wrappers.<IcePutApply>lambdaQuery().eq(IcePutApply::getApplyNumber, icePutOrder.getApplyNumber()));
        if (icePutApply == null) {
            log.info("??????:??????????????????,??????????????????????????????-> {}", JSON.toJSONString(icePutOrder));
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }
        icePutApply.setStoreSignStatus(StoreSignStatus.ALREADY_SIGN.getStatus());
        icePutApply.setUpdateTime(new Date());
        icePutApplyDao.updateById(icePutApply);

        //????????????????????????
        iceBox.setPutStatus(PutStatus.FINISH_PUT.getStatus());
        /**
         * 6.29fix?????????????????????????????????  ??????icebox??????storenumber???????????????  ?????????applrelatemodel?????????   ????????????iceboxextend??????
         */
        IceBoxExtend extend = new IceBoxExtend();
        extend.setId(iceBox.getId());
        extend.setLastPutId(icePutApply.getId());
        extend.setLastApplyNumber(icePutApply.getApplyNumber());
        iceBoxExtendDao.updateById(extend);

        List<ApplyRelatePutStoreModel> applyRelatePutStoreModels = applyRelatePutStoreModelDao.selectList(Wrappers.<ApplyRelatePutStoreModel>lambdaQuery().eq(ApplyRelatePutStoreModel::getApplyNumber, icePutOrder.getApplyNumber()).eq(ApplyRelatePutStoreModel::getFreeType, FreePayTypeEnum.UN_FREE.getType()));
        if(applyRelatePutStoreModels.size() == 0){
            log.info("??????:??????????????????,????????????applyrelatemodel????????????-> {}", JSON.toJSONString(icePutOrder));
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }
        List<Integer> storeRelateModelIds = applyRelatePutStoreModels.stream().map(x->x.getStoreRelateModelId()).collect(Collectors.toList());

        //iceBoxDao.update(null,Wrappers.<IceBox>lambdaUpdate().set(IceBox::getPutStatus,PutStatus.FINISH_PUT.getStatus()).eq(IceBox::getId,iceBox.getId()));
        LambdaQueryWrapper<PutStoreRelateModel> wrapper = Wrappers.<PutStoreRelateModel>lambdaQuery();
        //wrapper.eq(PutStoreRelateModel::getPutStoreNumber, iceBox.getPutStoreNumber());
        wrapper.eq(PutStoreRelateModel::getSupplierId, iceBox.getSupplierId());
        wrapper.eq(PutStoreRelateModel::getModelId,iceBox.getModelId());
        wrapper.in(PutStoreRelateModel::getId,storeRelateModelIds);
        wrapper.eq(PutStoreRelateModel::getPutStatus, PutStatus.DO_PUT.getStatus());
        wrapper.eq(PutStoreRelateModel::getExamineStatus, ExamineStatusEnum.IS_PASS.getStatus());
        List<PutStoreRelateModel> relateModelList = putStoreRelateModelDao.selectList(wrapper);
        if (CollectionUtil.isNotEmpty(relateModelList)) {
            PutStoreRelateModel relateModel = relateModelList.get(0);
            relateModel.setPutStatus(PutStatus.FINISH_PUT.getStatus());
            relateModel.setUpdateTime(new Date());
            relateModel.setSignTime(new Date());
            putStoreRelateModelDao.updateById(relateModel);
            if(StringUtils.isNotEmpty(relateModel.getPutStoreNumber())){
                iceBox.setPutStoreNumber(relateModel.getPutStoreNumber());
                //??????????????????????????????????????????
                if(relateModel.getPutStoreNumber().startsWith("C0")){
                    StoreInfoDtoVo store = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(relateModel.getPutStoreNumber()));
                    if(store != null){
                        iceBox.setDeptId(store.getMarketArea());
                    }
                }else {
                    SubordinateInfoVo supplier = FeignResponseUtil.getFeignData(feignSupplierClient.findByNumber(relateModel.getPutStoreNumber()));
                    if(supplier != null){
                        iceBox.setDeptId(supplier.getMarketAreaId());
                    }
                }
            }
        }
//        iceBoxDao.updateById(iceBox);
        //todo ???????????????????????????
        //???????????????????????????
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

        ApplyRelatePutStoreModel applyRelatePutStoreModel1 = applyRelatePutStoreModelDao.selectOne(Wrappers.<ApplyRelatePutStoreModel>lambdaQuery().eq(ApplyRelatePutStoreModel::getApplyNumber,icePutApply.getApplyNumber()).last("limit 1"));

        /**
         *????????????????????????????????????????????????
         */
        IcePutApplyRelateBox isExist = icePutApplyRelateBoxDao.selectOne(Wrappers.<IcePutApplyRelateBox>lambdaQuery().eq(IcePutApplyRelateBox::getApplyNumber, icePutApply.getApplyNumber()).eq(IcePutApplyRelateBox::getBoxId, iceBox.getId()));
        if (isExist == null) {
            IcePutApplyRelateBox applyRelateBox = new IcePutApplyRelateBox();
            applyRelateBox.setApplyNumber(icePutApply.getApplyNumber());
            applyRelateBox.setBoxId(iceBox.getId());
            applyRelateBox.setModelId(iceBox.getModelId());
            applyRelateBox.setFreeType(applyRelatePutStoreModel1.getFreeType());
            icePutApplyRelateBoxDao.insert(applyRelateBox);
        }

        /**
         * ????????????  ??????????????????icebox ??????????????????
         */
        /*IceBoxPutReport putReport = iceBoxPutReportDao.selectOne(Wrappers.<IceBoxPutReport>lambdaQuery().eq(IceBoxPutReport::getIceBoxId, reportMsg.getIceBoxId())
                .eq(IceBoxPutReport::getApplyNumber, reportMsg.getApplyNumber())
                .eq(IceBoxPutReport::getPutStatus, PutStatus.DO_PUT.getStatus()).last("limit 1"));*/
        if(CollectionUtil.isNotEmpty(relateModelList)){
            IceBoxPutReport putReport = iceBoxPutReportDao.selectOne(Wrappers.<IceBoxPutReport>lambdaQuery().eq(IceBoxPutReport::getPutStoreModelId,relateModelList.get(0).getId()));
            if(putReport != null){
                putReport.setIceBoxId(iceBox.getId());
                putReport.setIceBoxAssetId(iceBox.getAssetId());
                putReport.setApplyNumber(icePutOrder.getApplyNumber());
                putReport.setSupplierId(iceBox.getSupplierId());
                putReport.setIceBoxModelId(iceBox.getModelId());
                putReport.setPutStatus(PutStatus.FINISH_PUT.getStatus());
                putReport.setSignTime(new Date());
                if(putReport.getSubmitterId() != null){
                    iceBox.setResponseManId(putReport.getSubmitterId());
                }
                if(StringUtils.isNotEmpty(putReport.getSubmitterName())){
                    iceBox.setResponseMan(putReport.getSubmitterName());
                }
                iceBoxPutReportDao.updateById(putReport);
            }
        }
        iceBoxDao.updateById(iceBox);
        //??????mq??????,???????????????????????????
//        CompletableFuture.runAsync(() -> {
//            IceBoxPutReportMsg report = new IceBoxPutReportMsg();
//            report.setIceBoxId(iceBox.getId());
//            report.setIceBoxAssetId(iceBox.getAssetId());
//            report.setApplyNumber(icePutOrder.getApplyNumber());
//            report.setPutStatus(PutStatus.FINISH_PUT.getStatus());
//            report.setOperateType(OperateTypeEnum.UPDATE.getType());
//            rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.iceboxReportKey, report);
//        }, ExecutorServiceFactory.getInstance());

        /**
         * ???????????? ?????????????????????????????? ????????????
         */
        /*IceTransferRecord transferRecord = iceTransferRecordDao.selectOne(Wrappers.<IceTransferRecord>lambdaQuery().eq(IceTransferRecord::getBoxId, iceBox.getId()).eq(IceTransferRecord::getApplyNumber, icePutApply.getApplyNumber()));
        if (transferRecord != null) {
            transferRecord.setRecordStatus(RecordStatus.SEND_ING.getStatus());
            transferRecord.setUpdateTime(new Date());
            iceTransferRecordDao.updateById(transferRecord);
        }*/
        IceTransferRecord transferRecord = iceTransferRecordDao.selectOne(Wrappers.<IceTransferRecord>lambdaQuery().eq(IceTransferRecord::getBoxId, iceBox.getId()).eq(IceTransferRecord::getApplyNumber, icePutApply.getApplyNumber()));
        if(transferRecord == null){
            IceTransferRecord iceTransferRecord = IceTransferRecord.builder()
                    .applyNumber(icePutApply.getApplyNumber())
                    .applyTime(new Date())
                    .applyUserId(icePutApply.getUserId())
                    .boxId(iceBox.getId())
                    .createTime(new Date())
                    .recordStatus(RecordStatus.APPLY_ING.getStatus())
                    .serviceType(ServiceType.IS_PUT.getType())
                    .storeNumber(iceBox.getPutStoreNumber())
                    .supplierId(iceBox.getSupplierId())
                    .build();
            iceTransferRecord.setTransferMoney(new BigDecimal(0));
            iceTransferRecord.setRecordStatus(RecordStatus.SEND_ING.getStatus());
            IcePutApplyRelateBox relateBox = icePutApplyRelateBoxDao.selectOne(Wrappers.<IcePutApplyRelateBox>lambdaQuery().eq(IcePutApplyRelateBox::getBoxId, iceBox.getId()).eq(IcePutApplyRelateBox::getApplyNumber, icePutApply.getApplyNumber()));
            if (relateBox != null && FreePayTypeEnum.UN_FREE.getType().equals(relateBox.getFreeType())) {
                iceTransferRecord.setTransferMoney(iceBox.getDepositMoney());
            }
            iceTransferRecordDao.insert(iceTransferRecord);
            log.info("applyNumber-->???{}???????????????????????????",icePutApply.getApplyNumber());
        }

        //??????????????????????????????
        IceInspectionReportMsg msg = new IceInspectionReportMsg();
        msg.setOperateType(1);
        msg.setBoxId(iceBox.getId());
        rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.iceInspectionReportKey,msg);
    }

    @Override
    public Boolean getPayStatus(String orderNumber) throws Exception {
        boolean flag = false;
        //????????????????????????????????????
        IcePutOrder icePutOrder = icePutOrderDao.selectOne(Wrappers.<IcePutOrder>lambdaQuery().eq(IcePutOrder::getOrderNum, orderNumber));
        if (Objects.isNull(icePutOrder)) {
            log.info("??????:????????????????????????,?????????????????? -> {}", JSON.toJSONString(orderNumber));
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }

        //????????????????????????
        IceBox iceBox = iceBoxDao.selectById(icePutOrder.getChestId());
        if (Objects.isNull(iceBox)) {
            log.info("??????:????????????????????????,??????????????????-> {}", JSON.toJSONString(icePutOrder));
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }
        //????????????????????????????????????
        IcePutApply icePutApply = icePutApplyDao.selectOne(Wrappers.<IcePutApply>lambdaQuery().eq(IcePutApply::getApplyNumber, icePutOrder.getApplyNumber()));
        if (Objects.isNull(icePutApply)) {
            log.info("??????:??????????????????,??????????????????????????????-> {}", JSON.toJSONString(icePutOrder));
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }

        //?????????????????????, ?????????????????????
        if (icePutOrder.getStatus().equals(OrderStatus.IS_FINISH.getStatus())) {
            return true;
        }

        //?????????????????????, ????????????
        if (icePutOrder.getStatus().equals(OrderStatus.IS_CANCEL.getStatus())) {
            throw new NormalOptionException(ResultEnum.ORDER_IS_CANCEL_AND_RETRY_NEW_ORDER.getCode(), ResultEnum.ORDER_IS_CANCEL_AND_RETRY_NEW_ORDER.getMessage());
        }

        //?????????????????????, ??????????????????????????????
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

        log.info("???????????? -> {}", JSON.toJSONString(orderPayBack));

        //????????????
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
            //?????????????????????????????????
            iceBox.setPutStatus(PutStatus.FINISH_PUT.getStatus());
            //??????????????????????????????????????????
            if(iceBox.getPutStoreNumber().startsWith("C0")){
                StoreInfoDtoVo store = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(iceBox.getPutStoreNumber()));
                if(store != null){
                    iceBox.setDeptId(store.getMarketArea());
                }
            }else {
                SubordinateInfoVo supplier = FeignResponseUtil.getFeignData(feignSupplierClient.findByNumber(iceBox.getPutStoreNumber()));
                if(supplier != null){
                    iceBox.setDeptId(supplier.getMarketAreaId());
                }
            }
            iceBoxDao.updateById(iceBox);
            //todo ???????????????????????????

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

            //???????????????????????????
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
            //??????mq??????,???????????????????????????
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

            // ?????? ????????????/????????????
            JSONObject jsonObject = iceBoxService.setAssetReportJson(iceBox,"getPayStatus");
            rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.ICEBOX_ASSETS_REPORT_ROUTING_KEY, jsonObject.toString());
            //??????????????????????????????
            IceInspectionReportMsg msg = new IceInspectionReportMsg();
            msg.setOperateType(1);
            msg.setBoxId(iceBox.getId());
            rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.iceInspectionReportKey,msg);
        }

        return flag;
    }
}
