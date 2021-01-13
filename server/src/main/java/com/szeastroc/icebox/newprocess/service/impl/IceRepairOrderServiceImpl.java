package com.szeastroc.icebox.newprocess.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.entity.customer.vo.StoreInfoDtoVo;
import com.szeastroc.common.entity.customer.vo.SupplierInfoSessionVo;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.exception.NormalOptionException;
import com.szeastroc.common.feign.customer.FeignStoreClient;
import com.szeastroc.common.feign.customer.FeignSupplierClient;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.consumer.common.IceRepairOrderMsg;
import com.szeastroc.icebox.newprocess.dao.IceRepairOrderDao;
import com.szeastroc.icebox.newprocess.entity.IceRepairOrder;
import com.szeastroc.icebox.newprocess.enums.SupplierTypeEnum;
import com.szeastroc.icebox.newprocess.service.IceRepairOrderService;
import com.szeastroc.icebox.newprocess.vo.request.IceRepairRequest;
import com.szeastroc.icebox.newprocess.webservice.WbSiteRequestVO;
import com.szeastroc.icebox.newprocess.webservice.WbSiteResponseVO;
import com.szeastroc.icebox.newprocess.webservice.WebSite;
import com.szeastroc.icebox.newprocess.webservice.WebSitePortType;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

/**
 * 冰柜维修订单表(IceRepairOrder)表服务实现类
 *
 * @author chenchao
 * @since 2021-01-12 15:58:24
 */
@Slf4j
@Service
public class IceRepairOrderServiceImpl extends ServiceImpl<IceRepairOrderDao, IceRepairOrder> implements IceRepairOrderService {
    @Autowired
    private FeignStoreClient feignStoreClient;
    @Autowired
    private FeignSupplierClient feignSupplierClient;

    @Value("${hisense.repair.account}")
    private String account;
    @Value("${hisense.repair.password}")
    private String password;

    @Transactional(rollbackFor = Exception.class, transactionManager = "transactionManager")
    @Override
    public CommonResponse<Void> createOrder(IceRepairRequest iceRepairRequest) {
        String msg = null;
        try {
            Integer businessDeptId = null;
            Integer headquartersDeptId= null;
            Integer serviceDeptId= null;
            Integer groupDeptId= null;
            Integer regionDeptId= null;
            String headquartersDeptName= null;
            String businessDeptName= null;
            String regionDeptName= null;
            String serviceDeptName= null;
            String groupDeptName= null;
            if (SupplierTypeEnum.IS_STORE.getType().equals(iceRepairRequest.getCustomerType())) {
                StoreInfoDtoVo store = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(iceRepairRequest.getCustomerNumber()));
                businessDeptId = store.getBusinessDeptId();
                headquartersDeptId = store.getHeadquartersDeptId();
                regionDeptId = store.getRegionDeptId();
                serviceDeptId = store.getServiceDeptId();
                groupDeptId = store.getGroupDeptId();
                businessDeptName = store.getBusinessDeptName();
                headquartersDeptName = store.getHeadquartersDeptName();
                regionDeptName = store.getRegionDeptName();
                serviceDeptName  = store.getServiceDeptName();
                groupDeptName = store.getGroupDeptName();
            }else{
                SupplierInfoSessionVo supplier = FeignResponseUtil.getFeignData(feignSupplierClient.getSuppliserInfoByNumber(iceRepairRequest.getCustomerNumber()));
                businessDeptId = supplier.getBusinessDeptId();
                headquartersDeptId = supplier.getHeadquartersDeptId();
                regionDeptId = supplier.getRegionDeptId();
                serviceDeptId = supplier.getServiceDeptId();
                groupDeptId = supplier.getGroupDeptId();
                businessDeptName = supplier.getBusinessDeptName();
                headquartersDeptName = supplier.getHeadquartersDeptName();
                regionDeptName = supplier.getRegionDeptName();
                serviceDeptName  = supplier.getServiceDeptName();
                groupDeptName = supplier.getGroupDeptName();
            }

            iceRepairRequest.setServiceTypeId("WX");
            iceRepairRequest.setOriginFlag("DP");
            iceRepairRequest.setPsnAccount(account);
            iceRepairRequest.setPsnPwd(password);
            String orderNumber = "REP" + new DateTime().toString("yyyyMMddHHmmss") + RandomUtil.randomNumbers(4);
            IceRepairOrder repairOrder = IceRepairOrder.builder().orderNumber(orderNumber).boxId(iceRepairRequest.getBoxId())
                    .businessDeptId(businessDeptId).businessDeptName(businessDeptName)
                    .headquartersDeptId(headquartersDeptId).headquartersDeptName(headquartersDeptName)
                    .regionDeptId(regionDeptId).regionDeptName(regionDeptName)
                    .serviceDeptId(serviceDeptId).serviceDeptName(serviceDeptName)
                    .groupDeptId(groupDeptId).groupDeptName(groupDeptName)
                    .customerNumber(iceRepairRequest.getCustomerNumber()).customerName(iceRepairRequest.getCustomerName())
                    .customerAddress(iceRepairRequest.getCustomerAddress()).customerType(iceRepairRequest.getCustomerType()).assetId(iceRepairRequest.getAssetId())
                    .linkMan(iceRepairRequest.getLinkMan()).linkMobile(iceRepairRequest.getLinkMobile())
                    .modelName(iceRepairRequest.getModelName()).modelId(iceRepairRequest.getModelId())
                    .remark(iceRepairRequest.getRemark()).description(iceRepairRequest.getDescription())
                    .province(iceRepairRequest.getProvince()).city(iceRepairRequest.getCity()).area(iceRepairRequest.getArea())
                    .build();
            this.baseMapper.insert(repairOrder);
            iceRepairRequest.setSaleOrderId(orderNumber);
            WbSiteRequestVO wbSiteRequestVO = iceRepairRequest.convertToWbSite();
            WebSite webSite = new WebSite();
            WebSitePortType httpEndpoint = webSite.getWebSiteHttpSoap12Endpoint();
            WbSiteResponseVO responseVO = httpEndpoint.getWBSite(wbSiteRequestVO);
            String value = responseVO.getResultCode().getValue();
            if (!"1".equals(value)) {
                msg = responseVO.getResultMsg().getValue();
                throw new ImproperOptionException("创建维修订单失败");
            }
        } catch (ImproperOptionException e) {
            //手动回滚事务
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new CommonResponse(Constants.API_CODE_FAIL, "海信" + msg);
        } catch (NormalOptionException e) {
            //手动回滚事务
            log.error("创建维修订单异常,{}", e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new CommonResponse(Constants.API_CODE_FAIL, "创建维修订单失败");
        }
        return new CommonResponse(Constants.API_CODE_SUCCESS, null);

    }

    @Override
    public IPage<IceRepairOrder> findByPage(IceRepairOrderMsg msg) {

        return null;
    }

    @Override
    public CommonResponse<Void> sendExportMsg(IceRepairOrderMsg msg) {

        return null;
    }
}