package com.szeastroc.icebox.newprocess.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.icebox.newprocess.entity.IcePutOrder;
import com.szeastroc.icebox.oldprocess.vo.ClientInfoRequest;
import com.szeastroc.icebox.oldprocess.vo.OrderPayBack;
import com.szeastroc.icebox.vo.IceBoxAssetReportVo;

import java.util.Map;

public interface IcePutOrderService extends IService<IcePutOrder> {


    Map<String, Object> applyPayIceBox(ClientInfoRequest clientInfoRequest) throws Exception;

    IceBoxAssetReportVo notifyOrderInfo(OrderPayBack orderPayBack);

    Map<String, Object> getPayStatus(String orderNumber) throws Exception;
}
