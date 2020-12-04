package com.szeastroc.icebox.newprocess.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.consumer.common.IceBoxPutReportMsg;
import com.szeastroc.icebox.newprocess.entity.IceBoxPutReport;
import com.szeastroc.icebox.newprocess.entity.OldIceBoxSignNotice;
import com.szeastroc.icebox.newprocess.service.IceBoxPutReportService;
import com.szeastroc.icebox.newprocess.service.OldIceBoxSignNoticeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("oldIceBoxSignNotice")
public class OldIceBoxSignNoticeController {

    @Resource
    private OldIceBoxSignNoticeService oldIceBoxSignNoticeService;


    @RequestMapping("findListByPxtNumber")
    public CommonResponse<List<OldIceBoxSignNotice>> findListByPxtNumber(String pxtNumber){
        List<OldIceBoxSignNotice> list = oldIceBoxSignNoticeService.findListByPxtNumber(pxtNumber);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS,null, list);
    }


}
