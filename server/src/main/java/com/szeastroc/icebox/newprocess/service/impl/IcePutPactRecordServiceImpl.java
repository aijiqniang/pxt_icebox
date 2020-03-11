package com.szeastroc.icebox.newprocess.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.enums.CommonStatus;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.enums.ClientType;
import com.szeastroc.icebox.newprocess.dao.IceBoxDao;
import com.szeastroc.icebox.newprocess.dao.IceBoxExtendDao;
import com.szeastroc.icebox.newprocess.dao.IcePutApplyDao;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.entity.IceBoxExtend;
import com.szeastroc.icebox.newprocess.entity.IcePutApply;
import com.szeastroc.icebox.oldprocess.entity.ClientInfo;
import com.szeastroc.icebox.oldprocess.entity.PactRecord;
import com.szeastroc.icebox.oldprocess.vo.ClientInfoRequest;
import lombok.RequiredArgsConstructor;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.icebox.newprocess.entity.IcePutPactRecord;
import com.szeastroc.icebox.newprocess.dao.IcePutPactRecordDao;
import com.szeastroc.icebox.newprocess.service.IcePutPactRecordService;
@Service
@RequiredArgsConstructor(onConstructor = @_(@Autowired))
public class IcePutPactRecordServiceImpl extends ServiceImpl<IcePutPactRecordDao, IcePutPactRecord> implements IcePutPactRecordService{

    private final IceBoxExtendDao iceBoxExtendDao;
    private final IcePutApplyDao icePutApplyDao;

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

}
