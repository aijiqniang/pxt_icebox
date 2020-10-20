package com.szeastroc.icebox.newprocess.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.consumer.common.IceBoxPutReportMsg;
import com.szeastroc.icebox.newprocess.entity.IceBoxPutReport;
import com.szeastroc.icebox.newprocess.service.IceBoxPutReportService;
import com.szeastroc.icebox.newprocess.service.IceBoxTransferHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Slf4j
@RestController
@RequestMapping("iceBoxTransferHistory")
public class IceBoxTransferHistoryController {

    @Resource
    private IceBoxTransferHistoryService iceBoxTransferHistoryService;


//    @RequestMapping("findByPage")
//    public CommonResponse<IPage<IceBoxPutReport>> findByPage(IceBoxPutReportMsg reportMsg){
//        IPage<IceBoxPutReport> reportIPage = iceBoxPutReportService.findByPage(reportMsg);
//        return new CommonResponse<>(Constants.API_CODE_SUCCESS,null, reportIPage);
//    }
}
