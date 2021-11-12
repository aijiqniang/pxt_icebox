package com.szeastroc.icebox.newprocess.consumer;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.szeastroc.common.entity.customer.msg.CustomerChangeMsg;
import com.szeastroc.common.entity.customer.vo.StoreInfoDtoVo;
import com.szeastroc.common.entity.customer.vo.SupplierInfoSessionVo;
import com.szeastroc.common.entity.user.vo.SessionDeptInfoVo;
import com.szeastroc.common.feign.customer.FeignStoreClient;
import com.szeastroc.common.feign.customer.FeignSupplierClient;
import com.szeastroc.common.feign.user.FeignCacheClient;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.icebox.newprocess.dao.*;
import com.szeastroc.icebox.newprocess.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    @Resource
    private FeignCacheClient feignCacheClient;
    @Resource
    private IceBackApplyReportDao iceBackApplyReportDao;
    @Resource
    private IceBoxExamineExceptionReportDao iceBoxExamineExceptionReportDao;
    @Resource
    private IceBoxHandoverDao iceBoxHandoverDao;
    @Resource
    private IceBoxPutReportDao iceBoxPutReportDao;
    @Resource
    private IceBoxTransferHistoryDao iceBoxTransferHistoryDao;
    @Resource
    private IceRepairOrderDao iceRepairOrderDao;

    @RabbitHandler
    public void task(CustomerChangeMsg changeMsg) throws Exception {
        log.info("修改冰柜营销区域信息的请求参数---》【{}】", JSON.toJSONString(changeMsg));
        List<IceBox> iceBoxList = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getPutStoreNumber, changeMsg.getCustomerNumber()));

        Integer serviceDeptId = null;
        StoreInfoDtoVo storeInfo = new StoreInfoDtoVo();
        SupplierInfoSessionVo supplierInfo = new SupplierInfoSessionVo();

        if(changeMsg.getIsStore()){
            storeInfo = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(changeMsg.getCustomerNumber()));
            if(storeInfo == null){
                return;
            }
            serviceDeptId = storeInfo.getServiceDeptId();

        }else {
            supplierInfo = FeignResponseUtil.getFeignData(feignSupplierClient.getSuppliserInfoByNumber(changeMsg.getCustomerNumber()));
            if(supplierInfo == null){
                return;
            }

            serviceDeptId = supplierInfo.getServiceDeptId();
        }

        //icebox信息
        if(CollectionUtil.isNotEmpty(iceBoxList)){
            for(IceBox info:iceBoxList){
                try {
                    IceBox iceBox = new IceBox();
                    iceBox.setId(info.getId());
                    iceBox.setDeptId(serviceDeptId);
                    iceBoxDao.updateById(iceBox);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }

        Map<Integer, SessionDeptInfoVo> deptMap = FeignResponseUtil.getFeignData(feignCacheClient.getFiveLevelDept(serviceDeptId));
        Integer groupId = null;
        String groupName = null;
        Integer serviceId = null;
        String serviceName = null;
        Integer regionId = null;
        String regionName = null;
        Integer businessId = null;
        String businessName = null;
        Integer headquartersId = null;
        String headquartersName = null;
        SessionDeptInfoVo group = deptMap.get(1);
        if(Objects.nonNull(group)){
            groupId = group.getId();
            groupName = group.getName();
        }
        SessionDeptInfoVo service = deptMap.get(2);
        if(Objects.nonNull(service)){
            serviceId = service.getId();
            serviceName = service.getName();
        }
        SessionDeptInfoVo region = deptMap.get(3);
        if(Objects.nonNull(region)){
            regionId = region.getId();
            regionName = region.getName();
        }
        SessionDeptInfoVo business = deptMap.get(4);
        if(Objects.nonNull(business)){
            businessId = business.getId();
            businessName = business.getName();
        }
        SessionDeptInfoVo headquarters = deptMap.get(5);
        if(Objects.nonNull(headquarters)){
            headquartersId = headquarters.getId();
            headquartersName = headquarters.getName();
        }

        //t_ice_back_apply_report
        List<IceBackApplyReport> iceBackApplyReports = iceBackApplyReportDao.selectList(Wrappers.<IceBackApplyReport>lambdaQuery().eq(IceBackApplyReport::getCustomerNumber, changeMsg.getCustomerNumber()));
        for(IceBackApplyReport report : iceBackApplyReports){
            try {
                iceBackApplyReportDao.update(null, new LambdaUpdateWrapper<IceBackApplyReport>()
                        .set(IceBackApplyReport::getBusinessDeptId, businessId)
                        .set(IceBackApplyReport::getBusinessDeptName, businessName)
                        .set(IceBackApplyReport::getRegionDeptId, regionId)
                        .set(IceBackApplyReport::getRegionDeptName, regionName)
                        .set(IceBackApplyReport::getServiceDeptId, serviceId)
                        .set(IceBackApplyReport::getServiceDeptName, serviceName)
                        .set(IceBackApplyReport::getGroupDeptId, groupId)
                        .set(IceBackApplyReport::getGroupDeptName, groupName)
                );
            }catch (Exception e){
                log.info("冰柜退还报表更新失败，id：{}->营销区域:{}",report.getId(),groupId+groupName+serviceId+serviceName+regionId+regionName+businessId+businessName+headquartersId+headquartersName);
                e.printStackTrace();
            }
        }

        //t_ice_box_examine_exception_report
        List<IceBoxExamineExceptionReport> examineExceptionReports = iceBoxExamineExceptionReportDao.selectList(Wrappers.<IceBoxExamineExceptionReport>lambdaQuery().eq(IceBoxExamineExceptionReport::getPutCustomerNumber, changeMsg.getCustomerNumber()));
        for(IceBoxExamineExceptionReport examineExceptionReport : examineExceptionReports){
            try {
                iceBoxExamineExceptionReportDao.update(null, new LambdaUpdateWrapper<IceBoxExamineExceptionReport>()
                        .set(IceBoxExamineExceptionReport::getBusinessDeptId, businessId)
                        .set(IceBoxExamineExceptionReport::getBusinessDeptName, businessName)
                        .set(IceBoxExamineExceptionReport::getRegionDeptId, regionId)
                        .set(IceBoxExamineExceptionReport::getRegionDeptName, regionName)
                        .set(IceBoxExamineExceptionReport::getServiceDeptId, serviceId)
                        .set(IceBoxExamineExceptionReport::getServiceDeptName, serviceName)
                        .set(IceBoxExamineExceptionReport::getGroupDeptId, groupId)
                        .set(IceBoxExamineExceptionReport::getGroupDeptName, groupName)
                );

            }catch (Exception e){
                log.info("冰柜巡检报表更新失败，id：{}->营销区域:{}",examineExceptionReport.getId(),groupId+groupName+serviceId+serviceName+regionId+regionName+businessId+businessName+headquartersId+headquartersName);
                e.printStackTrace();
            }
        }

        //t_ice_box_handover
        List<IceBoxHandover> iceBoxHandovers = iceBoxHandoverDao.selectList(Wrappers.<IceBoxHandover>lambdaQuery().eq(IceBoxHandover::getStoreNumber, changeMsg.getCustomerNumber()));
        for(IceBoxHandover handover : iceBoxHandovers){
            try {
                iceBoxHandoverDao.update(null, new LambdaUpdateWrapper<IceBoxHandover>()
                        .set(IceBoxHandover::getBusinessDeptId, businessId)
                        .set(IceBoxHandover::getBusinessDeptName, businessName)
                        .set(IceBoxHandover::getRegionDeptId, regionId)
                        .set(IceBoxHandover::getRegionDeptName, regionName)
                        .set(IceBoxHandover::getServiceDeptId, serviceId)
                        .set(IceBoxHandover::getServiceDeptName, serviceName)
                        .set(IceBoxHandover::getGroupDeptId, groupId)
                        .set(IceBoxHandover::getGroupDeptName, groupName)
                );

            }catch (Exception e){
                log.info("冰柜交接报表更新失败，id：{}->营销区域:{}",handover.getId(),groupId+groupName+serviceId+serviceName+regionId+regionName+businessId+businessName+headquartersId+headquartersName);
                e.printStackTrace();
            }
        }

        //t_ice_box_put_report
        List<IceBoxPutReport> iceBoxPutReports = iceBoxPutReportDao.selectList(Wrappers.<IceBoxPutReport>lambdaQuery().eq(IceBoxPutReport::getPutCustomerNumber, changeMsg.getCustomerNumber()));
        for(IceBoxPutReport putReport : iceBoxPutReports){
            try {
                iceBoxPutReportDao.update(null, new LambdaUpdateWrapper<IceBoxPutReport>()
                        .set(IceBoxPutReport::getBusinessDeptId, businessId)
                        .set(IceBoxPutReport::getBusinessDeptName, businessName)
                        .set(IceBoxPutReport::getRegionDeptId, regionId)
                        .set(IceBoxPutReport::getRegionDeptName, regionName)
                        .set(IceBoxPutReport::getServiceDeptId, serviceId)
                        .set(IceBoxPutReport::getServiceDeptName, serviceName)
                        .set(IceBoxPutReport::getGroupDeptId, groupId)
                        .set(IceBoxPutReport::getGroupDeptName, groupName)
                );

            }catch (Exception e){
                log.info("冰柜投放报表更新失败，id：{}->营销区域:{}",putReport.getId(),groupId+groupName+serviceId+serviceName+regionId+regionName+businessId+businessName+headquartersId+headquartersName);
                e.printStackTrace();
            }
        }

        //t_ice_box_transfer_history
        List<IceBoxTransferHistory> iceBoxTransferHistories = iceBoxTransferHistoryDao.selectList(Wrappers.<IceBoxTransferHistory>lambdaQuery().eq(IceBoxTransferHistory::getOldSupplierNumber, changeMsg.getCustomerNumber()));
        for(IceBoxTransferHistory transferHistory : iceBoxTransferHistories){
            try {
                iceBoxTransferHistoryDao.update(null, new LambdaUpdateWrapper<IceBoxTransferHistory>()
                        .set(IceBoxTransferHistory::getBusinessDeptId, businessId)
                        .set(IceBoxTransferHistory::getBusinessDeptName, businessName)
                        .set(IceBoxTransferHistory::getRegionDeptId, regionId)
                        .set(IceBoxTransferHistory::getRegionDeptName, regionName)
                        .set(IceBoxTransferHistory::getServiceDeptId, serviceId)
                        .set(IceBoxTransferHistory::getServiceDeptName, serviceName)
                        .set(IceBoxTransferHistory::getGroupDeptId, groupId)
                        .set(IceBoxTransferHistory::getGroupDeptName, groupName)
                );

            }catch (Exception e){
                log.info("冰柜变更报表更新失败，id：{}->营销区域:{}",transferHistory.getId(),groupId+groupName+serviceId+serviceName+regionId+regionName+businessId+businessName+headquartersId+headquartersName);
                e.printStackTrace();
            }
        }

        //t_ice_repair_order
        List<IceRepairOrder> iceRepairOrders = iceRepairOrderDao.selectList(Wrappers.<IceRepairOrder>lambdaQuery().eq(IceRepairOrder::getCustomerNumber, changeMsg.getCustomerNumber()));
        for(IceRepairOrder order : iceRepairOrders){
            try {
                iceRepairOrderDao.update(null, new LambdaUpdateWrapper<IceRepairOrder>()
                        .set(IceRepairOrder::getBusinessDeptId, businessId)
                        .set(IceRepairOrder::getBusinessDeptName, businessName)
                        .set(IceRepairOrder::getRegionDeptId, regionId)
                        .set(IceRepairOrder::getRegionDeptName, regionName)
                        .set(IceRepairOrder::getServiceDeptId, serviceId)
                        .set(IceRepairOrder::getServiceDeptName, serviceName)
                        .set(IceRepairOrder::getGroupDeptId, groupId)
                        .set(IceRepairOrder::getGroupDeptName, groupName)
                );

            }catch (Exception e){
                log.info("冰柜维修订单报表更新失败，id：{}->营销区域:{}",order.getId(),groupId+groupName+serviceId+serviceName+regionId+regionName+businessId+businessName+headquartersId+headquartersName);
                e.printStackTrace();
            }
        }

    }
}