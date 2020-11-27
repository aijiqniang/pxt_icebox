package com.szeastroc.icebox.sync;

import com.szeastroc.common.entity.customer.enums.SupplierTypeEnum;
import com.szeastroc.common.entity.customer.vo.RequestCustomerVo;
import com.szeastroc.common.entity.customer.vo.StoreInfoDtoVo;
import com.szeastroc.common.entity.customer.vo.SubordinateInfoVo;
import com.szeastroc.common.feign.customer.FeignStoreClient;
import com.szeastroc.common.feign.customer.FeignSupplierClient;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.icebox.oldprocess.entity.ClientInfo;
import com.szeastroc.icebox.oldprocess.service.ClientInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Component
public class CommonConvert {

    @Autowired
    private FeignSupplierClient feignSupplierClient;
    @Autowired
    private ClientInfoService clientInfoService;
    @Autowired
    private FeignStoreClient feignStoreClient;

    /**
     * 根据clientId, 转换为鹏讯通的经销商ID
     * @param clientId
     * @return
     */
    public Integer convertSupplierIdByClientId(Integer clientId) throws Exception {
        ClientInfo clientInfo = clientInfoService.getById(clientId);
        SubordinateInfoVo subordinateInfoVo = getSuppilerVoByClientNumber(clientInfo.getClientNumber());
        if(subordinateInfoVo.getId() == null){
            log.info("未找到匹配的经销商clientId -> clientId: [{}]", clientId);
            throw new Exception("数据不正确, 停止同步");
        }
        return subordinateInfoVo.getId();
    }

    /**
     * 根据clientNumber, 转换为鹏讯通的经销商ID
     * @param number
     * @return
     */
    public SubordinateInfoVo getSuppilerVoByClientNumber(String number) {
        RequestCustomerVo customerVo = new RequestCustomerVo();
        customerVo.setNumbers(Collections.singletonList(number));
        customerVo.setType(SupplierTypeEnum.IS_DEALER.getType());
        Map<String, SubordinateInfoVo> map = FeignResponseUtil.getFeignData(feignSupplierClient.getCustomersByNumbers(customerVo));
        SubordinateInfoVo subordinateInfoVo = map.getOrDefault(number, new SubordinateInfoVo());
        return subordinateInfoVo;
    }

    /**
     * 根据clientId, 得到鹏讯通部门ID
     * @param clientId
     * @return
     */
    public Integer getDeptIdByClientId(Integer clientId) throws Exception {
        ClientInfo clientInfo = clientInfoService.getById(clientId);
        SubordinateInfoVo subordinateInfoVo = getSuppilerVoByClientNumber(clientInfo.getClientNumber());
        if(subordinateInfoVo.getMarketAreaId() == null){
            log.info("未找到匹配的经销商clientId -> clientId: [{}]", clientId);
            throw new Exception("数据不正确, 停止同步");
        }
        return subordinateInfoVo.getMarketAreaId();
    }

    /**
     * 根据clientId 获取鹏讯通的门店storeNumber
     * @param clientId
     * @return
     */
    public String getStoreNumberByClientId(Integer clientId) throws Exception {
        // 获取旧表中的对象, 从而获取门店pxtId
        ClientInfo clientInfo = clientInfoService.getById(clientId);
        // 调用customer服务接口, 获取门店Vo
        StoreInfoDtoVo storeInfoDtoVo = FeignResponseUtil.getFeignData(feignStoreClient.getDtoVoByPxtId(clientInfo.getClientNumber()));
        if(storeInfoDtoVo == null){
            log.info("未找到匹配的门店 -> pxtId: [{}]", clientInfo.getClientNumber());
            throw new Exception("数据不正确, 停止同步");
        }
        return storeInfoDtoVo.getStoreNumber();
    }
}
