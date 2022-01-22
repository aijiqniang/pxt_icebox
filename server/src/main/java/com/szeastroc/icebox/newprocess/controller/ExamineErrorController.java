package com.szeastroc.icebox.newprocess.controller;

import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.exception.NormalOptionException;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.entity.ExamineError;
import com.szeastroc.icebox.newprocess.service.ExamineErrorService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * TODO
 *
 * @author Aijinqiang
 * @version 1.0
 * @date 2022/1/5 14:52
 */
@RestController
@RequestMapping("/examineError")
@Api(tags = "无法巡检")
public class ExamineErrorController {

    @Resource
    private ExamineErrorService examineErrorService;


    @ApiOperation("新增无法巡检")
    @PostMapping("/insert")
    public CommonResponse<Void> cantExamin(@RequestBody()ExamineError examineError){
        if(examineError == null){
            throw new NormalOptionException(Constants.API_CODE_FAIL,"参数不能为空");
        }
        if(StringUtils.isEmpty(examineError.getBoxAssetid())){
            throw new NormalOptionException(Constants.API_CODE_FAIL,"资产编号不能为空");
        }
        if(examineError.getDeptId() == null || examineError.getDeptId() == 0){
            throw new NormalOptionException(Constants.API_CODE_FAIL,"部门不能为空");
        }
        if(examineError.getOfficeId() == null || examineError.getOfficeId() == 0){
            throw new NormalOptionException(Constants.API_CODE_FAIL,"职位不能为空");
        }

        examineErrorService.insert(examineError);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, null);
    }
}
