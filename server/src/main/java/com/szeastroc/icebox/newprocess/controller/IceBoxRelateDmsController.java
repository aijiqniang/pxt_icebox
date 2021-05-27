package com.szeastroc.icebox.newprocess.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.consumer.common.IceBoxPutReportMsg;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.entity.IceBoxPutReport;
import com.szeastroc.icebox.newprocess.entity.IceBoxRelateDms;
import com.szeastroc.icebox.newprocess.service.IceBoxRelateDmsService;
import com.szeastroc.icebox.newprocess.vo.IceBoxPutReportVo;
import com.szeastroc.icebox.newprocess.vo.IceBoxRelateDmsVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * TODO
 *
 * @author Aijinqiang
 * @version 1.0
 * @date 2021/5/11 10:14
 */
@Slf4j
@RestController
@RequestMapping("iceBoxRelateDms")
@Api(tags = "冰柜配送")
public class IceBoxRelateDmsController {

    @Resource
    private IceBoxRelateDmsService iceBoxRelateDmsService;

    @ApiOperation(value = "通过id查询投放数据")
    @GetMapping("findById")
    public CommonResponse<IceBoxRelateDmsVo> findById(@RequestParam("id")Integer id) {
        IceBoxRelateDmsVo dmsVo = iceBoxRelateDmsService.findById(id);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, dmsVo);
    }

    @ApiOperation(value = "确认接单")
    @PostMapping("confirmAccept")
    public CommonResponse<Void>  confirmAccept(@RequestBody(required = false)IceBoxRelateDmsVo iceBoxRelateDmsVo){
        try{
            iceBoxRelateDmsService.confirmAccept(iceBoxRelateDmsVo);
        }catch (Exception e){
            return new CommonResponse(Constants.API_CODE_FAIL,e.getMessage());
        }
        return new CommonResponse(Constants.API_CODE_SUCCESS,null);
    }

    @ApiOperation(value = "扫描二维码获取冰柜信息")
    @GetMapping("getIceBoxInfoByQrcode")
    public CommonResponse<IceBox> getIceBoxInfoByQrcode(@RequestParam("qrcode")String qrcode){
        if(StringUtils.isEmpty(qrcode)){
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }
        return new CommonResponse(Constants.API_CODE_SUCCESS,null,iceBoxRelateDmsService.getIceBoxInfoByQrcode(qrcode));
    }

    @ApiOperation(value = "确认送达")
    @PostMapping("confirmArrvied")
    public CommonResponse<Void>  confirmArrvied(@RequestBody(required = false)IceBoxRelateDmsVo iceBoxRelateDmsVo){
        try{
            iceBoxRelateDmsService.confirmArrvied(iceBoxRelateDmsVo);
        }catch (Exception e){
            return new CommonResponse(Constants.API_CODE_FAIL,e.getMessage());
        }
        return new CommonResponse(Constants.API_CODE_SUCCESS,null);
    }
}
