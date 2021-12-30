package com.szeastroc.icebox.newprocess.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.entity.icebox.vo.IceExamineCheckVo;
import com.szeastroc.common.entity.icebox.vo.ShelfInspectRequest;
import com.szeastroc.common.entity.visit.ShelfInspectModel;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.entity.DisplayShelfInspectApply;
import com.szeastroc.icebox.newprocess.service.DisplayShelfInspectApplyService;
import com.szeastroc.icebox.newprocess.vo.request.ShelfInspectPage;
import com.szeastroc.icebox.newprocess.vo.request.ShelfInspectVo;
import freemarker.core.CommonTemplateMarkupOutputModel;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;


/**
 * 陈列架巡检(DisplayShelfInspectApply)表控制层
 *
 * @author chenchao
 * @since 2021-06-07 14:41:15
 */
@RestController
@RequestMapping("shelfInspect")
public class DisplayShelfInspectApplyController {
    /**
     * 服务对象
     */
    @Autowired
    private DisplayShelfInspectApplyService displayShelfInspectApplyService;

    @PostMapping("shelfInspect")
    @ApiOperation(value = "小程序陈列架巡检", notes = "陈列架巡检", produces = "application/json")
    public CommonResponse shelfInspect(@RequestBody ShelfInspectModel model){
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null,displayShelfInspectApplyService.shelfInspect(model));
    }

    @PostMapping("/submitShelfInspectDetails")
    @ApiOperation(value = "小程序陈列架巡检", notes = "陈列架巡检", produces = "application/json")
    public CommonResponse submitShelfInspectDetails(@RequestBody ShelfInspectModel model){
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null,displayShelfInspectApplyService.submitShelfInspectDetails(model));
    }

    @PostMapping("doInspect")
    public CommonResponse doInspect(@RequestBody ShelfInspectRequest request){
        displayShelfInspectApplyService.doInspect(request);
        return new CommonResponse(Constants.API_CODE_SUCCESS,null);
    }


    @PostMapping("history")
    @ApiOperation(value = "小程序陈列架巡检历史记录", notes = "小程序陈列架巡检历史记录", produces = "application/json")
    public CommonResponse history(@RequestBody ShelfInspectPage page){
        return new CommonResponse(Constants.API_CODE_SUCCESS,null,displayShelfInspectApplyService.history(page));
    }

    @GetMapping("submitted")
    @ApiOperation(value = "小程序陈列架巡检已提交记录", notes = "小程序陈列架巡检已提交记录", produces = "application/json")
    public CommonResponse<List<ShelfInspectModel>> submitted(@RequestParam String customerNumber){
        return new CommonResponse(Constants.API_CODE_SUCCESS,null,displayShelfInspectApplyService.submitted(customerNumber));
    }

    @PostMapping("inspectHistory")
    @ApiOperation(value = "小程序陈列架巡检已提交记录", notes = "小程序陈列架巡检已提交记录", produces = "application/json")
    public CommonResponse inspectHistory(@RequestBody ShelfInspectPage page){
        return new CommonResponse(Constants.API_CODE_SUCCESS,null,displayShelfInspectApplyService.inspectHistory(page));
    }

}
