package com.szeastroc.icebox.newprocess.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.BeanProperty;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.entity.customer.vo.MemberInfoVo;
import com.szeastroc.common.entity.customer.vo.StoreInfoDtoVo;
import com.szeastroc.common.entity.customer.vo.StoreRequest;
import com.szeastroc.common.entity.customer.vo.SupplierInfo;
import com.szeastroc.common.entity.user.vo.UserInfoVo;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.feign.customer.FeignStoreClient;
import com.szeastroc.common.feign.customer.FeignStoreRelateMemberClient;
import com.szeastroc.common.feign.customer.FeignSupplierClient;
import com.szeastroc.common.feign.user.FeignCacheClient;
import com.szeastroc.common.feign.user.FeignUserClient;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.config.DmsUrlConfig;
import com.szeastroc.icebox.constant.IceBoxConstant;
import com.szeastroc.icebox.newprocess.dao.*;
import com.szeastroc.icebox.newprocess.entity.*;
import com.szeastroc.icebox.newprocess.enums.PutStatus;
import com.szeastroc.icebox.newprocess.enums.SendDmsIceboxTypeEnum;
import com.szeastroc.icebox.newprocess.service.IceBoxRelateDmsService;
import com.szeastroc.icebox.newprocess.vo.IceBoxRelateDmsVo;
import com.szeastroc.icebox.util.SendRequestUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.*;

/**
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @_(@Autowired))
public class IceBoxRelateDmsServiceImpl extends ServiceImpl<IceBoxRelateDmsDao, IceBoxRelateDms> implements IceBoxRelateDmsService{

    private final IceBoxDao iceBoxDao;
    private final IceBoxExtendDao iceBoxExtendDao;
    private final IceModelDao iceModelDao;
    private final IceBoxRelateDmsDao iceBoxRelateDmsDao;
    private final IceBoxPutReportDao iceBoxPutReportDao;
    private final PutStoreRelateModelDao putStoreRelateModelDao;
    private final OldIceBoxSignNoticeDao oldIceBoxSignNoticeDao;
    private final IcePutApplyDao icePutApplyDao;
    private final FeignCacheClient feignCacheClient;
    private final FeignStoreRelateMemberClient feignStoreRelateMemberClient;
    private final FeignStoreClient feignStoreClient;
    private final FeignUserClient feignUserClient;
    private final FeignSupplierClient feignSupplierClient;
    private final DmsUrlConfig dmsUrlConfig;


    @Override
    public IceBoxRelateDmsVo findById(Integer id) {
        IceBoxRelateDms iceBoxRelateDms = iceBoxRelateDmsDao.selectById(id);
        IceBoxRelateDmsVo returnVo = new IceBoxRelateDmsVo();
        if(iceBoxRelateDms != null){
            returnVo.setPutType("协议投放");
            BeanUtils.copyProperties(iceBoxRelateDms,returnVo);
            if(iceBoxRelateDms.getModelId() != null && iceBoxRelateDms.getModelId() > 0){
                //冰柜信息
                IceBox iceBox = iceBoxDao.selectOne(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getModelId,iceBoxRelateDms.getModelId()).last("limit 1"));
                if(iceBox != null){
                    returnVo.setChestName(iceBox.getChestName());
                    returnVo.setModelName(iceBox.getModelName());
                    returnVo.setChestNorm(iceBox.getChestNorm());
                    returnVo.setBrandName(iceBox.getBrandName());
                }
            }
            if(StringUtils.isNotEmpty(iceBoxRelateDms.getPutStoreNumber())){
                //门店信息
                List<String> storeNumbers = new ArrayList<>();
                storeNumbers.add(iceBoxRelateDms.getPutStoreNumber());
                CommonResponse<Map<String, StoreInfoDtoVo>> response = feignStoreClient.getMapByStoreNumbers(new StoreRequest(storeNumbers));
                if(response != null && response.getData() != null){
                    Map<String, StoreInfoDtoVo> map = response.getData();
                    StoreInfoDtoVo storeInfoDtoVo = map.get(iceBoxRelateDms.getPutStoreNumber());
                    if(storeInfoDtoVo != null){
                        returnVo.setPutStoreName(storeInfoDtoVo.getStoreName());
                        returnVo.setAddress(storeInfoDtoVo.getAddress());
                    }
                }
                //店主信息
                CommonResponse<MemberInfoVo> keeperResponse = feignStoreRelateMemberClient.getShopKeeperByStoreNumber(iceBoxRelateDms.getPutStoreNumber());
                if(keeperResponse != null && keeperResponse.getData() != null){
                    MemberInfoVo memberInfoVo = keeperResponse.getData();
                    if(memberInfoVo != null){
                        returnVo.setShopkeeper(memberInfoVo.getName());
                        returnVo.setShopkeeperPhoneNumber(memberInfoVo.getMobile());
                    }
                }
                //主业务员信息
                CommonResponse<Integer> mainSaleManResponse = feignStoreClient.getMainSaleManId(iceBoxRelateDms.getPutStoreNumber());
                if(mainSaleManResponse != null && mainSaleManResponse.getData() != null){
                    Integer mainSaleManId = mainSaleManResponse.getData();
                    if(mainSaleManId != null && mainSaleManId > 0){
                        CommonResponse<UserInfoVo> userResponse = feignUserClient.findById(mainSaleManId);
                        if(userResponse != null && userResponse.getData() != null){
                            UserInfoVo mainSaleMan = userResponse.getData();
                            returnVo.setSaleManId(mainSaleManId);
                            if(mainSaleMan != null){
                                returnVo.setSaleManName(mainSaleMan.getRealname());
                                returnVo.setSaleManPhoneNumber(mainSaleMan.getMobile());
                            }
                        }
                    }
                }
            }
            if(iceBoxRelateDms.getPutStoreRelateModelId() != null && iceBoxRelateDms.getPutStoreRelateModelId() > 0){
                IceBoxPutReport report = iceBoxPutReportDao.selectOne(Wrappers.<IceBoxPutReport>lambdaQuery().eq(IceBoxPutReport::getPutStoreModelId,iceBoxRelateDms.getPutStoreRelateModelId()).last("limit 1"));
                if(report != null){
                    //营销区域等
                    returnVo.setFreeType(report.getFreeType());
                    returnVo.setDepositMoney(report.getDepositMoney());
                    returnVo.setHeadquartersDeptName(report.getHeadquartersDeptName());
                    returnVo.setBusinessDeptName(report.getBusinessDeptName());
                    returnVo.setServiceDeptName(report.getServiceDeptName());
                    returnVo.setRegionDeptName(report.getRegionDeptName());
                    returnVo.setGroupDeptName(report.getGroupDeptName());
                }
            }
        }
        return returnVo;
    }

    @Override
    public void confirmAccept(IceBoxRelateDmsVo iceBoxRelateDmsVo) {
        IceBoxRelateDms iceBoxRelateDms = iceBoxRelateDmsDao.selectById(iceBoxRelateDmsVo.getId());
        Map params = new HashMap();
        if(iceBoxRelateDms != null){
            if(iceBoxRelateDms.getPutStoreRelateModelId() != null && iceBoxRelateDms.getPutStoreRelateModelId() > 0){
                /**
                 * 去putrelatestoremodel找到数据  如果商户已签收就不让接单
                 */
                PutStoreRelateModel relateModel = putStoreRelateModelDao.selectById(iceBoxRelateDms.getPutStoreRelateModelId());

                if(relateModel != null){
                    if(relateModel.getSupplierId() != null && relateModel.getSupplierId() > 0){
                        SupplierInfo supplierInfo = FeignResponseUtil.getFeignData(feignSupplierClient.findInfoById(relateModel.getSupplierId()));
                        params.put("pxtNumber",supplierInfo.getNumber());
                    }
                    if(PutStatus.FINISH_PUT.getStatus().equals(relateModel.getPutStatus())){
                        params.put("id",iceBoxRelateDms.getId()+"");
                        SendRequestUtils.sendPostRequest(dmsUrlConfig.getToDmsUrl()+"/drpOpen/pxtAndIceBox/updateBacklogStatus",params);
                        throw new ImproperOptionException("商户已签收");
                    }
                }

                /**
                 * 逻辑修改 不去修改 报表内容   可以跳过配送直接签收
                 */
                /*PutStoreRelateModel putStoreRelateModel = PutStoreRelateModel.builder()
                        .id(iceBoxRelateDms.getPutStoreRelateModelId())
                        .putStatus(PutStatus.IS_ACCEPT.getStatus())
                        .build();
                putStoreRelateModelDao.updateById(putStoreRelateModel);

                IceBoxPutReport report = new IceBoxPutReport();
                report.setPutStoreModelId(iceBoxRelateDms.getPutStoreRelateModelId());
                report.setPutStatus(PutStatus.IS_ACCEPT.getStatus());
                iceBoxPutReportDao.update(report,Wrappers.<IceBoxPutReport>lambdaUpdate()
                        .eq(IceBoxPutReport::getPutStoreModelId,report.getPutStoreModelId())
                        .set(IceBoxPutReport::getPutStatus,report.getPutStatus()));*/
            }
            BeanUtils.copyProperties(iceBoxRelateDmsVo,iceBoxRelateDms);
            iceBoxRelateDms.setUpdateTime(new Date());
            iceBoxRelateDms.setAcceptTime(new Date());
            iceBoxRelateDms.setPutstatus(PutStatus.IS_ACCEPT.getStatus());
            iceBoxRelateDmsDao.updateById(iceBoxRelateDms);
            /**
             * 发送dms送达通知
             */
            IceBoxRelateDms dmsVo = iceBoxRelateDmsDao.selectById(iceBoxRelateDmsVo.getId());
            params.put("relateCode",iceBoxRelateDms.getId()+"");
            if(dmsVo.getType() == 1){
                //投放
                params.put("type", SendDmsIceboxTypeEnum.PUT_ARRIVRD.getCode()+"");
                SendRequestUtils.sendPostRequest(dmsUrlConfig.getToDmsUrl()+"/drpOpen/pxtAndIceBox/pxtToDmsIceBoxMsg",params);
            }else if(dmsVo.getType() == 2){
                //退还
                params.put("type", SendDmsIceboxTypeEnum.BACK_ARRIVED.getCode()+"");
                SendRequestUtils.sendPostRequest(dmsUrlConfig.getToDmsUrl()+"/drpOpen/pxtAndIceBox/pxtToDmsIceBoxMsg",params);
            }
        }else{
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }
    }

    @Override
    public IceBox getIceBoxInfoByQrcode(String qrcode) {
        if(StringUtils.isNotEmpty(qrcode)){
            IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectOne(Wrappers.<IceBoxExtend>lambdaQuery().eq(IceBoxExtend::getQrCode, qrcode));
            if(iceBoxExtend != null && iceBoxExtend.getId() != null && iceBoxExtend.getId() > 0){
                IceBox iceBox = iceBoxDao.selectById(iceBoxExtend.getId());
                return iceBox;
            }else {
                throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
            }
        }
        return null;
    }

    @Override
    public void confirmArrvied(IceBoxRelateDmsVo iceBoxRelateDmsVo) {
        if (iceBoxRelateDmsVo != null && StringUtils.isNotEmpty(iceBoxRelateDmsVo.getIceBoxAssetId()) && iceBoxRelateDmsVo.getId() != null && iceBoxRelateDmsVo.getId() > 0) {
            Map params = new HashMap();

            IceBoxRelateDms relateDms = iceBoxRelateDmsDao.selectById(iceBoxRelateDmsVo.getId());
            if (relateDms != null) {
                /**
                 * 修改报表状态
                 */
                if (relateDms.getPutStoreRelateModelId() != null && relateDms.getPutStoreRelateModelId() > 0) {
                    /**
                     * 去putrelatestoremodel找到数据  如果商户已签收就不让接单
                     */
                    PutStoreRelateModel relateModel = putStoreRelateModelDao.selectById(relateDms.getPutStoreRelateModelId());
                    if (relateModel != null) {
                        if (relateModel.getSupplierId() != null && relateModel.getSupplierId() > 0) {
                            SupplierInfo supplierInfo = FeignResponseUtil.getFeignData(feignSupplierClient.findInfoById(relateModel.getSupplierId()));
                            params.put("pxtNumber", supplierInfo.getNumber());
                        }
                        if (PutStatus.FINISH_PUT.getStatus().equals(relateModel.getPutStatus())) {
                            params.put("id", relateDms.getId() + "");
                            SendRequestUtils.sendPostRequest(dmsUrlConfig.getToDmsUrl() + "/drpOpen/pxtAndIceBox/updateBacklogStatus", params);
                            throw new ImproperOptionException("商户已签收");
                        }
                    }
                    /**
                     * 校验是否修改资产编号
                     */

                    IceBox iceBox = iceBoxDao.selectOne(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getSupplierId,relateDms.getSupplierId()).eq(IceBox::getModelId,relateDms.getModelId()).eq(IceBox::getStatus,1).eq(IceBox::getPutStatus,PutStatus.NO_PUT.getStatus()).eq(IceBox::getAssetId, iceBoxRelateDmsVo.getIceBoxAssetId()).last("limit 1"));
                    if (iceBox == null) {
                        throw new ImproperOptionException("该冰柜编号不存在");
                    }

                    if(StringUtils.isNotEmpty(iceBoxRelateDmsVo.getRemark())){
                        relateDms.setRemark(iceBoxRelateDmsVo.getRemark());
                    }
                    if(StringUtils.isNotEmpty(iceBoxRelateDmsVo.getPhoto())){
                        relateDms.setPhoto(iceBoxRelateDmsVo.getPhoto());
                    }
                    relateDms.setIceBoxId(iceBoxRelateDmsVo.getIceBoxId());
                    relateDms.setIceBoxAssetId(iceBoxRelateDmsVo.getIceBoxAssetId());
                    relateDms.setPutstatus(PutStatus.IS_ARRIVED.getStatus());
                    relateDms.setUpdateTime(new Date());
                    relateDms.setArrviedTime(new Date());
                    iceBoxRelateDmsDao.updateById(relateDms);
                    /*                /**
                     * 发送dms送达通知
                     */
                    params.put("id", relateDms.getId() + "");
                    SendRequestUtils.sendPostRequest(dmsUrlConfig.getToDmsUrl() + "/drpOpen/pxtAndIceBox/updateBacklogStatus", params);
                } else {
                    throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
                }

            }
        }
    }
}




