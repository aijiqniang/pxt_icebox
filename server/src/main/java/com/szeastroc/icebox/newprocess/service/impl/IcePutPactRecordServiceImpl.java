package com.szeastroc.icebox.newprocess.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.entity.customer.dto.CustomerLabelDetailDto;
import com.szeastroc.common.entity.customer.vo.SubordinateInfoVo;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.feign.customer.FeignCusLabelClient;
import com.szeastroc.common.feign.customer.FeignSupplierClient;
import com.szeastroc.common.utils.ExecutorServiceFactory;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.icebox.enums.ExamineStatusEnum;
import com.szeastroc.icebox.enums.FreePayTypeEnum;
import com.szeastroc.icebox.enums.ServiceType;
import com.szeastroc.icebox.newprocess.dao.*;
import com.szeastroc.icebox.newprocess.entity.*;
import com.szeastroc.icebox.newprocess.enums.PutStatus;
import com.szeastroc.icebox.newprocess.enums.RecordStatus;
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
import java.util.List;
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
    private final IceTransferRecordDao iceTransferRecordDao;
    private final PutStoreRelateModelDao putStoreRelateModelDao;
    private final ApplyRelatePutStoreModelDao applyRelatePutStoreModelDao;
    private final IcePutPactRecordDao icePutPactRecordDao;
    private final IcePutOrderDao icePutOrderDao;

    @Override
    public void createPactRecord(ClientInfoRequest clientInfoRequest) {
        // ???????????????????????????????????????
        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(clientInfoRequest.getIceChestId());

        PutStoreRelateModel relateModel = putStoreRelateModelDao.selectOne(Wrappers.<PutStoreRelateModel>lambdaQuery().eq(PutStoreRelateModel::getPutStoreNumber, clientInfoRequest.getClientNumber()).eq(PutStoreRelateModel::getPutStatus, PutStatus.DO_PUT.getStatus()).eq(PutStoreRelateModel::getExamineStatus, ExamineStatusEnum.IS_PASS.getStatus()).eq(PutStoreRelateModel::getStatus, 1).orderByDesc(PutStoreRelateModel::getId).last("limit 1"));
        if(relateModel == null){
            throw new ImproperOptionException("????????????????????????");
        }
        ApplyRelatePutStoreModel applyRelatePutStoreModel = applyRelatePutStoreModelDao.selectOne(Wrappers.<ApplyRelatePutStoreModel>lambdaQuery().eq(ApplyRelatePutStoreModel::getStoreRelateModelId, relateModel.getId()).last("limit 1"));
        if(applyRelatePutStoreModel == null){
            throw new ImproperOptionException("????????????????????????");
        }
        IcePutApply icePutApply = icePutApplyDao.selectOne(Wrappers.<IcePutApply>lambdaQuery().eq(IcePutApply::getApplyNumber, applyRelatePutStoreModel.getApplyNumber()));
        if (icePutApply == null) {
            throw new ImproperOptionException("???????????????????????????");
        }

        /**???????????? ???????????????????????????????????????
        /*IceTransferRecord transferRecord = iceTransferRecordDao.selectOne(Wrappers.<IceTransferRecord>lambdaQuery().eq(IceTransferRecord::getBoxId, iceBox.getId()).eq(IceTransferRecord::getApplyNumber, icePutApply.getApplyNumber()));
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
            log.info("applyNumber-->???{}???????????????????????????",icePutApply.getApplyNumber());
        }*/

        // ????????????
        IcePutPactRecord icePutPactRecord = getOne(Wrappers.<IcePutPactRecord>lambdaQuery()
                .eq(IcePutPactRecord::getApplyNumber, icePutApply.getApplyNumber())
                .eq(IcePutPactRecord::getBoxId, iceBoxExtend.getId())
                .eq(IcePutPactRecord::getStoreNumber, icePutApply.getPutStoreNumber()));

        if(Objects.isNull(icePutPactRecord)){
            saveIcePutRecord(iceBoxExtend, icePutApply);
            return;
        }
        icePutPactRecord.setPutTime(icePutApply.getCreatedTime()); // TODO ????????????????????? ???????????? ?????? ????????????
        icePutPactRecord.setPutExpireTime(expireTimeRule(icePutPactRecord.getCreatedTime()));
        icePutPactRecord.setUpdatedBy(0);
        icePutPactRecord.setUpdatedTime(new Date());
        updateById(icePutPactRecord);

        // ????????????
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
        icePutPactRecord.setPutTime(icePutApply.getCreatedTime()); // TODO ????????????????????? ???????????? ?????? ????????????
        icePutPactRecord.setPutExpireTime(expireTimeRule(icePutPactRecord.getPutTime()));
        icePutPactRecord.setCreatedBy(0);
        icePutPactRecord.setCreatedTime(new Date());
        icePutPactRecord.setUpdatedBy(0);
        icePutPactRecord.setUpdatedTime(icePutPactRecord.getCreatedTime());
        save(icePutPactRecord);
        // ????????????
        CompletableFuture.runAsync(() -> {
           addLabel(icePutPactRecord,iceBoxExtend.getAssetId());
        }, ExecutorServiceFactory.getInstance());
    }

    @Override
    public boolean checkPactRecordByBoxId(Integer iceBoxId,String storeNumber,String assetId) {
        // ???????????????????????????????????????
        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(iceBoxId);
        IceBox applyBox = iceBoxDao.selectById(iceBoxId);
        //IcePutApply icePutApply = icePutApplyDao.selectOne(Wrappers.<IcePutApply>lambdaQuery().eq(IcePutApply::getApplyNumber, iceBoxExtend.getLastApplyNumber()));
        PutStoreRelateModel relateModel = putStoreRelateModelDao.selectOne(Wrappers.<PutStoreRelateModel>lambdaQuery().eq(PutStoreRelateModel::getSupplierId,applyBox.getSupplierId()).eq(PutStoreRelateModel::getModelId,applyBox.getModelId()).eq(PutStoreRelateModel::getPutStoreNumber, storeNumber).eq(PutStoreRelateModel::getPutStatus, PutStatus.DO_PUT.getStatus()).eq(PutStoreRelateModel::getExamineStatus, ExamineStatusEnum.IS_PASS.getStatus()).eq(PutStoreRelateModel::getStatus, 1).orderByDesc(PutStoreRelateModel::getId).last("limit 1"));
        if(relateModel == null){
            throw new ImproperOptionException("????????????????????????");
        }
        ApplyRelatePutStoreModel applyRelatePutStoreModel = applyRelatePutStoreModelDao.selectOne(Wrappers.<ApplyRelatePutStoreModel>lambdaQuery().eq(ApplyRelatePutStoreModel::getStoreRelateModelId, relateModel.getId()).last("limit 1"));
        if(applyRelatePutStoreModel == null){
            throw new ImproperOptionException("????????????????????????");
        }
        IcePutApply icePutApply = icePutApplyDao.selectOne(Wrappers.<IcePutApply>lambdaQuery().eq(IcePutApply::getApplyNumber, applyRelatePutStoreModel.getApplyNumber()));
        if (icePutApply == null) {
            throw new ImproperOptionException("???????????????????????????");
        }


        // ????????????
        /*int count = count(Wrappers.<IcePutPactRecord>lambdaQuery()
                .eq(IcePutPactRecord::getApplyNumber, icePutApply.getApplyNumber())
                .eq(IcePutPactRecord::getBoxId, iceBoxExtend.getId())
                .eq(IcePutPactRecord::getStoreNumber, icePutApply.getPutStoreNumber()));*/
        List<IcePutPactRecord> icePutPactRecords = icePutPactRecordDao.selectList(Wrappers.<IcePutPactRecord>lambdaQuery()
                .eq(IcePutPactRecord::getApplyNumber, icePutApply.getApplyNumber())
                .eq(IcePutPactRecord::getBoxId, iceBoxExtend.getId())
                .eq(IcePutPactRecord::getStoreNumber, icePutApply.getPutStoreNumber())
                .orderByDesc(IcePutPactRecord::getId));
        if( icePutPactRecords.size() <= 0){
            return false;
        }
        /**
         * ????????????????????????????????? ??????id???
         */
        IceBox iceBox = iceBoxDao.selectOne(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getSupplierId, relateModel.getSupplierId()).eq(IceBox::getModelId, relateModel.getModelId()).eq(IceBox::getStatus, 1).eq(IceBox::getPutStatus, PutStatus.NO_PUT.getStatus()).eq(IceBox::getAssetId,assetId));
        if(iceBox == null){
            throw new ImproperOptionException("??????????????????????????????");
        }
        /**
         * ???????????????
         */
        if(! iceBox.getId().equals(iceBoxId)){
            IcePutPactRecord updateOne = new IcePutPactRecord();
            updateOne.setId(icePutPactRecords.get(0).getId());
            updateOne.setBoxId(iceBox.getId());
            icePutPactRecordDao.updateById(updateOne);
        }
        return icePutPactRecords.size() > 0;
    }

    private void addLabel(IcePutPactRecord icePutPactRecord, String assetId) {
        Date putTime = icePutPactRecord.getPutTime();
        Date putExpireTime = icePutPactRecord.getPutExpireTime();
        CustomerLabelDetailDto customerLabelDetailDto = new CustomerLabelDetailDto();
        customerLabelDetailDto.setLabelId(9999);
        customerLabelDetailDto.setCreateTime(putTime);
        customerLabelDetailDto.setCustomerNumber(icePutPactRecord.getStoreNumber());
        customerLabelDetailDto.setCreateBy(0);
        customerLabelDetailDto.setCreateByName("??????");
        customerLabelDetailDto.setPutProject("??????");
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
