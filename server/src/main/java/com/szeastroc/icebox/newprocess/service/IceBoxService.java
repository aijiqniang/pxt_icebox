package com.szeastroc.icebox.newprocess.service;

import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.icebox.newprocess.vo.IceBoxStatusVo;
import com.szeastroc.icebox.newprocess.vo.IceBoxStoreVo;

import java.util.List;

public interface IceBoxService extends IService<IceBox> {

    List<IceBoxStoreVo> getIceBoxStoreVoByPxtNumber(String pxtNumber);

    IceBoxStatusVo checkBoxByQrcode(String qrcode, String pxtNumber);
}








