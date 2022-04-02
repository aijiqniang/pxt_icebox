package com.szeastroc.icebox.newprocess.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.szeastroc.common.annotation.MonitorAnnotation;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.entity.customer.vo.SimpleSupplierInfoVo;
import com.szeastroc.common.entity.icebox.vo.IceBoxRequest;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.service.IceBackOrderService;
import com.szeastroc.icebox.newprocess.service.IceBoxExtendService;
import com.szeastroc.icebox.newprocess.service.IceBoxService;
import com.szeastroc.icebox.newprocess.vo.IceBoxDetailVo;
import com.szeastroc.icebox.newprocess.vo.SimpleIceBoxDetailVo;
import com.szeastroc.icebox.oldprocess.vo.IceDepositResponse;
import com.szeastroc.icebox.oldprocess.vo.query.IceDepositPage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/refund")
public class RefundController {
    @Autowired
    private IceBoxExtendService iceBoxExtendService;

    @Autowired
    private IceBoxService iceBoxService;

    @Autowired
    private IceBackOrderService iceBackOrderService;


    /**
     * 根据字冰柜的资产编号查询冰柜简易信息
     *
     * @param assetId
     * @return
     */
    @RequestMapping("/findSimpleIceBoxByAssetId")
    @MonitorAnnotation
    public CommonResponse<SimpleIceBoxDetailVo> findIceBoxByAssetId(@RequestParam("assetId") String assetId) {

        if (StringUtils.isBlank(assetId)) {
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }
        SimpleIceBoxDetailVo simpleIceBoxDetailVo = iceBoxExtendService.getByAssetId(assetId);

        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, simpleIceBoxDetailVo);
    }


    /**
     * 根据冰柜的id 查询冰柜的完整信息
     *
     * @param id
     * @return
     */
    @RequestMapping("/findIceBoxById")
    @MonitorAnnotation
    public CommonResponse<IceBoxDetailVo> findIceBoxById(@RequestParam("id") Integer id) {

        if (id == null) {
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }

        IceBoxDetailVo iceBoxDetailVo = iceBoxService.findIceBoxById(id);


        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, iceBoxDetailVo);
    }


    /**
     * @param deptId 根据部门id 查看该部门下面所有有冰柜的经销商
     * @return
     */
    @RequestMapping("/findSupplierByDeptId")
    @MonitorAnnotation
    public CommonResponse<List<SimpleSupplierInfoVo>> findSupplierByDeptId(@RequestParam("deptId") Integer deptId,@RequestParam(value = "assetId",required = false) String assetId) {

        if (deptId == null) {
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }


        List<SimpleSupplierInfoVo> simpleSupplierInfoVoList = iceBoxService.findSupplierByDeptId(deptId,assetId);

        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, simpleSupplierInfoVoList);
    }


    /**
     * 业务员待办事项提交之后触发 创建审批流
     * @param simpleIceBoxDetailVo
     * @return
     */
    @RequestMapping("/doRefund")
    @MonitorAnnotation
    public CommonResponse doRefund(@RequestBody SimpleIceBoxDetailVo simpleIceBoxDetailVo) {

        // 创建审批流
        iceBackOrderService.doRefund(simpleIceBoxDetailVo);



        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, null);

    }

    /**
     * 业务员直接申请冰柜退还 创建审批流
     * @param simpleIceBoxDetailVo
     * @return
     */
    @RequestMapping("/doBackOrder")
    @MonitorAnnotation
    public CommonResponse doBackOrder(@RequestBody SimpleIceBoxDetailVo simpleIceBoxDetailVo) {
        // 创建审批流
        iceBackOrderService.doBackOrder(simpleIceBoxDetailVo);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, null);

    }

    /**
     * 当审批流通过之后，业代确认  修改冰柜的状态
     * @param applyNumber
     * @return
     */
    @RequestMapping("/confirm")
    @MonitorAnnotation
    public CommonResponse confirm(@RequestParam String applyNumber) {
        // 创建审批流
        iceBackOrderService.confirm(applyNumber);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);

    }


    /**
     * 根据冰柜的id判断当前冰柜是否在协议期间内
     *
     * @param id
     * @return false-还在协议期内
     */
    @RequestMapping("/judgeRecordTime")
    @MonitorAnnotation
    public CommonResponse judgeRecordTime(@RequestParam("id") Integer id) {

        if (id == null) {
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }

        boolean result = iceBoxService.judgeRecordTime(id);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, result);

    }


    /**
     * 用于修改back_order表审批流状态 当审批状态未通过的时候 调用转账服务
     * @param iceBoxRequest
     * @return
     */
    @RequestMapping("/updateExamineStatus")
    public CommonResponse updateExamineStatus(@RequestBody IceBoxRequest iceBoxRequest) {

        iceBackOrderService.updateExamineStatus(iceBoxRequest);

        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, null);
    }


    @RequestMapping("/advanceRefund")
    public CommonResponse advanceRefund(@RequestParam("assetId") String assetId) {

        iceBoxExtendService.advanceRefund(assetId);

        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, null);
    }



    @RequestMapping("/findRefundTransferByPage")
    public CommonResponse<IPage<IceDepositResponse>> findRefundTransferByPage(@RequestBody IceDepositPage iceDepositPage){

        IPage<IceDepositResponse> iPage =  iceBackOrderService.findRefundTransferByPage(iceDepositPage);

        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, iPage);
    }

    @RequestMapping("/exportRefundTransfer")
    public CommonResponse<IPage<IceDepositResponse>> exportRefundTransfer(@RequestBody IceDepositPage iceDepositPage){

        iceBackOrderService.exportRefundTransferByMq(iceDepositPage);

        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, null);
    }
}
