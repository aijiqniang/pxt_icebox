package com.szeastroc.icebox.newprocess.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.service.IceBoxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;


@Slf4j
@RestController
@RequestMapping("/system")
public class SystemController {

    @Resource
    private IceBoxService iceBoxService;


    @GetMapping("/heart")
    public CommonResponse<Boolean> getSystemStatus() {

        IceBox iceBox = iceBoxService.getOne(Wrappers.<IceBox>lambdaQuery().last("limit 1"));

        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, true);
    }
}
