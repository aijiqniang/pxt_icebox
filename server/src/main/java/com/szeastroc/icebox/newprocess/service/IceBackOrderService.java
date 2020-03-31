package com.szeastroc.icebox.newprocess.service;

import com.szeastroc.icebox.newprocess.entity.IceBackOrder;
import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.icebox.newprocess.vo.SimpleIceBoxDetailVo;

public interface IceBackOrderService extends IService<IceBackOrder> {

    void takeBackOrder(Integer iceBoxId);

    void doRefund(SimpleIceBoxDetailVo simpleIceBoxDetailVo);
}

