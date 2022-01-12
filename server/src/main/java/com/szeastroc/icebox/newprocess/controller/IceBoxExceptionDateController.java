package com.szeastroc.icebox.newprocess.controller;

import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.service.IceBoxService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Api(tags = {"冰柜异常数据处理接口"})
@RestController
@RequestMapping("/iceBoxExceptionDate")
public class IceBoxExceptionDateController {
    @Autowired
    private IceBoxService iceBoxService;
    //导入之后 直接对异常数据做处理
    @PostMapping("/handelIceBoxDate")
    @ApiOperation(value = "处理导入的异常数据", notes = "处理导入的异常数据", produces = "application/json")
    public CommonResponse handelIceBoxDate(@RequestParam("file") MultipartFile file) throws IOException {
        iceBoxService.handelIceBoxDate(file);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }
}
