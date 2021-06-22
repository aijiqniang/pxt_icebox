package com.szeastroc.icebox.newprocess.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.utils.ExecutorServiceFactory;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.consumer.common.IceBoxExamineExceptionReportMsg;
import com.szeastroc.icebox.newprocess.consumer.common.IceBoxPutReportMsg;
import com.szeastroc.icebox.newprocess.entity.IceBoxExamineExceptionReport;
import com.szeastroc.icebox.newprocess.entity.IceBoxPutReport;
import com.szeastroc.icebox.newprocess.service.IceBoxExamineExceptionReportService;
import com.szeastroc.icebox.newprocess.service.IceBoxPutReportService;
import com.szeastroc.icebox.newprocess.vo.IceBoxExamineVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("iceBoxExamineExceptionReport")
public class IceBoxExamineExceptionReportController {

    @Resource
    private IceBoxExamineExceptionReportService iceBoxExamineExceptionReportService;

    /**
     * 查询异常报备列表
     * @param reportMsg
     * @return
     */
    @RequestMapping("findByPage")
    public CommonResponse<IPage<IceBoxExamineExceptionReport>> findByPage(@RequestBody IceBoxExamineExceptionReportMsg reportMsg){
        IPage<IceBoxExamineExceptionReport> reportIPage = iceBoxExamineExceptionReportService.findByPage(reportMsg);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS,null, reportIPage);
    }

    /**
     * 导出异常报备记录
     * @param reportMsg
     * @return
     */
    @RequestMapping("sendExportMsg")
    public CommonResponse<IceBoxExamineExceptionReport> sendExportMsg(@RequestBody IceBoxExamineExceptionReportMsg reportMsg){
        return iceBoxExamineExceptionReportService.sendExportMsg(reportMsg);
    }

    /**
     * 查询冰柜巡检列表
     * @param reportMsg
     * @return
     */
    @RequestMapping("findIceExamineByPage")
    public CommonResponse<IPage<IceBoxExamineVo>> findIceExamineByPage(@RequestBody IceBoxExamineExceptionReportMsg reportMsg){
        IPage<IceBoxExamineVo> reportIPage = iceBoxExamineExceptionReportService.findIceExamineByPage(reportMsg);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS,null, reportIPage);
    }

    /**
     * 添加冰柜导入时间
     * @return
     */
    @RequestMapping("updateIceboxImportTime")
    public CommonResponse<Void> updateIceboxImportTime(){

        try {
            CompletableFuture.runAsync(()->iceBoxExamineExceptionReportService.updateIceboxImportTime(), ExecutorServiceFactory.getInstance());
        }catch (Exception e){
            return new CommonResponse(Constants.API_CODE_FAIL,e.getMessage());
        }
        return new CommonResponse(Constants.API_CODE_SUCCESS,null);
    }

    /**
     * 导出冰柜巡检记录
     * @param reportMsg
     * @return
     */
    @RequestMapping("sendIceExamineExportMsg")
    public CommonResponse<IceBoxExamineExceptionReport> sendIceExamineExportMsg(@RequestBody IceBoxExamineExceptionReportMsg reportMsg){
        return iceBoxExamineExceptionReportService.sendIceExamineExportMsg(reportMsg);
    }

    /**
     * @Date: 2021/1/8 14:42 xiao
     *  对历史数据(t_ice_box_examine_exception_report)补充审批人职务
     */
    @GetMapping("updateExamineUserOfficeName")
    public CommonResponse<Void> updateExamineUserOfficeName(){
        ExecutorServiceFactory.getInstance().execute(()->{
            iceBoxExamineExceptionReportService.updateExamineUserOfficeName();
        });
        return new CommonResponse<>(Constants.API_CODE_SUCCESS,null);
    }

}
