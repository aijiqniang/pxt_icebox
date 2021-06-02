package com.szeastroc.icebox.newprocess.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.entity.DisplayShelfPutApply;
import com.szeastroc.icebox.newprocess.service.DisplayShelfPutApplyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 业务员申请表 
 * </p>
 *
 * @author 陈超
 * @since 2021-06-01
 */
@Api(tags = {"业务员申请表 接口"}, description = "ShelfPutApplyController[陈超]")
@RestController
@RequestMapping("/shelf-put-apply")
    public class ShelfPutApplyController {
    
    @Autowired
    DisplayShelfPutApplyService shelfPutApplyService;

    @ApiOperation(value = "查询分页数据", notes = "查询分页数据接口", produces = "application/json")
    @GetMapping("/list")
    public CommonResponse<IPage<DisplayShelfPutApply>> findListByPage(@ModelAttribute Page<DisplayShelfPutApply> page, @ModelAttribute DisplayShelfPutApply displayShelfPutApply) {
        LambdaQueryWrapper<DisplayShelfPutApply> wrapper = new LambdaQueryWrapper<DisplayShelfPutApply>(displayShelfPutApply);
        IPage<DisplayShelfPutApply> pageList = shelfPutApplyService.page(page, wrapper);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, null);
    }

    @ApiOperation(value = "查询全部数据", notes = "查询全部数据接口", produces = "application/json")
    @GetMapping("/all")
    public CommonResponse<List<DisplayShelfPutApply>> findAll(){
        List<DisplayShelfPutApply> list = shelfPutApplyService.list();
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, null);
    }

    @ApiOperation(value = "根据id查询数据", notes = "根据id查询数据接口", produces = "application/json")
    @GetMapping("/findById")
    public CommonResponse<DisplayShelfPutApply> findById(@RequestParam Long id){
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, shelfPutApplyService.getById(id));
    }

    @ApiOperation(value = "新增数据", notes = "新增数据接口", produces = "application/json")
    @PostMapping(value = "/insert", produces = "application/json")
    public CommonResponse add(@RequestBody DisplayShelfPutApply shelfPutApply){
        boolean isOk = shelfPutApplyService.save(shelfPutApply);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, null);
    }

    @ApiOperation(value = "更新数据", notes = "更新数据接口", produces = "application/json")
    @PostMapping(value = "/update", produces = "application/json")
    public CommonResponse update(@RequestBody DisplayShelfPutApply displayShelfPutApply){
        boolean isOk = shelfPutApplyService.updateById(displayShelfPutApply);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, null);
    }

    @ApiOperation(value = "更新状态", notes = "更新状态接口", produces = "application/json")
    @PostMapping(value = "/updateStatus", produces = "application/json")
    public CommonResponse updateStatus(@RequestBody DisplayShelfPutApply displayShelfPutApply){
        boolean isOk = shelfPutApplyService.updateById(displayShelfPutApply);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, null);
    }

    @ApiOperation(value = "根据Id删除数据", notes = "根据Id删除数据接口", produces = "application/json")
    @DeleteMapping("/deleteById")
    public CommonResponse deleteById(@RequestParam Long id){
        boolean isOk = shelfPutApplyService.removeById(id);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, null);
    }

}

