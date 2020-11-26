package com.szeastroc.icebox.newprocess.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.exception.NormalOptionException;
import com.szeastroc.common.utils.ExecutorServiceFactory;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.consumer.common.IceBoxPutReportMsg;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.entity.IceBoxPutReport;
import com.szeastroc.icebox.newprocess.entity.PutStoreRelateModel;
import com.szeastroc.icebox.newprocess.service.IceBoxPutReportService;
import com.szeastroc.icebox.newprocess.service.IceBoxService;
import com.szeastroc.icebox.newprocess.service.IcePutApplyRelateBoxService;
import com.szeastroc.icebox.newprocess.vo.IceBoxVo;
import com.szeastroc.icebox.newprocess.vo.request.IceBoxRequestVo;
import com.szeastroc.icebox.vo.IceBoxRequest;
import com.szeastroc.icebox.vo.IceBoxTransferHistoryVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("iceBoxPutReport")
public class IceBoxPutReportController {

    @Resource
    private IceBoxPutReportService iceBoxPutReportService;


    @RequestMapping("findByPage")
    public CommonResponse<IPage<IceBoxPutReport>> findByPage(@RequestBody IceBoxPutReportMsg reportMsg){
        IPage<IceBoxPutReport> reportIPage = iceBoxPutReportService.findByPage(reportMsg);
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

}
