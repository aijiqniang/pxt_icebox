package com.szeastroc.icebox.newprocess.controller;

import com.alibaba.fastjson.JSON;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.utils.ExecutorServiceFactory;
import com.szeastroc.common.utils.HttpUtils;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.service.IceBoxService;
import com.szeastroc.icebox.newprocess.vo.IceBoxVo;
import com.szeastroc.icebox.newprocess.vo.request.IceBoxRequestVo;
import lombok.extern.slf4j.Slf4j;
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
    public CommonResponse<Map<String,String>> submitApply(@RequestBody IceBoxRequestVo IceBoxRequestVo) throws InterruptedException {
        Map<String,String> map = iceBoxService.submitApply(IceBoxRequestVo);
        return new CommonResponse(Constants.API_CODE_SUCCESS,null,map);
    }

    @RequestMapping("test")
    public CommonResponse<Map<String,String>> test(@RequestBody IceBoxRequestVo IceBoxRequestVo) throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            CompletableFuture.runAsync(() -> {
                try {
                    Thread thread = Thread.currentThread();
                    IceBoxRequestVo requestVo = new IceBoxRequestVo();
                    requestVo.setStoreNumber("ceshi000001-->"+thread.getName());
                    requestVo.setModelId(1);
                    requestVo.setSupplierId(510);
                    Map<String,String> map = iceBoxService.submitApply(requestVo);
                    log.info("申请到的冰柜---》【{}】", JSON.toJSONString(map));
                }catch (Exception e) {
                    log.error("通知dms客户新增人员出错");
                    e.printStackTrace();
                }
            }, ExecutorServiceFactory.getInstance());

        }//此处 设置数值  受限于 线程池中的数量
        return new CommonResponse(Constants.API_CODE_SUCCESS,null);
    }



}
