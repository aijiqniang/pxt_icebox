package com.szeastroc.icebox.oldprocess.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.oldprocess.entity.IceChestPutRecord;
import com.szeastroc.icebox.oldprocess.vo.ClientInfoRequest;
import com.szeastroc.icebox.oldprocess.vo.IceDepositResponse;
import com.szeastroc.icebox.oldprocess.vo.OrderPayResponse;
import com.szeastroc.icebox.oldprocess.vo.query.IceDepositPage;

/**
 * Created by Tulane
 * 2019/5/21
 */
public interface IceChestPutRecordService extends IService<IceChestPutRecord> {

    CommonResponse<OrderPayResponse> applyPayIceChest(ClientInfoRequest clientInfoRequest) throws Exception;

    IPage<IceDepositResponse> queryIceDeposits(IceDepositPage iceDepositPage);

    IPage<IceDepositResponse> queryIceDepositsForPut(IceDepositPage iceDepositPage);
}
