package com.szeastroc.icebox.newprocess.convert;

import com.szeastroc.icebox.newprocess.entity.IcePutApplyRelateBox;
import com.szeastroc.icebox.newprocess.vo.IcePutApplyRelateBoxVo;

public class IcePutApplyRelateBoxConverter {

    public static IcePutApplyRelateBoxVo convertToVo(IcePutApplyRelateBox icePutApplyRelateBox){
        IcePutApplyRelateBoxVo icePutApplyRelateBoxVo = new IcePutApplyRelateBoxVo();
        icePutApplyRelateBoxVo.setApplyNumber(icePutApplyRelateBox.getApplyNumber());
        icePutApplyRelateBoxVo.setBoxId(icePutApplyRelateBox.getBoxId());
        icePutApplyRelateBoxVo.setFreeType(icePutApplyRelateBox.getFreeType());
        return icePutApplyRelateBoxVo;
    }
}
