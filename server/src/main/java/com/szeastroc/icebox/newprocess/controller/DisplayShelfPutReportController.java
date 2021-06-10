package com.szeastroc.icebox.newprocess.controller;


import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.consumer.common.ShelfPutReportMsg;
import com.szeastroc.icebox.newprocess.service.DisplayShelfPutReportService;
import freemarker.core.CommonTemplateMarkupOutputModel;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * (DisplayShelfPutReport)表控制层
 *
 * @author chenchao
 * @since 2021-06-07 10:26:40
 */
@RestController
@RequestMapping("shelfPutReport")
@Api(tags = {"陈列架投放报表接口"}, description = "DisplayShelfPutReportController[陈超]")
public class DisplayShelfPutReportController {
    /**
     * 服务对象
     */
    @Autowired
    private DisplayShelfPutReportService displayShelfPutReportService;


    @PostMapping("page")
    public CommonResponse page(ShelfPutReportMsg reportMsg){
        return new CommonResponse(Constants.API_CODE_SUCCESS,null,displayShelfPutReportService.selectPage(reportMsg));
    }

    @GetMapping("detail")
    public CommonResponse detail(String applyNumber){
        return new CommonResponse(Constants.API_CODE_SUCCESS,null,displayShelfPutReportService.detail(applyNumber));
    }

    @PostMapping("export")
    public CommonResponse export(){
        return new CommonResponse(Constants.API_CODE_SUCCESS,null);
    }



}
