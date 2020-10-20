package com.szeastroc.icebox.newprocess.controller.store;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.exception.NormalOptionException;
import com.szeastroc.common.utils.ExecutorServiceFactory;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.customer.client.FeignStoreClient;
import com.szeastroc.customer.client.FeignSupplierClient;
import com.szeastroc.customer.common.vo.StoreInfoDtoVo;
import com.szeastroc.customer.common.vo.SubordinateInfoVo;
import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.icebox.enums.IceBoxStatus;
import com.szeastroc.icebox.enums.OrderStatus;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.entity.IcePutOrder;
import com.szeastroc.icebox.newprocess.enums.OrderSourceEnums;
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
import com.szeastroc.icebox.rabbitMQ.DataPack;
import com.szeastroc.icebox.rabbitMQ.DirectProducer;
import com.szeastroc.icebox.rabbitMQ.MethodNameOfMQ;
import com.szeastroc.icebox.util.CommonUtil;
import com.szeastroc.icebox.util.ExcelUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
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
    private final FeignStoreClient feignStoreClient;
    private final FeignSupplierClient feignSupplierClient;
    private final DirectProducer directProducer;

    /**
     * 根据门店编号获取所属冰柜信息
     *
     * @param pxtNumber
     * @return
     * @throws NormalOptionException
     * @throws ImproperOptionException
     */
    @RequestMapping("/getIceBox")
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
     * 检查当前冰柜状态(新)
     *
     * @param qrcode
     * @param pxtNumber
     * @return
     */
    @PostMapping("/checkIceBoxByQrcodeNew")
    public CommonResponse<IceBoxStatusVo> checkIceBoxByQrcodeNew(String qrcode, String pxtNumber) {
        if (StringUtils.isBlank(qrcode) || StringUtils.isBlank(pxtNumber)) {
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, iceBoxService.checkIceBoxByQrcodeNew(qrcode, pxtNumber));
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
     * 根据冰柜二维码查找冰柜信息
     *
     * @param qrcode
     * @return
     * @throws ImproperOptionException
     * @throws NormalOptionException
     */
    @PostMapping("/getIceBoxByQrcodeNew")
    public CommonResponse<IceBoxVo> getIceBoxByQrcodeNew(String qrcode, String pxtNumber) {
        if (StringUtils.isBlank(qrcode) || StringUtils.isBlank(pxtNumber)) {
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, iceBoxService.getIceBoxByQrcodeNew(qrcode,pxtNumber));
    }

    /**
     * 门店老板签署电子协议(otoc)
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
        StoreInfoDtoVo storeInfoDtoVo = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(clientInfoRequest.getClientNumber()));
        if (storeInfoDtoVo == null || storeInfoDtoVo.getMarketArea() == null) {
            log.error("createPactRecord传入参数错误 -> {}", JSON.toJSON(clientInfoRequest));
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }
        clientInfoRequest.setMarketAreaId(storeInfoDtoVo.getMarketArea() + "");
        icePutPactRecordService.createPactRecord(clientInfoRequest);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }

    /**
     * 门店老板签署电子协议(DMS)
     *
     * @param clientInfoRequest
     * @return
     * @throws ImproperOptionException
     */
    @PostMapping("/createPactRecordDMS")
    public CommonResponse<Void> createPactRecordDMS(ClientInfoRequest clientInfoRequest) {
        if (!clientInfoRequest.validate()) {
            log.error("createPactRecord传入参数错误 -> {}", JSON.toJSON(clientInfoRequest));
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }

        SubordinateInfoVo subordinateInfoVo = FeignResponseUtil.getFeignData(feignSupplierClient.findByNumber(clientInfoRequest.getClientNumber()));
        if (subordinateInfoVo == null || subordinateInfoVo.getMarketAreaId() == null) {
            log.error("createPactRecord传入参数错误 -> {}", JSON.toJSON(clientInfoRequest));
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }
        clientInfoRequest.setMarketAreaId(subordinateInfoVo.getMarketAreaId() + "");

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
     * 获取微信支付信息(otoc)
     *
     * @param clientInfoRequest
     * @return
     */
    @PostMapping("/applyPayIceBox")
    public CommonResponse<OrderPayResponse> applyPayIceBox(ClientInfoRequest clientInfoRequest) throws Exception {
        if (!clientInfoRequest.validate()) {
            log.error("applyPayIceBox传入参数错误 -> {}", JSON.toJSON(clientInfoRequest));
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }

        StoreInfoDtoVo storeInfoDtoVo = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(clientInfoRequest.getClientNumber()));
        if (storeInfoDtoVo == null || storeInfoDtoVo.getMarketArea() == null) {
            log.error("createPactRecord传入参数错误 -> {}", JSON.toJSON(clientInfoRequest));
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }
        clientInfoRequest.setMarketAreaId(storeInfoDtoVo.getMarketArea() + "");
        clientInfoRequest.setOrderSource(OrderSourceEnums.OTOC.getType());
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, icePutOrderService.applyPayIceBox(clientInfoRequest));
    }

    /**
     * 获取微信支付信息(dms)
     *
     * @param clientInfoRequest
     * @return
     */
    @PostMapping("/applyPayIceBoxDMS")
    public CommonResponse<OrderPayResponse> applyPayIceBoxDMS(ClientInfoRequest clientInfoRequest) throws Exception {
        if (!clientInfoRequest.validate()) {
            log.error("applyPayIceBox传入参数错误 -> {}", JSON.toJSON(clientInfoRequest));
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }

        SubordinateInfoVo subordinateInfoVo = FeignResponseUtil.getFeignData(feignSupplierClient.findByNumber(clientInfoRequest.getClientNumber()));
        if (subordinateInfoVo == null || subordinateInfoVo.getMarketAreaId() == null) {
            log.error("createPactRecord传入参数错误 -> {}", JSON.toJSON(clientInfoRequest));
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }
        clientInfoRequest.setMarketAreaId(subordinateInfoVo.getMarketAreaId() + "");
        clientInfoRequest.setOrderSource(OrderSourceEnums.DMS.getType());
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
     * 退回冰柜
     *
     * @param iceBoxId
     * @return
     */
    @RequestMapping("/checkBackIceBox")
    public CommonResponse<String> checkBackIceBox(Integer iceBoxId) {
        if (iceBoxId == null) {
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }
        String status = iceBackOrderService.checkBackIceBox(iceBoxId);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, status);
    }


    /**
     * @Date: 2020/4/21 16:57 xiao
     * 冰柜管理--列表
     */
    @PostMapping("/readPage")
    public CommonResponse<IPage> findPage(@RequestBody IceBoxPage iceBoxPage) {

        IPage iPage = iceBoxService.findPage(iceBoxPage);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, iPage == null ? new Page() : iPage);
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
    public CommonResponse<String> importExcel(@RequestParam("excelFile") MultipartFile mfile) throws Exception {

        List<Map<String, Object>> lists = iceBoxService.importByEasyExcel(mfile);
        /**
         * @Date: 2020/10/19 14:50 xiao
         *  将报表中导入数据库中的数据异步更新到报表中
         */
//        List<String> assetIds = Lists.newArrayList("测试报表");
        if(CollectionUtils.isNotEmpty(lists)){
            DataPack dataPack = new DataPack(); // 数据包
            dataPack.setMethodName(MethodNameOfMQ.CREATE_ICE_BOX_ASSETS_REPORT);
            dataPack.setObj(lists);
            ExecutorServiceFactory.getInstance().execute(()->{
                // 发送mq消息
                directProducer.sendMsg(MqConstant.directRoutingKeyReport, dataPack);
            });
        }

        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }

    /**
     * @Date: 2020/4/28 10:22 xiao
     * 获取模板 (冰柜管理--导入excel)
     */
    @GetMapping("/getImportExcel")
    public void getImportExcel(HttpServletResponse response) throws Exception {
        String fileName = "19年冰柜需交押导入样例表";
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
        String titleName = "冰柜需交押导入excel";
        String[] columnName = {"序号", "冰箱控制器ID", "设备编号", "蓝牙设备ID", "蓝牙设备地址", "冰箱二维码链接", "gps模块mac地址", "设备名称", "生产厂家"
                , "设备型号", "设备规格", "冰柜价值", "冰柜押金", "经销商鹏讯通编号", "经销商名称", "经销商地址", "经销商联系人"
                , "经销商联系人电话", "所属服务处", "生产日期", "保修起算日期"
        };
        ExcelUtil excelUtil = new ExcelUtil();
        List storeExcelVoList = new ArrayList();
        excelUtil.exportExcel(fileName, titleName, columnName, storeExcelVoList, response, null);
    }

    /**
     * @Date: 2020/4/28 10:23 xiao
     * 获取模板 (冰柜管理--根据导入的excel更新数据库)
     */
//    @GetMapping("/getImportExcelAndUpdate")
//    public void getImportExcelAndUpdate(HttpServletResponse response) throws Exception {
//        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
//        String fileName = "19年冰柜数据导入待更新样例表";
//        String titleName = "19年冰柜数据导入待更新excel";
//        String[] columnName = {"序号", "设备编号", "冰柜押金", "经销商鹏讯通编号", "经销商名称", "经销商地址", "经销商联系人"
//                , "经销商联系人电话", "所属服务处"
//        };
//        ExcelUtil excelUtil = new ExcelUtil();
//        List storeExcelVoList = new ArrayList();
//        excelUtil.exportExcel(fileName, titleName, columnName, storeExcelVoList, response, null);
//    }

    /**
     * 轮训订单状态
     *
     * @param orderNumber
     * @return
     * @throws InterruptedException
     */
    @PostMapping("/loopPutOrderPayStatus")
    public CommonResponse<Boolean> loopPutOrderPayStatus(String orderNumber)
            throws Exception {

        long startTime = System.currentTimeMillis();
        int breakCode = -1;
        boolean flag = false;
        while (true) {
            Thread.sleep(2000);

            flag = icePutOrderService.getPayStatus(orderNumber);

            // 订单未完成时, 长连接时间判断
            long nowTime = System.currentTimeMillis();
            if (breakCode < 0 && (nowTime - startTime) > 8000) {
                //一次长连接结束
                breakCode = -3;
            }

            if (breakCode == -2) {
                /**
                 * 如果breakCode等于-2, 代表查询订单未支付, 继续死循环
                 */
            } else if (breakCode == -1) {
                /**
                 * 如果breakCode等于-1, 系统错误, 终止
                 */
                break;
            } else if (breakCode == 0) {
                /**
                 * 如果breakCode等于0, 代表订单已支付, 终止
                 */
                break;
            } else if (breakCode == -3) {
                /**
                 * 如果breakCode等于-3, 代表长链接结束, 终止
                 */
                break;
            } else {
                /**
                 * 如果breakCode等于其他值, 可允许错误, 终止
                 */
                break;
            }

        }
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, flag);
    }
    @RequestMapping("dealIceBoxOrder")
    public CommonResponse<IceBox> dealIceBoxOrder() throws Exception {
        List<IcePutOrder> icePutOrders = icePutOrderService.list(Wrappers.<IcePutOrder>lambdaQuery().eq(IcePutOrder::getStatus, OrderStatus.IS_PAY_ING.getStatus()));
        if(CollectionUtil.isNotEmpty(icePutOrders)){
            for(IcePutOrder order:icePutOrders){
                this.loopPutOrderPayStatus(order.getOrderNum());
            }
        }
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }


    @GetMapping("/judge/customer/bindIceBox")
    CommonResponse<Boolean> judgeCustomerBindIceBox(@RequestParam("number") String  number){

        int count = iceBoxService.count(new LambdaQueryWrapper<IceBox>().eq(IceBox::getPutStoreNumber, number).eq(IceBox::getPutStatus, IceBoxStatus.IS_PUTED.getStatus()));
       if( count>0){
           return new CommonResponse<Boolean>(Constants.API_CODE_SUCCESS, null,Boolean.TRUE);
       }else{
           return new CommonResponse<Boolean>(Constants.API_CODE_SUCCESS, null,Boolean.FALSE);
       }
    }
}
