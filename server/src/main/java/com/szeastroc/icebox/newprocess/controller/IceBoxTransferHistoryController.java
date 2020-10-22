package com.szeastroc.icebox.newprocess.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.entity.IceBoxTransferHistory;
import com.szeastroc.icebox.newprocess.enums.ExamineStatus;
import com.szeastroc.icebox.newprocess.service.IceBoxTransferHistoryService;
import com.szeastroc.icebox.newprocess.vo.IceBoxTransferHistoryPageVo;
import com.szeastroc.icebox.newprocess.vo.IceBoxTransferHistoryVo;
import com.szeastroc.icebox.newprocess.vo.request.ExamineStatusVo;
import com.szeastroc.icebox.newprocess.vo.request.IceTransferRecordPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
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
    @RequestMapping("/getExamineStatus")
    public CommonResponse<List<ExamineStatusVo>> getExamineStatus() {
        List<ExamineStatusVo> list = new ArrayList<>();
        for (ExamineStatus examineStatus : ExamineStatus.values()) {
            Integer status = examineStatus.getStatus();
            String desc = examineStatus.getDesc();
            ExamineStatusVo examineStatusVo = ExamineStatusVo.builder()
                    .type(status)
                    .message(desc)
                    .build();
            list.add(examineStatusVo);
        }
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, list);
    }


    @PostMapping("/report")
    public CommonResponse<IPage<IceBoxTransferHistoryPageVo>> report(@RequestBody IceTransferRecordPage iceTransferRecordPage) {

        IPage<IceBoxTransferHistoryPageVo> page = iceBoxTransferHistoryService.report(iceTransferRecordPage);
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

    @RequestMapping("findListBySupplierId")
    public CommonResponse<List<IceBoxTransferHistoryVo>> findListBySupplierId(@RequestParam("supplierId") Integer supplierId) {
        List<IceBoxTransferHistoryVo> historyVos = iceBoxTransferHistoryService.findListBySupplierId(supplierId);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, historyVos);

    }
}
