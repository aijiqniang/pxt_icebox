package com.szeastroc.icebox.newprocess.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.szeastroc.common.annotation.MonitorAnnotation;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.entity.icebox.vo.IceExamineCheckVo;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.commondb.config.annotation.RedisLock;
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

import java.util.List;
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
//    @RedisLock(key = "#iceExamineVo.iceBoxId")
    public CommonResponse<Map<String, Object>> doExamineNew(@RequestBody IceExamineVo iceExamineVo) {
        if (iceExamineVo == null) {
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }

        Map<String, Object> map = iceExamineService.doExamineNew(iceExamineVo);

        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null,map);
    }

    @RequestMapping("/updateExamineStatus")
    @MonitorAnnotation
    public CommonResponse<IceExamineCheckVo> updateExamineStatus(@RequestBody IceExamineCheckVo iceExamineCheckVo) {
        if (StringUtils.isEmpty(iceExamineCheckVo.getRedisKey())) {
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }

        iceExamineService.dealIceExamineCheck(iceExamineCheckVo.getRedisKey(),iceExamineCheckVo.getStatus(),iceExamineCheckVo.getUpdateBy(),iceExamineCheckVo.getExamineRemark());

        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null,new IceExamineCheckVo());
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

    /**
     * 同步历史巡检数据到报表
     * @param ids
     * @return
     */
    @RequestMapping("syncExamineDataToReport")
    public CommonResponse<Void> syncExamineDataToReport(@RequestBody List<Integer> ids){
        iceExamineService.syncExamineDataToReport(ids);
        return new CommonResponse(Constants.API_CODE_SUCCESS,null);
    }
}
