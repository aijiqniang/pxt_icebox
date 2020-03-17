package com.szeastroc.icebox.newprocess.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.entity.IceExamine;
import com.szeastroc.icebox.newprocess.service.IceExamineService;
import com.szeastroc.icebox.newprocess.vo.IceExamineVo;
import com.szeastroc.icebox.newprocess.vo.request.IceExamineRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public CommonResponse<IceExamineVo> findOneExamine(@RequestBody IceExamineRequest iceExamineRequest) {
        if (iceExamineRequest == null) {
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }
        IceExamineVo iceExamineVo = iceExamineService.findOneExamine(iceExamineRequest);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, iceExamineVo);
    }


    @PostMapping("/findExamine")
    public CommonResponse<IPage<IceExamineVo>> findExamine(@RequestBody IceExamineRequest iceExamineRequest) {
        if (iceExamineRequest == null) {
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }
        IPage<IceExamineVo> page = iceExamineService.findExamine(iceExamineRequest);


        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, page);

    }


    @PostMapping("/doExamine")
    public CommonResponse<Boolean> doExamine(@RequestBody IceExamine iceExamine) {
        if (iceExamine == null) {
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }

        iceExamineService.doExamine(iceExamine);

        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, true);
    }
}
