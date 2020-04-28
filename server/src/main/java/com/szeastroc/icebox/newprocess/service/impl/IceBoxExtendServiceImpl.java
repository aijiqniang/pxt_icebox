package com.szeastroc.icebox.newprocess.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.customer.client.FeignStoreClient;
import com.szeastroc.customer.common.vo.SessionStoreInfoVo;
import com.szeastroc.customer.common.vo.StoreInfoDtoVo;
import com.szeastroc.icebox.newprocess.dao.*;
import com.szeastroc.icebox.newprocess.entity.*;
import com.szeastroc.icebox.newprocess.vo.SimpleIceBoxDetailVo;
import com.szeastroc.icebox.oldprocess.dao.PactRecordDao;
import com.szeastroc.icebox.oldprocess.entity.PactRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.*;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.icebox.newprocess.service.IceBoxExtendService;

@Service
public class IceBoxExtendServiceImpl extends ServiceImpl<IceBoxExtendDao, IceBoxExtend> implements IceBoxExtendService {

    @Autowired
    private IceBoxExtendDao iceBoxExtendDao;

    @Autowired
    private IceBoxDao iceBoxDao;

    @Autowired
    private IceModelDao iceModelDao;

    @Autowired
    private FeignStoreClient feignStoreClient;

    @Resource
    private IcePutPactRecordDao icePutPactRecordDao;

    @Resource
    private IcePutApplyRelateBoxDao icePutApplyRelateBoxDao;


    @Override
    public SimpleIceBoxDetailVo getByAssetId(String assetId) {

        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectOne(Wrappers.<IceBoxExtend>lambdaQuery().eq(IceBoxExtend::getAssetId, assetId));

        Integer id = iceBoxExtend.getId();

        IceBox iceBox = iceBoxDao.selectById(id);
        Integer modelId = iceBox.getModelId();

        IceModel iceModel = iceModelDao.selectById(modelId);

        String storeNumber = iceBox.getPutStoreNumber();

        IcePutApplyRelateBox icePutApplyRelateBox = icePutApplyRelateBoxDao.selectOne(Wrappers.<IcePutApplyRelateBox>lambdaQuery()
                .eq(IcePutApplyRelateBox::getApplyNumber, iceBoxExtend.getLastApplyNumber())
                .eq(IcePutApplyRelateBox::getBoxId, id));

//        StoreInfoDtoVo storeInfoDtoVo = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(storeNumber));

        Map<String, SessionStoreInfoVo> map = FeignResponseUtil.getFeignData(feignStoreClient.getSessionStoreInfoVo(Collections.singletonList(storeNumber)));

        if(map == null) {
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }

        SessionStoreInfoVo sessionStoreInfoVo = map.get(storeNumber);

        return SimpleIceBoxDetailVo.builder()
                .id(iceBox.getId())
                .assetId(assetId)
                .chestModelId(iceModel.getId())
                .chestModel(iceModel.getChestModel())
                .chestName(iceModel.getChestName())
                .depositMoney(iceBox.getDepositMoney())
                .lastPutNumber(iceBoxExtend.getLastApplyNumber())
                .lastPutTime(iceBoxExtend.getLastPutTime())
                .putStoreNumber(storeNumber)
                .storeAddress(sessionStoreInfoVo.getParserAddress())
                .memberMobile(sessionStoreInfoVo.getMemberMobile())
                .memberName(sessionStoreInfoVo.getMemberName())
                .deptId(iceBox.getDeptId())
                .supplierId(iceBox.getSupplierId())
                .freeType(icePutApplyRelateBox.getFreeType())
                .build();
    }

    @Override
    public void advanceRefund(String assetId) {

        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectOne(Wrappers.<IceBoxExtend>lambdaQuery().eq(IceBoxExtend::getAssetId, assetId));

        if(iceBoxExtend == null) {
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }

        IcePutPactRecord icePutPactRecord = icePutPactRecordDao.selectOne(Wrappers.<IcePutPactRecord>lambdaQuery().eq(IcePutPactRecord::getApplyNumber, iceBoxExtend.getLastApplyNumber()));
        if(icePutPactRecord == null) {
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }
        icePutPactRecord.setPutExpireTime(new Date());
        icePutPactRecordDao.updateById(icePutPactRecord);

    }
}

