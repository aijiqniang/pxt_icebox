package com.szeastroc.icebox.newprocess.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.icebox.newprocess.entity.IceBackOrder;
import com.szeastroc.icebox.newprocess.vo.SimpleIceBoxDetailVo;
import com.szeastroc.icebox.newprocess.vo.request.IceBoxPage;

public interface IceBackOrderService extends IService<IceBackOrder> {

    void takeBackOrder(Integer iceBoxId);

    void doRefund(SimpleIceBoxDetailVo simpleIceBoxDetailVo);

    void doTransfer(String applyNumber);

    IPage findPage(IceBoxPage iceBoxPage);

}

