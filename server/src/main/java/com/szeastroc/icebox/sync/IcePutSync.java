package com.szeastroc.icebox.sync;

import cn.hutool.core.util.IdUtil;
import com.google.common.collect.Lists;
import com.szeastroc.icebox.enums.FreePayTypeEnum;
import com.szeastroc.icebox.enums.RecordStatus;
import com.szeastroc.icebox.enums.ServiceType;
import com.szeastroc.icebox.newprocess.entity.*;
import com.szeastroc.icebox.newprocess.enums.BackType;
import com.szeastroc.icebox.newprocess.enums.ExamineStatus;
import com.szeastroc.icebox.newprocess.enums.StoreSignStatus;
import com.szeastroc.icebox.newprocess.service.*;
import com.szeastroc.icebox.newprocess.vo.PutApplyWithPutApplyRelateBox;
import com.szeastroc.icebox.oldprocess.entity.IceChestPutRecord;
import com.szeastroc.icebox.oldprocess.service.IceChestPutRecordService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class IcePutSync {

    @Autowired
    private IceChestPutRecordService iceChestPutRecordService;
    @Autowired
    private IcePutApplyService icePutApplyService;
    @Autowired
    private IcePutApplyRelateBoxService icePutApplyRelateBoxService;
    @Autowired
    private IceBackApplyService iceBackApplyService;
    @Autowired
    private IceBackApplyRelateBoxService iceBackApplyRelateBoxService;
    @Autowired
    private IceTransferRecordService iceTransferRecordService;
    @Autowired
    private IceBoxService iceBoxService;
    @Autowired
    private CommonConvert commonConvert;


    public void convertOldPutRecordToIcePutAndBack() throws Exception {
        // 取出冰柜投放记录 (IceChestPutRecord)
        List<IceChestPutRecord> iceChestPutRecords = findAllIceChestPutRecord();
        // 转换为冰柜投放申请 冰柜退还申请 及 冰柜转移记录
        PutApplyWithPutApplyRelateBox putApplyWithPutApplyRelateBox = buildIcePutApplyAndBackApplyAndTransferRecord(iceChestPutRecords);

        // 清空并保存相关数据
        savePutApply(putApplyWithPutApplyRelateBox.getIcePutApplies());
        savePutApplyRelateBox(putApplyWithPutApplyRelateBox.getIcePutApplyRelateBoxes());

        saveBackApply(putApplyWithPutApplyRelateBox.getIceBackApplies());
        saveBackApplyRelateBox(putApplyWithPutApplyRelateBox.getIceBackApplyRelateBoxes());

        saveTransferRecord(putApplyWithPutApplyRelateBox.getIceTransferRecords());
    }

    /**
     * 查询旧冰柜投放记录
     *
     * @return
     */
    private List<IceChestPutRecord> findAllIceChestPutRecord() {
        return iceChestPutRecordService.list();
    }

    /**
     * 建立业务员投放申请及记录 (IcePutApply + IcePutApplyRelateBox + IcePutRecord)
     *
     * @param iceChestPutRecords
     * @return
     */
    private PutApplyWithPutApplyRelateBox buildIcePutApplyAndBackApplyAndTransferRecord(List<IceChestPutRecord> iceChestPutRecords) throws Exception {
        PutApplyWithPutApplyRelateBox putApplyWithPutApplyRelateBox = new PutApplyWithPutApplyRelateBox();
        putApplyWithPutApplyRelateBox.setIcePutApplies(Lists.newArrayList());
        putApplyWithPutApplyRelateBox.setIcePutApplyRelateBoxes(Lists.newArrayList());
        putApplyWithPutApplyRelateBox.setIceBackApplies(Lists.newArrayList());
        putApplyWithPutApplyRelateBox.setIceBackApplyRelateBoxes(Lists.newArrayList());
        putApplyWithPutApplyRelateBox.setIceTransferRecords(Lists.newArrayList());

        for (IceChestPutRecord iceChestPutRecord : iceChestPutRecords) {
            // 需要跳过一些测试数据
            if (iceChestPutRecord.getChestId() == 1 || iceChestPutRecord.getChestId() == 2 || iceChestPutRecord.getChestId() == 3) {
                continue;
            }
            // 未接收的冰柜, 为无效数据需过滤
            if (!iceChestPutRecord.getRecordStatus().equals(RecordStatus.RECEIVE_FINISH.getStatus())) {
                continue;
            }
            buildPutApplyAndBackApplyAndTransferRecord(putApplyWithPutApplyRelateBox, iceChestPutRecord);
        }
        return putApplyWithPutApplyRelateBox;
    }

    private void buildPutApplyAndBackApplyAndTransferRecord(PutApplyWithPutApplyRelateBox putApplyWithPutApplyRelateBox, IceChestPutRecord iceChestPutRecord) throws Exception {
        String applyNumber = StringUtils.EMPTY;
        // 旧记录业务类型为投放时, 可计入业务员申请表
        if (iceChestPutRecord.getServiceType().equals(ServiceType.IS_PUT.getType())) {
            // 建立冰柜投放申请
            applyNumber = buildIcePutApplyAndPutApplyRelateBox(putApplyWithPutApplyRelateBox, iceChestPutRecord);
        } else if (iceChestPutRecord.getServiceType().equals(ServiceType.ENTER_WAREHOUSE.getType())) {
            // 建立冰柜退还申请
            applyNumber = buildIceBackApplyAndBackApplyRelateBox(putApplyWithPutApplyRelateBox, iceChestPutRecord);
        }

        // 建立冰柜转移记录 (IcePutRecord)
        buildIceTransferRecord(putApplyWithPutApplyRelateBox, iceChestPutRecord, applyNumber);
    }

    /**
     * 建立冰柜投放申请记录
     *
     * @param putApplyWithPutApplyRelateBox
     * @param iceChestPutRecord
     * @return
     * @throws Exception
     */
    private String buildIcePutApplyAndPutApplyRelateBox(PutApplyWithPutApplyRelateBox putApplyWithPutApplyRelateBox, IceChestPutRecord iceChestPutRecord) throws Exception {
        // 初始化该次申请编号
        String applyNumber = "PUT" + IdUtil.simpleUUID().substring(0, 29);
        // 建立冰柜申请记录
        IcePutApply icePutApply = buildIcePutApply(iceChestPutRecord, applyNumber);
        List<IcePutApplyRelateBox> icePutApplyRelateBoxes = buildIcePutApplyRelateBox(iceChestPutRecord, applyNumber);
        // 加入集合
        putApplyWithPutApplyRelateBox.getIcePutApplies().add(icePutApply);
        putApplyWithPutApplyRelateBox.getIcePutApplyRelateBoxes().addAll(icePutApplyRelateBoxes);
        return applyNumber;
    }

    private IcePutApply buildIcePutApply(IceChestPutRecord iceChestPutRecord, String applyNumber) throws Exception {
        IcePutApply icePutApply = new IcePutApply();
        icePutApply.setApplyNumber(applyNumber);
        icePutApply.setUserId(0);
        icePutApply.setPutStoreNumber(commonConvert.getStoreNumberByClientId(iceChestPutRecord.getReceiveClientId()));
        icePutApply.setStoreSignStatus(StoreSignStatus.ALREADY_SIGN.getStatus());
        icePutApply.setExamineStatus(ExamineStatus.PASS_EXAMINE.getStatus());
        icePutApply.setOldPutId(iceChestPutRecord.getId());
        icePutApply.setCreatedBy(0);
        icePutApply.setCreatedTime(new Date());
        icePutApply.setUpdatedBy(0);
        icePutApply.setUpdateTime(icePutApply.getCreatedTime());
        return icePutApply;
    }

    private List<IcePutApplyRelateBox> buildIcePutApplyRelateBox(IceChestPutRecord iceChestPutRecord, String applyNumber) {
        IcePutApplyRelateBox icePutApplyRelateBox = new IcePutApplyRelateBox();
        icePutApplyRelateBox.setApplyNumber(applyNumber);
        icePutApplyRelateBox.setBoxId(iceChestPutRecord.getChestId());
        icePutApplyRelateBox.setModelId(getModelIdByBoxId(iceChestPutRecord.getChestId()));
        icePutApplyRelateBox.setFreeType(iceChestPutRecord.getFreePayType());
        return Arrays.asList(icePutApplyRelateBox);
    }

    private Integer getModelIdByBoxId(Integer chestId) {
        IceBox iceBox = iceBoxService.getById(chestId);
        return iceBox.getModelId();
    }

    /**
     * 建立冰柜退还申请记录
     *
     * @param putApplyWithPutApplyRelateBox
     * @param iceChestPutRecord
     * @return
     */
    private String buildIceBackApplyAndBackApplyRelateBox(PutApplyWithPutApplyRelateBox putApplyWithPutApplyRelateBox, IceChestPutRecord iceChestPutRecord) throws Exception {
        // 初始化该次申请编号
        String applyNumber = "BAC" + IdUtil.simpleUUID().substring(0, 29);
        // 建立冰柜退还记录
        IceBackApply iceBackApply = buildIceBackApply(iceChestPutRecord, applyNumber);
        List<IceBackApplyRelateBox> iceBackApplyRelateBoxes = buildIceBackApplyRelateBox(iceChestPutRecord, applyNumber);
        // 加入集合
        putApplyWithPutApplyRelateBox.getIceBackApplies().add(iceBackApply);
        putApplyWithPutApplyRelateBox.getIceBackApplyRelateBoxes().addAll(iceBackApplyRelateBoxes);
        return applyNumber;
    }

    private IceBackApply buildIceBackApply(IceChestPutRecord iceChestPutRecord, String applyNumber) throws Exception {
        IceBackApply iceBackApply = new IceBackApply();
        iceBackApply.setApplyNumber(applyNumber);
        iceBackApply.setUserId(0);
        iceBackApply.setBackStoreNumber(commonConvert.getStoreNumberByClientId(iceChestPutRecord.getSendClientId()));
        iceBackApply.setExamineStatus(ExamineStatus.PASS_EXAMINE.getStatus());
        iceBackApply.setOldPutId(iceChestPutRecord.getId());
        iceBackApply.setCreatedBy(0);
        iceBackApply.setCreatedTime(new Date());
        iceBackApply.setUpdatedBy(0);
        iceBackApply.setUpdatedTime(iceBackApply.getCreatedTime());
        return iceBackApply;
    }

    private List<IceBackApplyRelateBox> buildIceBackApplyRelateBox(IceChestPutRecord iceChestPutRecord, String applyNumber) throws Exception {
        IceBackApplyRelateBox iceBackApplyRelateBox = new IceBackApplyRelateBox();
        iceBackApplyRelateBox.setApplyNumber(applyNumber);
        iceBackApplyRelateBox.setBoxId(iceChestPutRecord.getChestId());
        iceBackApplyRelateBox.setModelId(getModelIdByBoxId(iceChestPutRecord.getChestId()));
        iceBackApplyRelateBox.setBackType(BackType.BACK_MONEY.getType());
        iceBackApplyRelateBox.setFreeType(iceChestPutRecord.getFreePayType());
        iceBackApplyRelateBox.setBackSupplierId(commonConvert.convertSupplierIdByClientId(iceChestPutRecord.getReceiveClientId()));
        return Arrays.asList(iceBackApplyRelateBox);
    }

    /**
     * 建立投放(转移)记录
     *
     * @param putApplyWithPutApplyRelateBox
     * @param iceChestPutRecord
     * @param applyNumber
     */
    private void buildIceTransferRecord(PutApplyWithPutApplyRelateBox putApplyWithPutApplyRelateBox, IceChestPutRecord iceChestPutRecord, String applyNumber) throws Exception {
        IceTransferRecord iceTransferRecord = new IceTransferRecord();
        iceTransferRecord.setApplyNumber(applyNumber);
        iceTransferRecord.setServiceType(iceChestPutRecord.getServiceType());
        iceTransferRecord.setBoxId(iceChestPutRecord.getChestId());

        if (iceChestPutRecord.getServiceType().equals(com.szeastroc.icebox.newprocess.enums.ServiceType.IS_PUT.getType())) {
            iceTransferRecord.setSupplierId(commonConvert.convertSupplierIdByClientId(iceChestPutRecord.getSendClientId()));
            iceTransferRecord.setStoreNumber(commonConvert.getStoreNumberByClientId(iceChestPutRecord.getReceiveClientId()));
        } else {
            iceTransferRecord.setSupplierId(commonConvert.convertSupplierIdByClientId(iceChestPutRecord.getReceiveClientId()));
            iceTransferRecord.setStoreNumber(commonConvert.getStoreNumberByClientId(iceChestPutRecord.getSendClientId()));
        }
        iceTransferRecord.setSendTime(iceChestPutRecord.getCreateTime());
        iceTransferRecord.setReceiveTime(iceChestPutRecord.getCreateTime());
        iceTransferRecord.setApplyUserId(0);
        iceTransferRecord.setApplyTime(iceChestPutRecord.getCreateTime());
        iceTransferRecord.setTransferMoney(iceChestPutRecord.getFreePayType().equals(FreePayTypeEnum.UN_FREE.getType()) ? iceChestPutRecord.getDepositMoney() : new BigDecimal(0));
        iceTransferRecord.setRecordStatus(com.szeastroc.icebox.newprocess.enums.RecordStatus.SEND_ING.getStatus());
        iceTransferRecord.setCreateTime(iceChestPutRecord.getCreateTime());
        iceTransferRecord.setUpdateTime(iceChestPutRecord.getUpdateTime());
        putApplyWithPutApplyRelateBox.getIceTransferRecords().add(iceTransferRecord);
    }


    private void savePutApply(List<IcePutApply> icePutApplies) {
        icePutApplyService.saveOrUpdateBatch(icePutApplies);
    }

    private void savePutApplyRelateBox(List<IcePutApplyRelateBox> icePutApplyRelateBoxes) {
        icePutApplyRelateBoxService.saveOrUpdateBatch(icePutApplyRelateBoxes);
    }

    private void saveBackApply(List<IceBackApply> iceBackApplies) {
        iceBackApplyService.saveOrUpdateBatch(iceBackApplies);
    }

    private void saveBackApplyRelateBox(List<IceBackApplyRelateBox> iceBackApplyRelateBoxes) {
        iceBackApplyRelateBoxService.saveOrUpdateBatch(iceBackApplyRelateBoxes);
    }

    private void saveTransferRecord(List<IceTransferRecord> iceTransferRecords) {
        iceTransferRecordService.saveOrUpdateBatch(iceTransferRecords);
    }


}
