package com.szeastroc.icebox.newprocess.controller.store;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.exception.NormalOptionException;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.service.IceBackOrderService;
import com.szeastroc.icebox.newprocess.service.IceBoxService;
import com.szeastroc.icebox.newprocess.service.IcePutOrderService;
import com.szeastroc.icebox.newprocess.service.IcePutPactRecordService;
import com.szeastroc.icebox.newprocess.vo.IceBoxStatusVo;
import com.szeastroc.icebox.newprocess.vo.IceBoxStoreVo;
import com.szeastroc.icebox.newprocess.vo.IceBoxVo;
import com.szeastroc.icebox.newprocess.vo.request.IceBoxPage;
import com.szeastroc.icebox.newprocess.vo.request.IceExaminePage;
import com.szeastroc.icebox.newprocess.vo.request.IceTransferRecordPage;
import com.szeastroc.icebox.oldprocess.vo.ClientInfoRequest;
import com.szeastroc.icebox.oldprocess.vo.OrderPayBack;
import com.szeastroc.icebox.oldprocess.vo.OrderPayResponse;
import com.szeastroc.icebox.util.CommonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    private final IcePutPactRecordService icePutPactRecordService;
    private final IcePutOrderService icePutOrderService;
    private final IceBackOrderService iceBackOrderService;

    /**
     * 根据门店编号获取所属冰柜信息
     *
     * @param pxtNumber
     * @return
     * @throws NormalOptionException
     * @throws ImproperOptionException
     */
    @PostMapping("/getIceBox")
    public CommonResponse<List<IceBoxStoreVo>> getIceBox(String pxtNumber) {
        if (StringUtils.isBlank(pxtNumber)) {
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, iceBoxService.getIceBoxStoreVoByPxtNumber(pxtNumber));
    }

    /**
     * 检查当前冰柜状态
     *
     * @param qrcode
     * @param pxtNumber
     * @return
     */
    @PostMapping("/checkIceBoxByQrcode")
    public CommonResponse<IceBoxStatusVo> checkIceBoxByQrcode(String qrcode, String pxtNumber) {
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
    @PostMapping("/getIceBoxByQrcode")
    public CommonResponse<IceBoxVo> getIceBoxByQrcode(String qrcode, String pxtNumber) {
        if (StringUtils.isBlank(qrcode) || StringUtils.isBlank(pxtNumber)) {
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, iceBoxService.getIceBoxByQrcode(qrcode));
    }


    /**
     * 门店老板签署电子协议
     *
     * @param clientInfoRequest
     * @return
     * @throws ImproperOptionException
     */
    @PostMapping("/createPactRecord")
    public CommonResponse<Void> createPactRecord(ClientInfoRequest clientInfoRequest) {
        if (!clientInfoRequest.validate()) {
            log.error("createPactRecord传入参数错误 -> {}", JSON.toJSON(clientInfoRequest));
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }
        icePutPactRecordService.createPactRecord(clientInfoRequest);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }

    /**
     * 检查协议是否完成
     *
     * @param iceBoxId
     * @return
     */
    @PostMapping("/checkPactRecordByBoxId")
    public CommonResponse<Boolean> checkPactRecordByBoxId(Integer iceBoxId) {
        if (Objects.isNull(iceBoxId)) {
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, icePutPactRecordService.checkPactRecordByBoxId(iceBoxId));
    }

    /**
     * 获取微信支付信息
     *
     * @param clientInfoRequest
     * @return
     */
    @PostMapping("/applyPayIceBox")
    public CommonResponse<OrderPayResponse> applyPayIceBox(@RequestBody ClientInfoRequest clientInfoRequest) throws Exception {
        if (!clientInfoRequest.validate()) {
            log.error("applyPayIceBox传入参数错误 -> {}", JSON.toJSON(clientInfoRequest));
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, icePutOrderService.applyPayIceBox(clientInfoRequest));
    }

    /**
     * 回调支付成功, 修改订单状态
     *
     * @param request
     * @return
     * @throws Exception
     */
    @RequestMapping("/notifyPay")
    public CommonResponse<String> notifyPay(HttpServletRequest request) throws Exception {
        OrderPayBack orderPayBack = CommonUtil.xmlToObj(request);
        if (orderPayBack.getReturnCode().equals("SUCCESS")) {
            //修改订单信息
            icePutOrderService.notifyOrderInfo(orderPayBack);
        }
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }

    /**
     * 获取订单信息, 并修改冰柜状态
     *
     * @param orderNumber
     * @return
     */
    @GetMapping("/udpateAndGetOrderPayStatus")
    public CommonResponse<String> udpateAndGetOrderPayStatus(String orderNumber) throws Exception {
        boolean flag = icePutOrderService.getPayStatus(orderNumber);
        if (flag) {
            return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
        } else {
            return new CommonResponse<>(Constants.API_CODE_FAIL_LOOP, null);
        }
    }

    /**
     * 退回冰柜
     *
     * @param iceBoxId
     * @return
     */
    @RequestMapping("/takeBackIceBox")
    public CommonResponse<String> takeBackIceBox(Integer iceBoxId) {
        if (iceBoxId == null) {
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }
        iceBackOrderService.takeBackOrder(iceBoxId);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }

    /**
     * @Date: 2020/4/21 16:57 xiao
     * 冰柜管理--列表
     */
    @PostMapping("/readPage")
    public CommonResponse<IPage> findPage(@RequestBody IceBoxPage iceBoxPage) {

        IPage iPage = iceBoxService.findPage(iceBoxPage);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, iPage);
    }

    /**
     * @Date: 2020/4/23 9:32 xiao
     * 冰柜管理--基本信息
     */
    @GetMapping("/readBasic")
    public CommonResponse<Map<String, Object>> readBasic(Integer id) {
        Map<String, Object> map = iceBoxService.readBasic(id);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, map);
    }

    /**
     * @Date: 2020/4/23 15:56 xiao
     * 冰柜管理--模块信息
     */
    @GetMapping("/readModule")
    public CommonResponse<Map<String, Object>> readModule(Integer id) {
        Map<String, Object> map = iceBoxService.readModule(id);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, map);
    }

    /**
     * @Date: 2020/4/23 16:05 xiao
     * 冰柜管理--设备动态
     */
    @GetMapping("/readEquipNews")
    public CommonResponse<Map<String, Object>> readEquipNews(Integer id) {
        Map<String, Object> map = iceBoxService.readEquipNews(id);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, map);
    }

    /**
     * @Date: 2020/4/23 16:19 xiao
     * 冰柜管理--往来记录
     */
    @PostMapping("/readTransferRecord")
    public CommonResponse<IPage> readTransferRecord(@RequestBody IceTransferRecordPage iceTransferRecordPage) {
        IPage iPage = iceBoxService.readTransferRecord(iceTransferRecordPage);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, iPage);
    }

    /**
     * @Date: 2020/4/23 16:19 xiao
     * 冰柜管理--巡检记录
     */
    @PostMapping("/readExamine")
    public CommonResponse<IPage> readExamine(@RequestBody IceExaminePage iceExaminePage) {
        IPage iPage = iceBoxService.readExamine(iceExaminePage);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, iPage);
    }

    /**
     * @Date: 2020/4/24 10:44 xiao
     * 冰柜管理--导入excel
     */
    @PostMapping("/importExcel")
    public CommonResponse<String> importExcel(@RequestParam("excelFile") MultipartFile file) throws Exception {
        iceBoxService.importExcel(file);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, null);
    }

    /**
     * @Date: 2020/4/26 15:11 xiao
     * 冰柜管理--根据导入的excel更新数据库
     */
    @PostMapping("/importExcelAndUpdate")
    public CommonResponse<List<String>> importExcelAndUpdate(@RequestParam("excelFile") MultipartFile file) throws Exception {
        List<String> list = iceBoxService.importExcelAndUpdate(file);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, list);
    }

}
