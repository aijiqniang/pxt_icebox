package com.szeastroc.icebox.newprocess.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.commondb.config.annotation.RedisLock;
import com.szeastroc.icebox.newprocess.entity.IceBoxChangeHistory;
import com.szeastroc.icebox.newprocess.entity.IceModel;
import com.szeastroc.icebox.newprocess.enums.IceBoxEnums;
import com.szeastroc.icebox.newprocess.service.IceBoxChangeHistoryService;
import com.szeastroc.icebox.newprocess.service.IceBoxService;
import com.szeastroc.icebox.newprocess.service.IceModelService;
import com.szeastroc.icebox.newprocess.vo.IceBoxManagerVo;
import com.szeastroc.icebox.newprocess.vo.IceStatusVo;
import com.szeastroc.icebox.newprocess.vo.request.IceChangeHistoryPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

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
    public CommonResponse<List<IceStatusVo>> getAllStatus() {

        List<IceStatusVo> list = new ArrayList<>();

        for (IceBoxEnums.StatusEnum statusEnum : IceBoxEnums.StatusEnum.values()) {

            Integer type = statusEnum.getType();
            if (!IceBoxEnums.StatusEnum.ABNORMAL.getType().equals(type)) {
                String desc = statusEnum.getDesc();
                IceStatusVo iceStatusVo = IceStatusVo.builder().status(type).message(desc).build();
                list.add(iceStatusVo);
            }
        }
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, list);
    }


    @PostMapping("/changeIcebox")
    @RedisLock(includeToken = true)
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

    /**
     *  变更记录
     * @param iceChangeHistoryPage
     * @return
     */
    @PostMapping("/findChangeHistory")
    public CommonResponse<IPage<IceBoxChangeHistory>> findChangeHistory(@RequestBody IceChangeHistoryPage iceChangeHistoryPage) {
        if (null == iceChangeHistoryPage || null == iceChangeHistoryPage.getIceBoxId()) {
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }

        IPage<IceBoxChangeHistory> page = iceBoxChangeHistoryService.iceBoxChangeHistoryService(iceChangeHistoryPage);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, page);
    }

}
