package com.szeastroc.icebox.newprocess.controller;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.utils.ExecutorServiceFactory;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.entity.IceBoxHandover;
import com.szeastroc.icebox.newprocess.service.IceBoxHandoverService;
import com.szeastroc.icebox.newprocess.vo.request.IceBoxHandoverPage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 *
 *
 * @author Aijinqiang
 * @version 1.0
 * @date 2021/6/3 10:30
 */
@RestController
@RequestMapping("/iceBoxHandover")
@RequiredArgsConstructor(onConstructor = @_(@Autowired))
@Api(tags = "冰柜交接")
public class IceBoxHandOverController {

    private final IceBoxHandoverService iceBoxHandoverService;

    /**
     * sfa冰柜交接列表
     * @param sendUserId
     * @param storeName
     * @return
     */
    @ApiOperation(value = "sfa交接列表")
    @GetMapping("/findByUserid")
    public CommonResponse<Map<String, List<Map<String,Object>>>> findByUserid(@RequestParam("sendUserId")Integer sendUserId,@RequestParam(value = "receiveUserId",required = false)Integer receiveUserId ,@RequestParam(value = "storeName",required = false)String storeName,@RequestParam(value = "relateCode",required = false)String relateCode){
        if(sendUserId == null  || sendUserId == 0){
            return new CommonResponse<>(Constants.API_CODE_FAIL, "业务员编号不能为空",null);
        }
        Map<String, List<Map<String,Object>>> map = iceBoxHandoverService.findByUseridNew(sendUserId,receiveUserId,storeName,relateCode);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null,map);
    }

    /**
     * 交接记录后台列表
     * @param iceBoxHandoverPage
     * @return
     */
    @ApiOperation(value = "交接记录后台列表")
    @PostMapping("/findByPage")
    public CommonResponse<IPage<IceBoxHandover>> findByPage(@RequestBody IceBoxHandoverPage iceBoxHandoverPage){
        IPage<IceBoxHandover> page = iceBoxHandoverService.findByPage(iceBoxHandoverPage);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS,null, page);
    }

    /**
     * 导出
     * @param iceBoxHandoverPage
     * @return
     */
    @ApiOperation(value = "导出")
    @PostMapping("/exportIceHandover")
    public CommonResponse exportIceHandover(@RequestBody IceBoxHandoverPage iceBoxHandoverPage){
        return iceBoxHandoverService.exportIceHandover(iceBoxHandoverPage);
    }

    /**
     * 发送交接申请
     * @return
     */
    @ApiOperation(value = "发送交接申请")
    @PostMapping("/sendHandOverRequest")
    public CommonResponse sendHandOverRequest(@RequestBody()List<IceBoxHandover> iceBoxHandovers){
        if(iceBoxHandovers.size()>0){
            for(IceBoxHandover iceBoxHandover : iceBoxHandovers){
                if(iceBoxHandover.getSendUserId() == null  || iceBoxHandover.getSendUserId() == 0){
                    return new CommonResponse<>(Constants.API_CODE_FAIL, "交接人员id不能为空",null);
                }
                if(iceBoxHandover.getReceiveUserId() == null  || iceBoxHandover.getReceiveUserId() == 0){
                    return new CommonResponse<>(Constants.API_CODE_FAIL, "接收人员id不能为空",null);
                }
                if(iceBoxHandover.getIceBoxId() == null || iceBoxHandover.getIceBoxId() == 0){
                    return new CommonResponse<>(Constants.API_CODE_FAIL, "冰柜id不能未空",null);
                }
            }
        }else{
            return new CommonResponse<>(Constants.API_CODE_FAIL, "参数为空",null);
        }
        iceBoxHandoverService.sendHandOverRequest(iceBoxHandovers);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null,null);
    }

    @ApiOperation(value = "接收交接")
    @PostMapping("/passHandOverRequest")
    public CommonResponse passHandOverRequest(@RequestBody List<Integer> ids){
        if(ids.size() <= 0){
            return new CommonResponse<>(Constants.API_CODE_FAIL, "参数为空",null);
        }
        iceBoxHandoverService.passHandOverRequest(ids);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null,null);
    }

    @ApiOperation(value = "驳回交接")
    @PostMapping("/rejectHandOverRequest")
    public CommonResponse rejectHandOverRequest(@RequestBody List<Integer> ids){
        if(ids.size() <= 0){
            return new CommonResponse<>(Constants.API_CODE_FAIL, "参数为空",null);
        }
        iceBoxHandoverService.rejectHandOverRequest(ids);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null,null);
    }
    /**
     * 更新冰柜责任人
     * @param iceboxIds
     * @return
     */
    @PostMapping("/updateResponseMan")
    public CommonResponse updateResponseMan(@RequestBody List<Integer> iceboxIds){
        try {
            CompletableFuture.runAsync(()->iceBoxHandoverService.updateResponseMan(iceboxIds), ExecutorServiceFactory.getInstance());
        }catch (Exception e){
            return new CommonResponse(Constants.API_CODE_FAIL,e.getMessage());
        }
        return new CommonResponse(Constants.API_CODE_SUCCESS,null);
    }
}
