package com.szeastroc.icebox.client;

import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.client.constant.RegisterConstant;
import com.szeastroc.icebox.vo.IceBoxRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Created by hbl
 * 2020.04.22
 */
@FeignClient(name = RegisterConstant.REGISTER_NAME)
public interface FeignIceBoxClient {

    @PostMapping("/iceBox/checkIceBox")
    CommonResponse<IceBoxRequest> checkIceBox(@RequestBody IceBoxRequest iceBoxRequest);
}
