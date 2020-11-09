package com.szeastroc.icebox.newprocess.service;

import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.entity.IcePutOrder;
import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.icebox.oldprocess.vo.ClientInfoRequest;
import com.szeastroc.icebox.oldprocess.vo.OrderPayBack;
import com.szeastroc.icebox.oldprocess.vo.OrderPayResponse;

public interface IcePutOrderService extends IService<IcePutOrder>{


    OrderPayResponse applyPayIceBox(ClientInfoRequest clientInfoRequest) throws Exception;

    OrderPayResponse createByFree(ClientInfoRequest clientInfoRequest, IceBox iceBox) throws ImproperOptionException;

    void notifyOrderInfo(OrderPayBack orderPayBack);

    boolean getPayStatus(String orderNumber) throws Exception;
}
