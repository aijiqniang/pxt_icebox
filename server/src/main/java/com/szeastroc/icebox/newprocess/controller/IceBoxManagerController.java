package com.szeastroc.icebox.newprocess.controller;

import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.entity.IceBoxChangeHistory;
import com.szeastroc.icebox.newprocess.entity.IceModel;
import com.szeastroc.icebox.newprocess.enums.IceBoxEnums;
import com.szeastroc.icebox.newprocess.service.IceBoxChangeHistoryService;
import com.szeastroc.icebox.newprocess.service.IceBoxService;
import com.szeastroc.icebox.newprocess.service.IceModelService;
import com.szeastroc.icebox.newprocess.vo.IceBoxManagerVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/manager")
public class IceBoxManagerController {

    @Resource
    private IceModelService iceModelService;
    @Resource
    private IceBoxService iceBoxService;
    @Resource
    private IceBoxChangeHistoryService iceBoxChangeHistoryService;


    /**
     * 获取所有的冰柜型号
     *
     * @return
     */
    @GetMapping("/getAllModel")
    public CommonResponse<List<IceModel>> getAllModel(@RequestParam("type") Integer type) {
        if (null == type) {
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }
        List<IceModel> list = iceModelService.getAllModel(type);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, list);
    }

    /**
     * 获取所有的冰柜状态
     *
     * @return
     */
    @GetMapping("/getAllStatus")
    public CommonResponse<Map<String, Integer>> getAllStatus() {

        Map<String, Integer> map = new HashMap<>();

        for (IceBoxEnums.StatusEnum statusEnum : IceBoxEnums.StatusEnum.values()) {

            Integer type = statusEnum.getType();
            if (!IceBoxEnums.StatusEnum.ABNORMAL.getType().equals(type)) {
                String desc = statusEnum.getDesc();
                map.put(desc, type);
            }
        }
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, map);
    }


    @PostMapping("/changeIcebox")
    public CommonResponse<Void> changeIcebox(@RequestBody IceBoxManagerVo iceBoxManagerVo) {
        iceBoxService.changeIcebox(iceBoxManagerVo);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }


    @GetMapping("/test")
    public CommonResponse<Void> test() {
        // 0518201905002
        iceBoxService.changeAssetId(479, "0000000001", true);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }


    @GetMapping("/changeAssetId")
    public CommonResponse<Void> changeAssetId(@RequestParam("iceBoxId") Integer iceBoxId, @RequestParam("assetId") String assetId, @RequestParam("reconfirm") boolean reconfirm) {
        iceBoxService.changeAssetId(iceBoxId, assetId, reconfirm);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }


    @PostMapping("/findChangeHistory")
    public CommonResponse<List<IceBoxChangeHistory>> findChangeHistory(@RequestParam("iceBoxId") Integer iceBoxId) {
        if (null == iceBoxId) {
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }

        List<IceBoxChangeHistory> list = iceBoxChangeHistoryService.iceBoxChangeHistoryService(iceBoxId);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, list);
    }

}
