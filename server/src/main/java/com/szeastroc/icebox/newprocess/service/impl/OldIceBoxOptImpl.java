package com.szeastroc.icebox.newprocess.service.impl;

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
import com.szeastroc.icebox.newprocess.dao.IceBoxDao;
import com.szeastroc.icebox.newprocess.dao.IceBoxExtendDao;
import com.szeastroc.icebox.newprocess.dao.IceModelDao;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.entity.IceBoxExtend;
import com.szeastroc.icebox.newprocess.entity.IceModel;
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
import java.util.List;
import java.util.Objects;

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


    @Override
    @Transactional
    public List<JSONObject> opt(List<OldIceBoxImportVo> oldIceBoxImportVoList) {

        List<JSONObject> lists = Lists.newArrayList();
        for (int i = 0; i < oldIceBoxImportVoList.size(); i++) {
            OldIceBoxImportVo oldIceBoxImportVo = oldIceBoxImportVoList.get(i);
            String type = oldIceBoxImportVo.getType();
            // excel 具体行数
            int index = i + 2;
            // 校验主要数据
            validateMain(index, oldIceBoxImportVo);
            // Optional.ofNullable(OldIceBoxOptType.item(type)).ifPresent(event -> event.operating(index, oldIceBoxImportVo, iceBoxDao, iceBoxExtendDao, feignDeptClient, feignSupplierClient, iceModelDao));

            OldIceBoxOptType item = OldIceBoxOptType.item(type);
            if (item == null) {
                continue;
            }
            JSONObject jsonObject = item.operating(index, oldIceBoxImportVo, iceBoxDao, iceBoxExtendDao, feignDeptClient, feignSupplierClient, iceModelDao, iceBoxService,rabbitTemplate,feignStoreClient);
            lists.add(jsonObject);
        }
        return lists;
    }


    @Getter
    private enum OldIceBoxOptType {
        CREATE("新增", "旧冰柜入库") {
            @Override
            public JSONObject operating(Integer index, OldIceBoxImportVo oldIceBoxImportVo, IceBoxDao iceBoxDao, IceBoxExtendDao iceBoxExtendDao,
                                        FeignDeptClient feignDeptClient, FeignSupplierClient feignSupplierClient, IceModelDao iceModelDao,
                                        IceBoxService iceBoxService,RabbitTemplate rabbitTemplate,FeignStoreClient feignStoreClient) {
                // 导入冰柜参数限制较多，需要多重校验
                IceBox iceBox = new IceBox();
                IceBoxExtend iceBoxExtend = new IceBoxExtend();
                // 资产编号
                String assetId = oldIceBoxImportVo.getAssetId();
                iceBox.setAssetId(assetId);
                iceBoxExtend.setAssetId(assetId);
                // 冰柜名称
                String chestName = oldIceBoxImportVo.getChestName();
                iceBox.setChestName(chestName);
                // 品牌
                String brandName = oldIceBoxImportVo.getBrandName();
                iceBox.setBrandName(brandName);
                // 型号
                String modelName = oldIceBoxImportVo.getModelName();
                iceBox.setModelName(modelName);
                // 规格
                String chestNorm = oldIceBoxImportVo.getChestNorm();
                iceBox.setChestNorm(chestNorm);
                String service = oldIceBoxImportVo.getService();
                Integer integer = FeignResponseUtil.getFeignData(feignDeptClient.findMaxIdByName(service));
                if (null == integer) {
                    throw new NormalOptionException(Constants.API_CODE_FAIL, "第" + index + "行数据 服务处信息查询有误，请核对服务处名称");
                }
                iceBox.setDeptId(integer);

                String suppName = null;// 经销商名称
                // 经销商编号
                String supplierNumber = oldIceBoxImportVo.getSupplierNumber();

                if (StringUtils.isNotBlank(supplierNumber)) {
                    SubordinateInfoVo subordinateInfoVo = FeignResponseUtil.getFeignData(feignSupplierClient.findByNumber(supplierNumber));
                    if (null == subordinateInfoVo.getSupplierId()) {
                        throw new NormalOptionException(Constants.API_CODE_FAIL, "第" + index + "行数据 经销商信息查询有误，请核对经销商编号");
                    }
                    iceBox.setSupplierId(subordinateInfoVo.getSupplierId());
                    suppName = subordinateInfoVo.getName();
                }

                IceModel iceModel = iceModelDao.selectOne(Wrappers.<IceModel>lambdaQuery().eq(IceModel::getChestModel, modelName));
                if (null == iceModel) {
                    throw new NormalOptionException(Constants.API_CODE_FAIL, "第" + index + "行数据 型号未导入数据库，请联系相关人员补充");
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
                    //修改冰柜部门为投放客户的部门
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
                    iceBoxDao.updateById(iceBox);
                } else {
                    iceBox.setIceBoxType(IceBoxEnums.TypeEnum.OLD_ICE_BOX.getType());
                    iceBoxDao.insert(iceBox);
                    iceBoxExtend.setId(iceBox.getId());
                    iceBoxExtendDao.insert(iceBoxExtend);
                }
                // 新的 冰柜状态/投放状态
                JSONObject jsonObject = iceBoxService.setAssetReportJson(iceBox,"旧冰柜入库");
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                      @Override
                      public void afterCommit() {
                          if (iceBox.getPutStatus().equals(PutStatus.FINISH_PUT.getStatus())) {
                              //巡检报表添加投放数据
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
        GO_BACK("退仓", "旧冰柜退回经销商") {
            @Override
            public JSONObject operating(Integer index, OldIceBoxImportVo oldIceBoxImportVo, IceBoxDao iceBoxDao, IceBoxExtendDao iceBoxExtendDao,
                                        FeignDeptClient feignDeptClient, FeignSupplierClient feignSupplierClient, IceModelDao iceModelDao,
                                        IceBoxService iceBoxService,RabbitTemplate rabbitTemplate,FeignStoreClient feignStoreClient) {

                // 退仓需要指定经销商
                // 资产编号
                String assetId = oldIceBoxImportVo.getAssetId();

                String supplierNumber = oldIceBoxImportVo.getSupplierNumber();

                if (StringUtils.isBlank(supplierNumber)) {
                    throw new NormalOptionException(Constants.API_CODE_FAIL, "第" + index + "行数据 经销商编号为空，请补充经销商编号");
                }

                SubordinateInfoVo subordinateInfoVo = FeignResponseUtil.getFeignData(feignSupplierClient.findByNumber(supplierNumber));
                if (null == subordinateInfoVo.getSupplierId()) {
                    throw new NormalOptionException(Constants.API_CODE_FAIL, "第" + index + "行数据 经销商信息查询有误，请核对经销商编号");
                }

                String suppName = subordinateInfoVo.getName(); // 经销商名称
                String service = oldIceBoxImportVo.getService();

                Integer serviceDeptId = FeignResponseUtil.getFeignData(feignDeptClient.findMaxIdByName(service));
                if (null == serviceDeptId) {
                    throw new NormalOptionException(Constants.API_CODE_FAIL, "第" + index + "行数据 服务处信息查询有误，请核对服务处名称");
                }
                IceBox iceBox = iceBoxDao.selectOne(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getAssetId, assetId));
                String storeNumber = iceBox.getPutStoreNumber();
                if (null != iceBox) {
                    Integer oldStatus = iceBox.getPutStatus();
                    // 更新冰柜状态及经销商信息
                    iceBox.setDeptId(serviceDeptId);
                    iceBox.setSupplierId(subordinateInfoVo.getSupplierId());
                    iceBox.setPutStoreNumber("0");
                    iceBox.setPutStatus(PutStatus.NO_PUT.getStatus());
                    iceBoxDao.updateById(iceBox);
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
                                    //退仓
                                    IceInspectionReportMsg reportMsg = new IceInspectionReportMsg();
                                    reportMsg.setOperateType(6);
                                    reportMsg.setUserId(userId);
                                    rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.iceInspectionReportKey,reportMsg);
                                }

                            }
                        });
                    }
                } else {
                    // 新增冰柜至数据库
                    // 导入冰柜参数限制较多，需要多重校验
                    iceBox = new IceBox();
                    IceBoxExtend iceBoxExtend = new IceBoxExtend();
                    iceBox.setDeptId(serviceDeptId);
                    iceBox.setSupplierId(subordinateInfoVo.getSupplierId());
                    iceBox.setAssetId(assetId);
                    iceBoxExtend.setAssetId(assetId);
                    // 冰柜名称
                    String chestName = oldIceBoxImportVo.getChestName();

                    iceBox.setChestName(chestName);
                    // 品牌
                    String brandName = oldIceBoxImportVo.getBrandName();
                    iceBox.setBrandName(brandName);
                    // 型号
                    String modelName = oldIceBoxImportVo.getModelName();
                    iceBox.setModelName(modelName);
                    // 规格
                    String chestNorm = oldIceBoxImportVo.getChestNorm();
                    iceBox.setChestNorm(chestNorm);
                    IceModel iceModel = iceModelDao.selectOne(Wrappers.<IceModel>lambdaQuery().eq(IceModel::getChestModel, modelName));
                    if (null == iceModel) {
                        throw new NormalOptionException(Constants.API_CODE_FAIL, "第" + index + "行数据 型号未导入数据库，请联系相关人员补充");
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
                // 新的 冰柜状态/投放状态
                JSONObject jsonObject = iceBoxService.setAssetReportJson(iceBox,"旧冰柜退回经销商");
                return jsonObject;
            }
        },
        SCRAP("报废", "旧冰柜报废") {
            @Override
            public JSONObject operating(Integer index, OldIceBoxImportVo oldIceBoxImportVo, IceBoxDao iceBoxDao, IceBoxExtendDao iceBoxExtendDao,
                                        FeignDeptClient feignDeptClient, FeignSupplierClient feignSupplierClient, IceModelDao iceModelDao,
                                        IceBoxService iceBoxService,RabbitTemplate rabbitTemplate,FeignStoreClient feignStoreClient
            ) {

                // 冰柜报废，目前把冰柜退回经销商然后 冰柜状态置为异常

                String assetId = oldIceBoxImportVo.getAssetId();


                String supplierNumber = oldIceBoxImportVo.getSupplierNumber();
                SubordinateInfoVo subordinateInfoVo = null;
                if (StringUtils.isNotBlank(supplierNumber)) {
                    SubordinateInfoVo feignData = FeignResponseUtil.getFeignData(feignSupplierClient.findByNumber(supplierNumber));
                    if (null == feignData.getSupplierId()) {
                        throw new NormalOptionException(Constants.API_CODE_FAIL, "第" + index + "行数据 经销商信息查询有误，请核对经销商编号");
                    } else {
                        subordinateInfoVo = feignData;
                    }
                }
                String suppName = subordinateInfoVo.getName(); // 经销商名称
                String service = oldIceBoxImportVo.getService();


                Integer serviceDeptId = FeignResponseUtil.getFeignData(feignDeptClient.findMaxIdByName(service));
                if (null == serviceDeptId) {
                    throw new NormalOptionException(Constants.API_CODE_FAIL, "第" + index + "行数据 服务处信息查询有误，请核对服务处名称");
                }

                IceBox iceBox = iceBoxDao.selectOne(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getAssetId, assetId));
                if (null != iceBox) {
                    // 更新冰柜状态及经销商信息
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
                                //报废
                                IceInspectionReportMsg reportMsg = new IceInspectionReportMsg();
                                reportMsg.setOperateType(5);
                                reportMsg.setBoxId(boxId);
                                rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.iceInspectionReportKey,reportMsg);
                            }
                        });
                    }

                } else {
                    // 新增冰柜至数据库
                    // 导入冰柜参数限制较多，需要多重校验
                    iceBox = new IceBox();
                    IceBoxExtend iceBoxExtend = new IceBoxExtend();
                    iceBox.setDeptId(serviceDeptId);
                    iceBox.setSupplierId(null == subordinateInfoVo ? null : subordinateInfoVo.getSupplierId());
                    iceBox.setAssetId(assetId);
                    iceBoxExtend.setAssetId(assetId);
                    // 冰柜名称
                    String chestName = oldIceBoxImportVo.getChestName();

                    iceBox.setChestName(chestName);
                    // 品牌
                    String brandName = oldIceBoxImportVo.getBrandName();

                    iceBox.setBrandName(brandName);
                    // 型号
                    String modelName = oldIceBoxImportVo.getModelName();

                    iceBox.setModelName(modelName);
                    // 规格
                    String chestNorm = oldIceBoxImportVo.getChestNorm();

                    iceBox.setChestNorm(chestNorm);
                    IceModel iceModel = iceModelDao.selectOne(Wrappers.<IceModel>lambdaQuery().eq(IceModel::getChestModel, modelName));
                    if (null == iceModel) {
                        throw new NormalOptionException(Constants.API_CODE_FAIL, "第" + index + "行数据 型号未导入数据库，请联系相关人员补充");
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
                // 新的 冰柜状态/投放状态
                JSONObject jsonObject = iceBoxService.setAssetReportJson(iceBox,"旧冰柜报废");
                return jsonObject;
            }
        },
        LOST("遗失", "旧冰柜遗失") {
            @Override
            public JSONObject operating(Integer index, OldIceBoxImportVo oldIceBoxImportVo, IceBoxDao iceBoxDao, IceBoxExtendDao iceBoxExtendDao,
                                        FeignDeptClient feignDeptClient, FeignSupplierClient feignSupplierClient, IceModelDao iceModelDao,
                                        IceBoxService iceBoxService,RabbitTemplate rabbitTemplate,FeignStoreClient feignStoreClient) {

                // 冰柜报废，目前把冰柜退回经销商然后 冰柜状态置为异常
                String assetId = oldIceBoxImportVo.getAssetId();
                String supplierNumber = oldIceBoxImportVo.getSupplierNumber();
                SubordinateInfoVo subordinateInfoVo = null;
                if (StringUtils.isNotBlank(supplierNumber)) {
                    SubordinateInfoVo feignData = FeignResponseUtil.getFeignData(feignSupplierClient.findByNumber(supplierNumber));
                    if (null == feignData.getSupplierId()) {
                        throw new NormalOptionException(Constants.API_CODE_FAIL, "第" + index + "行数据 经销商信息查询有误，请核对经销商编号");
                    } else {
                        subordinateInfoVo = feignData;
                    }
                }

                String suppName = subordinateInfoVo.getName(); // 经销商名称
                String service = oldIceBoxImportVo.getService();

                Integer serviceDeptId = FeignResponseUtil.getFeignData(feignDeptClient.findMaxIdByName(service));
                if (null == serviceDeptId) {
                    throw new NormalOptionException(Constants.API_CODE_FAIL, "第" + index + "行数据 服务处信息查询有误，请核对服务处名称");
                }

                IceBox iceBox = iceBoxDao.selectOne(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getAssetId, assetId));
                if (null != iceBox) {
                    // 更新冰柜状态及经销商信息
                    iceBox.setDeptId(serviceDeptId);
                    iceBox.setSupplierId(null == subordinateInfoVo ? null : subordinateInfoVo.getSupplierId());

                    iceBox.setStatus(IceBoxEnums.StatusEnum.LOSE.getType());
                    iceBoxDao.updateById(iceBox);
                    Integer boxId = iceBox.getId();
                    if(PutStatus.FINISH_PUT.getStatus().equals(iceBox.getPutStatus())){
                        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                            @Override
                            public void afterCommit() {
                                //报废
                                IceInspectionReportMsg reportMsg = new IceInspectionReportMsg();
                                reportMsg.setOperateType(5);
                                reportMsg.setBoxId(boxId);
                                rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.iceInspectionReportKey,reportMsg);
                            }
                        });
                    }
                } else {
                    // 新增冰柜至数据库
                    // 导入冰柜参数限制较多，需要多重校验
                    iceBox = new IceBox();
                    IceBoxExtend iceBoxExtend = new IceBoxExtend();
                    iceBox.setDeptId(serviceDeptId);
                    iceBox.setSupplierId(null == subordinateInfoVo ? null : subordinateInfoVo.getSupplierId());
                    iceBox.setAssetId(assetId);
                    iceBoxExtend.setAssetId(assetId);
                    // 冰柜名称
                    String chestName = oldIceBoxImportVo.getChestName();

                    iceBox.setChestName(chestName);
                    // 品牌
                    String brandName = oldIceBoxImportVo.getBrandName();

                    iceBox.setBrandName(brandName);
                    // 型号
                    String modelName = oldIceBoxImportVo.getModelName();

                    iceBox.setModelName(modelName);
                    // 规格
                    String chestNorm = oldIceBoxImportVo.getChestNorm();

                    iceBox.setChestNorm(chestNorm);
                    IceModel iceModel = iceModelDao.selectOne(Wrappers.<IceModel>lambdaQuery().eq(IceModel::getChestModel, modelName));
                    if (null == iceModel) {
                        throw new NormalOptionException(Constants.API_CODE_FAIL, "第" + index + "行数据 型号未导入数据库，请联系相关人员补充");
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

                // 新的 冰柜状态/投放状态
                JSONObject jsonObject = iceBoxService.setAssetReportJson(iceBox,"旧冰柜遗失");
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
                                             IceBoxService iceBoxService,RabbitTemplate rabbitTemplate,FeignStoreClient feignStoreClient);

    }

    public void validateMain(Integer index, OldIceBoxImportVo oldIceBoxImportVo) {
        // 资产编号
        String assetId = oldIceBoxImportVo.getAssetId();
        if (StringUtils.isBlank(assetId)) {
            throw new NormalOptionException(Constants.API_CODE_FAIL, "第" + index + "行数据 资产编号为空,请补充资产编号");
        }
        // 冰柜名称
        String chestName = oldIceBoxImportVo.getChestName();
        if (StringUtils.isBlank(chestName)) {
            throw new NormalOptionException(Constants.API_CODE_FAIL, "第" + index + "行数据 冰柜名称为空,请补充冰柜名称");
        }
        // 品牌
        String brandName = oldIceBoxImportVo.getBrandName();
        if (StringUtils.isBlank(brandName)) {
            throw new NormalOptionException(Constants.API_CODE_FAIL, "第" + index + "行数据 品牌为空,请补充品牌");
        }
        // 型号
        String modelName = oldIceBoxImportVo.getModelName();
        if (StringUtils.isBlank(modelName)) {
            throw new NormalOptionException(Constants.API_CODE_FAIL, "第" + index + "行数据 型号为空,请补充型号");
        }
        // 规格
        String chestNorm = oldIceBoxImportVo.getChestNorm();
        if (StringUtils.isBlank(chestNorm)) {
            throw new NormalOptionException(Constants.API_CODE_FAIL, "第" + index + "行数据 规格为空,请补充规格");
        }
        // 服务处
        String service = oldIceBoxImportVo.getService();
        if (StringUtils.isBlank(service)) {
            throw new NormalOptionException(Constants.API_CODE_FAIL, "第" + index + "行数据 服务处为空,请补充服务处信息");
        }

    }
}
