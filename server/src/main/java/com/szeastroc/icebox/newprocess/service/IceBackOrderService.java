package com.szeastroc.icebox.newprocess.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.icebox.newprocess.entity.IceBackOrder;
import com.szeastroc.icebox.newprocess.vo.SimpleIceBoxDetailVo;
import com.szeastroc.icebox.vo.IceBoxRequest;

public interface IceBackOrderService extends IService<IceBackOrder> {

    void takeBackOrder(Integer iceBoxId);

    void doRefund(SimpleIceBoxDetailVo simpleIceBoxDetailVo);

    void updateExamineStatus(IceBoxRequest iceBoxRequest);

    String checkBackIceBox(Integer iceBoxId);
}

