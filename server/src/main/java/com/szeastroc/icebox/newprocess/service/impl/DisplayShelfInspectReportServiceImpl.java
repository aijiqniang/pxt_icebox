package com.szeastroc.icebox.newprocess.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.entity.customer.vo.StoreInfoDtoVo;
import com.szeastroc.common.entity.customer.vo.SupplierInfoSessionVo;
import com.szeastroc.common.entity.icebox.vo.ShelfInspectRequest;
import com.szeastroc.common.entity.user.session.UserManageVo;
import com.szeastroc.common.entity.user.vo.SessionDeptInfoVo;
import com.szeastroc.common.entity.user.vo.SimpleUserInfoVo;
import com.szeastroc.common.entity.visit.ShelfInspectModel;
import com.szeastroc.common.feign.customer.FeignStoreClient;
import com.szeastroc.common.feign.customer.FeignSupplierClient;
import com.szeastroc.common.feign.user.FeignCacheClient;
import com.szeastroc.common.feign.user.FeignUserClient;
import com.szeastroc.common.feign.visit.FeignExportRecordsClient;
import com.szeastroc.common.utils.ExecutorServiceFactory;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.commondb.config.redis.JedisClient;
import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.icebox.constant.RedisConstant;
import com.szeastroc.icebox.enums.ExamineStatusEnum;
import com.szeastroc.icebox.newprocess.consumer.common.ShelfInspectReportMsg;
import com.szeastroc.icebox.newprocess.dao.DisplayShelfInspectReportDao;
import com.szeastroc.icebox.newprocess.entity.DisplayShelf;
import com.szeastroc.icebox.newprocess.entity.DisplayShelfInspectReport;
import com.szeastroc.icebox.newprocess.entity.DisplayShelfPutApply;
import com.szeastroc.icebox.newprocess.entity.DisplayShelfPutReport;
import com.szeastroc.icebox.newprocess.enums.DeptTypeEnum;
import com.szeastroc.icebox.newprocess.enums.ExamineExceptionStatusEnums;
import com.szeastroc.icebox.newprocess.enums.ExamineStatus;
import com.szeastroc.icebox.newprocess.enums.SupplierTypeEnum;
import com.szeastroc.icebox.newprocess.service.DisplayShelfInspectReportService;
import com.szeastroc.icebox.newprocess.service.DisplayShelfPutApplyService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * (DisplayShelfInspectReport)表服务实现类
 *
 * @author chenchao
 * @since 2021-06-11 09:38:04
 */
@Service
public class DisplayShelfInspectReportServiceImpl extends ServiceImpl<DisplayShelfInspectReportDao, DisplayShelfInspectReport> implements DisplayShelfInspectReportService {
    @Autowired
    FeignUserClient feignUserClient;
    @Autowired
    FeignCacheClient feignCacheClient;
    @Autowired
    DisplayShelfPutApplyService shelfPutApplyService;
    @Autowired
    FeignSupplierClient feignSupplierClient;
    @Autowired
    FeignStoreClient feignStoreClient;
    @Autowired
    JedisClient jedis;
    @Autowired
    FeignExportRecordsClient feignExportRecordsClient;
    @Autowired
    RabbitTemplate rabbitTemplate;

    @Override
    public Object selectPage(ShelfInspectReportMsg reportMsg) {
        LambdaQueryWrapper<DisplayShelfInspectReport> wrapper = this.fillWrapper(reportMsg);
        IPage<DisplayShelfInspectReport> page = this.page(reportMsg, wrapper);
        return page;
    }

    @Override
    public Object detail(String applyNumber) {
        return null;
    }

    @Override
    public CommonResponse export(ShelfInspectReportMsg reportMsg) {
        // 获取当前用户相关信息
        UserManageVo userManageVo = FeignResponseUtil.getFeignData(feignUserClient.getSessionUserInfo());
        String key = String.format("%s%s", RedisConstant.SHELF_PUT_REPORT_EXPORT_KEY, userManageVo.getSessionUserInfoVo().getId());
        if (null != jedis.get(key)) {
            return new CommonResponse<>(Constants.API_CODE_FAIL, "请求导出操作频繁，请稍候操作");
        }
        LambdaQueryWrapper<DisplayShelfInspectReport> wrapper = fillWrapper(reportMsg);
        Integer count = Optional.ofNullable(this.selectByExportCount(wrapper)).orElse(0);
        if (0 == count) {
            return new CommonResponse<>(Constants.API_CODE_FAIL, "暂无可下载数据");
        }
        // 生成下载任务
        Integer recordsId = FeignResponseUtil.getFeignData(feignExportRecordsClient.createExportRecords(userManageVo.getSessionUserInfoVo().getId(),
                userManageVo.getSessionUserInfoVo().getRealname(), JSON.toJSONString(reportMsg), "陈列架投放报表-导出"));

        //发送mq消息,同步申请数据到报表
        CompletableFuture.runAsync(() -> {
            reportMsg.setRecordsId(recordsId);
            rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.shelfInspectReportKey, reportMsg);
        }, ExecutorServiceFactory.getInstance());
        // 三分钟间隔
        jedis.set(key, "ex", 300, TimeUnit.SECONDS);
        return new CommonResponse(Constants.API_CODE_SUCCESS, null);
    }

    @Override
    public LambdaQueryWrapper<DisplayShelfInspectReport> fillWrapper(ShelfInspectReportMsg reportMsg) {
        LambdaQueryWrapper<DisplayShelfInspectReport> wrapper = Wrappers.lambdaQuery();
        if (reportMsg.getGroupDeptId() != null) {
            wrapper.eq(DisplayShelfInspectReport::getGroupDeptId, reportMsg.getGroupDeptId());
        }
        if (reportMsg.getServiceDeptId() != null) {
            wrapper.eq(DisplayShelfInspectReport::getServiceDeptId, reportMsg.getServiceDeptId());
        }
        if (reportMsg.getRegionDeptId() != null) {
            wrapper.eq(DisplayShelfInspectReport::getRegionDeptId, reportMsg.getRegionDeptId());
        }
        if (reportMsg.getBusinessDeptId() != null) {
            wrapper.eq(DisplayShelfInspectReport::getBusinessDeptId, reportMsg.getBusinessDeptId());
        }
        if (reportMsg.getHeadquartersDeptId() != null) {
            wrapper.eq(DisplayShelfInspectReport::getHeadquartersDeptId, reportMsg.getHeadquartersDeptId());
        }
        if (StringUtils.isNotEmpty(reportMsg.getCustomerName())) {
            wrapper.like(DisplayShelfInspectReport::getPutCustomerName, reportMsg.getCustomerName());
        }
        if (StringUtils.isNotEmpty(reportMsg.getCustomerNumber())) {
            wrapper.eq(DisplayShelfInspectReport::getPutCustomerNumber, reportMsg.getCustomerNumber());
        }
        if (StringUtils.isNotEmpty(reportMsg.getStartTime())) {
            wrapper.ge(DisplayShelfInspectReport::getCreateTime, reportMsg.getStartTime());
        }
        if (StringUtils.isNotEmpty(reportMsg.getEndTime())) {
            wrapper.le(DisplayShelfInspectReport::getCreateTime, reportMsg.getEndTime());
        }
        wrapper.orderByDesc(DisplayShelfInspectReport::getCreateTime);
        return wrapper;
    }

    @Override
    public Integer selectByExportCount(LambdaQueryWrapper<DisplayShelfInspectReport> wrapper) {
        return this.count(wrapper);
    }

    @Override
    public void build(ShelfInspectModel model, DisplayShelf displayShelf) {
        SimpleUserInfoVo user = FeignResponseUtil.getFeignData(feignUserClient.findUserById(model.getCreateBy()));
        Map<Integer, SessionDeptInfoVo> deptMap = FeignResponseUtil.getFeignData(feignCacheClient.getFiveLevelDept(model.getDeptId()));
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
        if (Objects.nonNull(group)) {
            groupId = group.getId();
            groupName = group.getName();
        }
        SessionDeptInfoVo service = deptMap.get(2);
        if (Objects.nonNull(service)) {
            serviceId = service.getId();
            serviceName = service.getName();
        }
        SessionDeptInfoVo region = deptMap.get(3);
        if (Objects.nonNull(region)) {
            regionId = region.getId();
            regionName = region.getName();
        }
        SessionDeptInfoVo business = deptMap.get(4);
        SessionDeptInfoVo headquarters = deptMap.get(5);
        if (!DeptTypeEnum.BUSINESS_UNIT.getType().equals(business.getDeptType())) {
            business = null;
            headquarters = deptMap.get(4);
        }
        if (Objects.nonNull(business)) {
            businessId = business.getId();
            businessName = business.getName();
        }

        if (Objects.nonNull(headquarters)) {
            headquartersId = headquarters.getId();
            headquartersName = headquarters.getName();
        }
        String customerName;
        String shNumber = null;
        String customerLevel = null;
        if (SupplierTypeEnum.IS_STORE.getType().equals(displayShelf.getCustomerType())) {
            StoreInfoDtoVo store = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(displayShelf.getPutNumber()));
            customerName = store.getStoreName();
            shNumber = store.getMerchantNumber();
            customerLevel = store.getStoreLevel();
        } else {
            SupplierInfoSessionVo supplier = FeignResponseUtil.getFeignData(feignSupplierClient.getSuppliserInfoByNumber(displayShelf.getPutNumber()));
            customerName = supplier.getName();
            customerLevel = supplier.getLevel();
        }
        DisplayShelfInspectReport report = new DisplayShelfInspectReport();
        report.setInspectRemark(model.getRemark())
                .setSubmitTime(new Date())
                .setSubmitterName(model.getCreateByName())
                .setSubmitterId(model.getCreateBy())
                .setSubmitterPosition(user.getPosion())
                .setPutCustomerNumber(displayShelf.getPutNumber())
                .setPutCustomerType(displayShelf.getCustomerType())
                .setPutCustomerName(customerName)
                .setPutCustomerLevel(customerLevel)
                .setShNumber(shNumber)
                .setSupplierNumber(displayShelf.getSupplierNumber())
                .setSupplierName(displayShelf.getSupplierName())
                .setSupplierType(displayShelf.getSupplierType())
                .setStatus(0)
                .setHeadquartersDeptId(headquartersId).setHeadquartersDeptName(headquartersName)
                .setBusinessDeptId(businessId).setBusinessDeptName(businessName)
                .setRegionDeptId(regionId).setRegionDeptName(regionName)
                .setServiceDeptId(serviceId).setServiceDeptName(serviceName)
                .setGroupDeptId(groupId).setGroupDeptName(groupName)
                .setApplyNumber(model.getApplyNumber());
        this.save(report);

    }

    @Override
    public void updateStatus(ShelfInspectRequest request) {
        //审批状态 0:审批中 1:批准 2:驳回
        DisplayShelfInspectReport report = this.getOne(Wrappers.<DisplayShelfInspectReport>lambdaQuery().eq(DisplayShelfInspectReport::getApplyNumber, request.getModel().getApplyNumber()));
        if (1 == request.getStatus()) {
            report.setStatus(ExamineExceptionStatusEnums.allow_report.getStatus());
        } else if (2 == report.getStatus()) {
            report.setStatus(ExamineExceptionStatusEnums.is_unpass.getStatus());
        }
        this.updateById(report);
    }
}
