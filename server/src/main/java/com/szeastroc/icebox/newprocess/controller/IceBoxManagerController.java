package com.szeastroc.icebox.newprocess.controller;

import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.entity.IceModel;
import com.szeastroc.icebox.newprocess.service.IceBoxService;
import com.szeastroc.icebox.newprocess.service.IceModelService;
import com.szeastroc.icebox.newprocess.vo.IceBoxManagerVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/manager")
public class IceBoxManagerController {

    @Resource
    private IceModelService iceModelService;
    @Resource
    private IceBoxService iceBoxService;


    /**
     * 获取所有的冰柜型号
     *
     * @return
     */
    @GetMapping("/getAllModel")
    public CommonResponse<List<IceModel>> getAllModel() {
        List<IceModel> list = iceModelService.getAllModel();
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, list);
    }

    /**
     * 获取所有的冰柜状态
     *
     * @return
     */
    @GetMapping("/getAllStatus")
    public CommonResponse<Void> getAllStatus() {
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }


    @PostMapping("/changeIcebox")
    public CommonResponse<Void> changeIcebox(@RequestBody IceBoxManagerVo iceBoxManagerVo) {
        iceBoxService.changeIcebox(iceBoxManagerVo);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }


    @GetMapping("/test")
    public CommonResponse<Void> test() {
        // 0518201905002
        iceBoxService.changeAssetId(479,"0518201905068",true);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }


}
