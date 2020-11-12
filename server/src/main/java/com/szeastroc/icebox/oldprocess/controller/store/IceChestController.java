package com.szeastroc.icebox.oldprocess.controller.store;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.szeastroc.common.annotation.IgnoreResponseAdvice;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.entity.customer.vo.StoreInfoDtoVo;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.exception.NormalOptionException;
import com.szeastroc.common.feign.customer.FeignStoreClient;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.oldprocess.entity.ClientInfo;
import com.szeastroc.icebox.oldprocess.entity.PactRecord;
import com.szeastroc.icebox.oldprocess.service.ClientInfoService;
import com.szeastroc.icebox.oldprocess.service.IceChestInfoService;
import com.szeastroc.icebox.oldprocess.service.PactRecordService;
import com.szeastroc.icebox.oldprocess.vo.ClientInfoRequest;
import com.szeastroc.icebox.oldprocess.vo.IceChestResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Created by Tulane
 * 2019/5/21
 */
@Slf4j
@RestController
@RequestMapping("/external/store")
public class IceChestController {

    @Autowired
    private PactRecordService pactRecordService;
    @Autowired
    private ClientInfoService clientInfoService;
    @Autowired
    private IceChestInfoService iceChestInfoService;
    @Autowired
    private FeignStoreClient feignStoreClient;

    /**
     * 根据门店编号获取所属冰柜信息
     *
     * @param clientNumber
     * @return
     * @throws NormalOptionException
     * @throws ImproperOptionException
     */
    @GetMapping("/getIceChest")
    public CommonResponse<IceChestResponse> getIceChest(String clientNumber) throws NormalOptionException, ImproperOptionException {
        if (StringUtils.isBlank(clientNumber)) {
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }
        IceChestResponse iceChestResponse = iceChestInfoService.getIceChestByClientNumber(clientNumber);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, iceChestResponse);
    }

    /**
     * 根据冰柜二维码查找冰柜信息
     *
     * @param qrcode
     * @return
     * @throws ImproperOptionException
     * @throws NormalOptionException
     */
    @PostMapping("/getIceChestByQrcode")
    public CommonResponse<IceChestResponse> getIceChestByQrcode(String qrcode, String clientNumber) throws ImproperOptionException, NormalOptionException {
        if (StringUtils.isBlank(qrcode)) {
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }
        IceChestResponse iceChestResponse = iceChestInfoService.getIceChestByQrcode(qrcode, clientNumber);
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, iceChestResponse);
    }

    /**
     * 根据鹏讯通编号查看是否签署协议
     * @param clientNumber
     * @return
     * @throws ImproperOptionException
     */
    @Deprecated
    @GetMapping("/getPactRecord")
    public CommonResponse<Boolean> getPactRecord(String clientNumber) throws ImproperOptionException {
        if (StringUtils.isBlank(clientNumber)) {
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }
        ClientInfo clientInfo = clientInfoService.getOne(Wrappers.<ClientInfo>lambdaQuery().eq(ClientInfo::getClientNumber, clientNumber));
        if(clientInfo == null){
            return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, false);
        }
        PactRecord pactRecord = pactRecordService.getOne(Wrappers.<PactRecord>lambdaQuery().eq(PactRecord::getClientId, clientInfo.getId()));
        if(pactRecord == null){
            return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, false);
        }
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, true);
    }

    /**
     * 根据鹏讯通编号及冰柜ID查看是否签署协议
     * @param clientNumber
     * @param chestId
     * @return
     * @throws ImproperOptionException
     */
    @GetMapping("/getPactRecordByClientNumberAndChestId")
    public CommonResponse<Boolean> getPactRecordByClientNumberAndChestId(String clientNumber, Integer chestId) throws ImproperOptionException {
        if (StringUtils.isBlank(clientNumber)) {
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }
        ClientInfo clientInfo = clientInfoService.getOne(Wrappers.<ClientInfo>lambdaQuery().eq(ClientInfo::getClientNumber, clientNumber));
        if(clientInfo == null){
            return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, false);
        }
        PactRecord pactRecord = pactRecordService.getOne(Wrappers.<PactRecord>lambdaQuery()
                .eq(PactRecord::getClientId, clientInfo.getId())
                .eq(PactRecord::getChestId, chestId));
        if(pactRecord == null){
            return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, false);
        }
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null, true);
    }

    /**
     * 门店老板签署电子协议(废)
     * @param clientInfoRequest
     * @return
     * @throws ImproperOptionException
     */
    @IgnoreResponseAdvice
    @PostMapping("/createPactRecord")
    public CommonResponse<String> createPactRecord(@RequestBody ClientInfoRequest clientInfoRequest) throws ImproperOptionException {
        if (!clientInfoRequest.validate()) {
            log.error("createPactRecord传入参数错误 -> {}", JSON.toJSON(clientInfoRequest));
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }
        StoreInfoDtoVo storeInfoDtoVo = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(clientInfoRequest.getClientNumber()));
        if(storeInfoDtoVo == null || storeInfoDtoVo.getMarketArea() == null){
            log.error("createPactRecord传入参数错误 -> {}", JSON.toJSON(clientInfoRequest));
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }
        clientInfoRequest.setMarketAreaId(storeInfoDtoVo.getMarketArea()+"");
        return pactRecordService.createPactRecord(clientInfoRequest);
    }


}
