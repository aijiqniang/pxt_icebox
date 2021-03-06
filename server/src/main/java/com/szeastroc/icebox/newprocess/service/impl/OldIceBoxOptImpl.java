package com.szeastroc.icebox.newprocess.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Lists;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.entity.customer.vo.StoreInfoDtoVo;
import com.szeastroc.common.entity.customer.vo.SubordinateInfoVo;
import com.szeastroc.common.exception.NormalOptionException;
import com.szeastroc.common.feign.customer.FeignStoreClient;
import com.szeastroc.common.feign.customer.FeignSupplierClient;
import com.szeastroc.common.feign.user.FeignDeptClient;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.common.entity.icebox.vo.IceInspectionReportMsg;
import com.szeastroc.icebox.newprocess.dao.*;
import com.szeastroc.icebox.newprocess.entity.*;
import com.szeastroc.icebox.newprocess.enums.IceBoxEnums;
import com.szeastroc.icebox.newprocess.enums.PutStatus;
import com.szeastroc.icebox.newprocess.service.IceBoxService;
import com.szeastroc.icebox.newprocess.service.OldIceBoxOpt;
import com.szeastroc.icebox.newprocess.vo.OldIceBoxImportVo;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.*;

@Component
public class OldIceBoxOptImpl implements OldIceBoxOpt {


    @Resource
    private IceBoxDao iceBoxDao;
    @Resource
    private IceBoxExtendDao iceBoxExtendDao;
    @Resource
    private FeignDeptClient feignDeptClient;
    @Resource
    private FeignSupplierClient feignSupplierClient;
    @Resource
    private IceModelDao iceModelDao;
    @Autowired
    private IceBoxService iceBoxService;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private FeignStoreClient feignStoreClient;
    @Resource
    private OldIceBoxSignNoticeDao oldIceBoxSignNoticeDao;
    @Resource
    private ApplyRelatePutStoreModelDao applyRelatePutStoreModelDao;
    @Resource
    private IcePutApplyRelateBoxDao icePutApplyRelateBoxDao;
    @Resource
    private PutStoreRelateModelDao putStoreRelateModelDao;


    @Override
    @Transactional
    public List<JSONObject> opt(List<OldIceBoxImportVo> oldIceBoxImportVoList) {

        List<JSONObject> lists = Lists.newArrayList();
        Map<String, List<IceBox>> map = new HashMap<>();
        for (int i = 0; i < oldIceBoxImportVoList.size(); i++) {
            OldIceBoxImportVo oldIceBoxImportVo = oldIceBoxImportVoList.get(i);
            String type = oldIceBoxImportVo.getType();
            // excel ????????????
            int index = i + 2;
            // ??????????????????
            validateMain(index, oldIceBoxImportVo);
            // Optional.ofNullable(OldIceBoxOptType.item(type)).ifPresent(event -> event.operating(index, oldIceBoxImportVo, iceBoxDao, iceBoxExtendDao, feignDeptClient, feignSupplierClient, iceModelDao));

            OldIceBoxOptType item = OldIceBoxOptType.item(type);
            if (item == null) {
                continue;
            }
            JSONObject jsonObject = item.operating(index, oldIceBoxImportVo, iceBoxDao, iceBoxExtendDao, feignDeptClient, feignSupplierClient, iceModelDao, iceBoxService, rabbitTemplate, feignStoreClient, map, applyRelatePutStoreModelDao, icePutApplyRelateBoxDao, putStoreRelateModelDao);
            lists.add(jsonObject);
        }
        if (CollectionUtil.isNotEmpty(map)) {
            map.forEach((key, value) -> {
                if (value.size() == 1) {
                    IceBox iceBox = value.get(0);
                    String newApplyNumber = iceBoxService.createIcePutData(iceBox, iceBox.getPutStoreNumber());
                    iceBoxService.saveIceBoxPutReport(iceBox, newApplyNumber, iceBox.getPutStoreNumber());
                    iceBoxService.createOldIceBoxSignNotice(iceBox, newApplyNumber, iceBox.getPutStoreNumber());
                }else {
                    IceBox newIceBox = value.get(0);
                    IceBox oldIceBox = value.get(1);
                    iceBoxService.changeCustomer(newIceBox, oldIceBox);
                }
            });
        }
        return lists;
    }


    @Getter
    private enum OldIceBoxOptType {
        CREATE("??????", "???????????????") {
            @Override
            public JSONObject operating(Integer index, OldIceBoxImportVo oldIceBoxImportVo, IceBoxDao iceBoxDao, IceBoxExtendDao iceBoxExtendDao,
                                        FeignDeptClient feignDeptClient, FeignSupplierClient feignSupplierClient, IceModelDao iceModelDao,
                                        IceBoxService iceBoxService, RabbitTemplate rabbitTemplate, FeignStoreClient feignStoreClient, Map<String, List<IceBox>> map, ApplyRelatePutStoreModelDao applyRelatePutStoreModelDao, IcePutApplyRelateBoxDao icePutApplyRelateBoxDao, PutStoreRelateModelDao putStoreRelateModelDao) {
                // ???????????????????????????????????????????????????
                IceBox iceBox = new IceBox();
                IceBoxExtend iceBoxExtend = new IceBoxExtend();
                // ????????????
                String assetId = oldIceBoxImportVo.getAssetId();
                iceBox.setAssetId(assetId);
                iceBoxExtend.setAssetId(assetId);
                // ????????????
                String chestName = oldIceBoxImportVo.getChestName();
                iceBox.setChestName(chestName);
                // ??????
                String brandName = oldIceBoxImportVo.getBrandName();
                iceBox.setBrandName(brandName);
                // ??????
                String modelName = oldIceBoxImportVo.getModelName();
                iceBox.setModelName(modelName);
                // ??????
                String chestNorm = oldIceBoxImportVo.getChestNorm();
                iceBox.setChestNorm(chestNorm);
                String service = oldIceBoxImportVo.getService();
                Integer integer = FeignResponseUtil.getFeignData(feignDeptClient.findMaxIdByName(service));
                if (null == integer) {
                    throw new NormalOptionException(Constants.API_CODE_FAIL, "???" + index + "????????? ??????????????????????????????????????????????????????");
                }
                iceBox.setDeptId(integer);
                // ???????????????
                String supplierNumber = oldIceBoxImportVo.getSupplierNumber();

                if (StringUtils.isNotBlank(supplierNumber)) {
                    SubordinateInfoVo subordinateInfoVo = FeignResponseUtil.getFeignData(feignSupplierClient.findByNumber(supplierNumber));
                    if (null == subordinateInfoVo.getSupplierId()) {
                        throw new NormalOptionException(Constants.API_CODE_FAIL, "???" + index + "????????? ??????????????????????????????????????????????????????");
                    }
                    iceBox.setSupplierId(subordinateInfoVo.getSupplierId());
                }

                IceModel iceModel = iceModelDao.selectOne(Wrappers.<IceModel>lambdaQuery().eq(IceModel::getChestModel, modelName));
                if (null == iceModel) {
                    throw new NormalOptionException(Constants.API_CODE_FAIL, "???" + index + "????????? ??????????????????????????????????????????????????????");
                } else {
                    Integer type = iceModel.getType();
                    if (IceBoxEnums.TypeEnum.NEW_ICE_BOX.getType().equals(type)) {
                        throw new NormalOptionException(Constants.API_CODE_FAIL, "???" + index + "????????? ????????????????????????????????????????????????");
                    }
                }
                iceBox.setModelId(iceModel.getId());
                BigDecimal depositMoney = oldIceBoxImportVo.getDepositMoney();

                if (null == depositMoney) {
                    iceBox.setDepositMoney(BigDecimal.ZERO);
                } else {
                    iceBox.setDepositMoney(depositMoney);
                }

                String storeNumber = oldIceBoxImportVo.getStoreNumber();

                if (StringUtils.isNotBlank(storeNumber)) {
                    iceBox.setPutStoreNumber(storeNumber);
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
                } else {
                    iceBox.setPutStoreNumber("0");
                    iceBox.setPutStatus(PutStatus.NO_PUT.getStatus());
                }

                IceBox selectIceBox = iceBoxDao.selectOne(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getAssetId, assetId));

                if (null != selectIceBox) {
                    iceBox.setId(selectIceBox.getId());
                    if (StringUtils.isNotBlank(storeNumber)) {
                        String oldPutStoreNumber = selectIceBox.getPutStoreNumber();
                        if ((null == oldPutStoreNumber || "0".equals(oldPutStoreNumber)) || (!oldPutStoreNumber.equals(storeNumber))) {
                            // ????????? ????????????
                            List<IceBox> list = new ArrayList<>();
                            list.add(iceBox);
                            list.add(selectIceBox);
                            map.put(assetId, list);
                        }
                    }
                    iceBoxDao.updateById(iceBox);
                } else {
                    iceBox.setIceBoxType(IceBoxEnums.TypeEnum.OLD_ICE_BOX.getType());
                    iceBoxDao.insert(iceBox);
                    iceBoxExtend.setId(iceBox.getId());
                    iceBoxExtendDao.insert(iceBoxExtend);
                    if (StringUtils.isNotBlank(storeNumber)) {
                        // ??????????????????????????????????????????????????????
                        List<IceBox> list = new ArrayList<>();
                        list.add(iceBox);
                        map.put(assetId, list);
                    }
                }
                // ?????? ????????????/????????????
                JSONObject jsonObject = iceBoxService.setAssetReportJson(iceBox,"???????????????");
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                      @Override
                      public void afterCommit() {
                          if (iceBox.getPutStatus().equals(PutStatus.FINISH_PUT.getStatus())) {
                              //??????????????????????????????
                              IceInspectionReportMsg reportMsg = new IceInspectionReportMsg();
                              reportMsg.setOperateType(1);
                              reportMsg.setBoxId(iceBox.getId());
                              rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.iceInspectionReportKey, reportMsg);
                          }
                      }
                });
                return jsonObject;
            }
        },
        GO_BACK("??????", "????????????????????????") {
            @Override
            public JSONObject operating(Integer index, OldIceBoxImportVo oldIceBoxImportVo, IceBoxDao iceBoxDao, IceBoxExtendDao iceBoxExtendDao,
                                        FeignDeptClient feignDeptClient, FeignSupplierClient feignSupplierClient, IceModelDao iceModelDao,
                                        IceBoxService iceBoxService, RabbitTemplate rabbitTemplate, FeignStoreClient feignStoreClient, Map<String, List<IceBox>> map, ApplyRelatePutStoreModelDao applyRelatePutStoreModelDao, IcePutApplyRelateBoxDao icePutApplyRelateBoxDao, PutStoreRelateModelDao putStoreRelateModelDao) {

                // ???????????????????????????
                // ????????????
                String assetId = oldIceBoxImportVo.getAssetId();

                String supplierNumber = oldIceBoxImportVo.getSupplierNumber();

                if (StringUtils.isBlank(supplierNumber)) {
                    throw new NormalOptionException(Constants.API_CODE_FAIL, "???" + index + "????????? ????????????????????????????????????????????????");
                }

                SubordinateInfoVo subordinateInfoVo = FeignResponseUtil.getFeignData(feignSupplierClient.findByNumber(supplierNumber));
                if (null == subordinateInfoVo.getSupplierId()) {
                    throw new NormalOptionException(Constants.API_CODE_FAIL, "???" + index + "????????? ??????????????????????????????????????????????????????");
                }

                String service = oldIceBoxImportVo.getService();

                Integer serviceDeptId = FeignResponseUtil.getFeignData(feignDeptClient.findMaxIdByName(service));
                if (null == serviceDeptId) {
                    throw new NormalOptionException(Constants.API_CODE_FAIL, "???" + index + "????????? ??????????????????????????????????????????????????????");
                }
                IceBox iceBox = iceBoxDao.selectOne(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getAssetId, assetId));
                if (null != iceBox) {
                    String storeNumber = iceBox.getPutStoreNumber();
                    Integer oldStatus = iceBox.getPutStatus();
                    // ????????????????????????????????????
                    iceBox.setDeptId(serviceDeptId);
                    iceBox.setSupplierId(subordinateInfoVo.getSupplierId());
                    iceBox.setPutStoreNumber("0");
                    iceBox.setPutStatus(PutStatus.NO_PUT.getStatus());
                    iceBoxDao.updateById(iceBox);

                    IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(iceBox.getId());
                    String lastApplyNumber = iceBoxExtend.getLastApplyNumber();
                    if (null != lastApplyNumber) { // ????????????????????????
                        // ????????????????????????
                        IcePutApplyRelateBox icePutApplyRelateBox = icePutApplyRelateBoxDao.selectOne(Wrappers.<IcePutApplyRelateBox>lambdaQuery().eq(IcePutApplyRelateBox::getApplyNumber, lastApplyNumber));
                        if (null != icePutApplyRelateBox) {
                            List<ApplyRelatePutStoreModel> applyRelatePutStoreModelList = applyRelatePutStoreModelDao.selectList(Wrappers.<ApplyRelatePutStoreModel>lambdaQuery()
                                    .eq(ApplyRelatePutStoreModel::getApplyNumber, iceBoxExtend.getLastApplyNumber())
                                    .eq(ApplyRelatePutStoreModel::getFreeType, icePutApplyRelateBox.getFreeType()));
                            if (CollectionUtil.isNotEmpty(applyRelatePutStoreModelList)) {
                                Integer modelId = iceBox.getModelId();
                                for (ApplyRelatePutStoreModel applyRelatePutStoreModel : applyRelatePutStoreModelList) {
                                    Integer storeRelateModelId = applyRelatePutStoreModel.getStoreRelateModelId();
                                    PutStoreRelateModel putStoreRelateModel = putStoreRelateModelDao.selectOne(Wrappers.<PutStoreRelateModel>lambdaQuery()
                                            .eq(PutStoreRelateModel::getId, storeRelateModelId)
                                            .eq(PutStoreRelateModel::getModelId, modelId)
                                            .eq(PutStoreRelateModel::getPutStatus, com.szeastroc.icebox.newprocess.enums.PutStatus.FINISH_PUT.getStatus()));
                                    if (null != putStoreRelateModel) {
                                        putStoreRelateModelDao.update(putStoreRelateModel, Wrappers.<PutStoreRelateModel>lambdaUpdate()
                                                .set(PutStoreRelateModel::getPutStatus, com.szeastroc.icebox.newprocess.enums.PutStatus.NO_PUT.getStatus())
                                                .set(PutStoreRelateModel::getUpdateTime, new Date())
                                                .eq(PutStoreRelateModel::getId, storeRelateModelId));
                                        break;
                                    }
                                }
                            }
                            iceBoxExtendDao.update(null, Wrappers.<IceBoxExtend>lambdaUpdate()
                                    .eq(IceBoxExtend::getId, iceBox.getId())
                                    .set(IceBoxExtend::getLastPutId, 0)
                                    .set(IceBoxExtend::getLastApplyNumber, null));
                        }
                    }
                    Integer boxId = iceBox.getId();
                    if(PutStatus.FINISH_PUT.getStatus().equals(oldStatus)){
                        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                            @Override
                            public void afterCommit() {
                                Integer userId = FeignResponseUtil.getFeignData(feignStoreClient.getMainSaleManId(storeNumber));
                                if (Objects.isNull(userId)) {
                                    userId = FeignResponseUtil.getFeignData(feignSupplierClient.getMainSaleManId(storeNumber));
                                }
                                if(Objects.nonNull(userId)){
                                    //??????
                                    IceInspectionReportMsg reportMsg = new IceInspectionReportMsg();
                                    reportMsg.setOperateType(6);
                                    reportMsg.setUserId(userId);
                                    rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.iceInspectionReportKey,reportMsg);
                                }

                            }
                        });
                    }
                } else {
                    // ????????????????????????
                    // ???????????????????????????????????????????????????
                    iceBox = new IceBox();
                    IceBoxExtend iceBoxExtend = new IceBoxExtend();
                    iceBox.setDeptId(serviceDeptId);
                    iceBox.setSupplierId(subordinateInfoVo.getSupplierId());
                    iceBox.setAssetId(assetId);
                    iceBoxExtend.setAssetId(assetId);
                    // ????????????
                    String chestName = oldIceBoxImportVo.getChestName();

                    iceBox.setChestName(chestName);
                    // ??????
                    String brandName = oldIceBoxImportVo.getBrandName();
                    iceBox.setBrandName(brandName);
                    // ??????
                    String modelName = oldIceBoxImportVo.getModelName();
                    iceBox.setModelName(modelName);
                    // ??????
                    String chestNorm = oldIceBoxImportVo.getChestNorm();
                    iceBox.setChestNorm(chestNorm);
                    IceModel iceModel = iceModelDao.selectOne(Wrappers.<IceModel>lambdaQuery().eq(IceModel::getChestModel, modelName));
                    if (null == iceModel) {
                        throw new NormalOptionException(Constants.API_CODE_FAIL, "???" + index + "????????? ??????????????????????????????????????????????????????");
                    } else {
                        Integer type = iceModel.getType();
                        if (IceBoxEnums.TypeEnum.NEW_ICE_BOX.getType().equals(type)) {
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "???" + index + "????????? ????????????????????????????????????????????????");
                        }
                    }
                    iceBox.setModelId(iceModel.getId());
                    BigDecimal depositMoney = oldIceBoxImportVo.getDepositMoney();
                    if (null == depositMoney) {
                        iceBox.setDepositMoney(BigDecimal.ZERO);
                    } else {
                        iceBox.setDepositMoney(depositMoney);
                    }
//                    iceBox.setPutStoreNumber("0");
//                    iceBox.setPutStatus(PutStatus.NO_PUT.getStatus());
                    iceBox.setIceBoxType(IceBoxEnums.TypeEnum.OLD_ICE_BOX.getType());
                    iceBoxDao.insert(iceBox);
                    iceBoxExtend.setId(iceBox.getId());
                    iceBoxExtendDao.insert(iceBoxExtend);
                }
                // ?????? ????????????/????????????
                JSONObject jsonObject = iceBoxService.setAssetReportJson(iceBox, "????????????????????????");
                return jsonObject;
            }
        },
        SCRAP("??????", "???????????????") {
            @Override
            public JSONObject operating(Integer index, OldIceBoxImportVo oldIceBoxImportVo, IceBoxDao iceBoxDao, IceBoxExtendDao iceBoxExtendDao,
                                        FeignDeptClient feignDeptClient, FeignSupplierClient feignSupplierClient, IceModelDao iceModelDao,
                                        IceBoxService iceBoxService, RabbitTemplate rabbitTemplate, FeignStoreClient feignStoreClient,
                                        Map<String, List<IceBox>> map, ApplyRelatePutStoreModelDao applyRelatePutStoreModelDao, IcePutApplyRelateBoxDao icePutApplyRelateBoxDao, PutStoreRelateModelDao putStoreRelateModelDao) {

                // ??????????????????????????????????????????????????? ????????????????????????

                String assetId = oldIceBoxImportVo.getAssetId();


                String supplierNumber = oldIceBoxImportVo.getSupplierNumber();
                SubordinateInfoVo subordinateInfoVo = null;
                if (StringUtils.isNotBlank(supplierNumber)) {
                    SubordinateInfoVo feignData = FeignResponseUtil.getFeignData(feignSupplierClient.findByNumber(supplierNumber));
                    if (null == feignData.getSupplierId()) {
                        throw new NormalOptionException(Constants.API_CODE_FAIL, "???" + index + "????????? ??????????????????????????????????????????????????????");
                    } else {
                        subordinateInfoVo = feignData;
                    }
                }
                String service = oldIceBoxImportVo.getService();


                Integer serviceDeptId = FeignResponseUtil.getFeignData(feignDeptClient.findMaxIdByName(service));
                if (null == serviceDeptId) {
                    throw new NormalOptionException(Constants.API_CODE_FAIL, "???" + index + "????????? ??????????????????????????????????????????????????????");
                }

                IceBox iceBox = iceBoxDao.selectOne(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getAssetId, assetId));
                if (null != iceBox) {
                    // ????????????????????????????????????
                    iceBox.setDeptId(serviceDeptId);
                    iceBox.setSupplierId(null == subordinateInfoVo ? null : subordinateInfoVo.getSupplierId());
//                    iceBox.setPutStoreNumber("0");
//                    iceBox.setPutStatus(PutStatus.NO_PUT.getStatus());
                    iceBox.setStatus(IceBoxEnums.StatusEnum.SCRAP.getType());
                    iceBoxDao.updateById(iceBox);
                    Integer boxId = iceBox.getId();
                    if(PutStatus.FINISH_PUT.getStatus().equals(iceBox.getPutStatus())){
                        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                            @Override
                            public void afterCommit() {
                                //??????
                                IceInspectionReportMsg reportMsg = new IceInspectionReportMsg();
                                reportMsg.setOperateType(5);
                                reportMsg.setBoxId(boxId);
                                rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.iceInspectionReportKey,reportMsg);
                            }
                        });
                    }

                } else {
                    // ????????????????????????
                    // ???????????????????????????????????????????????????
                    iceBox = new IceBox();
                    IceBoxExtend iceBoxExtend = new IceBoxExtend();
                    iceBox.setDeptId(serviceDeptId);
                    iceBox.setSupplierId(null == subordinateInfoVo ? null : subordinateInfoVo.getSupplierId());
                    iceBox.setAssetId(assetId);
                    iceBoxExtend.setAssetId(assetId);
                    // ????????????
                    String chestName = oldIceBoxImportVo.getChestName();

                    iceBox.setChestName(chestName);
                    // ??????
                    String brandName = oldIceBoxImportVo.getBrandName();

                    iceBox.setBrandName(brandName);
                    // ??????
                    String modelName = oldIceBoxImportVo.getModelName();

                    iceBox.setModelName(modelName);
                    // ??????
                    String chestNorm = oldIceBoxImportVo.getChestNorm();

                    iceBox.setChestNorm(chestNorm);
                    IceModel iceModel = iceModelDao.selectOne(Wrappers.<IceModel>lambdaQuery().eq(IceModel::getChestModel, modelName));
                    if (null == iceModel) {
                        throw new NormalOptionException(Constants.API_CODE_FAIL, "???" + index + "????????? ??????????????????????????????????????????????????????");
                    } else {
                        Integer type = iceModel.getType();
                        if (IceBoxEnums.TypeEnum.NEW_ICE_BOX.getType().equals(type)) {
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "???" + index + "????????? ????????????????????????????????????????????????");
                        }
                    }
                    iceBox.setModelId(iceModel.getId());
                    BigDecimal depositMoney = oldIceBoxImportVo.getDepositMoney();
                    if (null == depositMoney) {
                        iceBox.setDepositMoney(BigDecimal.ZERO);
                    } else {
                        iceBox.setDepositMoney(depositMoney);
                    }
//                    iceBox.setPutStoreNumber("0");
//                    iceBox.setPutStatus(PutStatus.NO_PUT.getStatus());
                    iceBox.setStatus(IceBoxEnums.StatusEnum.SCRAP.getType());
                    iceBox.setIceBoxType(IceBoxEnums.TypeEnum.OLD_ICE_BOX.getType());
                    iceBoxDao.insert(iceBox);
                    iceBoxExtend.setId(iceBox.getId());
                    iceBoxExtendDao.insert(iceBoxExtend);
                }
                // ?????? ????????????/????????????
                JSONObject jsonObject = iceBoxService.setAssetReportJson(iceBox, "???????????????");
                return jsonObject;
            }
        },
        LOST("??????", "???????????????") {
            @Override
            public JSONObject operating(Integer index, OldIceBoxImportVo oldIceBoxImportVo, IceBoxDao iceBoxDao, IceBoxExtendDao iceBoxExtendDao,
                                        FeignDeptClient feignDeptClient, FeignSupplierClient feignSupplierClient, IceModelDao iceModelDao,
                                        IceBoxService iceBoxService, RabbitTemplate rabbitTemplate, FeignStoreClient feignStoreClient, Map<String, List<IceBox>> map, ApplyRelatePutStoreModelDao applyRelatePutStoreModelDao, IcePutApplyRelateBoxDao icePutApplyRelateBoxDao, PutStoreRelateModelDao putStoreRelateModelDao) {

                // ??????????????????????????????????????????????????? ????????????????????????
                String assetId = oldIceBoxImportVo.getAssetId();
                String supplierNumber = oldIceBoxImportVo.getSupplierNumber();
                SubordinateInfoVo subordinateInfoVo = null;
                if (StringUtils.isNotBlank(supplierNumber)) {
                    SubordinateInfoVo feignData = FeignResponseUtil.getFeignData(feignSupplierClient.findByNumber(supplierNumber));
                    if (null == feignData.getSupplierId()) {
                        throw new NormalOptionException(Constants.API_CODE_FAIL, "???" + index + "????????? ??????????????????????????????????????????????????????");
                    } else {
                        subordinateInfoVo = feignData;
                    }
                }

                String service = oldIceBoxImportVo.getService();

                Integer serviceDeptId = FeignResponseUtil.getFeignData(feignDeptClient.findMaxIdByName(service));
                if (null == serviceDeptId) {
                    throw new NormalOptionException(Constants.API_CODE_FAIL, "???" + index + "????????? ??????????????????????????????????????????????????????");
                }

                IceBox iceBox = iceBoxDao.selectOne(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getAssetId, assetId));
                if (null != iceBox) {
                    // ????????????????????????????????????
                    iceBox.setDeptId(serviceDeptId);
                    iceBox.setSupplierId(null == subordinateInfoVo ? null : subordinateInfoVo.getSupplierId());

                    iceBox.setStatus(IceBoxEnums.StatusEnum.LOSE.getType());
                    iceBoxDao.updateById(iceBox);
                    Integer boxId = iceBox.getId();
                    if(PutStatus.FINISH_PUT.getStatus().equals(iceBox.getPutStatus())){
                        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                            @Override
                            public void afterCommit() {
                                //??????
                                IceInspectionReportMsg reportMsg = new IceInspectionReportMsg();
                                reportMsg.setOperateType(5);
                                reportMsg.setBoxId(boxId);
                                rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.iceInspectionReportKey,reportMsg);
                            }
                        });
                    }
                } else {
                    // ????????????????????????
                    // ???????????????????????????????????????????????????
                    iceBox = new IceBox();
                    IceBoxExtend iceBoxExtend = new IceBoxExtend();
                    iceBox.setDeptId(serviceDeptId);
                    iceBox.setSupplierId(null == subordinateInfoVo ? null : subordinateInfoVo.getSupplierId());
                    iceBox.setAssetId(assetId);
                    iceBoxExtend.setAssetId(assetId);
                    // ????????????
                    String chestName = oldIceBoxImportVo.getChestName();

                    iceBox.setChestName(chestName);
                    // ??????
                    String brandName = oldIceBoxImportVo.getBrandName();

                    iceBox.setBrandName(brandName);
                    // ??????
                    String modelName = oldIceBoxImportVo.getModelName();

                    iceBox.setModelName(modelName);
                    // ??????
                    String chestNorm = oldIceBoxImportVo.getChestNorm();

                    iceBox.setChestNorm(chestNorm);
                    IceModel iceModel = iceModelDao.selectOne(Wrappers.<IceModel>lambdaQuery().eq(IceModel::getChestModel, modelName));
                    if (null == iceModel) {
                        throw new NormalOptionException(Constants.API_CODE_FAIL, "???" + index + "????????? ??????????????????????????????????????????????????????");
                    } else {
                        Integer type = iceModel.getType();
                        if (IceBoxEnums.TypeEnum.NEW_ICE_BOX.getType().equals(type)) {
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "???" + index + "????????? ????????????????????????????????????????????????");
                        }
                    }
                    iceBox.setModelId(iceModel.getId());
                    BigDecimal depositMoney = oldIceBoxImportVo.getDepositMoney();
                    if (null == depositMoney) {
                        iceBox.setDepositMoney(BigDecimal.ZERO);
                    } else {
                        iceBox.setDepositMoney(depositMoney);
                    }

                    iceBox.setStatus(IceBoxEnums.StatusEnum.LOSE.getType());
                    iceBox.setIceBoxType(IceBoxEnums.TypeEnum.OLD_ICE_BOX.getType());
                    iceBoxDao.insert(iceBox);
                    iceBoxExtend.setId(iceBox.getId());
                    iceBoxExtendDao.insert(iceBoxExtend);
                }

                // ?????? ????????????/????????????
                JSONObject jsonObject = iceBoxService.setAssetReportJson(iceBox, "???????????????");
                return jsonObject;
            }
        };


        private final String type;
        private final String desc;

        OldIceBoxOptType(String type, String desc) {
            this.type = type;
            this.desc = desc;
        }

        public static OldIceBoxOptType item(@NotNull String type) {
            for (OldIceBoxOptType e : OldIceBoxOptType.values()) {
                if (e.type.equalsIgnoreCase(type)) return e;
            }
            return null;
        }

        abstract public JSONObject operating(Integer index, OldIceBoxImportVo oldIceBoxImportVo, IceBoxDao iceBoxDao,
                                             IceBoxExtendDao iceBoxExtendDao, FeignDeptClient feignDeptClient,
                                             FeignSupplierClient feignSupplierClient, IceModelDao iceModelDao,
                                             IceBoxService iceBoxService, RabbitTemplate rabbitTemplate, FeignStoreClient feignStoreClient, Map<String, List<IceBox>> map, ApplyRelatePutStoreModelDao applyRelatePutStoreModelDao, IcePutApplyRelateBoxDao icePutApplyRelateBoxDao, PutStoreRelateModelDao putStoreRelateModelDao);

    }

    public void validateMain(Integer index, OldIceBoxImportVo oldIceBoxImportVo) {
        // ????????????
        String assetId = oldIceBoxImportVo.getAssetId();
        if (StringUtils.isBlank(assetId)) {
            throw new NormalOptionException(Constants.API_CODE_FAIL, "???" + index + "????????? ??????????????????,?????????????????????");
        }
        // ????????????
        String chestName = oldIceBoxImportVo.getChestName();
        if (StringUtils.isBlank(chestName)) {
            throw new NormalOptionException(Constants.API_CODE_FAIL, "???" + index + "????????? ??????????????????,?????????????????????");
        }
        // ??????
        String brandName = oldIceBoxImportVo.getBrandName();
        if (StringUtils.isBlank(brandName)) {
            throw new NormalOptionException(Constants.API_CODE_FAIL, "???" + index + "????????? ????????????,???????????????");
        }
        // ??????
        String modelName = oldIceBoxImportVo.getModelName();
        if (StringUtils.isBlank(modelName)) {
            throw new NormalOptionException(Constants.API_CODE_FAIL, "???" + index + "????????? ????????????,???????????????");
        }
        // ??????
        String chestNorm = oldIceBoxImportVo.getChestNorm();
        if (StringUtils.isBlank(chestNorm)) {
            throw new NormalOptionException(Constants.API_CODE_FAIL, "???" + index + "????????? ????????????,???????????????");
        }
        // ?????????
        String service = oldIceBoxImportVo.getService();
        if (StringUtils.isBlank(service)) {
            throw new NormalOptionException(Constants.API_CODE_FAIL, "???" + index + "????????? ???????????????,????????????????????????");
        }

    }
}
