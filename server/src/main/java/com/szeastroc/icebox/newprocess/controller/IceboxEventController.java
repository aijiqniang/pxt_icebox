package com.szeastroc.icebox.newprocess.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.utils.ExecutorServiceFactory;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.entity.IceAlarm;
import com.szeastroc.icebox.newprocess.service.IceAlarmService;
import com.szeastroc.icebox.newprocess.vo.IceEventVo;
import com.szeastroc.icebox.newprocess.vo.IceExamineVo;
import com.szeastroc.icebox.oldprocess.service.IceEventRecordService;
import com.szeastroc.icebox.oldprocess.vo.HisenseDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Param;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/event")
@Api(tags = "智能冰柜")
public class IceboxEventController {


    @Resource
    private IceEventRecordService iceEventRecordService;
    @Resource
    private IceAlarmService iceAlarmService;
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

    @GetMapping("/createTableMonth")
    public void createTableMonth(){
        iceEventRecordService.createTableMonth();
    };


    @ApiOperation("智能冰柜小程序列表")
    @GetMapping("/xfaList")
    public CommonResponse<List<IceEventVo.IceboxList>> xfaList(@RequestParam("userId")Integer userId,@RequestParam(required = false,value = "assetId")String assetId,@RequestParam(required = false,value = "relateCode")String relateCode){
        if(userId == null || userId == 0){
            return new CommonResponse<List<IceEventVo.IceboxList>>(Constants.API_CODE_FAIL, "用户不能为空",null);
        }
        List<IceEventVo.IceboxList> list = iceEventRecordService.xfaList(userId,assetId,relateCode);
        return new CommonResponse<List<IceEventVo.IceboxList>>(Constants.API_CODE_SUCCESS, null,list);
    }

    @ApiOperation("智能冰柜详情")
    @GetMapping("/boxDetail")
    public CommonResponse<IceEventVo.IceboxDetail> boxDetail(@RequestParam("assetId")String assetId){
        if(StringUtils.isEmpty(assetId)){
            return new CommonResponse<IceEventVo.IceboxDetail>(Constants.API_CODE_FAIL, "资产id不能为空",null);
        }
        IceEventVo.IceboxDetail detail = iceEventRecordService.boxDetail(assetId);
        return new CommonResponse<IceEventVo.IceboxDetail>(Constants.API_CODE_SUCCESS, null,detail);
    }

    @ApiOperation("异常反馈")
    @PostMapping("/submitFeedBack")
    public CommonResponse<Void> submitFeedBack(@RequestBody(required = false)IceAlarm iceAlarm){
        iceAlarm.setUpdateTime(new Date());
        iceAlarmService.updateById(iceAlarm);
        return new CommonResponse(Constants.API_CODE_SUCCESS, null,null);
    }

    @ApiOperation("定时任务消除报警")
    @GetMapping("/sychAlarm")
    public void sychAlarm(@RequestParam(value = "alarmId",required = false)Integer alarmId){
        iceEventRecordService.sychAlarm(alarmId);
    }

    @ApiOperation("定时任务消除人流量报警")
    @GetMapping("/sychAlarmPerson")
    public void sychAlarmPerson(@RequestParam(value = "alarmId",required = false)Integer alarmId){
        iceEventRecordService.sychAlarmPerson(alarmId);
    }

    @ApiOperation(("后台报警记录"))
    @PostMapping("/getAlarmList")
    public CommonResponse<IPage<IceAlarm>> getAlarmList(@RequestBody  IceAlarm.PageRequest pageRequest){
        IPage<IceAlarm> list = iceAlarmService.findByPage(pageRequest);
        return new CommonResponse<IPage<IceAlarm>>(Constants.API_CODE_SUCCESS, null,list);
    }

    @ApiOperation(("通过类型获取报警反馈"))
    @GetMapping("/getFeedBacks")
    public CommonResponse<Map<String,String>> getFeedBacks(@RequestParam(value = "type",required = false)Integer type){
        Map<String,String> str = iceAlarmService.getFeedBacks(type);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null,str);
    }


}
