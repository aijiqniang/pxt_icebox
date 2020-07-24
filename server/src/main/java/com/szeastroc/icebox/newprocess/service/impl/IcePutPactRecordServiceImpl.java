package com.szeastroc.icebox.newprocess.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.utils.ExecutorServiceFactory;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.customer.client.FeignCusLabelClient;
import com.szeastroc.customer.client.FeignSupplierClient;
import com.szeastroc.customer.common.dto.CustomerLabelDetailDto;
import com.szeastroc.customer.common.vo.SubordinateInfoVo;
import com.szeastroc.icebox.enums.FreePayTypeEnum;
import com.szeastroc.icebox.enums.RecordStatus;
import com.szeastroc.icebox.enums.ServiceType;
import com.szeastroc.icebox.newprocess.dao.*;
import com.szeastroc.icebox.newprocess.entity.*;
import com.szeastroc.icebox.newprocess.service.IcePutPactRecordService;
import com.szeastroc.icebox.oldprocess.vo.ClientInfoRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @_(@Autowired))
public class IcePutPactRecordServiceImpl extends ServiceImpl<IcePutPactRecordDao, IcePutPactRecord> implements IcePutPactRecordService {

    private final IceBoxExtendDao iceBoxExtendDao;
    private final IcePutApplyDao icePutApplyDao;
    private final FeignSupplierClient feignSupplierClient;
    private final FeignCusLabelClient feignCusLabelClient;
    private final IcePutApplyRelateBoxDao icePutApplyRelateBoxDao;
    private final IceBoxDao iceBoxDao;
    private final PutStoreRelateModelDao putStoreRelateModelDao;
    private final ApplyRelatePutStoreModelDao applyRelatePutStoreModelDao;
    private final IceTransferRecordDao iceTransferRecordDao;

    @Override
    public void createPactRecord(ClientInfoRequest clientInfoRequest) {
        // 通过冰柜找到申请投放的门店
        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(clientInfoRequest.getIceChestId());
        IcePutApply icePutApply = icePutApplyDao.selectOne(Wrappers.<IcePutApply>lambdaQuery().eq(IcePutApply::getApplyNumber, iceBoxExtend.getLastApplyNumber()));

        // 创建协议
        IcePutPactRecord icePutPactRecord = getOne(Wrappers.<IcePutPactRecord>lambdaQuery()
                .eq(IcePutPactRecord::getApplyNumber, icePutApply.getApplyNumber())
                .eq(IcePutPactRecord::getBoxId, iceBoxExtend.getId())
                .eq(IcePutPactRecord::getStoreNumber, icePutApply.getPutStoreNumber()));

        if(Objects.isNull(icePutPactRecord)){
            saveIcePutRecord(iceBoxExtend, icePutApply);
            return;
        }
        icePutPactRecord.setPutTime(icePutApply.getCreatedTime()); // TODO 投放时间是按照 签收时间 还是 申请时间
        icePutPactRecord.setPutExpireTime(expireTimeRule(icePutPactRecord.getCreatedTime()));
        icePutPactRecord.setUpdatedBy(0);
        icePutPactRecord.setUpdatedTime(new Date());
        updateById(icePutPactRecord);

        IceBox iceBox = iceBoxDao.selectById(clientInfoRequest.getIceChestId());
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
            IcePutApplyRelateBox relateBox = icePutApplyRelateBoxDao.selectOne(Wrappers.<IcePutApplyRelateBox>lambdaQuery().eq(IcePutApplyRelateBox::getBoxId, iceBox.getId()).eq(IcePutApplyRelateBox::getApplyNumber, icePutApply.getApplyNumber()));
            if (relateBox != null && FreePayTypeEnum.UN_FREE.getType().equals(relateBox.getFreeType())) {
                iceTransferRecord.setTransferMoney(iceBox.getDepositMoney());
            }
            iceTransferRecordDao.insert(iceTransferRecord);
            log.info("applyNumber-->【{}】创建往来记录成功",icePutApply.getApplyNumber());
        }

        // 添加标签
        CompletableFuture.runAsync(() -> {
            addLabel(icePutPactRecord,iceBoxExtend.getAssetId());
        }, ExecutorServiceFactory.getInstance());
    }

    private Date expireTimeRule(Date createdTime) {
        DateTime dateTime = new DateTime(createdTime.getTime()).plusYears(1);
        return dateTime.toDate();
    }

    private void saveIcePutRecord(IceBoxExtend iceBoxExtend, IcePutApply icePutApply) {
        IcePutPactRecord icePutPactRecord;
        icePutPactRecord = new IcePutPactRecord();
        icePutPactRecord.setStoreNumber(icePutApply.getPutStoreNumber());
        icePutPactRecord.setBoxId(iceBoxExtend.getId());
        icePutPactRecord.setApplyNumber(icePutApply.getApplyNumber());
        icePutPactRecord.setPutTime(icePutApply.getCreatedTime()); // TODO 投放时间是按照 签收时间 还是 申请时间
        icePutPactRecord.setPutExpireTime(expireTimeRule(icePutPactRecord.getPutTime()));
        icePutPactRecord.setCreatedBy(0);
        icePutPactRecord.setCreatedTime(new Date());
        icePutPactRecord.setUpdatedBy(0);
        icePutPactRecord.setUpdatedTime(icePutPactRecord.getCreatedTime());
        save(icePutPactRecord);
        // 添加标签
        CompletableFuture.runAsync(() -> {
           addLabel(icePutPactRecord,iceBoxExtend.getAssetId());
        }, ExecutorServiceFactory.getInstance());
    }

    @Override
    public boolean checkPactRecordByBoxId(Integer iceBoxId) {
        // 通过冰柜找到申请投放的门店
        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(iceBoxId);
        IcePutApply icePutApply = icePutApplyDao.selectOne(Wrappers.<IcePutApply>lambdaQuery().eq(IcePutApply::getApplyNumber, iceBoxExtend.getLastApplyNumber()));

        // 检查协议
        int count = count(Wrappers.<IcePutPactRecord>lambdaQuery()
                .eq(IcePutPactRecord::getApplyNumber, icePutApply.getApplyNumber())
                .eq(IcePutPactRecord::getBoxId, iceBoxExtend.getId())
                .eq(IcePutPactRecord::getStoreNumber, icePutApply.getPutStoreNumber()));
        return count > 0;
    }

    private void addLabel(IcePutPactRecord icePutPactRecord, String assetId) {
        Date putTime = icePutPactRecord.getPutTime();
        Date putExpireTime = icePutPactRecord.getPutExpireTime();
        CustomerLabelDetailDto customerLabelDetailDto = new CustomerLabelDetailDto();
        customerLabelDetailDto.setLabelId(9999);
        customerLabelDetailDto.setCreateTime(putTime);
        customerLabelDetailDto.setCustomerNumber(icePutPactRecord.getStoreNumber());
        customerLabelDetailDto.setCreateBy(0);
        customerLabelDetailDto.setCreateByName("系统");
        customerLabelDetailDto.setPutProject("冰柜");
        customerLabelDetailDto.setCancelTime(putExpireTime);
        customerLabelDetailDto.setRemarks(assetId);
        SubordinateInfoVo subordinateInfoVo = FeignResponseUtil.getFeignData(feignSupplierClient.findByNumber(icePutPactRecord.getStoreNumber()));
        if (subordinateInfoVo != null && StringUtils.isNotBlank(subordinateInfoVo.getNumber())) {
            customerLabelDetailDto.setCustomerType(1);
        } else {
            customerLabelDetailDto.setCustomerType(0);
        }
        feignCusLabelClient.createCustomerLabelDetail(customerLabelDetailDto);
    }

}
