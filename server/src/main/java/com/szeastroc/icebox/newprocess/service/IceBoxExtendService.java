package com.szeastroc.icebox.newprocess.service;

import com.szeastroc.icebox.newprocess.entity.IceBoxExtend;
import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.icebox.newprocess.vo.SimpleIceBoxDetailVo;

public interface IceBoxExtendService extends IService<IceBoxExtend> {


    SimpleIceBoxDetailVo getByAssetId(String assetId);

    void advanceRefund(String assetId);
}

