package com.szeastroc.icebox.newprocess.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.consumer.common.IceRepairOrderMsg;
import com.szeastroc.icebox.newprocess.entity.IceRepairOrder;
import com.szeastroc.icebox.newprocess.service.IceRepairOrderService;
import com.szeastroc.icebox.newprocess.vo.request.IceRepairRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 冰柜维修订单表(IceRepairOrder)表控制层
 *
 * @author chenchao
 * @since 2021-01-12 15:58:24
 */
@RestController
@RequestMapping("iceRepairOrder")
public class IceRepairOrderController {
    /**
     * 服务对象
     */
    @Autowired
    private IceRepairOrderService iceRepairOrderService;


    @PostMapping("/create")
    public CommonResponse<Void> createOrder(@RequestBody IceRepairRequest iceRepairRequest){
        return iceRepairOrderService.createOrder(iceRepairRequest);
    }

    @RequestMapping("findByPage")
    public CommonResponse<IPage<IceRepairOrder>> findByPage(@RequestBody IceRepairOrderMsg msg){
        IPage<IceRepairOrder> reportIPage = iceRepairOrderService.findByPage(msg);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS,null, reportIPage);
    }

    @RequestMapping("sendExportMsg")
    public CommonResponse<Void> sendExportMsg(@RequestBody IceRepairOrderMsg msg){
        return iceRepairOrderService.sendExportMsg(msg);
    }

}