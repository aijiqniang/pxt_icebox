package com.szeastroc.icebox.newprocess.controller;

import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.entity.visit.SessionExamineVo;
import com.szeastroc.common.entity.visit.ShelfBackModel;
import com.szeastroc.common.entity.visit.ShelfPutModel;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.entity.ShelfBack;
import com.szeastroc.icebox.newprocess.service.DisplayShelfBackApplyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = {"陈列架退还申请表接口"})
@RestController
@RequestMapping("/shelf-back-apply")
public class DisplayShelfBackController {

    @Autowired
    private DisplayShelfBackApplyService displayShelfBackApplyService;

    @PostMapping("shelfBack")
    @ApiOperation(value = "小程序陈列架退还", notes = "陈列架退还", produces = "application/json")
    public CommonResponse shelfBack(@RequestBody ShelfBackModel model){
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null,displayShelfBackApplyService.shelfBack(model));
    }

    //陈列架退还发送待办给业代
    @PostMapping("shelfBacklog")
    @ApiOperation(value = "陈列架退还发送待办给业代", notes = "陈列架退还发送待办给业代", produces = "application/json")
    public CommonResponse shelfBacklog(@RequestBody ShelfBackModel model){
        displayShelfBackApplyService.shelfBacklog(model);
        return new CommonResponse(Constants.API_CODE_SUCCESS, null);
    }

    @GetMapping("shelfBackDetails")
    @ApiOperation(value = "查询陈列架退还详情", notes = "查询陈列架退还详情", produces = "application/json")
    public CommonResponse<List<ShelfBack>> shelfBackDetails(@RequestParam String uuid){

        return new CommonResponse(Constants.API_CODE_SUCCESS, null,displayShelfBackApplyService.shelfBackDetails(uuid));
    }
}
