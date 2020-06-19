package com.szeastroc.icebox.newprocess.controller;

import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.exception.NormalOptionException;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.service.IceBoxService;
import com.szeastroc.icebox.newprocess.vo.IceBoxVo;
import com.szeastroc.icebox.newprocess.vo.request.IceBoxRequestVo;
import com.szeastroc.icebox.vo.IceBoxRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("iceBox")
public class MyIceBoxController {

    @Resource
    private IceBoxService iceBoxService;

    /**
     * 查询冰柜列表：0-已投放，1-可供申请
     * @param requestVo
     * @return
     */
    @RequestMapping("findIceBoxList")
    public CommonResponse<List<IceBoxVo>> findIceBoxList(@RequestBody IceBoxRequestVo requestVo){
        List<IceBoxVo> iceBoxVos = iceBoxService.findIceBoxList(requestVo);
        return new CommonResponse(Constants.API_CODE_SUCCESS,null,iceBoxVos);
    }
    /**
     * 查询冰柜列表：2-处理中
     * @param requestVo
     * @return
     */
    @RequestMapping("findPutingIceBoxList")
    public CommonResponse<Map<String,List<IceBoxVo>>> findPutingIceBoxList(@RequestBody IceBoxRequestVo requestVo){
        Map<String,List<IceBoxVo>> map = iceBoxService.findPutingIceBoxList(requestVo);
        return new CommonResponse(Constants.API_CODE_SUCCESS,null,map);
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

    /**
     * 申请冰柜
     * @param iceBoxRequestVos
     * @return
     * @throws InterruptedException
     */
    @RequestMapping("submitApply")
    public CommonResponse<Map<String,Object>> submitApply(@RequestBody List<IceBoxRequestVo> iceBoxRequestVos) throws InterruptedException {
        Map<String,Object> map = iceBoxService.submitApply(iceBoxRequestVos);
        return new CommonResponse(Constants.API_CODE_SUCCESS,null,map);
    }

    /**
     * 申请冰柜(新)
     * @param iceBoxRequestVos
     * @return
     * @throws InterruptedException
     */
    @RequestMapping("submitApplyNew")
    public CommonResponse<Map<String,Object>> submitApplyNew(@RequestBody List<IceBoxRequestVo> iceBoxRequestVos) throws InterruptedException {
        Map<String,Object> map = iceBoxService.submitApplyNew(iceBoxRequestVos);
        return new CommonResponse(Constants.API_CODE_SUCCESS,null,map);
    }

    /**
     * 审批冰柜
     * @param iceBoxRequest
     * @return
     */
    @PostMapping("/checkIceBox")
    public void checkIceBox(@RequestBody IceBoxRequest iceBoxRequest){
        iceBoxService.checkIceBox(iceBoxRequest);
//        return new CommonResponse(Constants.API_CODE_SUCCESS,null);
    }
    /**
     * 根据门店编号获取所属冰柜信息
     *
     * @param pxtNumber
     * @return
     * @throws NormalOptionException
     * @throws ImproperOptionException
     */
    @RequestMapping("/getIceBoxList")
    public CommonResponse<List<IceBox>> getIceBoxList(String pxtNumber) {
        if (StringUtils.isBlank(pxtNumber)) {
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, iceBoxService.getIceBoxList(pxtNumber));
    }
    /**
     * 根据门店编号已签收的冰柜信息
     * @param pxtNumber
     * @return
     */
    @RequestMapping("findPutIceBoxList")
    public CommonResponse<List<IceBoxVo>> findPutIceBoxList(String pxtNumber){
        List<IceBoxVo> iceBoxVos = iceBoxService.findPutIceBoxList(pxtNumber);
        return new CommonResponse(Constants.API_CODE_SUCCESS,null,iceBoxVos);
    }
}
