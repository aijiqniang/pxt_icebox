package com.szeastroc.icebox.newprocess.convert;

import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.entity.IceBoxExtend;
import com.szeastroc.icebox.newprocess.entity.IceModel;
import com.szeastroc.icebox.newprocess.entity.IcePutApplyRelateBox;
import com.szeastroc.icebox.newprocess.vo.IceBoxStoreVo;
import com.szeastroc.icebox.newprocess.vo.IceBoxVo;
import com.szeastroc.icebox.oldprocess.entity.IceEventRecord;

public class IceBoxConverter {

    public static IceBoxVo convertToVo(IceBox iceBox, IceBoxExtend iceBoxExtend, IceModel iceModel){
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
        return iceBoxVo;
    }

    public static IceBoxStoreVo convertToStoreVo(IceBox iceBox, IceBoxExtend iceBoxExtend, IceModel iceModel, IcePutApplyRelateBox icePutApplyRelateBox, IceEventRecord iceEventRecord){
        IceBoxStoreVo iceBoxStoreVo =  new IceBoxStoreVo();
        iceBoxStoreVo.setIceBoxVo(convertToVo(iceBox, iceBoxExtend, iceModel));
        iceBoxStoreVo.setIcePutApplyRelateBoxVo(IcePutApplyRelateBoxConverter.convertToVo(icePutApplyRelateBox));
        iceBoxStoreVo.setIceEventRecord(iceEventRecord);
        return iceBoxStoreVo;
    }
}
