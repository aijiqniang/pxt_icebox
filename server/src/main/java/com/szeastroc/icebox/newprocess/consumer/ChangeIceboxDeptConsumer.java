package com.szeastroc.icebox.newprocess.consumer;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
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

        if(CollectionUtil.isEmpty(iceBoxList) ){
            return;
        }

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
                report.setGroupDeptId(groupId);
                report.setGroupDeptName(groupName);
                report.setServiceDeptId(serviceDeptId);
                report.setServiceDeptName(serviceName);
                report.setRegionDeptId(regionId);
                report.setRegionDeptName(regionName);
                report.setBusinessDeptId(businessId);
                report.setBusinessDeptName(businessName);
                report.setHeadquartersDeptId(headquartersId);
                report.setHeadquartersDeptName(headquartersName);
                iceBackApplyReportDao.updateById(report);
            }catch (Exception e){
                log.info("冰柜退还报表更新失败，id：{}->营销区域:{}",report.getId(),groupId+groupName+serviceDeptId+serviceName+regionId+regionName+businessId+businessName+headquartersId+headquartersName);
                e.printStackTrace();
            }
        }

        //t_ice_box_examine_exception_report
        List<IceBoxExamineExceptionReport> examineExceptionReports = iceBoxExamineExceptionReportDao.selectList(Wrappers.<IceBoxExamineExceptionReport>lambdaQuery().eq(IceBoxExamineExceptionReport::getPutCustomerNumber, changeMsg.getCustomerNumber()));
        for(IceBoxExamineExceptionReport examineExceptionReport : examineExceptionReports){
            try {
                examineExceptionReport.setGroupDeptId(groupId);
                examineExceptionReport.setGroupDeptName(groupName);
                examineExceptionReport.setServiceDeptId(serviceDeptId);
                examineExceptionReport.setServiceDeptName(serviceName);
                examineExceptionReport.setRegionDeptId(regionId);
                examineExceptionReport.setRegionDeptName(regionName);
                examineExceptionReport.setBusinessDeptId(businessId);
                examineExceptionReport.setBusinessDeptName(businessName);
                examineExceptionReport.setHeadquartersDeptId(headquartersId);
                examineExceptionReport.setHeadquartersDeptName(headquartersName);
                iceBoxExamineExceptionReportDao.updateById(examineExceptionReport);
            }catch (Exception e){
                log.info("冰柜巡检报表更新失败，id：{}->营销区域:{}",examineExceptionReport.getId(),groupId+groupName+serviceDeptId+serviceName+regionId+regionName+businessId+businessName+headquartersId+headquartersName);
                e.printStackTrace();
            }
        }

        //t_ice_box_handover
        List<IceBoxHandover> iceBoxHandovers = iceBoxHandoverDao.selectList(Wrappers.<IceBoxHandover>lambdaQuery().eq(IceBoxHandover::getStoreNumber, changeMsg.getCustomerNumber()));
        for(IceBoxHandover handover : iceBoxHandovers){
            try {
                handover.setGroupDeptId(groupId);
                handover.setGroupDeptName(groupName);
                handover.setServiceDeptId(serviceDeptId);
                handover.setServiceDeptName(serviceName);
                handover.setRegionDeptId(regionId);
                handover.setRegionDeptName(regionName);
                handover.setBusinessDeptId(businessId);
                handover.setBusinessDeptName(businessName);
                handover.setHeadquartersDeptId(headquartersId);
                handover.setHeadquartersDeptName(headquartersName);
                iceBoxHandoverDao.updateById(handover);
            }catch (Exception e){
                log.info("冰柜交接报表更新失败，id：{}->营销区域:{}",handover.getId(),groupId+groupName+serviceDeptId+serviceName+regionId+regionName+businessId+businessName+headquartersId+headquartersName);
                e.printStackTrace();
            }
        }

        //t_ice_box_put_report
        List<IceBoxPutReport> iceBoxPutReports = iceBoxPutReportDao.selectList(Wrappers.<IceBoxPutReport>lambdaQuery().eq(IceBoxPutReport::getPutCustomerNumber, changeMsg.getCustomerNumber()));
        for(IceBoxPutReport putReport : iceBoxPutReports){
            try {
                putReport.setGroupDeptId(groupId);
                putReport.setGroupDeptName(groupName);
                putReport.setServiceDeptId(serviceDeptId);
                putReport.setServiceDeptName(serviceName);
                putReport.setRegionDeptId(regionId);
                putReport.setRegionDeptName(regionName);
                putReport.setBusinessDeptId(businessId);
                putReport.setBusinessDeptName(businessName);
                putReport.setHeadquartersDeptId(headquartersId);
                putReport.setHeadquartersDeptName(headquartersName);
                iceBoxPutReportDao.updateById(putReport);
            }catch (Exception e){
                log.info("冰柜投放报表更新失败，id：{}->营销区域:{}",putReport.getId(),groupId+groupName+serviceDeptId+serviceName+regionId+regionName+businessId+businessName+headquartersId+headquartersName);
                e.printStackTrace();
            }
        }

        //t_ice_box_transfer_history
        List<IceBoxTransferHistory> iceBoxTransferHistories = iceBoxTransferHistoryDao.selectList(Wrappers.<IceBoxTransferHistory>lambdaQuery().eq(IceBoxTransferHistory::getOldSupplierNumber, changeMsg.getCustomerNumber()));
        for(IceBoxTransferHistory transferHistory : iceBoxTransferHistories){
            try {
                transferHistory.setGroupDeptId(groupId);
                transferHistory.setGroupDeptName(groupName);
                transferHistory.setServiceDeptId(serviceDeptId);
                transferHistory.setServiceDeptName(serviceName);
                transferHistory.setRegionDeptId(regionId);
                transferHistory.setRegionDeptName(regionName);
                transferHistory.setBusinessDeptId(businessId);
                transferHistory.setBusinessDeptName(businessName);
                transferHistory.setHeadquartersDeptId(headquartersId);
                transferHistory.setHeadquartersDeptName(headquartersName);
                iceBoxTransferHistoryDao.updateById(transferHistory);
            }catch (Exception e){
                log.info("冰柜变更报表更新失败，id：{}->营销区域:{}",transferHistory.getId(),groupId+groupName+serviceDeptId+serviceName+regionId+regionName+businessId+businessName+headquartersId+headquartersName);
                e.printStackTrace();
            }
        }

        //t_ice_repair_order
        List<IceRepairOrder> iceRepairOrders = iceRepairOrderDao.selectList(Wrappers.<IceRepairOrder>lambdaQuery().eq(IceRepairOrder::getCustomerNumber, changeMsg.getCustomerNumber()));
        for(IceRepairOrder order : iceRepairOrders){
            try {
                order.setGroupDeptId(groupId);
                order.setGroupDeptName(groupName);
                order.setServiceDeptId(serviceDeptId);
                order.setServiceDeptName(serviceName);
                order.setRegionDeptId(regionId);
                order.setRegionDeptName(regionName);
                order.setBusinessDeptId(businessId);
                order.setBusinessDeptName(businessName);
                order.setHeadquartersDeptId(headquartersId);
                order.setHeadquartersDeptName(headquartersName);
                iceRepairOrderDao.updateById(order);
            }catch (Exception e){
                log.info("冰柜维修订单报表更新失败，id：{}->营销区域:{}",order.getId(),groupId+groupName+serviceDeptId+serviceName+regionId+regionName+businessId+businessName+headquartersId+headquartersName);
                e.printStackTrace();
            }
        }

    }
}