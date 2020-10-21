package com.szeastroc.icebox.newprocess.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.entity.IceBoxTransferHistory;
import com.szeastroc.icebox.newprocess.service.IceBoxTransferHistoryService;
import com.szeastroc.icebox.newprocess.vo.request.IceTransferRecordPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

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


    @PostMapping("/report")
    public CommonResponse<IPage<IceBoxTransferHistory>> report(@RequestBody IceTransferRecordPage iceTransferRecordPage) {

        IPage<IceBoxTransferHistory> page = iceBoxTransferHistoryService.report(iceTransferRecordPage);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, page);
    }


    @PostMapping("/reportExport")
    public CommonResponse<Void> reportExport(@RequestBody IceTransferRecordPage iceTransferRecordPage) {
        iceBoxTransferHistoryService.reportExport(iceTransferRecordPage);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, null);
    }


    @GetMapping("/findByIceBoxId")
    public CommonResponse<List<IceBoxTransferHistory>> findByIceBoxId(@RequestParam Integer iceBoxId) {
        List<IceBoxTransferHistory> list = iceBoxTransferHistoryService.findByIceBoxId(iceBoxId);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, list);
    }
}
