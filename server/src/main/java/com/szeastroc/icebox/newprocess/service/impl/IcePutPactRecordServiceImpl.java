package com.szeastroc.icebox.newprocess.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.enums.CommonStatus;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.enums.ClientType;
import com.szeastroc.icebox.newprocess.dao.*;
import com.szeastroc.icebox.newprocess.entity.*;
import com.szeastroc.icebox.newprocess.enums.PutStatus;
import com.szeastroc.icebox.oldprocess.entity.ClientInfo;
import com.szeastroc.icebox.oldprocess.entity.PactRecord;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.utils.ExecutorServiceFactory;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.customer.client.FeignCusLabelClient;
import com.szeastroc.customer.client.FeignSupplierClient;
import com.szeastroc.customer.common.dto.CustomerLabelDetailDto;
import com.szeastroc.customer.common.vo.SubordinateInfoVo;
import com.szeastroc.icebox.newprocess.dao.IceBoxExtendDao;
import com.szeastroc.icebox.newprocess.dao.IcePutApplyDao;
import com.szeastroc.icebox.newprocess.dao.IcePutPactRecordDao;
import com.szeastroc.icebox.newprocess.entity.IceBoxExtend;
import com.szeastroc.icebox.newprocess.entity.IcePutApply;
import com.szeastroc.icebox.newprocess.entity.IcePutPactRecord;
import com.szeastroc.icebox.newprocess.service.IcePutPactRecordService;
import com.szeastroc.icebox.oldprocess.vo.ClientInfoRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.icebox.newprocess.service.IcePutPactRecordService;
import org.springframework.transaction.annotation.Transactional;

@Service
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
