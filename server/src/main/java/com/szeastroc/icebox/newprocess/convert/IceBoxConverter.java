package com.szeastroc.icebox.newprocess.convert;

import com.szeastroc.icebox.enums.FreePayTypeEnum;
import com.szeastroc.icebox.newprocess.entity.*;
import com.szeastroc.icebox.newprocess.vo.IceBoxStoreVo;
import com.szeastroc.icebox.newprocess.vo.IceBoxVo;
import com.szeastroc.icebox.oldprocess.entity.IceEventRecord;

public class IceBoxConverter {

    public static IceBoxVo convertToVo(IceBox iceBox, IceBoxExtend iceBoxExtend, IceModel iceModel, IceBackApply iceBackApply) {
        IceBoxVo iceBoxVo = new IceBoxVo();
        iceBoxVo.setIceBoxId(iceBox.getId());
        iceBoxVo.setAssetId(iceBoxExtend.getAssetId());
        iceBoxVo.setChestName(iceBox.getChestName());
        iceBoxVo.setChestModel(iceModel.getChestModel());
        iceBoxVo.setChestNorm(iceBox.getChestNorm());
        iceBoxVo.setBrandName(iceBox.getBrandName());
        iceBoxVo.setDepositMoney(iceBox.getDepositMoney());
        iceBoxVo.setOpenTotal(iceBoxExtend.getOpenTotal());
        iceBoxVo.setQrCode(iceBoxExtend.getQrCode());
        iceBoxVo.setLastPutTime(iceBoxExtend.getLastPutTime());
        if(iceBackApply != null) {
            Integer examineStatus = iceBackApply.getExamineStatus();
            iceBoxVo.setBackStatus(examineStatus);
        }
        return iceBoxVo;
    }

    public static IceBoxVo convertToVo(IceBox iceBox, IceBoxExtend iceBoxExtend, IceModel iceModel, FreePayTypeEnum freePayTypeEnum) {
        IceBoxVo iceBoxVo = new IceBoxVo();
        iceBoxVo.setIceBoxId(iceBox.getId());
        iceBoxVo.setAssetId(iceBoxExtend.getAssetId());
        iceBoxVo.setChestName(iceBox.getChestName());
        iceBoxVo.setChestModel(iceModel.getChestModel());
        iceBoxVo.setChestNorm(iceBox.getChestNorm());
        iceBoxVo.setBrandName(iceBox.getBrandName());
        iceBoxVo.setDepositMoney(iceBox.getDepositMoney());
        iceBoxVo.setOpenTotal(iceBoxExtend.getOpenTotal());
        iceBoxVo.setQrCode(iceBoxExtend.getQrCode());
        iceBoxVo.setFreeType(freePayTypeEnum.getType());
        return iceBoxVo;
    }

    public static IceBoxStoreVo convertToStoreVo(IceBox iceBox, IceBoxExtend iceBoxExtend, IceModel iceModel, IcePutApplyRelateBox icePutApplyRelateBox, IceEventRecord iceEventRecord, IceBackApply iceBackApply) {
        IceBoxStoreVo iceBoxStoreVo = new IceBoxStoreVo();
        iceBoxStoreVo.setIceBoxVo(convertToVo(iceBox, iceBoxExtend, iceModel, iceBackApply));
        iceBoxStoreVo.setIcePutApplyRelateBoxVo(IcePutApplyRelateBoxConverter.convertToVo(icePutApplyRelateBox));
        iceBoxStoreVo.setIceEventRecord(iceEventRecord);
        return iceBoxStoreVo;
    }
}
