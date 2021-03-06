package com.szeastroc.icebox.oldprocess.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.exception.NormalOptionException;
import com.szeastroc.icebox.oldprocess.dao.*;
import com.szeastroc.icebox.oldprocess.entity.*;
import com.szeastroc.icebox.enums.ResultEnum;
import com.szeastroc.icebox.oldprocess.service.IceChestInfoService;
import com.szeastroc.icebox.oldprocess.vo.IceChestInfoExcelVo;
import com.szeastroc.icebox.oldprocess.vo.IceChestResponse;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

/**
 * Created by Tulane
 * 2019/5/21
 */
@Service
public class IceChestInfoServiceImpl extends ServiceImpl<IceChestInfoDao, IceChestInfo> implements IceChestInfoService {

    private final ClientInfoDao clientInfoDao;
    private final IceChestInfoDao iceChestInfoDao;
    private final IceChestInfoImportDao iceChestInfoImportDao;
    private final IceEventRecordDao iceEventRecordDao;
    @Autowired
    private IceChestPutRecordDao iceChestPutRecordDao;

    @Autowired
    public IceChestInfoServiceImpl(ClientInfoDao clientInfoDao, IceChestInfoDao iceChestInfoDao, IceChestInfoImportDao iceChestInfoImportDao, IceEventRecordDao iceEventRecordDao) {
        this.clientInfoDao = clientInfoDao;
        this.iceChestInfoDao = iceChestInfoDao;
        this.iceChestInfoImportDao = iceChestInfoImportDao;
        this.iceEventRecordDao = iceEventRecordDao;
    }

    @Override
    public IceChestResponse getIceChestByClientNumber(String clientNumber) throws NormalOptionException {
        ClientInfo clientInfo = clientInfoDao.selectOne(Wrappers.<ClientInfo>lambdaQuery().eq(ClientInfo::getClientNumber, clientNumber));
        if (clientInfo == null) {
            throw new NormalOptionException(ResultEnum.CLIENT_IS_NOT_REGISTER.getCode(), ResultEnum.CLIENT_IS_NOT_REGISTER.getMessage());
        }
        IceChestInfo iceChestInfo = iceChestInfoDao.selectOne(Wrappers.<IceChestInfo>lambdaQuery().eq(IceChestInfo::getClientId, clientInfo.getId()));
        if (iceChestInfo == null) {
            throw new NormalOptionException(ResultEnum.CLIENT_ICECHEST_IS_NOT_PUT.getCode(), ResultEnum.CLIENT_ICECHEST_IS_NOT_PUT.getMessage());
        }
        DateTime now = new DateTime();
        Date todayStart = now.withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0).toDate();
        Date todayEnd = now.withHourOfDay(23).withMinuteOfHour(59).withSecondOfMinute(59).toDate();

        IceEventRecord iceEventRecord = iceEventRecordDao.selectOne(Wrappers.<IceEventRecord>lambdaQuery().eq(IceEventRecord::getAssetId, iceChestInfo.getAssetId()).between(IceEventRecord::getOccurrenceTime,todayStart,todayEnd).orderByDesc(IceEventRecord::getCreateTime).last("limit 1"));

        IceChestPutRecord iceChestPutRecord = iceChestPutRecordDao.selectById(iceChestInfo.getLastPutId());
        return new IceChestResponse(iceChestInfo, clientInfo, iceEventRecord, iceChestPutRecord);
    }

    @Override
    public IceChestResponse getIceChestByExternalId(String externalId) throws NormalOptionException, ImproperOptionException {
        IceChestInfo iceChestInfo = iceChestInfoDao.selectOne(Wrappers.<IceChestInfo>lambdaQuery().eq(IceChestInfo::getExternalId, externalId));
        if (iceChestInfo == null) {
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }
        ClientInfo clientInfo = clientInfoDao.selectById(iceChestInfo.getClientId());
        return new IceChestResponse(iceChestInfo, clientInfo);
    }

    @Override
    public IceChestResponse getIceChestByQrcode(String qrcode, String clientNumber) throws NormalOptionException, ImproperOptionException {
        IceChestInfo iceChestInfo = iceChestInfoDao.selectOne(Wrappers.<IceChestInfo>lambdaQuery().eq(IceChestInfo::getQrCode, qrcode));
        if (iceChestInfo == null) {
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }
        ClientInfo clientInfo = clientInfoDao.selectById(iceChestInfo.getClientId());
        ClientInfo currentClientInfo = clientInfoDao.selectOne(Wrappers.<ClientInfo>lambdaQuery().eq(ClientInfo::getClientNumber, clientNumber));
        return new IceChestResponse(iceChestInfo, clientInfo, currentClientInfo);
    }

    @Override
    public String importIceInfoExcelVo(List<IceChestInfoExcelVo> list, IceChestInfoImport iceChestInfoImport) {
        IceChestInfoImport blackImport = iceChestInfoImportDao.selectOne(Wrappers.<IceChestInfoImport>lambdaQuery().eq(IceChestInfoImport::getName, iceChestInfoImport.getName()));
        if (null != blackImport) {
            return "?????????????????????";
        }
        //????????????????????????
        int totalNum = list.size();
        int successNum = 0;
        //??????????????????
        iceChestInfoImport.setTotalNum(totalNum);
        iceChestInfoImport.setCreateTime(new Timestamp(System.currentTimeMillis()));
        iceChestInfoImport.setStatus(1);
        iceChestInfoImport.setSuccessNum(successNum);
        iceChestInfoImportDao.insert(iceChestInfoImport);
        IceChestInfoImportThread iceChestInfoImportThread = new IceChestInfoImportThread(list, iceChestInfoImport.getId());
        Thread thread = new Thread(iceChestInfoImportThread);
        thread.start();
        return "success";
    }
}
