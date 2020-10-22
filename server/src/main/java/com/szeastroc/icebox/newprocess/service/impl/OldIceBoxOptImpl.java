package com.szeastroc.icebox.newprocess.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Lists;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.exception.NormalOptionException;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.customer.client.FeignSupplierClient;
import com.szeastroc.customer.common.vo.SubordinateInfoVo;
import com.szeastroc.icebox.newprocess.dao.IceBoxDao;
import com.szeastroc.icebox.newprocess.dao.IceBoxExtendDao;
import com.szeastroc.icebox.newprocess.dao.IceModelDao;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.entity.IceBoxExtend;
import com.szeastroc.icebox.newprocess.entity.IceModel;
import com.szeastroc.icebox.newprocess.enums.IceBoxEnums;
import com.szeastroc.icebox.newprocess.enums.PutStatus;
import com.szeastroc.icebox.newprocess.service.OldIceBoxOpt;
import com.szeastroc.icebox.newprocess.vo.IceBoxAssetReportVo;
import com.szeastroc.icebox.newprocess.vo.OldIceBoxImportVo;
import com.szeastroc.user.client.FeignDeptClient;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

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


    @Override
    @Transactional
    public List<IceBoxAssetReportVo> opt(List<OldIceBoxImportVo> oldIceBoxImportVoList) {

        List<IceBoxAssetReportVo> lists = Lists.newArrayList();
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
            IceBoxAssetReportVo assetReportVo = item.operating(index, oldIceBoxImportVo, iceBoxDao, iceBoxExtendDao, feignDeptClient, feignSupplierClient, iceModelDao);
            lists.add(assetReportVo);
        }
        return lists;
    }


    @Getter
    private enum OldIceBoxOptType {
        CREATE("新增", "旧冰柜入库") {
            @Override
            public IceBoxAssetReportVo operating(Integer index, OldIceBoxImportVo oldIceBoxImportVo, IceBoxDao iceBoxDao, IceBoxExtendDao iceBoxExtendDao, FeignDeptClient feignDeptClient, FeignSupplierClient feignSupplierClient, IceModelDao iceModelDao) {
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
                } else {
                    iceBox.setPutStoreNumber("0");
                    iceBox.setPutStatus(PutStatus.NO_PUT.getStatus());
                }

                IceBox selectIceBox = iceBoxDao.selectOne(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getAssetId, assetId));

                // 旧的 冰柜状态/投放状态
                Integer oldPutStatus = selectIceBox == null ? null : selectIceBox.getPutStatus();
                Integer oldStatus = selectIceBox == null ? null : selectIceBox.getStatus();
                if (null != selectIceBox) {
                    iceBox.setId(selectIceBox.getId());
                    iceBoxDao.updateById(iceBox);
                } else {
                    iceBoxDao.insert(iceBox);
                    iceBoxExtend.setId(iceBox.getId());
                    iceBoxExtendDao.insert(iceBoxExtend);
                }
                // 新的 冰柜状态/投放状态
                Integer newPutStatus = iceBox.getPutStatus() == null ? PutStatus.NO_PUT.getStatus() : iceBox.getPutStatus();
                Integer newStatus = iceBox.getStatus() == null ? IceBoxEnums.StatusEnum.ABNORMAL.getType() : iceBox.getStatus();

                IceBoxAssetReportVo assetReportVo = IceBoxAssetReportVo.builder()
                        .assetId(assetId)
                        .modelId(iceBox.getModelId())
                        .modelName(iceBox.getModelName())
                        .suppName(suppName)
                        .suppNumber(supplierNumber)
                        .oldPutStatus(oldPutStatus)
                        .oldStatus(oldStatus)
                        .newPutStatus(newPutStatus)
                        .newStatus(newStatus)
                        .suppDeptId(iceBox.getDeptId()).build();
                return assetReportVo;
            }
        },
        GO_BACK("退仓", "旧冰柜退回经销商") {
            @Override
            public IceBoxAssetReportVo operating(Integer index, OldIceBoxImportVo oldIceBoxImportVo, IceBoxDao iceBoxDao, IceBoxExtendDao iceBoxExtendDao, FeignDeptClient feignDeptClient, FeignSupplierClient feignSupplierClient, IceModelDao iceModelDao) {

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

                // 旧的 冰柜状态/投放状态
                Integer oldPutStatus = iceBox == null ? null : iceBox.getPutStatus();
                Integer oldStatus = iceBox == null ? null : iceBox.getStatus();
                if (null != iceBox) {
                    // 更新冰柜状态及经销商信息
                    iceBox.setDeptId(serviceDeptId);
                    iceBox.setSupplierId(subordinateInfoVo.getSupplierId());
                    iceBox.setPutStoreNumber("0");
                    iceBox.setPutStatus(PutStatus.NO_PUT.getStatus());
                    iceBoxDao.updateById(iceBox);
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
                    iceBox.setPutStoreNumber("0");
                    iceBox.setPutStatus(PutStatus.NO_PUT.getStatus());
                    iceBoxDao.insert(iceBox);
                    iceBoxExtend.setId(iceBox.getId());
                    iceBoxExtendDao.insert(iceBoxExtend);
                }
                // 新的 冰柜状态/投放状态
                Integer newPutStatus = iceBox.getPutStatus() == null ? PutStatus.NO_PUT.getStatus() : iceBox.getPutStatus();
                Integer newStatus = iceBox.getStatus() == null ? IceBoxEnums.StatusEnum.ABNORMAL.getType() : iceBox.getStatus();


                IceBoxAssetReportVo assetReportVo = IceBoxAssetReportVo.builder()
                        .assetId(assetId)
                        .modelId(iceBox.getModelId())
                        .modelName(iceBox.getModelName())
                        .suppName(suppName)
                        .suppNumber(supplierNumber)
                        .oldPutStatus(oldPutStatus)
                        .oldStatus(oldStatus)
                        .newPutStatus(newPutStatus)
                        .newStatus(newStatus)
                        .suppDeptId(iceBox.getDeptId()).build();
                return assetReportVo;
            }
        },
        SCRAP("报废", "旧冰柜报废") {
            @Override
            public IceBoxAssetReportVo operating(Integer index, OldIceBoxImportVo oldIceBoxImportVo, IceBoxDao iceBoxDao, IceBoxExtendDao iceBoxExtendDao, FeignDeptClient feignDeptClient, FeignSupplierClient feignSupplierClient, IceModelDao iceModelDao) {

                // 冰柜报废，目前把冰柜退回经销商然后 冰柜状态置为异常

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
                // 旧的 冰柜状态/投放状态
                Integer oldPutStatus = iceBox == null ? null : iceBox.getPutStatus();
                Integer oldStatus = iceBox == null ? null : iceBox.getStatus();
                if (null != iceBox) {
                    // 更新冰柜状态及经销商信息
                    iceBox.setDeptId(serviceDeptId);
                    iceBox.setSupplierId(subordinateInfoVo.getSupplierId());
                    iceBox.setPutStoreNumber("0");
                    iceBox.setPutStatus(PutStatus.NO_PUT.getStatus());
                    iceBox.setStatus(IceBoxEnums.StatusEnum.ABNORMAL.getType());
                    iceBoxDao.updateById(iceBox);
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
                    iceBox.setPutStoreNumber("0");
                    iceBox.setPutStatus(PutStatus.NO_PUT.getStatus());
                    iceBox.setStatus(IceBoxEnums.StatusEnum.ABNORMAL.getType());
                    iceBoxDao.insert(iceBox);
                    iceBoxExtend.setId(iceBox.getId());
                    iceBoxExtendDao.insert(iceBoxExtend);
                }

                // 新的 冰柜状态/投放状态
                Integer newPutStatus = iceBox.getPutStatus() == null ? PutStatus.NO_PUT.getStatus() : iceBox.getPutStatus();
                Integer newStatus = iceBox.getStatus() == null ? IceBoxEnums.StatusEnum.ABNORMAL.getType() : iceBox.getStatus();


                IceBoxAssetReportVo assetReportVo = IceBoxAssetReportVo.builder()
                        .assetId(assetId)
                        .modelId(iceBox.getModelId())
                        .modelName(iceBox.getModelName())
                        .suppName(suppName)
                        .suppNumber(supplierNumber)
                        .oldPutStatus(oldPutStatus)
                        .oldStatus(oldStatus)
                        .newPutStatus(newPutStatus)
                        .newStatus(newStatus)
                        .suppDeptId(iceBox.getDeptId()).build();
                return assetReportVo;
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

        abstract public IceBoxAssetReportVo operating(Integer index, OldIceBoxImportVo oldIceBoxImportVo, IceBoxDao iceBoxDao,
                                                      IceBoxExtendDao iceBoxExtendDao, FeignDeptClient feignDeptClient, FeignSupplierClient feignSupplierClient, IceModelDao iceModelDao);

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
