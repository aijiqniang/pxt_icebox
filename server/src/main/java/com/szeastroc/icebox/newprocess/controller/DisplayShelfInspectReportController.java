package com.szeastroc.icebox.newprocess.controller;


import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.consumer.common.ShelfInspectReportMsg;
import com.szeastroc.icebox.newprocess.service.DisplayShelfInspectReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/**
 * (DisplayShelfInspectReport)表控制层
 *
 * @author chenchao
 * @since 2021-06-11 09:38:04
 */
@RestController
@RequestMapping("displayShelfInspectReport")
public class DisplayShelfInspectReportController {
    /**
     * 服务对象
     */
    @Autowired
    private DisplayShelfInspectReportService displayShelfInspectReportService;


    @PostMapping("page")
    public CommonResponse page(@RequestBody ShelfInspectReportMsg reportMsg){
        return new CommonResponse(Constants.API_CODE_SUCCESS,null,displayShelfInspectReportService.selectPage(reportMsg));
    }

    @GetMapping("detail")
    public CommonResponse detail(@RequestParam String applyNumber){
        return new CommonResponse(Constants.API_CODE_SUCCESS,null,displayShelfInspectReportService.detail(applyNumber));
    }

    @PostMapping("export")
    public CommonResponse export(@RequestBody ShelfInspectReportMsg reportMsg){
        return displayShelfInspectReportService.export(reportMsg);
    }

}
