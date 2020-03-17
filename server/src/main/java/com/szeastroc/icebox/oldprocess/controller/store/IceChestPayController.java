package com.szeastroc.icebox.oldprocess.controller.store;

import com.alibaba.fastjson.JSON;
import com.szeastroc.common.annotation.IgnoreResponseAdvice;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.exception.NormalOptionException;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.oldprocess.entity.MarketArea;
import com.szeastroc.icebox.oldprocess.service.IceChestPutRecordService;
import com.szeastroc.icebox.oldprocess.service.MarketAreaService;
import com.szeastroc.icebox.oldprocess.service.OrderInfoService;
import com.szeastroc.icebox.util.CommonUtil;
import com.szeastroc.icebox.oldprocess.vo.ClientInfoRequest;
import com.szeastroc.icebox.oldprocess.vo.OrderPayBack;
import com.szeastroc.icebox.oldprocess.vo.OrderPayResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by Tulane
 * 2019/8/19
 */
@Slf4j
@RestController
@RequestMapping("/external/store/pay")
public class IceChestPayController {

    @Autowired
    private IceChestPutRecordService iceChestPutRecordService;
    @Autowired
    private OrderInfoService orderInfoService;
    @Autowired
    private MarketAreaService marketAreaService;

    /**
     * 创建订单并返回参数, 用于调起小程序
     *
     * @param ip
     * @param openid
     * @param iceChestId
     * @return
     * @throws ImproperOptionException
     */
    @RequestMapping("/createPayInfo")
    public CommonResponse<OrderPayResponse> createPayInfo(String ip, String openid, Integer iceChestId, Integer chestPutRecordId) throws ImproperOptionException {
        try {
            OrderPayResponse orderPayResponse = orderInfoService.createPayInfo(ip, openid, iceChestId, chestPutRecordId);
            return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, orderPayResponse);
        } catch (ImproperOptionException e) {
            throw new ImproperOptionException(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            throw new ImproperOptionException(e.getMessage());
        }
    }

    /**
     * 门店调用冰柜支付
     *
     * @param clientInfoRequest
     * @return
     * @throws Exception
     */
    @IgnoreResponseAdvice
    @PostMapping("/applyPayIceChest")
    public CommonResponse<OrderPayResponse> applyPayIceChest(@RequestBody ClientInfoRequest clientInfoRequest) throws ImproperOptionException, NormalOptionException {
        try {
            log.info("数据: {}", JSON.toJSONString(clientInfoRequest));
            if (!clientInfoRequest.validate()) {
                log.error("applyPayIceChest传入参数错误 -> {}", JSON.toJSON(clientInfoRequest));
                throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
            }
            //查询传入的服务处链并比对更新
//            marketAreaService.updateStoreMarketAreaList(JSON.parseArray(clientInfoRequest.getMarketAreas(), MarketArea.class));

            return iceChestPutRecordService.applyPayIceChest(clientInfoRequest);
        } catch (ImproperOptionException e) {
            throw new ImproperOptionException(e.getMessage());
        } catch (NormalOptionException e) {
            throw new NormalOptionException(e.getCode(), e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("异常applyPayIceChest -> {}, {}", JSON.toJSON(clientInfoRequest), e.getMessage());
            throw new ImproperOptionException(e.getMessage());
        }

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
            orderInfoService.notifyOrderInfo(orderPayBack);
        }
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }

    /**
     * 获取订单信息, 并修改冰柜状态
     *
     * @param orderNumber
     * @return
     * @throws NormalOptionException
     * @throws ImproperOptionException
     */
    @IgnoreResponseAdvice
    @RequestMapping("/udpateAndGetOrderPayStatus")
    public CommonResponse<String> udpateAndGetOrderPayStatus(String orderNumber) throws NormalOptionException, ImproperOptionException {
        try {
            boolean flag = orderInfoService.getPayStatus(orderNumber);
            if (flag) {
                return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
            } else {
                return new CommonResponse<>(Constants.API_CODE_FAIL_LOOP, null);
            }
        } catch (ImproperOptionException e) {
            throw new ImproperOptionException(e.getMessage());
        } catch (NormalOptionException e) {
            throw new NormalOptionException(e.getCode(), e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("异常getOrderPayStatus -> {}, {}", orderNumber, e.getMessage());
            throw new ImproperOptionException(e.getMessage());
        }

    }
}
