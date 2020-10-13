package com.szeastroc.icebox.client;

import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.client.constant.RegisterConstant;
import com.szeastroc.icebox.vo.IceBoxRequest;
import org.springframework.cloud.openfeign.FeignClient;
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

    @GetMapping("/store/judge/customer/bindIceBox")
    CommonResponse<Boolean> judgeCustomerBindIceBox(@RequestParam("number") String  number);
}
