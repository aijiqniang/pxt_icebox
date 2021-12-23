package com.szeastroc.icebox.newprocess.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
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
import com.szeastroc.icebox.newprocess.consumer.common.ShelfInspectReportMsg;
import com.szeastroc.icebox.newprocess.dao.DisplayShelfDao;
import com.szeastroc.icebox.newprocess.dao.DisplayShelfInspectReportDao;
import com.szeastroc.icebox.newprocess.entity.*;
import com.szeastroc.icebox.newprocess.enums.*;
import com.szeastroc.icebox.newprocess.service.DisplayShelfInspectReportService;
import com.szeastroc.icebox.newprocess.service.DisplayShelfPutApplyService;
import com.szeastroc.icebox.util.DateUtil;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;
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
    @Resource
    DisplayShelfInspectReportDao displayShelfInspectReportDao;
    @Autowired
    private DisplayShelfDao displayShelfDao;

    @Override
    public IPage<DisplayShelfInspectReport> selectPage(ShelfInspectReportMsg reportMsg) {
        LambdaQueryWrapper<DisplayShelfInspectReport> wrapper = this.fillWrapper(reportMsg);
        IPage<DisplayShelfInspectReport> iPage = displayShelfInspectReportDao.selectPage(reportMsg, wrapper);
        return iPage;
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
                userManageVo.getSessionUserInfoVo().getRealname(), JSON.toJSONString(reportMsg), "陈列架巡检报表-导出"));

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
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        LambdaQueryWrapper<DisplayShelfInspectReport> wrapper = Wrappers.lambdaQuery();
        if(Objects.nonNull(reportMsg)) {
            if (reportMsg.getDeptType() != null && reportMsg.getMarketAreaId() != null) {
                switch (reportMsg.getDeptType()) {
                    //deptType  1:服务处 2:大区 3:事业部 4:本部 5:组
                    case 1:
                        wrapper.eq(DisplayShelfInspectReport::getServiceDeptId, reportMsg.getMarketAreaId());
                        break;
                    case 2:
                        wrapper.eq(DisplayShelfInspectReport::getRegionDeptId, reportMsg.getMarketAreaId());
                        break;
                    case 3:
                        wrapper.eq(DisplayShelfInspectReport::getBusinessDeptId, reportMsg.getMarketAreaId());
                        break;
                    case 4:
                        wrapper.eq(DisplayShelfInspectReport::getHeadquartersDeptId, reportMsg.getMarketAreaId());
                        break;
                    case 5:
                        wrapper.eq(DisplayShelfInspectReport::getGroupDeptId, reportMsg.getMarketAreaId());
                        break;
                    default:
                        break;
                }
            }
        }
        if (StringUtils.isNotEmpty(reportMsg.getCustomerName())) {
            wrapper.like(DisplayShelfInspectReport::getPutCustomerName, reportMsg.getCustomerName());
        }
        if (StringUtils.isNotEmpty(reportMsg.getCustomerNumber())) {
            wrapper.eq(DisplayShelfInspectReport::getPutCustomerNumber, reportMsg.getCustomerNumber());
        }
        if (StringUtils.isNotEmpty(reportMsg.getShelfType())) {
            wrapper.like(DisplayShelfInspectReport::getName, reportMsg.getShelfType());
        }

        if (StringUtils.isNotEmpty(reportMsg.getStartTime()) && StringUtils.isNotEmpty(reportMsg.getEndTime())) {
            Date start = DateUtil.dayBegin(DateUtil.strToDate(reportMsg.getStartTime(), "yyyy-MM-dd"));
            Date end = DateUtil.dayEnd(DateUtil.strToDate(reportMsg.getEndTime(), "yyyy-MM-dd"));
            wrapper.apply("create_time >= '" + dateFormat.format(start) +"'"  + " and create_time <= '" + dateFormat.format(end) + "'");
        }

        if (StringUtils.isNotEmpty(reportMsg.getSubmitterName())) {
            wrapper.like(DisplayShelfInspectReport::getSubmitterName, reportMsg.getSubmitterName());
        }

        wrapper.orderByDesc(DisplayShelfInspectReport::getCreateTime);
        return wrapper;
    }

    @Override
    public Integer selectByExportCount(LambdaQueryWrapper<DisplayShelfInspectReport> wrapper) {
        return this.count(wrapper);
    }

    @Override
    public void build(ShelfInspectModel model) {
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
        Integer putCount = 0;
        if (SupplierTypeEnum.IS_STORE.getType().equals(model.getCustomerType())) {
            StoreInfoDtoVo store = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(model.getCustomerNumber()));
            customerName = store.getStoreName();
            shNumber = store.getMerchantNumber();
            customerLevel = store.getStoreLevel();
        } else {
            SupplierInfoSessionVo supplier = FeignResponseUtil.getFeignData(feignSupplierClient.getSuppliserInfoByNumber(model.getCustomerNumber()));
            customerName = supplier.getName();
            customerLevel = supplier.getLevel();
        }
        List<DisplayShelf> displayShelfList = displayShelfDao.selectList(Wrappers.<DisplayShelf>lambdaQuery().eq(DisplayShelf::getPutNumber, model.getCustomerNumber()).eq(DisplayShelf::getPutStatus,3).eq(DisplayShelf::getStatus, 1));
        if(CollectionUtils.isNotEmpty(displayShelfList)){
            putCount = displayShelfList.size();
        }
        if(model.getInspectStatus().equals(IceBoxEnums.StatusEnum.NORMAL.getType())) {
            List<ShelfInspectModel.NormalShelf> normalShelves = model.getNormalShelves();
            for (ShelfInspectModel.NormalShelf normalShelf : normalShelves) {
                DisplayShelfInspectReport report = new DisplayShelfInspectReport();
                report.setInspectRemark(model.getRemark())
                        .setSubmitTime(new Date())
                        .setSubmitterName(model.getCreateName())
                        .setSubmitterId(model.getCreateBy())
                        .setSubmitterPosition(user.getPosion())
                        .setPutCustomerNumber(model.getCustomerNumber())
                        .setPutCustomerType(model.getCustomerType())
                        .setPutCustomerName(customerName)
                        .setImageUrl(String.join(",", model.getImageUrls()))
                        .setShNumber(shNumber)
                        .setStatus(0)
                        .setHeadquartersDeptId(headquartersId).setHeadquartersDeptName(headquartersName)
                        .setBusinessDeptId(businessId).setBusinessDeptName(businessName)
                        .setRegionDeptId(regionId).setRegionDeptName(regionName)
                        .setServiceDeptId(serviceId).setServiceDeptName(serviceName)
                        .setGroupDeptId(groupId).setGroupDeptName(groupName)
                        .setApplyNumber(model.getApplyNumber())
                        .setPutCount(putCount)
                        .setName(normalShelf.getName())
                        .setSize(normalShelf.getSize())
                        .setInspectStatus(model.getInspectStatus())
                        .setUnusualNumber(normalShelf.getCount());
                displayShelfInspectReportDao.insert(report);
            }
        }else if(model.getInspectStatus().equals(IceBoxEnums.StatusEnum.SCRAP.getType())){
            List<ShelfInspectModel.ScrapShelf> scrapShelves = model.getScrapShelves();
            for (ShelfInspectModel.ScrapShelf scrapShelve : scrapShelves) {
                DisplayShelfInspectReport report = new DisplayShelfInspectReport();
                report.setInspectRemark(model.getRemark())
                        .setSubmitTime(new Date())
                        .setSubmitterName(model.getCreateName())
                        .setSubmitterId(model.getCreateBy())
                        .setSubmitterPosition(user.getPosion())
                        .setPutCustomerNumber(model.getCustomerNumber())
                        .setPutCustomerType(model.getCustomerType())
                        .setPutCustomerName(customerName)
                        .setImageUrl(String.join(",", model.getImageUrls()))
                        .setShNumber(shNumber)
                        .setStatus(0)
                        .setHeadquartersDeptId(headquartersId).setHeadquartersDeptName(headquartersName)
                        .setBusinessDeptId(businessId).setBusinessDeptName(businessName)
                        .setRegionDeptId(regionId).setRegionDeptName(regionName)
                        .setServiceDeptId(serviceId).setServiceDeptName(serviceName)
                        .setGroupDeptId(groupId).setGroupDeptName(groupName)
                        .setApplyNumber(model.getApplyNumber())
                        .setPutCount(putCount)
                        .setName(scrapShelve.getName())
                        .setSize(scrapShelve.getSize())
                        .setInspectStatus(model.getInspectStatus())
                        .setUnusualNumber(scrapShelve.getCount());
                displayShelfInspectReportDao.insert(report);
            }
        }else if(model.getInspectStatus().equals(IceBoxEnums.StatusEnum.LOSE.getType())){
            List<ShelfInspectModel.LostShelf> lostShelves = model.getLostShelves();
            for (ShelfInspectModel.LostShelf lostShelve : lostShelves) {
                DisplayShelfInspectReport report = new DisplayShelfInspectReport();
                report.setInspectRemark(model.getRemark())
                        .setSubmitTime(new Date())
                        .setSubmitterName(model.getCreateName())
                        .setSubmitterId(model.getCreateBy())
                        .setSubmitterPosition(user.getPosion())
                        .setPutCustomerNumber(model.getCustomerNumber())
                        .setPutCustomerType(model.getCustomerType())
                        .setPutCustomerName(customerName)
                        .setImageUrl(String.join(",", model.getImageUrls()))
                        .setShNumber(shNumber)
                        .setStatus(0)
                        .setHeadquartersDeptId(headquartersId).setHeadquartersDeptName(headquartersName)
                        .setBusinessDeptId(businessId).setBusinessDeptName(businessName)
                        .setRegionDeptId(regionId).setRegionDeptName(regionName)
                        .setServiceDeptId(serviceId).setServiceDeptName(serviceName)
                        .setGroupDeptId(groupId).setGroupDeptName(groupName)
                        .setApplyNumber(model.getApplyNumber())
                        .setPutCount(putCount)
                        .setName(lostShelve.getName())
                        .setSize(lostShelve.getSize())
                        .setInspectStatus(model.getInspectStatus())
                        .setUnusualNumber(lostShelve.getCount());
                displayShelfInspectReportDao.insert(report);
            }
        }
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
