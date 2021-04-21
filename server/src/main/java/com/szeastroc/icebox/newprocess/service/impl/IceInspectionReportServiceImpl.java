package com.szeastroc.icebox.newprocess.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rabbitmq.client.Channel;
import com.szeastroc.common.entity.icebox.vo.IceInspectionReportMsg;
import com.szeastroc.common.entity.user.vo.SessionDeptInfoVo;
import com.szeastroc.common.entity.user.vo.SimpleUserInfoVo;
import com.szeastroc.common.feign.customer.FeignStoreClient;
import com.szeastroc.common.feign.customer.FeignSupplierClient;
import com.szeastroc.common.feign.user.FeignCacheClient;
import com.szeastroc.common.feign.user.FeignDeptClient;
import com.szeastroc.common.feign.user.FeignUserClient;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.icebox.newprocess.dao.IceInspectionReportDao;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.entity.IceInspectionReport;
import com.szeastroc.icebox.newprocess.enums.DeptTypeEnum;
import com.szeastroc.icebox.newprocess.service.IceBoxService;
import com.szeastroc.icebox.newprocess.service.IceExamineService;
import com.szeastroc.icebox.newprocess.service.IceInspectionReportService;
import com.szeastroc.icebox.newprocess.vo.InspectionReportVO;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronization;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 冰柜巡检报表 (TIceInspectionReport)表服务实现类
 *
 * @author chenchao
 * @since 2020-12-16 16:46:21
 */
@Slf4j
@Service
public class IceInspectionReportServiceImpl extends ServiceImpl<IceInspectionReportDao, IceInspectionReport> implements IceInspectionReportService {

    @Autowired
    private IceBoxService iceBoxService;
    @Autowired
    private FeignStoreClient feignStoreClient;
    @Autowired
    private FeignSupplierClient feignSupplierClient;
    @Autowired
    FeignCacheClient feignCacheClient;
    @Autowired
    FeignDeptClient feignDeptClient;
    @Autowired
    FeignUserClient feignUserClient;
    @Autowired
    private IceExamineService iceExamineService;

    /**
     * 获取业务员当月报表
     *
     * @param userId
     * @return
     */
    @Override
    public IceInspectionReport getCurrentMonthReport(Integer userId) {
        LambdaQueryWrapper<IceInspectionReport> wrapper = Wrappers.<IceInspectionReport>lambdaQuery();
        wrapper.eq(IceInspectionReport::getUserId, userId).eq(IceInspectionReport::getInspectionDate, new DateTime().toString("yyyy-MM")).last("limit 1");
        return this.getOne(wrapper);
    }

    /**
     * 获取直接在服务处的业务员的报表
     *
     * @param deptId
     * @return
     */
    @Override
    public List<IceInspectionReport> getInService(Integer deptId) {
        LambdaQueryWrapper<IceInspectionReport> wrapper = Wrappers.<IceInspectionReport>lambdaQuery();
        wrapper.eq(IceInspectionReport::getServiceDeptId, deptId).eq(IceInspectionReport::getInspectionDate, new DateTime().toString("yyyy-MM")).isNull(IceInspectionReport::getGroupDeptId);
        return this.baseMapper.selectList(wrapper);
    }

    /**
     * 获取服务处下各组的总和
     *
     * @param deptId 服务处id
     * @return
     */
    @Override
    public List<InspectionReportVO> getGroupReports(Integer deptId) {
        return this.baseMapper.getGroupReports(deptId);
    }

    /**
     * 获取大区下个服务处的总和
     *
     * @param deptId
     * @return
     */
    @Override
    public List<InspectionReportVO> getServiceReports(Integer deptId) {
        return this.baseMapper.getServiceReports(deptId);
    }

    @Override
    public void truncate() {
        this.baseMapper.truncate();
    }

    @Transactional(rollbackFor = Exception.class, transactionManager = "transactionManager")
    @Override
    public void task(IceInspectionReportMsg reportMsg, Channel channel, long deliveryTag) throws IOException {
        try {
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
                    break;
                case 7:
                    updateDept(reportMsg);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            log.warn("小程序冰柜巡检报表消费消息异常，{}",e.getMessage());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
        channel.basicAck(deliveryTag, false);
    }


    private void buildReport(Integer userId) {
        if (Objects.isNull(userId)) {
            return;
        }
        Integer deptId = FeignResponseUtil.getFeignData(feignDeptClient.getMainDeptByUserId(userId));
        if (Objects.isNull(deptId)) {
            return;
        }
        IceInspectionReport report = this.getCurrentMonthReport(userId);
        List<Integer> putBoxIds = iceBoxService.getPutBoxIds(userId);
        Integer inspectionCount = iceExamineService.getInspectionBoxes(putBoxIds).size();
        int lostCount = iceBoxService.getLostScrapCount(putBoxIds);
        if (Objects.isNull(report)) {
            SimpleUserInfoVo userInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findUserById(userId));
            report = new IceInspectionReport();
            report.setInspectionDate(new DateTime().toString("yyyy-MM"))
                    .setUserId(userId)
                    .setUserName(userInfoVo.getRealname());
            Map<Integer, SessionDeptInfoVo> deptMap = FeignResponseUtil.getFeignData(feignCacheClient.getFiveLevelDept(deptId));
            SessionDeptInfoVo headquarter = deptMap.get(5);
            SessionDeptInfoVo business = deptMap.get(4);
            if (Objects.nonNull(business)&&!DeptTypeEnum.BUSINESS_UNIT.getType().equals(business.getDeptType())) {
                business = null;
                headquarter = deptMap.get(4);
            }
            SessionDeptInfoVo region = deptMap.get(3);
            SessionDeptInfoVo service = deptMap.get(2);
            SessionDeptInfoVo group = deptMap.get(1);
            if (Objects.nonNull(headquarter)) {
                report.setHeadquartersDeptId(headquarter.getId()).setHeadquartersDeptName(headquarter.getName());
            }
            if (Objects.nonNull(business)) {
                report.setBusinessDeptId(business.getId()).setBusinessDeptName(business.getName());
            }
            if (Objects.nonNull(region)) {
                report.setRegionDeptId(region.getId()).setRegionDeptName(region.getName());
            }
            if (Objects.nonNull(service)) {
                report.setServiceDeptId(service.getId()).setServiceDeptName(service.getName());
            }
            if (Objects.nonNull(group)) {
                report.setGroupDeptId(group.getId()).setGroupDeptName(group.getName());
            }
            report.setInspectionCount(inspectionCount).setLostScrapCount(lostCount).setPutCount(putBoxIds.size());
            this.save(report);
        } else {
            report.setInspectionCount(inspectionCount).setLostScrapCount(lostCount).setPutCount(putBoxIds.size());
            this.updateById(report);
        }
    }


    /**
     * 减少投放数
     *
     * @param reportMsg
     */
    private void decreasePutCount(IceInspectionReportMsg reportMsg) {
        if (Objects.isNull(reportMsg.getUserId())) {
            return;
        }
        IceInspectionReport currentMonthReport = this.getCurrentMonthReport(reportMsg.getUserId());
        if (Objects.nonNull(currentMonthReport)) {
            List<Integer> putBoxIds = iceBoxService.getPutBoxIds(reportMsg.getUserId());
            currentMonthReport.setPutCount(putBoxIds.size());
            int lostScrapCount = iceBoxService.getLostScrapCount(putBoxIds);
            currentMonthReport.setLostScrapCount(lostScrapCount);
            this.updateById(currentMonthReport);
        }
    }


    /**
     * 增加投放数量
     *
     * @param reportMsg
     */
    private void increasePutCount(IceInspectionReportMsg reportMsg) {
        if (Objects.isNull(reportMsg.getBoxId())) {
            return;
        }
        IceBox iceBox = iceBoxService.getById(reportMsg.getBoxId());
        String storeNumber = iceBox.getPutStoreNumber();
        Integer userId = FeignResponseUtil.getFeignData(feignStoreClient.getMainSaleManId(storeNumber));
        if (Objects.isNull(userId)) {
            userId = FeignResponseUtil.getFeignData(feignSupplierClient.getMainSaleManId(storeNumber));
        }
        if (Objects.nonNull(userId)) {
            IceInspectionReport currentMonthReport = this.getCurrentMonthReport(userId);
            if (Objects.nonNull(currentMonthReport)) {
                List<Integer> putBoxIds = iceBoxService.getPutBoxIds(userId);
                currentMonthReport.setPutCount(putBoxIds.size());
                this.updateById(currentMonthReport);
            } else {
                currentMonthReport = new IceInspectionReport();
                Integer deptId = FeignResponseUtil.getFeignData(feignDeptClient.getMainDeptByUserId(userId));
                if (Objects.isNull(deptId)) {
                    return;
                }
                Map<Integer, SessionDeptInfoVo> deptMap = FeignResponseUtil.getFeignData(feignCacheClient.getFiveLevelDept(deptId));
                SessionDeptInfoVo headquarter = deptMap.get(5);
                SessionDeptInfoVo business = deptMap.get(4);
                if (Objects.nonNull(business)&&!DeptTypeEnum.BUSINESS_UNIT.getType().equals(business.getDeptType())) {
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
                this.save(currentMonthReport);
            }
        }
    }

    /**
     * 增加巡检数量
     *
     * @param reportMsg
     */
    private void increaseInspectionCount(IceInspectionReportMsg reportMsg) {
        if (Objects.isNull(reportMsg.getBoxId())) {
            return;
        }
        int examineCount = iceExamineService.getExamineCount(reportMsg.getBoxId());
        if (examineCount <= 1) {
            IceBox iceBox = iceBoxService.getById(reportMsg.getBoxId());
            String storeNumber = iceBox.getPutStoreNumber();
            Integer userId = FeignResponseUtil.getFeignData(feignStoreClient.getMainSaleManId(storeNumber));
            if (Objects.isNull(userId)) {
                userId = FeignResponseUtil.getFeignData(feignSupplierClient.getMainSaleManId(storeNumber));
            }
            if (Objects.nonNull(userId)) {
                IceInspectionReport currentMonthReport = this.getCurrentMonthReport(userId);
                if (Objects.nonNull(currentMonthReport)) {
                    currentMonthReport.setInspectionCount(currentMonthReport.getInspectionCount() + 1);
                    this.updateById(currentMonthReport);
                }
            }
        }
    }


    /**
     * 重新计算遗失报废数量
     *
     * @param reportMsg
     */
    private void recalculateLostScrapCount(IceInspectionReportMsg reportMsg) {
        if (Objects.isNull(reportMsg.getBoxId())) {
            return;
        }
        IceBox iceBox = iceBoxService.getById(reportMsg.getBoxId());
        String storeNumber = iceBox.getPutStoreNumber();
        Integer userId = FeignResponseUtil.getFeignData(feignStoreClient.getMainSaleManId(storeNumber));
        if (Objects.isNull(userId)) {
            userId = FeignResponseUtil.getFeignData(feignSupplierClient.getMainSaleManId(storeNumber));
        }
        if (Objects.nonNull(userId)) {
            IceInspectionReport currentMonthReport = this.getCurrentMonthReport(userId);
            if (Objects.nonNull(currentMonthReport)) {
                int lostScrapCount = iceBoxService.getLostScrapCount(userId);
                currentMonthReport.setLostScrapCount(lostScrapCount);
                this.updateById(currentMonthReport);
            }
        }

    }

    /**
     * 删除报表数据（人员离职）
     *
     * @param reportMsg
     */
    private void deleteReport(IceInspectionReportMsg reportMsg) {
        if (Objects.isNull(reportMsg.getUserId())) {
            return;
        }
        LambdaQueryWrapper<IceInspectionReport> wrapper = new LambdaQueryWrapper<IceInspectionReport>()
                .eq(IceInspectionReport::getUserId, reportMsg.getUserId()).eq(IceInspectionReport::getInspectionDate, new DateTime().toString("yyyy-MM"));
        this.remove(wrapper);
    }

    /**
     * 更新人员部门
     *
     * @param reportMsg
     */
    private void updateDept(IceInspectionReportMsg reportMsg) {
        if (Objects.isNull(reportMsg.getUserId())) {
            return;
        }
        LambdaQueryWrapper<IceInspectionReport> wrapper = Wrappers.<IceInspectionReport>lambdaQuery();
        wrapper.eq(IceInspectionReport::getUserId, reportMsg.getUserId()).eq(IceInspectionReport::getInspectionDate, new DateTime().toString("yyyy-MM"));
        IceInspectionReport one = this.getOne(wrapper);
        if (Objects.isNull(one)) {
            return;
        }
        Integer deptId = FeignResponseUtil.getFeignData(feignDeptClient.getMainDeptByUserId(reportMsg.getUserId()));
        if (Objects.isNull(deptId)) {
            return;
        }
        Map<Integer, SessionDeptInfoVo> deptMap = FeignResponseUtil.getFeignData(feignCacheClient.getFiveLevelDept(deptId));
        SessionDeptInfoVo headquarter = deptMap.get(5);
        SessionDeptInfoVo business = deptMap.get(4);
        if (Objects.nonNull(business)&&!DeptTypeEnum.BUSINESS_UNIT.getType().equals(business.getDeptType())) {
            business = null;
            headquarter = deptMap.get(4);
        }
        SessionDeptInfoVo region = deptMap.get(3);
        SessionDeptInfoVo service = deptMap.get(2);
        SessionDeptInfoVo group = deptMap.get(1);
        if (Objects.nonNull(headquarter)) {
            one.setHeadquartersDeptId(headquarter.getId()).setHeadquartersDeptName(headquarter.getName());
        }
        if (Objects.nonNull(business)) {
            one.setBusinessDeptId(business.getId()).setBusinessDeptName(business.getName());
        }
        if (Objects.nonNull(region)) {
            one.setRegionDeptId(region.getId()).setRegionDeptName(region.getName());
        }
        if (Objects.nonNull(service)) {
            one.setServiceDeptId(service.getId()).setServiceDeptName(service.getName());
        }
        if (Objects.nonNull(group)) {
            one.setGroupDeptId(group.getId()).setGroupDeptName(group.getName());
        }
        this.updateById(one);
    }
}