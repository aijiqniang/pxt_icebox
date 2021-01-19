package com.szeastroc.icebox.newprocess.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.entity.customer.vo.StoreRequest;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.consumer.common.IceRepairOrderMsg;
import com.szeastroc.icebox.newprocess.entity.IceQuestionDesc;
import com.szeastroc.icebox.newprocess.entity.IceRepairOrder;
import com.szeastroc.icebox.newprocess.service.IceQuestionDescService;
import com.szeastroc.icebox.newprocess.service.IceRepairOrderService;
import com.szeastroc.icebox.newprocess.vo.IceRepairOrderVO;
import com.szeastroc.icebox.newprocess.vo.request.IceRepairRequest;
import com.szeastroc.icebox.newprocess.vo.request.IceRepairStatusRequest;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


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
    @Autowired
    private IceQuestionDescService iceQuestionDescService;

    @ApiOperation(value = "创建维修订单",httpMethod="POST")
    @PostMapping("/create")
    public CommonResponse createOrder(@RequestBody IceRepairRequest iceRepairRequest){
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

    @ApiOperation(value = "获取客户保修订单",httpMethod="GET")
    @GetMapping("getOrders")
    public CommonResponse<List<IceRepairOrder>> getOrders(@ApiParam(value = "客户编号",required = true )@RequestParam String customerNumber){
        return new CommonResponse<>(Constants.API_CODE_SUCCESS,null,iceRepairOrderService.getOrders(customerNumber));
    }

    @ApiOperation(value = "获取保修订单详情",httpMethod="GET")
    @GetMapping("detail")
    public CommonResponse<IceRepairOrderVO> getDetail(@ApiParam(value = "订单编号",required = true )@RequestParam String orderNumber){
        return new CommonResponse<>(Constants.API_CODE_SUCCESS,null,iceRepairOrderService.getDetail(orderNumber));
    }


    @ApiOperation(value = "获取冰柜问题描述",httpMethod="GET")
    @GetMapping("getDesc")
    public CommonResponse<List<IceQuestionDesc>> getDesc(){
        return new CommonResponse<>(Constants.API_CODE_SUCCESS,null,iceQuestionDescService.list());
    }


    @PostMapping("changeStatus")
    public CommonResponse changeStatus(@RequestBody IceRepairStatusRequest request){
        return iceRepairOrderService.changeStatus(request);
    }

}