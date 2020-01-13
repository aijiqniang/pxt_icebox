package com.szeastroc.icebox.controller.manage;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.service.IceChestPutRecordService;
import com.szeastroc.icebox.vo.IceDepositResponse;
import com.szeastroc.icebox.vo.query.IceDepositPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 报表模块
 */
@Slf4j
@RestController
@RequestMapping("/report")
public class ReportController {

    @Autowired
    private IceChestPutRecordService iceChestPutRecordService;

    /**
     * 查询押金明细
     * @param iceDepositPage
     * @return
     */
    @PostMapping("/queryIceDeposits")
    public CommonResponse<IPage<IceDepositResponse>> queryIceDeposits(@RequestBody IceDepositPage iceDepositPage){
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null,
                iceChestPutRecordService.queryIceDeposits(iceDepositPage));
    }
}
