package com.szeastroc.icebox.sync;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Lists;
import com.szeastroc.common.enums.CommonStatus;
import com.szeastroc.icebox.enums.PutStatus;
import com.szeastroc.icebox.enums.ServiceType;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.entity.IceBoxExtend;
import com.szeastroc.icebox.newprocess.entity.IceModel;
import com.szeastroc.icebox.newprocess.service.IceBoxExtendService;
import com.szeastroc.icebox.newprocess.service.IceBoxService;
import com.szeastroc.icebox.newprocess.service.IceModelService;
import com.szeastroc.icebox.oldprocess.entity.IceChestInfo;
import com.szeastroc.icebox.oldprocess.entity.IceChestPutRecord;
import com.szeastroc.icebox.oldprocess.service.IceChestInfoService;
import com.szeastroc.icebox.oldprocess.service.IceChestPutRecordService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class IceChestInfoSync {

    @Autowired
    private IceChestInfoService iceChestInfoService;
    @Autowired
    private IceBoxService iceBoxService;
    @Autowired
    private IceBoxExtendService iceBoxExtendService;
    @Autowired
    private IceModelService iceModelService;
    @Autowired
    private IceChestPutRecordService iceChestPutRecordService;
    @Autowired
    private CommonConvert commonConvert;

    public void convertOldIceInfoToIcebox() throws Exception {
        // 取出冰柜数据
        List<IceChestInfo> iceChestInfos = findAllIceInfo();
        // 转换 IceInfo 为 IceBox
        List<IceBox> iceBoxes = buildIceBox(iceChestInfos);
        // 转换 IceInfo 为 IceBoxExtend
        List<IceBoxExtend> iceBoxExtends = buildIceBoxExtend(iceChestInfos);
        // 清空并保存新冰柜
        saveIceBoxs(iceBoxes);
        saveIceBoxExtends(iceBoxExtends);
    }

    private List<IceChestInfo> findAllIceInfo() {
        return iceChestInfoService.list();
    }

    private List<IceBox> buildIceBox(List<IceChestInfo> iceChestInfos) throws Exception {
        List<IceBox> iceBoxes = Lists.newArrayList();
        for (IceChestInfo iceChestInfo : iceChestInfos) {
            // 需要跳过一些测试数据
            if (iceChestInfo.getId() == 1 || iceChestInfo.getId() == 2 || iceChestInfo.getId() == 3) {
                continue;
            }

            IceBox iceBox = new IceBox();
            iceBox.setId(iceChestInfo.getId());
            iceBox.setChestName(iceChestInfo.getChestName());

            // 需要创建型号
            iceBox.setModelId(convertModelId(iceChestInfo.getChestModel(), iceChestInfo.getChestName(), iceChestInfo.getDepositMoney()));

            iceBox.setBrandName(iceChestInfo.getBrandName());
            iceBox.setChestNorm(iceChestInfo.getChestNorm());
            iceBox.setChestMoney(iceChestInfo.getChestMoney());
            iceBox.setDepositMoney(iceChestInfo.getDepositMoney());

            // 需要追溯投放记录找到原始所属 & 找到对应鹏讯通的经销商
            // 需要先得出原始拥有者的clientId是多少 (用于共用)
            Integer originalClientIdFromOldSource = getClientIdFromTracePutRecord(iceChestInfo.getId(), iceChestInfo.getClientId());

            iceBox.setSupplierId(commonConvert.convertSupplierIdByClientId(originalClientIdFromOldSource));

            // 转换为鹏讯通的经销商ID
            // 判断是否存在已投放门店, 未投放则直接返回空
            if (PutStatus.NO_PUT.getStatus().equals(iceChestInfo.getPutStatus())) {
                iceBox.setPutStoreNumber(StringUtils.EMPTY);
            } else {
                iceBox.setPutStoreNumber(commonConvert.getStoreNumberByClientId(iceChestInfo.getClientId()));
            }

            // 需要找到对应鹏讯通的部门
            iceBox.setDeptId(commonConvert.getDeptIdByClientId(originalClientIdFromOldSource));

            iceBox.setRemark(iceChestInfo.getRemark());
            iceBox.setPutStatus(iceChestInfo.getPutStatus());
            iceBox.setStatus(CommonStatus.VALID.getStatus());
            iceBox.setCreatedBy(0);
            iceBox.setCreatedTime(iceChestInfo.getCreateTime());
            iceBox.setUpdatedBy(0);
            iceBox.setUpdatedTime(iceChestInfo.getUpdateTime());
            iceBoxes.add(iceBox);
        }
        return iceBoxes;
    }

    private Integer convertModelId(String chestModel, String chestName, BigDecimal DepositMoney) throws Exception {
        // 查询数据中是否已有该型号
        // |-> 有则返回
        // |-> 没有则创建
        int count = iceModelService.count(Wrappers.<IceModel>lambdaQuery().eq(IceModel::getChestModel, chestModel));
        if (count > 1) {
            log.error("冰柜型号表存在重复数据 -> chestModel: [{}]", chestModel);
            throw new Exception("数据不正确, 停止同步");
        }
        if (count > 0) {
            IceModel iceModel = iceModelService.getOne(Wrappers.<IceModel>lambdaQuery().eq(IceModel::getChestModel, chestModel));
            return iceModel.getId();
        }
        IceModel iceModel = new IceModel();
        iceModel.setChestModel(chestModel);
        iceModel.setChestName(chestName);
        iceModel.setDepositMoney(DepositMoney);
        iceModel.setCreatedBy(0);
        iceModel.setCreatedTime(new Date());
        iceModel.setUpdatedBy(0);
        iceModel.setUpdatedTime(iceModel.getCreatedTime());
        iceModelService.save(iceModel);
        return iceModel.getId();
    }

    /**
     * 追溯投放记录表, 得出冰柜的最初所属客户clientId
     *
     * @param iceChestId 冰柜ID
     * @param clientId   现在所在客户ID
     * @return
     */
    private Integer getClientIdFromTracePutRecord(Integer iceChestId, Integer clientId) {
        // 查询 对应冰柜 是否有投放记录
        // |-> 如果没有, 则直接返回当前冰柜所属clientId
        // |-> 如果有, 则查询最早一条投放记录 并返回sendClientId
        int count = iceChestPutRecordService.count(Wrappers.<IceChestPutRecord>lambdaQuery()
                .eq(IceChestPutRecord::getServiceType, ServiceType.IS_PUT.getType())
                .eq(IceChestPutRecord::getChestId, iceChestId));
        if (count <= 0) {
            return clientId;
        }
        List<IceChestPutRecord> iceChestPutRecords = iceChestPutRecordService.list(Wrappers.<IceChestPutRecord>lambdaQuery()
                .eq(IceChestPutRecord::getServiceType, ServiceType.IS_PUT.getType())
                .eq(IceChestPutRecord::getChestId, iceChestId)
                .orderByAsc(IceChestPutRecord::getCreateTime));
        return iceChestPutRecords.get(0).getSendClientId();
    }

    private List<IceBoxExtend> buildIceBoxExtend(List<IceChestInfo> iceChestInfos) {
        List<IceBoxExtend> iceBoxExtends = Lists.newArrayList();
        for (IceChestInfo iceChestInfo : iceChestInfos) {
            // 需要跳过一些测试数据
            if (iceChestInfo.getId() == 1 || iceChestInfo.getId() == 2 || iceChestInfo.getId() == 3) {
                continue;
            }
            IceBoxExtend iceBoxExtend = new IceBoxExtend();
            iceBoxExtend.setId(iceChestInfo.getId());
            iceBoxExtend.setExternalId(iceChestInfo.getExternalId());
            iceBoxExtend.setAssetId(iceChestInfo.getAssetId());
            iceBoxExtend.setBluetoothId(iceChestInfo.getBluetoothId());
            iceBoxExtend.setBluetoothMac(iceChestInfo.getBluetoothMac());
            iceBoxExtend.setQrCode(iceChestInfo.getQrCode());
            iceBoxExtend.setGpsMac(iceChestInfo.getGpsMac());
            iceBoxExtend.setLastExamineId(iceChestInfo.getLastExamineId());
            iceBoxExtend.setLastExamineTime(iceChestInfo.getLastExamineTime());
            iceBoxExtend.setLastPutId(iceChestInfo.getLastPutId());
            iceBoxExtend.setLastPutTime(iceChestInfo.getLastPutTime());
            iceBoxExtend.setReleaseTime(iceChestInfo.getReleaseTime());
            iceBoxExtend.setRepairBeginTime(iceChestInfo.getRepairBeginTime());
            iceBoxExtend.setOpenTotal(iceChestInfo.getOpenTotal());
            iceBoxExtends.add(iceBoxExtend);
        }
        return iceBoxExtends;
    }

    private void saveIceBoxs(List<IceBox> iceBoxes) {
        iceBoxService.saveOrUpdateBatch(iceBoxes);
    }

    private void saveIceBoxExtends(List<IceBoxExtend> iceBoxExtends) {
        iceBoxExtendService.saveOrUpdateBatch(iceBoxExtends);
    }
}
