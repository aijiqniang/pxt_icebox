package com.szeastroc.icebox.newprocess.controller;


import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.vo.IceExamineVo;
import com.szeastroc.icebox.oldprocess.service.IceEventRecordService;
import com.szeastroc.icebox.oldprocess.vo.HisenseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/event")
public class IceboxEventController {


    @Resource
    private IceEventRecordService iceEventRecordService;

    // 海信相关接口

    @RequestMapping("/hisenseEvent")
    public CommonResponse<IceExamineVo> eventPush(@RequestBody List<HisenseDTO> hisenseDTOList) {

        iceEventRecordService.newEventPush(hisenseDTOList);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, "推送成功");
    }


}
