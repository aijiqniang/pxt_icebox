package com.szeastroc.icebox.newprocess.service;

import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.entity.IcePutOrder;
import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.icebox.newprocess.entity.IcePutOrder;
import com.szeastroc.icebox.newprocess.vo.IceBoxAssetReportVo;
import com.szeastroc.icebox.oldprocess.vo.ClientInfoRequest;
import com.szeastroc.icebox.oldprocess.vo.OrderPayBack;

import java.util.Map;

public interface IcePutOrderService extends IService<IcePutOrder> {


    Map<String, Object> applyPayIceBox(ClientInfoRequest clientInfoRequest) throws Exception;

    OrderPayResponse createByFree(ClientInfoRequest clientInfoRequest, IceBox iceBox) throws ImproperOptionException;

    IceBoxAssetReportVo notifyOrderInfo(OrderPayBack orderPayBack);

    Map<String, Object> getPayStatus(String orderNumber) throws Exception;
}
