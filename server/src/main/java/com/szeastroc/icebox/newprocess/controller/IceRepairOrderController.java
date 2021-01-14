package com.szeastroc.icebox.newprocess.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.consumer.common.IceRepairOrderMsg;
import com.szeastroc.icebox.newprocess.entity.IceRepairOrder;
import com.szeastroc.icebox.newprocess.service.IceRepairOrderService;
import com.szeastroc.icebox.newprocess.vo.request.IceRepairRequest;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
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
@Api(tags = {"冰柜维修控制层"})
@RestController
@RequestMapping("iceRepairOrder")
public class IceRepairOrderController {
    /**
     * 服务对象
     */
    @Autowired
    private IceRepairOrderService iceRepairOrderService;

    @ApiOperation(value = "创建维修订单",httpMethod="POST")
    @PostMapping("/create")
    public CommonResponse<Void> createOrder(@RequestBody IceRepairRequest iceRepairRequest){
        return iceRepairOrderService.createOrder(iceRepairRequest);
    }

    @ApiOperation(value = "查询维修订单",httpMethod="POST")
    @RequestMapping("findByPage")
    public CommonResponse<IPage<IceRepairOrder>> findByPage(@RequestBody IceRepairOrderMsg msg){
        IPage<IceRepairOrder> reportIPage = iceRepairOrderService.findByPage(msg);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS,null, reportIPage);
    }

    @ApiOperation(value = "导出维修订单",httpMethod="POST")
    @RequestMapping("sendExportMsg")
    public CommonResponse<Void> sendExportMsg(@RequestBody IceRepairOrderMsg msg){
        return iceRepairOrderService.sendExportMsg(msg);
    }

}