package com.szeastroc.icebox.client;

import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.client.constant.RegisterConstant;
import com.szeastroc.icebox.vo.IceBoxRequest;
import com.szeastroc.icebox.vo.IceBoxTransferHistoryVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.*;

/**
 * Created by hbl
 * 2020.04.22
 */
@FeignClient(name = RegisterConstant.REGISTER_NAME)
public interface FeignIceBoxClient {

    @PostMapping("/iceBox/checkIceBox")
    void checkIceBox(@RequestBody IceBoxRequest iceBoxRequest);

    @PostMapping("/iceBox/checkIceBoxNew")
    void checkIceBoxNew(@RequestBody IceBoxRequest iceBoxRequest);


    @RequestMapping("/refund/updateExamineStatus")
    CommonResponse updateExamineStatus(@RequestBody IceBoxRequest iceBoxRequest);

    @RequestMapping("/iceBox/dealTransferCheck")
    CommonResponse<Void> dealTransferCheck(@RequestBody IceBoxTransferHistoryVo historyVo);

    @RequestMapping("/examine/dealIceExamineCheck")
    CommonResponse dealIceExamineCheck(@RequestParam("redisKey") String redisKey, @RequestParam("status") Integer status);

    @GetMapping("/store/judge/customer/bindIceBox")
    CommonResponse<Boolean> judgeCustomerBindIceBox(@RequestParam("number") String  number);
}
