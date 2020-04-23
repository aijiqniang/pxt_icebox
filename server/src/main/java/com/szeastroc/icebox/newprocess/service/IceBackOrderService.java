package com.szeastroc.icebox.newprocess.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.icebox.newprocess.entity.IceBackOrder;
import com.szeastroc.icebox.newprocess.vo.SimpleIceBoxDetailVo;
import com.szeastroc.icebox.newprocess.vo.request.IceBoxPage;
import com.szeastroc.icebox.vo.IceBoxRequest;

public interface IceBackOrderService extends IService<IceBackOrder> {

    void takeBackOrder(Integer iceBoxId);

    void doRefund(SimpleIceBoxDetailVo simpleIceBoxDetailVo);

    IPage findPage(IceBoxPage iceBoxPage);

    void updateExamineStatus(IceBoxRequest iceBoxRequest);
}

