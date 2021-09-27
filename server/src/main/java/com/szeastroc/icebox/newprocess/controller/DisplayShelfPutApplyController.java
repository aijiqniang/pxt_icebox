package com.szeastroc.icebox.newprocess.controller;


import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.entity.visit.ShelfPutModel;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.entity.ShelfSign;
import com.szeastroc.icebox.newprocess.service.DisplayShelfPutApplyService;
import com.szeastroc.icebox.newprocess.vo.DisplayShelfPutApplyVo;
import com.szeastroc.icebox.newprocess.vo.request.InvalidShelfApplyRequest;
import com.szeastroc.icebox.newprocess.vo.request.SignShelfRequest;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 业务员申请表
 * </p>
 *
 * @author 陈超
 * @since 2021-06-01
 */
@Api(tags = {"业务员申请表 接口"}, description = "ShelfPutApplyController[陈超]")
@RestController
@RequestMapping("/shelf-put-apply")
public class DisplayShelfPutApplyController {

    @Autowired
    DisplayShelfPutApplyService shelfPutApplyService;

    @PostMapping("shelfPut")
    @ApiOperation(value = "小程序陈列架投放", notes = "陈列架投放", produces = "application/json")
    public CommonResponse shelfPut(@RequestBody ShelfPutModel model){
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null,shelfPutApplyService.shelfPut(model));
    }

    @PostMapping("sign")
    @ApiOperation(value = "小程序签收陈列货架", notes = "签收陈列货架", produces = "application/json")
    public CommonResponse sign(@RequestBody SignShelfRequest request){
        shelfPutApplyService.sign(request);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }

    @GetMapping("putList")
    @ApiOperation(value = "小程序已投放陈列架", notes = "已投放陈列架", produces = "application/json")
    public CommonResponse<List<DisplayShelfPutApplyVo>> putList(@RequestParam String customerNumber) {
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, shelfPutApplyService.putList(customerNumber));
    }

    @GetMapping("processing")
    @ApiOperation(value = "小程序处理中陈列架", notes = "处理中陈列架", produces = "application/json")
    public CommonResponse<List<DisplayShelfPutApplyVo>> processing(@RequestParam String customerNumber) {
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, shelfPutApplyService.processing(customerNumber));
    }

    @PostMapping("invalid")
    @ApiOperation(value = "小程序作废申请", notes = "作废申请", produces = "application/json")
    public CommonResponse invalidApply(@RequestBody InvalidShelfApplyRequest request){
        shelfPutApplyService.invalid(request);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }

    @GetMapping("ShelfSign")
    public CommonResponse shelfSign(@RequestParam String applyNumber){
        shelfPutApplyService.shelfSign(applyNumber);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }

    @GetMapping("signInform")
    @ApiOperation(value = "陈列架签收待办通知", notes = "陈列架签收待办通知", produces = "application/json")
    public CommonResponse<List<ShelfSign>> signInform(@RequestParam String customerNumber){
        List<ShelfSign> shelfSignInforms = shelfPutApplyService.signInform(customerNumber);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null,shelfSignInforms);
    }

    @GetMapping("customerTotal")
    @ApiOperation(value = "客户投放统计", notes = "客户投放统计", produces = "application/json")
    public CommonResponse customerTotal(@RequestParam String applyNumber){
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null,shelfPutApplyService.customerTotal(applyNumber));
    }
}

