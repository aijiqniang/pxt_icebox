package com.szeastroc.icebox.newprocess.service;

import com.alibaba.fastjson.JSONObject;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.entity.IcePutOrder;
import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.icebox.newprocess.entity.IcePutOrder;
import com.szeastroc.icebox.newprocess.vo.IceBoxAssetReportVo;
import com.szeastroc.icebox.oldprocess.vo.ClientInfoRequest;
import com.szeastroc.icebox.oldprocess.vo.OrderPayBack;
import com.szeastroc.icebox.oldprocess.vo.OrderPayResponse;

import java.util.Map;

public interface IcePutOrderService extends IService<IcePutOrder> {


    OrderPayResponse applyPayIceBox(ClientInfoRequest clientInfoRequest) throws Exception;

    OrderPayResponse createByFree(ClientInfoRequest clientInfoRequest, IceBox iceBox) throws ImproperOptionException;

    JSONObject notifyOrderInfo(OrderPayBack orderPayBack);

    Boolean getPayStatus(String orderNumber) throws Exception;
}
