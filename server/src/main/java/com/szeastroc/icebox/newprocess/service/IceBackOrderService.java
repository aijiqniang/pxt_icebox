package com.szeastroc.icebox.newprocess.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.common.entity.icebox.vo.IceBoxRequest;
import com.szeastroc.icebox.newprocess.entity.IceBackApplyReport;
import com.szeastroc.icebox.newprocess.entity.IceBackOrder;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.vo.SimpleIceBoxDetailVo;
import com.szeastroc.icebox.oldprocess.vo.IceDepositResponse;
import com.szeastroc.icebox.oldprocess.vo.query.IceDepositPage;

public interface IceBackOrderService extends IService<IceBackOrder> {

    void takeBackOrder(Integer iceBoxId);

    void doBackOrder(SimpleIceBoxDetailVo simpleIceBoxDetailVo);

    void confirm(String applyNumber);

    void doRefund(SimpleIceBoxDetailVo simpleIceBoxDetailVo);

    void updateExamineStatus(IceBoxRequest iceBoxRequest);

    String checkBackIceBox(Integer iceBoxId);

    IPage<IceDepositResponse> findRefundTransferByPage(IceDepositPage iceDepositPage);

    void exportRefundTransferByMq(IceDepositPage iceDepositPage);

    void exportRefundTransfer(IceDepositPage iceDepositPage);

    IceBackApplyReport generateBackReport(IceBox iceBox, String applyNumber, String putStoreNumber, Integer freeType);
}

