package com.szeastroc.icebox.newprocess.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.utils.ExecutorServiceFactory;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.consumer.common.IceBoxPutReportMsg;
import com.szeastroc.icebox.newprocess.entity.IceBoxPutReport;
import com.szeastroc.icebox.newprocess.service.IceBoxPutReportService;
import com.szeastroc.icebox.newprocess.vo.IceBoxPutReportVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("iceBoxPutReport")
public class IceBoxPutReportController {

    @Resource
    private IceBoxPutReportService iceBoxPutReportService;


    @RequestMapping("findByPage")
    public CommonResponse<IPage<IceBoxPutReportVo>> findByPage(@RequestBody IceBoxPutReportMsg reportMsg){
        IPage<IceBoxPutReportVo> reportIPage = iceBoxPutReportService.findByPage(reportMsg);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS,null, reportIPage);
    }

    @RequestMapping("sendExportMsg")
    public CommonResponse<IceBoxPutReport> sendExportMsg(@RequestBody IceBoxPutReportMsg reportMsg){
        return iceBoxPutReportService.sendExportMsg(reportMsg);
    }

    @RequestMapping("dealHistoryData")
    public CommonResponse<IceBoxPutReport> dealHistoryData(){
        iceBoxPutReportService.dealHistoryData();
        return new CommonResponse<>(Constants.API_CODE_SUCCESS,null);
    }

    /**
     * 同步历史投放数据到报表
     * @param ids
     * @return
     */
    @RequestMapping("syncPutDataToReport")
    public CommonResponse<Void> syncPutDataToReport(@RequestBody List<Integer> ids){
        try {
            iceBoxPutReportService.syncPutDataToReport(ids);
        }catch (Exception e){
            return new CommonResponse(Constants.API_CODE_FAIL,e.getMessage());
        }
        return new CommonResponse(Constants.API_CODE_SUCCESS,null);
    }

    /**
     * 定时任务检查没有同步的数据  自动同步
     * @return
     */
    @RequestMapping("syncPutDataToReportSchedule")
    public CommonResponse<Void> syncPutDataToReportSchedule(){


        try {
            CompletableFuture.runAsync(()->iceBoxPutReportService.syncPutDataToReportSchedule(), ExecutorServiceFactory.getInstance());
        }catch (Exception e){
            return new CommonResponse(Constants.API_CODE_FAIL,e.getMessage());
        }
        return new CommonResponse(Constants.API_CODE_SUCCESS,null);
    }

    /**
     * 修复历史数据的新增字段
     * @return
     */
    @RequestMapping("repairIceBoxColumns")
    public CommonResponse<Void> repairIceBoxColumns(){
        try {
            iceBoxPutReportService.repairIceBoxColumns();
        }catch (Exception e){
            log.error(e.getMessage(),e);
            return new CommonResponse(Constants.API_CODE_FAIL,e.getMessage());
        }
        return new CommonResponse(Constants.API_CODE_SUCCESS,null);
    }

    /**
     * 修复历史数据的新增字段
     * @return
     */
    @RequestMapping("repairIceBoxCycleType")
    public CommonResponse<Void> repairIceBoxCycleType(){
        try {
            iceBoxPutReportService.repairIceBoxCycleType();
        }catch (Exception e){
            log.error(e.getMessage(),e);
            return new CommonResponse(Constants.API_CODE_FAIL,e.getMessage());
        }
        return new CommonResponse(Constants.API_CODE_SUCCESS,null);
    }

}
