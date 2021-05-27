package com.szeastroc.icebox.newprocess.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.entity.IceBoxRelateDms;
import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.icebox.newprocess.vo.IceBoxRelateDmsVo;

/**
 *
 */
public interface IceBoxRelateDmsService extends IService<IceBoxRelateDms> {

    IceBoxRelateDmsVo findById(Integer id);

    void confirmAccept(IceBoxRelateDmsVo iceBoxRelateDmsVo);

    IceBox getIceBoxInfoByQrcode(String qrcode);

    void confirmArrvied(IceBoxRelateDmsVo iceBoxRelateDmsVo);
}
