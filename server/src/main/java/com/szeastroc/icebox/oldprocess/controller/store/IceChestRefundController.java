package com.szeastroc.icebox.oldprocess.controller.store;

import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.oldprocess.service.WechatTransferOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by Tulane
 * 2019/8/19
 */
@Slf4j
@RestController
@RequestMapping("/external/store/refund")
public class IceChestRefundController {

    @Autowired
    private WechatTransferOrderService wechatTransferOrderService;

    /**
     * 退回冰柜
     * @param iceChestId
     * @param clientId
     * @return
     */
    @GetMapping("/takeBackIceChest")
    public CommonResponse<String> takeBackIceChest(Integer iceChestId, Integer clientId) throws Exception {
        if(iceChestId == null || clientId == null){
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }
        wechatTransferOrderService.takeBackIceChest(iceChestId, clientId);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }
}
