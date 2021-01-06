package com.szeastroc.icebox.newprocess.controller;

import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.entity.icebox.vo.IceBoxRequest;
import com.szeastroc.common.entity.icebox.vo.IceBoxTransferHistoryVo;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.exception.NormalOptionException;
import com.szeastroc.common.utils.ExecutorServiceFactory;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.entity.PutStoreRelateModel;
import com.szeastroc.icebox.newprocess.service.IceBoxService;
import com.szeastroc.icebox.newprocess.service.IcePutApplyRelateBoxService;
import com.szeastroc.icebox.newprocess.vo.IceBoxVo;
import com.szeastroc.icebox.newprocess.vo.request.IceBoxRequestVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("iceBox")
public class MyIceBoxController {

    @Resource
    private IceBoxService iceBoxService;
    @Resource
    private IcePutApplyRelateBoxService icePutApplyRelateBoxService;

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
     * 查询冰柜列表：0-已投放，1-可供申请
     * @param requestVo
     * @return
     */
    @RequestMapping("findIceBoxListNew")
    public CommonResponse<List<IceBoxVo>> findIceBoxListNew(@RequestBody IceBoxRequestVo requestVo){
        List<IceBoxVo> iceBoxVos = iceBoxService.findIceBoxListNew(requestVo);
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
     * 查询冰柜列表：2-处理中
     * @param requestVo
     * @return
     */
    @RequestMapping("findPutingIceBoxListNew")
    public CommonResponse<Map<String,List<IceBoxVo>>> findPutingIceBoxListNew(@RequestBody IceBoxRequestVo requestVo){
        Map<String,List<IceBoxVo>> map = iceBoxService.findPutingIceBoxListNew(requestVo);
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
     * 审批冰柜
     * @param iceBoxRequest
     * @return
     */
    @PostMapping("/checkIceBoxNew")
    public void checkIceBoxNew(@RequestBody IceBoxRequest iceBoxRequest){
        iceBoxService.checkIceBoxNew(iceBoxRequest);
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
     * 根据门店编号获取所属冰柜信息(新)
     *
     * @param pxtNumber
     * @return
     * @throws NormalOptionException
     * @throws ImproperOptionException
     */
    @RequestMapping("/getIceBoxListNew")
    public CommonResponse<List<PutStoreRelateModel>> getIceBoxListNew(String pxtNumber) {
        if (StringUtils.isBlank(pxtNumber)) {
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, iceBoxService.getIceBoxListNew(pxtNumber));
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


    @RequestMapping("/autoAddLabel")
    public CommonResponse<String> autoAddLabel() {
        CompletableFuture.runAsync(() -> iceBoxService.autoAddLabel(), ExecutorServiceFactory.getInstance());
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }

    /**
     * 根据申请编号作废申请信息
     * @param iceBoxVo
     * @return
     */
    @RequestMapping("cancelApplyByNumber")
    public CommonResponse<IceBoxVo> cancelApplyByNumber(@RequestBody IceBoxVo iceBoxVo){
        iceBoxService.cancelApplyByNumber(iceBoxVo);
        return new CommonResponse(Constants.API_CODE_SUCCESS,null);
    }

    /**
     * 根据申请编号查询申请信息
     * @param applyNumber
     * @return
     */
    @RequestMapping("getApplyInfoByNumber")
    public CommonResponse<PutStoreRelateModel> getApplyInfoByNumber(String applyNumber){
        if(StringUtils.isBlank(applyNumber)){
            throw new ImproperOptionException("请求信息缺失申请编号！");
        }
        PutStoreRelateModel relateModel = iceBoxService.getApplyInfoByNumber(applyNumber);
        return new CommonResponse(Constants.API_CODE_SUCCESS,null,relateModel);
    }

    /**
     * 根据经销商id获取所有的冰柜
     * @param supplierId
     * @return
     */
    @RequestMapping("findIceBoxsBySupplierId")
    public CommonResponse<List<IceBoxVo>> findIceBoxsBySupplierId(Integer supplierId){
        if (supplierId == null) {
            return new CommonResponse(Constants.API_CODE_FAIL,"请求参数错误");
        }
        List<IceBoxVo> iceBoxList = iceBoxService.findIceBoxsBySupplierId(supplierId);
        return new CommonResponse(Constants.API_CODE_SUCCESS,null,iceBoxList);
    }

    /**
     * 根据经销商id获取所有的冰柜型号
     * @param supplierId
     * @return
     */
    @RequestMapping("findIceBoxsModelBySupplierId")
    public CommonResponse<List<Map<String,String>>> findIceBoxsModelBySupplierId(Integer supplierId){
        if (supplierId == null) {
            return new CommonResponse(Constants.API_CODE_FAIL,"请求参数错误");
        }
        List<Map<String,String>> iceBoxModelList = iceBoxService.findIceBoxsModelBySupplierId(supplierId);
        return new CommonResponse(Constants.API_CODE_SUCCESS,null,iceBoxModelList);
    }

    /**
     * 根据经销商id和冰柜型号获取所有的冰柜
     * @param supplierId
     * @return
     */
    @RequestMapping("findIceBoxsBySupplierIdAndModelId")
    public CommonResponse<List<IceBoxVo>> findIceBoxsBySupplierIdAndModelId(Integer supplierId,Integer modelId){
        if (supplierId == null || modelId == null) {
            return new CommonResponse(Constants.API_CODE_FAIL,"请求参数错误");
        }
        List<IceBoxVo> iceBoxList = iceBoxService.findIceBoxsBySupplierIdAndModelId(supplierId,modelId);
        return new CommonResponse(Constants.API_CODE_SUCCESS,null,iceBoxList);
    }
    /**
     * 转移冰柜
     * @param historyVo
     * @return
     */
    @RequestMapping("transferIceBoxs")
    public CommonResponse<Map<String, Object>> transferIceBoxs(@RequestBody IceBoxTransferHistoryVo historyVo){
        if (historyVo.getOldMarketAreaId() == null) {
            throw new ImproperOptionException("冰柜所属经销商的营销区域为空！");
        }
        if (historyVo.getNewMarketAreaId() == null) {
            throw new ImproperOptionException("冰柜转移经销商的营销区域为空！");
        }
        Map<String, Object> map = iceBoxService.transferIceBoxs(historyVo);
        return new CommonResponse(Constants.API_CODE_SUCCESS,null,map);
    }

    /**
     * 处理冰柜转移申请审批结果
     * @param historyVo
     * @return
     */
    @RequestMapping("dealTransferCheck")
    public CommonResponse<IceBoxTransferHistoryVo> dealTransferCheck(@RequestBody IceBoxTransferHistoryVo historyVo){
        iceBoxService.dealTransferCheck(historyVo);
        return new CommonResponse(Constants.API_CODE_SUCCESS,null,new IceBoxTransferHistoryVo());
    }
}
