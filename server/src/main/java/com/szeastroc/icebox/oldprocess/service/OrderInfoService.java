package com.szeastroc.icebox.oldprocess.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.icebox.oldprocess.entity.OrderInfo;
import com.szeastroc.icebox.oldprocess.vo.OrderPayBack;
import com.szeastroc.icebox.oldprocess.vo.OrderPayResponse;

/**
 * Created by Tulane
 * 2019/5/23
 */
public interface OrderInfoService extends IService<OrderInfo> {

    OrderPayResponse createPayInfo(String ip, String openid, Integer iceChestId, Integer iceChestPutRecordId) throws Exception;

    boolean getPayStatus(String orderNum) throws Exception;

    void notifyOrderInfo(OrderPayBack orderPayBack) throws ImproperOptionException;

}
