package com.szeastroc.icebox.newprocess.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.customer.client.FeignStoreClient;
import com.szeastroc.customer.client.FeignSupplierClient;
import com.szeastroc.customer.common.vo.SessionStoreInfoVo;
import com.szeastroc.customer.common.vo.SubordinateInfoVo;
import com.szeastroc.icebox.newprocess.dao.*;
import com.szeastroc.icebox.newprocess.entity.*;
import com.szeastroc.icebox.newprocess.service.IceBoxExtendService;
import com.szeastroc.icebox.newprocess.vo.SimpleIceBoxDetailVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

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

    @Resource
    private IceBackApplyDao iceBackApplyDao;

    @Resource
    private IceBackApplyRelateBoxDao iceBackApplyRelateBoxDao;

    @Resource
    private FeignSupplierClient feignSupplierClient;

    @Resource
    private IcePutApplyDao icePutApplyDao;


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


        SimpleIceBoxDetailVo simpleIceBoxDetailVo = SimpleIceBoxDetailVo.builder()
                .id(iceBox.getId())
                .assetId(assetId)
                .chestModelId(iceModel.getId())
                .chestModel(iceModel.getChestModel())
                .chestName(iceModel.getChestName())
                .depositMoney(iceBox.getDepositMoney())
                .lastPutNumber(iceBoxExtend.getLastApplyNumber())
                .lastPutTime(iceBoxExtend.getLastPutTime())
                .deptId(iceBox.getDeptId())
                .supplierId(iceBox.getSupplierId())
                .build();

        if (icePutApplyRelateBox != null) {
            simpleIceBoxDetailVo.setFreeType(icePutApplyRelateBox.getFreeType());
//            IcePutApply icePutApply = icePutApplyDao.selectOne(Wrappers.<IcePutApply>lambdaQuery().eq(IcePutApply::getApplyNumber, iceBoxExtend.getLastApplyNumber()));
            IceBackApply iceBackApply = iceBackApplyDao.selectOne(Wrappers.<IceBackApply>lambdaQuery().eq(IceBackApply::getOldPutId, icePutApplyRelateBox.getId()).ne(IceBackApply::getExamineStatus, 3));
            if (iceBackApply != null) {
                storeNumber = iceBackApply.getBackStoreNumber();
                IceBackApplyRelateBox iceBackApplyRelateBox = iceBackApplyRelateBoxDao.selectOne(Wrappers.<IceBackApplyRelateBox>lambdaQuery().eq(IceBackApplyRelateBox::getBoxId, id).eq(IceBackApplyRelateBox::getApplyNumber, iceBackApply.getApplyNumber()));
                if (iceBackApplyRelateBox != null) {
                    Integer backSupplierId = iceBackApplyRelateBox.getBackSupplierId();
                    simpleIceBoxDetailVo.setBackType(iceBackApplyRelateBox.getBackType());
                    SubordinateInfoVo subordinateInfoVo = FeignResponseUtil.getFeignData(feignSupplierClient.findSupplierBySupplierId(backSupplierId));
                    if (subordinateInfoVo != null) {
                        simpleIceBoxDetailVo.setNewSupplierId(subordinateInfoVo.getId());
                        simpleIceBoxDetailVo.setNewSupplierName(subordinateInfoVo.getName());
                        simpleIceBoxDetailVo.setNewSupplierNumber(subordinateInfoVo.getNumber());
                    }
                }
            }
        }
        Map<String, SessionStoreInfoVo> map = FeignResponseUtil.getFeignData(feignStoreClient.getSessionStoreInfoVo(Collections.singletonList(storeNumber)));
        if (map != null) {
            SessionStoreInfoVo sessionStoreInfoVo = map.get(storeNumber);
            if (null != sessionStoreInfoVo && StringUtils.isNotBlank(sessionStoreInfoVo.getStoreNumber())) {
                simpleIceBoxDetailVo.setPutStoreNumber(storeNumber);
                simpleIceBoxDetailVo.setStoreAddress(sessionStoreInfoVo.getParserAddress());
                simpleIceBoxDetailVo.setStoreName(sessionStoreInfoVo.getStoreName());
                simpleIceBoxDetailVo.setMemberMobile(sessionStoreInfoVo.getMemberMobile());
                simpleIceBoxDetailVo.setMemberName(sessionStoreInfoVo.getMemberName());
            } else {
                // 可能是配送商
                SubordinateInfoVo subordinateInfoVo = FeignResponseUtil.getFeignData(feignSupplierClient.findByNumber(storeNumber));
                if (null != subordinateInfoVo && StringUtils.isNotBlank(subordinateInfoVo.getNumber())) {
                    simpleIceBoxDetailVo.setPutStoreNumber(storeNumber);
                    simpleIceBoxDetailVo.setStoreAddress(subordinateInfoVo.getAddress());
                    simpleIceBoxDetailVo.setStoreName(subordinateInfoVo.getName());
                    simpleIceBoxDetailVo.setMemberMobile(subordinateInfoVo.getLinkmanMobile());
                    simpleIceBoxDetailVo.setMemberName(subordinateInfoVo.getLinkman());
                } else {
                    throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
                }
            }
        }
        return simpleIceBoxDetailVo;
    }

    @Override
    public void advanceRefund(String assetId) {

        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectOne(Wrappers.<IceBoxExtend>lambdaQuery().eq(IceBoxExtend::getAssetId, assetId));

        if (iceBoxExtend == null) {
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }

        IcePutPactRecord icePutPactRecord = icePutPactRecordDao.selectOne(Wrappers.<IcePutPactRecord>lambdaQuery().eq(IcePutPactRecord::getApplyNumber, iceBoxExtend.getLastApplyNumber()));
        if (icePutPactRecord == null) {
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }
        icePutPactRecord.setPutExpireTime(new Date());
        icePutPactRecordDao.updateById(icePutPactRecord);

    }
}

