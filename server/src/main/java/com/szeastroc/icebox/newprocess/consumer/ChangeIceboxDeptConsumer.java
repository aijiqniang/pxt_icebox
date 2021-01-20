package com.szeastroc.icebox.newprocess.consumer;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.szeastroc.common.entity.customer.msg.CustomerChangeMsg;
import com.szeastroc.common.entity.customer.vo.StoreInfoDtoVo;
import com.szeastroc.common.entity.customer.vo.SupplierInfoSessionVo;
import com.szeastroc.common.feign.customer.FeignStoreClient;
import com.szeastroc.common.feign.customer.FeignSupplierClient;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.icebox.newprocess.dao.IceBoxDao;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 修改冗余的客户信息
 */
@Slf4j
@Component
@RabbitListener(queues = MqConstant.Q_STORE_CHANGE_ICEBOX_DEPT)
public class ChangeIceboxDeptConsumer {


    @Autowired
    private IceBoxDao iceBoxDao;

    @Autowired
    private FeignSupplierClient feignSupplierClient;

    @Autowired
    private FeignStoreClient feignStoreClient;

    @RabbitHandler
    public void task(CustomerChangeMsg changeMsg) throws Exception {
        log.info("修改冰柜营销区域信息的请求参数---》【{}】", JSON.toJSONString(changeMsg));
        List<IceBox> iceBoxList = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getPutStoreNumber, changeMsg.getCustomerNumber()));

        if(CollectionUtil.isEmpty(iceBoxList) ){
            return;
        }

        Integer serviceDeptId = null;

        if(changeMsg.getIsStore()){
            StoreInfoDtoVo storeInfo = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(changeMsg.getCustomerNumber()));
            if(storeInfo == null){
                return;
            }
            serviceDeptId = storeInfo.getServiceDeptId();

        }else {
            SupplierInfoSessionVo supplierInfo = FeignResponseUtil.getFeignData(feignSupplierClient.getSuppliserInfoByNumber(changeMsg.getCustomerNumber()));
            if(supplierInfo == null){
                return;
            }

            serviceDeptId = supplierInfo.getServiceDeptId();
        }

        if(CollectionUtil.isNotEmpty(iceBoxList)){
            for(IceBox info:iceBoxList){
                IceBox iceBox = new IceBox();
                iceBox.setId(info.getId());
                iceBox.setDeptId(serviceDeptId);
                iceBoxDao.updateById(iceBox);
            }
        }
    }
}