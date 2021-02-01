package com.szeastroc.icebox.newprocess.controller;


import com.szeastroc.icebox.newprocess.service.IceInspectionReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 冰柜巡检报表 (TIceInspectionReport)表控制层
 *
 * @author chenchao
 * @since 2020-12-16 16:46:22
 */
@RestController
@RequestMapping("inspectionReport")
public class IceInspectionReportController {
    /**
     * 服务对象
     */
    @Autowired
    private IceInspectionReportService iceInspectionReportService;

}