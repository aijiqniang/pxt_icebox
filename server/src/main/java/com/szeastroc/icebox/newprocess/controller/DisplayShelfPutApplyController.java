package com.szeastroc.icebox.newprocess.controller;


import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.service.DisplayShelfPutApplyService;
import com.szeastroc.icebox.newprocess.vo.DisplayShelfPutApplyVo;
import com.szeastroc.icebox.newprocess.vo.request.InvalidShelfApplyRequest;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("putList")
    @ApiOperation(value = "已投放陈列架", notes = "已投放陈列架", produces = "application/json")
    public CommonResponse<List<DisplayShelfPutApplyVo>> putList(@RequestParam String customerNumber) {
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, shelfPutApplyService.putList(customerNumber));
    }


    @GetMapping("processing")
    @ApiOperation(value = "处理中陈列架", notes = "处理中陈列架", produces = "application/json")
    public CommonResponse<List<DisplayShelfPutApplyVo>> processing(@RequestParam String customerNumber) {
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, shelfPutApplyService.processing(customerNumber));
    }

    @PostMapping("invalid")
    @ApiOperation(value = "作废申请", notes = "作废申请", produces = "application/json")
    public CommonResponse invalidApply(@RequestBody InvalidShelfApplyRequest request){
        shelfPutApplyService.invalid(request);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }

}

