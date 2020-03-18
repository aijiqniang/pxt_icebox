package com.szeastroc.icebox.newprocess.controller;

import com.szeastroc.common.annotation.MonitorAnnotation;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.customer.common.vo.SimpleSupplierInfoVo;
import com.szeastroc.icebox.newprocess.service.IceBoxExtendService;
import com.szeastroc.icebox.newprocess.service.IceBoxService;
import com.szeastroc.icebox.newprocess.vo.IceBoxDetailVo;
import com.szeastroc.icebox.newprocess.vo.SimpleIceBoxDetailVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/refund")
public class RefundController {
    @Autowired
    private IceBoxExtendService iceBoxExtendService;

    @Autowired
    private IceBoxService iceBoxService;


    /**
     * 根据字冰柜的资产编号查询冰柜简易信息
     *
     * @param assetId
     * @return
     */
    @RequestMapping("/findSimpleIceBoxByAssetId")
    @MonitorAnnotation
    public CommonResponse<SimpleIceBoxDetailVo> findIceBoxByAssetId(@RequestParam("assetId") String assetId) {

        if (StringUtils.isBlank(assetId)) {
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }
        SimpleIceBoxDetailVo simpleIceBoxDetailVo = iceBoxExtendService.getByAssetId(assetId);

        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, simpleIceBoxDetailVo);
    }


    /**
     * 根据冰柜的id 查询冰柜的完整信息
     *
     * @param id
     * @return
     */
    @RequestMapping("/findIceBoxById")
    @MonitorAnnotation
    public CommonResponse<IceBoxDetailVo> findIceBoxById(@RequestParam("id") Integer id) {

        if (id == null) {
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }

        IceBoxDetailVo iceBoxDetailVo = iceBoxService.findIceBoxById(id);


        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, iceBoxDetailVo);
    }


    @RequestMapping("/findSupplierByDeptId")
    @MonitorAnnotation
    public CommonResponse<List<SimpleSupplierInfoVo>> findSupplierByDeptId(@RequestParam("deptId") Integer deptId) {

        if (deptId == null) {
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }


        List<SimpleSupplierInfoVo> simpleSupplierInfoVoList = iceBoxService.findSupplierByDeptId(deptId);

        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, simpleSupplierInfoVoList);
    }


    @RequestMapping("/doRefund")
    @MonitorAnnotation
    public CommonResponse doRefund() {


        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, null);

    }


}
