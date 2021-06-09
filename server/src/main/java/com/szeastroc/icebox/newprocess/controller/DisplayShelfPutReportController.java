package com.szeastroc.icebox.newprocess.controller;


import com.szeastroc.icebox.newprocess.service.DisplayShelfPutReportService;
import org.springframework.beans.factory.annotation.Autowired;
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
public class DisplayShelfPutReportController {
    /**
     * 服务对象
     */
    @Autowired
    private DisplayShelfPutReportService displayShelfPutReportService;





}
