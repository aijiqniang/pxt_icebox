package com.szeastroc.icebox.oldprocess.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.enums.CommonStatus;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.oldprocess.dao.ClientInfoDao;
import com.szeastroc.icebox.oldprocess.dao.IceChestPutRecordDao;
import com.szeastroc.icebox.oldprocess.dao.PactRecordDao;
import com.szeastroc.icebox.oldprocess.entity.ClientInfo;
import com.szeastroc.icebox.oldprocess.entity.IceChestPutRecord;
import com.szeastroc.icebox.oldprocess.entity.PactRecord;
import com.szeastroc.icebox.enums.ClientType;
import com.szeastroc.icebox.oldprocess.service.PactRecordService;
import com.szeastroc.icebox.oldprocess.vo.ClientInfoRequest;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Tulane
 * 2019/5/29
 */
@Service
public class PactRecordServiceImpl extends ServiceImpl<PactRecordDao, PactRecord> implements PactRecordService {

    @Autowired
    private ClientInfoDao clientInfoDao;
    @Autowired
    private PactRecordDao pactRecordDao;
    @Autowired
    private IceChestPutRecordDao iceChestPutRecordDao;

    @Transactional(value = "transactionManager")
    @Override
    public CommonResponse<String> createPactRecord(ClientInfoRequest clientInfoRequest) {
        //查询对应客户的鹏讯通id是否存在
        clientInfoDao.selectOne(Wrappers.<ClientInfo>lambdaQuery().eq(ClientInfo::getClientNumber, clientInfoRequest.getClientNumber()));
        ClientInfo clientInfo = clientInfoDao.selectOne(Wrappers.<ClientInfo>lambdaQuery().eq(ClientInfo::getClientNumber, clientInfoRequest.getClientNumber()));
        if(clientInfo == null){
            //创建新的
            clientInfo = new ClientInfo(clientInfoRequest.getClientName(), ClientType.IS_STORE.getType(), clientInfoRequest.getClientNumber(), clientInfoRequest.getClientPlace(),
                    clientInfoRequest.getClientLevel(), CommonStatus.VALID.getStatus(), clientInfoRequest.getContactName(), clientInfoRequest.getContactMobile(), Integer.valueOf(clientInfoRequest.getMarketAreaId()));
            clientInfoDao.insert(clientInfo);
        }
        //查询客户id是否存在协议记录
        PactRecord pactRecord = pactRecordDao.selectOne(Wrappers.<PactRecord>lambdaQuery()
                .eq(PactRecord::getClientId, clientInfo.getId())
                .eq(PactRecord::getChestId, clientInfoRequest.getIceChestId()));
        if(pactRecord == null){
            //创建协议记录
            pactRecordDao.insert(new PactRecord(clientInfo.getId(), Integer.parseInt(clientInfoRequest.getIceChestId())));
        }
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }

    @Transactional(value = "transactionManager")
    @Override
    public CommonResponse<String> repairPactRecord() {

        List<PactRecord> list = pactRecordDao.selectList(Wrappers.<PactRecord>lambdaQuery().isNull(PactRecord::getChestId));
        if(CollectionUtil.isNotEmpty(list)){
            for (PactRecord pactRecord : list) {

                IceChestPutRecord iceChestPutRecord = iceChestPutRecordDao.selectOne(Wrappers.<IceChestPutRecord>lambdaQuery().eq(IceChestPutRecord::getReceiveClientId, pactRecord.getClientId()));
                if(iceChestPutRecord != null){

                    pactRecord.setChestId(iceChestPutRecord.getChestId());
                    pactRecord.setPutId(iceChestPutRecord.getId());
                    pactRecord.setPutTime(iceChestPutRecord.getCreateTime());

                    DateTime startTime = new DateTime(pactRecord.getPutTime());
                    DateTime endTime = startTime.plusYears(1);
                    pactRecord.setPutExpireTime(endTime.toDate());

                    pactRecordDao.updateById(pactRecord);
                }
            }

//            updateBatchById(list);
        }

        List<PactRecord> list2 = pactRecordDao.selectList(Wrappers.<PactRecord>lambdaQuery().isNull(PactRecord::getChestId));
        if(CollectionUtil.isNotEmpty(list2)) {
            removeByIds(list2.stream().map(PactRecord::getId).collect(Collectors.toList()));
        }
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }
}
