package com.szeastroc.icebox.newprocess.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.szeastroc.common.annotation.MonitorAnnotation;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.consumer.common.IceBoxExamineExceptionReportMsg;
import com.szeastroc.icebox.newprocess.entity.IceBoxExamineExceptionReport;
import com.szeastroc.icebox.newprocess.entity.IceExamine;
import com.szeastroc.icebox.newprocess.service.IceExamineService;
import com.szeastroc.icebox.newprocess.vo.IceExamineVo;
import com.szeastroc.icebox.newprocess.vo.request.IceExamineRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 冰柜巡检
 */
@Slf4j
@RestController
@RequestMapping("/examine")
public class ExamineController {

    @Autowired
    private IceExamineService iceExamineService;

    @PostMapping("/findOneExamine")
    @MonitorAnnotation
    public CommonResponse<IceExamineVo> findOneExamine(@RequestBody IceExamineRequest iceExamineRequest) {
        if (iceExamineRequest == null) {
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }
        IceExamineVo iceExamineVo = iceExamineService.findOneExamine(iceExamineRequest);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, iceExamineVo);
    }


    @PostMapping("/findExamine")
    @MonitorAnnotation
    public CommonResponse<IPage<IceExamineVo>> findExamine(@RequestBody IceExamineRequest iceExamineRequest) {
        if (iceExamineRequest == null) {
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }
        IPage<IceExamineVo> page = iceExamineService.findExamine(iceExamineRequest);


        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, page);

    }


    @PostMapping("/doExamine")
    @MonitorAnnotation
    public CommonResponse<Boolean> doExamine(@RequestBody IceExamine iceExamine) {
        if (iceExamine == null) {
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }

        iceExamineService.doExamine(iceExamine);

        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, true);
    }

    @PostMapping("/doExamineNew")
    @MonitorAnnotation
    public CommonResponse<Map<String, Object>> doExamineNew(@RequestBody IceExamineVo iceExamineVo) {
        if (iceExamineVo == null) {
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }

        Map<String, Object> map = iceExamineService.doExamineNew(iceExamineVo);

        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null,map);
    }

    @RequestMapping("/dealIceExamineCheck")
    @MonitorAnnotation
    public CommonResponse<Void> dealIceExamineCheck(String redisKey, Integer status,Integer updateBy) {
        if (redisKey == null) {
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }

        iceExamineService.dealIceExamineCheck(redisKey,status,updateBy);

        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null,null);
    }

    @RequestMapping("/findExamineByNumber")
    @MonitorAnnotation
    public CommonResponse<IceExamineVo> findExamineByNumber(String examineNumber) {
        if (StringUtils.isEmpty(examineNumber)) {
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }

        IceExamineVo iceExamineVo = iceExamineService.findExamineByNumber(examineNumber);

        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, iceExamineVo);
    }
}
