package com.szeastroc.icebox.newprocess.controller;

import com.alibaba.fastjson.JSON;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.utils.ExecutorServiceFactory;
import com.szeastroc.common.utils.HttpUtils;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.service.IceBoxService;
import com.szeastroc.icebox.newprocess.vo.IceBoxVo;
import com.szeastroc.icebox.newprocess.vo.request.IceBoxRequestVo;
import com.szeastroc.icebox.vo.IceBoxRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("iceBox")
public class MyIceBoxController {

    @Resource
    private IceBoxService iceBoxService;

    /**
     * 查询冰柜列表：0-已投放，1-可供申请，2-处理中
     * @param requestVo
     * @return
     */
    @RequestMapping("findIceBoxList")
    public CommonResponse<List<IceBoxVo>> findIceBoxList(@RequestBody IceBoxRequestVo requestVo){
        List<IceBoxVo> iceBoxVos = iceBoxService.findIceBoxList(requestVo);
        return new CommonResponse(Constants.API_CODE_SUCCESS,null,iceBoxVos);
    }

    /**
     * 根据冰柜所属经销商和冰柜型号查询冰柜信息
     * @param supplierId
     * @param modelId
     * @return
     */
    @RequestMapping("findBySupplierIdAndModelId")
    public CommonResponse<List<IceBoxVo>> findBySupplierIdAndModelId(Integer supplierId, Integer modelId){
        IceBoxVo iceBoxVo = iceBoxService.findBySupplierIdAndModelId(supplierId,modelId);
        return new CommonResponse(Constants.API_CODE_SUCCESS,null,iceBoxVo);
    }

    @RequestMapping("submitApply")
    public CommonResponse<Map<String,Object>> submitApply(@RequestBody List<IceBoxRequestVo> iceBoxRequestVos) throws InterruptedException {
        Map<String,Object> map = iceBoxService.submitApply(iceBoxRequestVos);
        return new CommonResponse(Constants.API_CODE_SUCCESS,null,map);
    }

    /**
     * 审批冰柜
     * @param iceBoxRequest
     * @return
     */
    @PostMapping("/checkIceBox")
    public CommonResponse<IceBoxRequest> checkIceBox(@RequestBody IceBoxRequest iceBoxRequest){
        iceBoxService.checkIceBox(iceBoxRequest);
        return new CommonResponse(Constants.API_CODE_SUCCESS,null);
    }
}
