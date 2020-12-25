package com.szeastroc.icebox.newprocess.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.consumer.common.IceBackApplyReportMsg;
import com.szeastroc.icebox.newprocess.entity.IceBackApply;
import com.szeastroc.icebox.newprocess.entity.IceBackApplyReport;
import com.szeastroc.icebox.newprocess.entity.IceBoxPutReport;
import com.szeastroc.icebox.newprocess.service.IceBackApplyReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/**
 *  (TIceBackApplyReport)表控制层
 *
 * @author chenchao
 * @since 2020-12-16 16:41:07
 */
@RestController
@RequestMapping("backApplyReport")
public class IceBackApplyReportController {
    /**
     * 服务对象
     */
    @Autowired
    private IceBackApplyReportService iceBackApplyReportService;


    @RequestMapping("findByPage")
    public CommonResponse<IPage<IceBackApplyReport>> findByPage(@RequestBody IceBackApplyReportMsg reportMsg){
        IPage<IceBackApplyReport> reportIPage = iceBackApplyReportService.findByPage(reportMsg);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS,null, reportIPage);
    }

    @RequestMapping("sendExportMsg")
    public CommonResponse<IceBackApplyReport> sendExportMsg(@RequestBody IceBackApplyReportMsg reportMsg){
        return iceBackApplyReportService.sendExportMsg(reportMsg);
    }

    @RequestMapping("updateDept")
    public CommonResponse<Void> updateDept(@RequestParam Integer boxId,@RequestParam Integer deptId){
        iceBackApplyReportService.updateDept(boxId,deptId);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS,null);
    }
}