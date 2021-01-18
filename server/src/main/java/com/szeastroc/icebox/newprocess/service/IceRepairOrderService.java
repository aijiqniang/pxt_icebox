package com.szeastroc.icebox.newprocess.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.consumer.common.IceRepairOrderMsg;
import com.szeastroc.icebox.newprocess.entity.IceRepairOrder;
import com.szeastroc.icebox.newprocess.vo.IceRepairOrderVO;
import com.szeastroc.icebox.newprocess.vo.request.IceRepairRequest;

import java.util.List;

/**
 * 冰柜维修订单表(IceRepairOrder)表服务接口
 *
 * @author chenchao
 * @since 2021-01-12 15:58:24
 */
public interface IceRepairOrderService extends IService<IceRepairOrder> {

    CommonResponse createOrder(IceRepairRequest iceRepairRequest);

    IPage<IceRepairOrder> findByPage(IceRepairOrderMsg msg);

    CommonResponse<Void> sendExportMsg(IceRepairOrderMsg msg);

    Integer selectByExportCount(LambdaQueryWrapper<IceRepairOrder> wrapper);

    LambdaQueryWrapper<IceRepairOrder> fillWrapper(IceRepairOrderMsg msg);

    List<IceRepairOrder> getOrders(String customerNumber);

    IceRepairOrderVO getDetail(String orderNumber);

    Integer getUnfinishOrderCount(Integer boxId);
}