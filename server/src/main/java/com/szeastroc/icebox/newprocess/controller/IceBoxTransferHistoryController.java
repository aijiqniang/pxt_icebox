package com.szeastroc.icebox.newprocess.controller;

import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.service.IceBoxTransferHistoryService;
import com.szeastroc.icebox.newprocess.vo.IceBoxTransferHistoryVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("iceBoxTransferHistory")
public class IceBoxTransferHistoryController {

    @Resource
    private IceBoxTransferHistoryService iceBoxTransferHistoryService;


    @RequestMapping("findListBySupplierId")
    public CommonResponse<List<IceBoxTransferHistoryVo>> findListBySupplierId(@RequestParam("supplierId") Integer supplierId){
        List<IceBoxTransferHistoryVo> historyVos = iceBoxTransferHistoryService.findListBySupplierId(supplierId);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS,null, historyVos);
    }
}
