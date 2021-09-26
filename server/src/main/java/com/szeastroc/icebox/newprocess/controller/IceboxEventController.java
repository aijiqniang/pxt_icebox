package com.szeastroc.icebox.newprocess.controller;


import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.vo.IceEventVo;
import com.szeastroc.icebox.newprocess.vo.IceExamineVo;
import com.szeastroc.icebox.oldprocess.service.IceEventRecordService;
import com.szeastroc.icebox.oldprocess.vo.HisenseDTO;
import io.swagger.annotations.Api;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Param;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/event")
@Api(tags = "智能冰柜")
public class IceboxEventController {


    @Resource
    private IceEventRecordService iceEventRecordService;

    // 海信相关接口

    @RequestMapping("/hisenseEvent")
    public CommonResponse<IceExamineVo> eventPush(@RequestBody List<HisenseDTO> hisenseDTOList) {

        iceEventRecordService.newEventPush(hisenseDTOList);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, "推送成功");
    }

    @GetMapping("/createTable")
    public CommonResponse<Void> createTable(@Param("startTime")String startTime,@Param("endTime") String endTime){
        iceEventRecordService.createTable(startTime,endTime);
        return  new CommonResponse<Void>();
    }

    @GetMapping("/xfaList")
    public CommonResponse<IceEventVo.IceboxList> xfaList(@RequestParam("userId")Integer userId,@RequestParam(required = false,value = "assetId")String assetId){
        `   `
    }
}
