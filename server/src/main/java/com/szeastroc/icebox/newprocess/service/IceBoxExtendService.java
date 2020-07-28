package com.szeastroc.icebox.newprocess.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.icebox.newprocess.entity.IceBoxExtend;
import com.szeastroc.icebox.newprocess.vo.SimpleIceBoxDetailVo;
import com.szeastroc.icebox.newprocess.vo.request.IceBoxPage;

public interface IceBoxExtendService extends IService<IceBoxExtend> {


    SimpleIceBoxDetailVo getByAssetId(String assetId);

    void advanceRefund(String assetId);

}

