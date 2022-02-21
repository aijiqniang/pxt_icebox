package com.szeastroc.icebox.newprocess.controller;

import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.exception.NormalOptionException;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.entity.ExamineError;
import com.szeastroc.icebox.newprocess.service.ExamineErrorService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.*;

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
    public CommonResponse<Void> insert(@RequestBody()ExamineError examineError){
        if(examineError == null){
            throw new NormalOptionException(Constants.API_CODE_FAIL,"参数不能为空");
        }
        if(StringUtils.isEmpty(examineError.getBoxAssetid())){
            throw new NormalOptionException(Constants.API_CODE_FAIL,"资产编号不能为空");
        }
        if(examineError.getDeptId() == null || examineError.getDeptId() == 0){
            throw new NormalOptionException(Constants.API_CODE_FAIL,"部门不能为空");
        }

        examineErrorService.insert(examineError);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, null);
    }

    @ApiOperation("id查询")
    @GetMapping("/selectById")
    public CommonResponse<ExamineError> selectById(@RequestParam("id")Integer id){
        ExamineError examineError = examineErrorService.getById(id);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, examineError);
    }

    @ApiOperation("更新")
    @PostMapping("/update")
    public CommonResponse<Void> update(@RequestBody()ExamineError examineError){
        if(examineError == null || examineError.getId() == null || examineError.getId() == 0){
            throw new NormalOptionException(Constants.API_CODE_FAIL,"id不能为空");
        }
        if(examineError.getUpdateUserId() == null || examineError.getUpdateUserId() == 0){
            throw new NormalOptionException(Constants.API_CODE_FAIL,"操作人不能为空");
        }
        ExamineError e = examineErrorService.getById(examineError.getId());
        if(e == null){
            throw new NormalOptionException(Constants.API_CODE_FAIL,"记录不存在");
        }
        if(e.getSendUserId1().equals(examineError.getUpdateUserId())){
            examineErrorService.updateById(examineError);
        }
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, null);
    }


}
