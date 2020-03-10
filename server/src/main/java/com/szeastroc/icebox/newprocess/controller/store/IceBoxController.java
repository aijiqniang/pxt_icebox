package com.szeastroc.icebox.newprocess.controller.store;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.exception.NormalOptionException;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.convert.IceBoxConverter;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.service.IceBoxService;
import com.szeastroc.icebox.newprocess.vo.IceBoxStatusVo;
import com.szeastroc.icebox.newprocess.vo.IceBoxStoreVo;
import com.szeastroc.icebox.oldprocess.vo.IceChestResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created by Tulane
 * 2019/5/21
 */
@Slf4j
@RestController
@RequestMapping("/store")
@RequiredArgsConstructor(onConstructor = @_(@Autowired))
public class IceBoxController {

    private final IceBoxService iceBoxService;

    /**
     * 根据门店编号获取所属冰柜信息
     *
     * @param pxtNumber
     * @return
     * @throws NormalOptionException
     * @throws ImproperOptionException
     */
    @GetMapping("/getIceBox")
    public CommonResponse<List<IceBoxStoreVo>> getIceBox(String pxtNumber){
        if (StringUtils.isBlank(pxtNumber)) {
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, iceBoxService.getIceBoxStoreVoByPxtNumber(pxtNumber));
    }

    /**
     * 检查当前冰柜状态
     * @param qrcode
     * @param pxtNumber
     * @return
     */
    @GetMapping("/checkIceBoxByQrcode")
    public CommonResponse<IceBoxStatusVo> checkIceBoxByQrcode(String qrcode, String pxtNumber){
        if (StringUtils.isBlank(qrcode) || StringUtils.isBlank(pxtNumber)) {
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, iceBoxService.checkBoxByQrcode(qrcode, pxtNumber));
    }


    /**
     * 根据冰柜二维码查找冰柜信息
     *
     * @param qrcode
     * @return
     * @throws ImproperOptionException
     * @throws NormalOptionException
     */
    @GetMapping("/getIceBoxByQrcode")
    public CommonResponse<IceChestResponse> getIceBoxByQrcode(String qrcode, String pxtNumber){
        if (StringUtils.isBlank(qrcode) || StringUtils.isBlank(pxtNumber)) {
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }
//        IceChestResponse iceChestResponse = iceChestInfoService.getIceChestByQrcode(qrcode, clientNumber);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, null);
    }

}
