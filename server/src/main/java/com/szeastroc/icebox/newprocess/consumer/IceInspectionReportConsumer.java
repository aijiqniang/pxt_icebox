package com.szeastroc.icebox.newprocess.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.szeastroc.common.entity.user.vo.SessionDeptInfoVo;
import com.szeastroc.common.entity.user.vo.SimpleUserInfoVo;
import com.szeastroc.common.feign.customer.FeignStoreClient;
import com.szeastroc.common.feign.customer.FeignSupplierClient;
import com.szeastroc.common.feign.user.FeignCacheClient;
import com.szeastroc.common.feign.user.FeignDeptClient;
import com.szeastroc.common.feign.user.FeignUserClient;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.common.entity.icebox.vo.IceInspectionReportMsg;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.entity.IceInspectionReport;
import com.szeastroc.icebox.newprocess.enums.DeptTypeEnum;
import com.szeastroc.icebox.newprocess.service.IceBoxService;
import com.szeastroc.icebox.newprocess.service.IceExamineService;
import com.szeastroc.icebox.newprocess.service.IceInspectionReportService;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @ClassName: IceInspectioReportConsumer
 * @Description:
 * @Author: 陈超
 * @Date: 2020/12/22 16:01
 **/
@Slf4j
@Component
public class IceInspectionReportConsumer {
    @Autowired
    private IceBoxService iceBoxService;
    @Autowired
    private FeignStoreClient feignStoreClient;
    @Autowired
    private FeignSupplierClient feignSupplierClient;
    @Autowired
    private IceInspectionReportService iceInspectionReportService;
    @Autowired
    FeignCacheClient feignCacheClient;
    @Autowired
    FeignDeptClient feignDeptClient;
    @Autowired
    FeignUserClient feignUserClient;
    @Autowired
    private IceExamineService iceExamineService;

    @RabbitListener(queues = MqConstant.iceInspectionReportQueue)
    public void task(IceInspectionReportMsg reportMsg) {
        switch (reportMsg.getOperateType()) {
            case 1:
                increasePutCount(reportMsg);
                break;
            case 2:
                increaseInspectionCount(reportMsg);
                break;
            case 3:
                buildReport(reportMsg.getUserId());
                break;
            case 4:
                deleteReport(reportMsg);
                break;
            case 5:
                recalculateLostScrapCount(reportMsg);
                break;
            case 6:
                decreasePutCount(reportMsg);
            default:
                break;
        }
    }

    private void buildReport(Integer userId){
        Integer deptId = FeignResponseUtil.getFeignData(feignDeptClient.getMainDeptByUserId(userId));
        if(Objects.isNull(deptId)){
            return;
        }
        IceInspectionReport report = iceInspectionReportService.getCurrentMonthReport(userId);
        List<Integer> putBoxIds = iceBoxService.getPutBoxIds(userId);
        Integer inspectionCount = iceExamineService.getInspectionBoxes(putBoxIds).size();
        int lostCount = iceBoxService.getLostScrapCount(putBoxIds);
        if(Objects.isNull(report)){
            SimpleUserInfoVo userInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findUserById(userId));
            report = new IceInspectionReport();
            report.setInspectionDate(new DateTime().toString("yyyy-MM"))
                    .setUserId(userId)
                    .setUserName(userInfoVo.getRealname());
            Map<Integer, SessionDeptInfoVo> deptMap = FeignResponseUtil.getFeignData(feignCacheClient.getFiveLevelDept(deptId));
            SessionDeptInfoVo headquarter = deptMap.get(5);
            SessionDeptInfoVo business = deptMap.get(4);
            if(!DeptTypeEnum.BUSINESS_UNIT.getType().equals(business.getDeptType())){
                business = null;
                headquarter = deptMap.get(4);
            }
            SessionDeptInfoVo region = deptMap.get(3);
            SessionDeptInfoVo service = deptMap.get(2);
            SessionDeptInfoVo group = deptMap.get(1);
            if(Objects.nonNull(headquarter)){
                report.setHeadquartersDeptId(headquarter.getId()).setHeadquartersDeptName(headquarter.getName());
            }
            if(Objects.nonNull(business)){
                report.setBusinessDeptId(business.getId()).setBusinessDeptName(business.getName());
            }
            if(Objects.nonNull(region)){
                report.setRegionDeptId(region.getId()).setRegionDeptName(region.getName());
            }
            if(Objects.nonNull(service)){
                report.setServiceDeptId(service.getId()).setServiceDeptName(service.getName());
            }
            if(Objects.nonNull(group)){
                report.setGroupDeptId(group.getId()).setGroupDeptName(group.getName());
            }
            report.setInspectionCount(inspectionCount).setLostScrapCount(lostCount).setPutCount(putBoxIds.size());
            iceInspectionReportService.save(report);
        }else{
            report.setInspectionCount(inspectionCount).setLostScrapCount(lostCount).setPutCount(putBoxIds.size());
            iceInspectionReportService.updateById(report);
        }
    }


    /**
     * 减少投放数
     *
     * @param reportMsg
     */
    private void decreasePutCount(IceInspectionReportMsg reportMsg) {
        IceBox iceBox = iceBoxService.getById(reportMsg.getBoxId());
        String storeNumber = iceBox.getPutStoreNumber();
        Integer userId = FeignResponseUtil.getFeignData(feignStoreClient.getMainSaleManId(storeNumber));
        if (Objects.isNull(userId)) {
            userId = FeignResponseUtil.getFeignData(feignSupplierClient.getMainSaleManId(storeNumber));
        }
        if (Objects.nonNull(userId)) {
            IceInspectionReport currentMonthReport = iceInspectionReportService.getCurrentMonthReport(userId);
            if (Objects.nonNull(currentMonthReport)) {
                currentMonthReport.setPutCount(currentMonthReport.getPutCount() - 1);
                iceInspectionReportService.updateById(currentMonthReport);
            }
        }
    }


    /**
     * 增加投放数量
     *
     * @param reportMsg
     */
    private void increasePutCount(IceInspectionReportMsg reportMsg) {
        IceBox iceBox = iceBoxService.getById(reportMsg.getBoxId());
        String storeNumber = iceBox.getPutStoreNumber();
        Integer userId = FeignResponseUtil.getFeignData(feignStoreClient.getMainSaleManId(storeNumber));
        if (Objects.isNull(userId)) {
            userId = FeignResponseUtil.getFeignData(feignSupplierClient.getMainSaleManId(storeNumber));
        }
        if (Objects.nonNull(userId)) {
            IceInspectionReport currentMonthReport = iceInspectionReportService.getCurrentMonthReport(userId);
            if (Objects.nonNull(currentMonthReport)) {
                currentMonthReport.setPutCount(currentMonthReport.getPutCount() + 1);
                iceInspectionReportService.updateById(currentMonthReport);
            }else{
                currentMonthReport = new IceInspectionReport();
                Integer deptId = FeignResponseUtil.getFeignData(feignDeptClient.getMainDeptByUserId(userId));
                if(Objects.isNull(deptId)){
                    return;
                }
                Map<Integer, SessionDeptInfoVo> deptMap = FeignResponseUtil.getFeignData(feignCacheClient.getFiveLevelDept(deptId));
                SessionDeptInfoVo headquarter = deptMap.get(5);
                SessionDeptInfoVo business = deptMap.get(4);
                if (!DeptTypeEnum.BUSINESS_UNIT.getType().equals(business.getDeptType())) {
                    business = null;
                    headquarter = deptMap.get(4);
                }
                SessionDeptInfoVo region = deptMap.get(3);
                SessionDeptInfoVo service = deptMap.get(2);
                SessionDeptInfoVo group = deptMap.get(1);
                if (Objects.nonNull(headquarter)) {
                    currentMonthReport.setHeadquartersDeptId(headquarter.getId()).setHeadquartersDeptName(headquarter.getName());
                }
                if (Objects.nonNull(business)) {
                    currentMonthReport.setBusinessDeptId(business.getId()).setBusinessDeptName(business.getName());
                }
                if (Objects.nonNull(region)) {
                    currentMonthReport.setRegionDeptId(region.getId()).setRegionDeptName(region.getName());
                }
                if (Objects.nonNull(service)) {
                    currentMonthReport.setServiceDeptId(service.getId()).setServiceDeptName(service.getName());
                }
                if (Objects.nonNull(group)) {
                    currentMonthReport.setGroupDeptId(group.getId()).setGroupDeptName(group.getName());
                }
                SimpleUserInfoVo userInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findUserById(userId));
                currentMonthReport.setInspectionDate(new DateTime().toString("yyyy-MM"))
                        .setPutCount(1)
                        .setUserId(userId)
                        .setUserName(userInfoVo.getRealname());
                iceInspectionReportService.save(currentMonthReport);
            }
        }
    }

    /**
     * 增加巡检数量
     *
     * @param reportMsg
     */
    private void increaseInspectionCount(IceInspectionReportMsg reportMsg) {
        IceBox iceBox = iceBoxService.getById(reportMsg.getBoxId());
        String storeNumber = iceBox.getPutStoreNumber();
        Integer userId = FeignResponseUtil.getFeignData(feignStoreClient.getMainSaleManId(storeNumber));
        if (Objects.isNull(userId)) {
            userId = FeignResponseUtil.getFeignData(feignSupplierClient.getMainSaleManId(storeNumber));
        }
        if (Objects.nonNull(userId)) {
            IceInspectionReport currentMonthReport = iceInspectionReportService.getCurrentMonthReport(userId);
            if (Objects.nonNull(currentMonthReport)) {
                currentMonthReport.setInspectionCount(currentMonthReport.getInspectionCount() + 1);
                iceInspectionReportService.updateById(currentMonthReport);
            }
        }
    }


    /**
     * 重新计算遗失报废数量
     *
     * @param reportMsg
     */
    private void recalculateLostScrapCount(IceInspectionReportMsg reportMsg) {
        IceInspectionReport currentMonthReport = iceInspectionReportService.getCurrentMonthReport(reportMsg.getUserId());
        if (Objects.nonNull(currentMonthReport)) {
            int lostScrapCount = iceBoxService.getLostScrapCount(reportMsg.getUserId());
            currentMonthReport.setLostScrapCount(lostScrapCount);
            iceInspectionReportService.updateById(currentMonthReport);
        }
    }

    /**
     * 删除报表数据（人员离职）
     *
     * @param reportMsg
     */
    private void deleteReport(IceInspectionReportMsg reportMsg) {
        LambdaQueryWrapper<IceInspectionReport> wrapper = new LambdaQueryWrapper<IceInspectionReport>()
                .eq(IceInspectionReport::getUserId, reportMsg.getUserId()).eq(IceInspectionReport::getInspectionDate, new DateTime().toString("yyyy-MM"));
        iceInspectionReportService.remove(wrapper);
    }
}
