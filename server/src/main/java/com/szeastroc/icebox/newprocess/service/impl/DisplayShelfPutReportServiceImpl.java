package com.szeastroc.icebox.newprocess.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.entity.customer.vo.BaseDistrictVO;
import com.szeastroc.common.entity.customer.vo.StoreInfoDtoVo;
import com.szeastroc.common.entity.customer.vo.SupplierInfoSessionVo;
import com.szeastroc.common.entity.user.session.UserManageVo;
import com.szeastroc.common.entity.user.vo.SessionDeptInfoVo;
import com.szeastroc.common.entity.user.vo.SimpleUserInfoVo;
import com.szeastroc.common.entity.visit.ShelfPutModel;
import com.szeastroc.common.feign.customer.FeignDistrictClient;
import com.szeastroc.common.feign.customer.FeignStoreClient;
import com.szeastroc.common.feign.customer.FeignSupplierClient;
import com.szeastroc.common.feign.user.FeignCacheClient;
import com.szeastroc.common.feign.user.FeignUserClient;
import com.szeastroc.common.feign.visit.FeignExportRecordsClient;
import com.szeastroc.common.feign.visit.FeignIceboxQueryClient;
import com.szeastroc.common.utils.ExecutorServiceFactory;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.commondb.config.redis.JedisClient;
import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.icebox.constant.RedisConstant;
import com.szeastroc.icebox.newprocess.consumer.common.ShelfPutReportMsg;
import com.szeastroc.icebox.newprocess.dao.DisplayShelfDao;
import com.szeastroc.icebox.newprocess.dao.DisplayShelfPutReportDao;
import com.szeastroc.icebox.newprocess.entity.*;
import com.szeastroc.icebox.newprocess.enums.DeptTypeEnum;
import com.szeastroc.icebox.newprocess.enums.SupplierTypeEnum;
import com.szeastroc.icebox.newprocess.enums.VisitCycleEnum;
import com.szeastroc.icebox.newprocess.service.DisplayShelfPutApplyRelateService;
import com.szeastroc.icebox.newprocess.service.DisplayShelfPutReportService;
import com.szeastroc.icebox.newprocess.service.DisplayShelfService;
import com.szeastroc.icebox.newprocess.vo.ShelfPutReportVo;
import com.szeastroc.icebox.newprocess.vo.request.DisplayShelfPage;
import org.joda.time.DateTime;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * (DisplayShelfPutReport)表服务实现类
 *
 * @author chenchao
 * @since 2021-06-07 10:26:40
 */
@Service
public class DisplayShelfPutReportServiceImpl extends ServiceImpl<DisplayShelfPutReportDao, DisplayShelfPutReport> implements DisplayShelfPutReportService {
    @Autowired
    private FeignUserClient feignUserClient;
    @Autowired
    private FeignCacheClient feignCacheClient;
    @Autowired
    private FeignSupplierClient feignSupplierClient;
    @Autowired
    private FeignStoreClient feignStoreClient;
    @Autowired
    FeignDistrictClient feignDistrictClient;
    @Autowired
    private DisplayShelfService displayShelfService;
    @Autowired
    DisplayShelfPutApplyRelateService displayShelfPutApplyRelateService;
    @Autowired
    private JedisClient jedis;
    @Autowired
    FeignExportRecordsClient feignExportRecordsClient;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private FeignIceboxQueryClient feignIceboxQueryClient;
    @Autowired
    private DisplayShelfDao displayShelfDao;
    @Resource
    private DisplayShelfPutReportDao displayShelfPutReportDao;


    @Override
    public void build(ShelfPutModel model) {
        SimpleUserInfoVo user = FeignResponseUtil.getFeignData(feignUserClient.findUserById(model.getCreateBy()));
        Map<Integer, SessionDeptInfoVo> deptMap = FeignResponseUtil.getFeignData(feignCacheClient.getFiveLevelDept(model.getMarketAreaId()));
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
        SessionDeptInfoVo headquarters = deptMap.get(5);
        if(!DeptTypeEnum.BUSINESS_UNIT.getType().equals(business.getDeptType())){
            business = null;
            headquarters = deptMap.get(4);
        }
        if(Objects.nonNull(business)){
            businessId = business.getId();
            businessName = business.getName();
        }

        if(Objects.nonNull(headquarters)){
            headquartersId = headquarters.getId();
            headquartersName = headquarters.getName();
        }
        Integer customerType = model.getCustomerType();
        String provinceName=null;
        String cityName=null;
        String districtName=null;
        String provinceCode = null;
        String cityCode = null;
        String districtCode = null;
        String customerAddress = null;
        String shNumber = null;
        String visitTypeName = null;
        String linkmanMobile = null;
        String linkmanName = null;
        if(customerType.equals(SupplierTypeEnum.IS_STORE.getType())){
            visitTypeName = VisitCycleEnum.getDescByCode(FeignResponseUtil.getFeignData(feignIceboxQueryClient.selectVisitTypeForReport(model.getCustomerNumber())));
            StoreInfoDtoVo store = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(model.getCustomerNumber()));
            linkmanMobile = store.getMainSaleManMobile();
            linkmanName = store.getMainSaleManName();
            customerAddress = store.getAddress();
            provinceCode = store.getProvinceCode();
            cityCode = store.getCityCode();
            districtCode = store.getDistrictCode();
            shNumber = store.getMerchantNumber();
        }else{
            SupplierInfoSessionVo supplierInfoSessionVo = FeignResponseUtil.getFeignData(feignSupplierClient.getSuppliserInfoByNumber(model.getCustomerNumber()));
            customerAddress = supplierInfoSessionVo.getAddress();
            provinceCode = supplierInfoSessionVo.getProvinceCode();
            cityCode = supplierInfoSessionVo.getCityCode();
            districtCode = supplierInfoSessionVo.getRegionCode();
        }
        BaseDistrictVO province = FeignResponseUtil.getFeignData(feignDistrictClient.getByCode(provinceCode));
        if(Objects.nonNull(province)){
            provinceName = province.getName();
        }
        BaseDistrictVO city = FeignResponseUtil.getFeignData(feignDistrictClient.getByCode(cityCode));
        if(Objects.nonNull(city)){
            cityName = city.getName();
        }
        BaseDistrictVO district = FeignResponseUtil.getFeignData(feignDistrictClient.getByCode(districtCode));
        if(Objects.nonNull(district)){
            districtName = district.getName();
        }
        DisplayShelfPutReport report = DisplayShelfPutReport.builder()
                .headquartersDeptId(headquartersId).headquartersDeptName(headquartersName)
                .businessDeptId(businessId).businessDeptName(businessName)
                .regionDeptName(regionName).regionDeptId(regionId)
                .serviceDeptId(serviceId).serviceDeptName(serviceName)
                .groupDeptName(groupName).groupDeptId(groupId)
                .customerAddress(customerAddress)
                .provinceName(provinceName).cityName(cityName).districtName(districtName)
                .applyNumber(model.getApplyNumber())
                .supplierNumber(model.getSupplierNumber())
                .supplierName(model.getSupplierName())
                .linkmanMobile(linkmanMobile)
                .linkmanName(linkmanName)
                .putCustomerLevel(model.getCustomerLevel())
                .putCustomerName(model.getCustomerName())
                .putCustomerNumber(model.getCustomerNumber())
                .putCustomerType(customerType)
                .visitTypeName(visitTypeName)
                .shNumber(shNumber)
                .submitterId(model.getCreateBy())
                .submitterName(model.getCreateByName())
                .submitTime(new DateTime(model.getCreateTimeStr()).toDate())
                .submitterMobile(user.getMobile())
                .putRemark(model.getRemark())
                .build();
        this.save(report);
    }

    @Override
    public IPage<DisplayShelfPutReport> selectPage(ShelfPutReportMsg reportMsg) {
        LambdaQueryWrapper<DisplayShelfPutReport> wrapper = this.fillWrapper(reportMsg);
        IPage<DisplayShelfPutReport> page = this.page(reportMsg, wrapper);
        return page;
    }

    @Override
    public IPage<ShelfPutReportVo> selectPutPage(DisplayShelfPage reportMsg) {
        IPage<ShelfPutReportVo> accountPage = new Page<>();
        List<ShelfPutReportVo> shelfPutReports = new ArrayList<>();
        IPage<DisplayShelf> displayShelfIPage = displayShelfDao.selectReportDetailsPage(reportMsg);
        List<DisplayShelf> records = displayShelfIPage.getRecords();
        for (DisplayShelf displayShelf : records) {
            ShelfPutReportVo shelfPutReportVo = new ShelfPutReportVo();
            DisplayShelfPutReport displayShelfPutReport = displayShelfPutReportDao.selectOne(Wrappers.<DisplayShelfPutReport>lambdaQuery().eq(DisplayShelfPutReport::getPutCustomerNumber, displayShelf.getPutNumber()).last(" limit 1"));
            BeanUtils.copyProperties(displayShelf, shelfPutReportVo);
            shelfPutReportVo.setShNumber(displayShelfPutReport.getShNumber())
                .setPutCustomerLevel(displayShelfPutReport.getPutCustomerLevel())
                .setVisitTypeName(displayShelfPutReport.getVisitTypeName())
                .setCustomerAddress(displayShelfPutReport.getCustomerAddress())
                .setLinkmanMobile(displayShelfPutReport.getLinkmanMobile())
                .setLinkmanName(displayShelfPutReport.getLinkmanName());
            shelfPutReports.add(shelfPutReportVo);
        }
        accountPage.setRecords(shelfPutReports);
        return accountPage;
    }

    @Override
    public Object detail(String applyNumber) {
        List<DisplayShelfPutApplyRelate> relates = displayShelfPutApplyRelateService.list(Wrappers.<DisplayShelfPutApplyRelate>lambdaQuery().eq(DisplayShelfPutApplyRelate::getApplyNumber, applyNumber));
        return displayShelfService.listByIds(relates.stream().map(DisplayShelfPutApplyRelate::getShelfId).collect(Collectors.toList()));
    }

    @Override
    public CommonResponse export(ShelfPutReportMsg reportMsg) {
        // 获取当前用户相关信息
        UserManageVo userManageVo = FeignResponseUtil.getFeignData(feignUserClient.getSessionUserInfo());
        String key = String.format("%s%s", RedisConstant.SHELF_PUT_REPORT_EXPORT_KEY, userManageVo.getSessionUserInfoVo().getId());
        if (null != jedis.get(key)) {
            return new CommonResponse<>(Constants.API_CODE_FAIL, "请求导出操作频繁，请稍候操作");
        }
        LambdaQueryWrapper<DisplayShelfPutReport> wrapper = fillWrapper(reportMsg);
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
            rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.shelfPutReportKey, reportMsg);
        }, ExecutorServiceFactory.getInstance());
        // 三分钟间隔
        jedis.set(key, "ex", 300, TimeUnit.SECONDS);
        return new CommonResponse(Constants.API_CODE_SUCCESS,null);
    }


    @Override
    public Integer selectByExportCount(LambdaQueryWrapper<DisplayShelfPutReport> wrapper) {
        return this.count(wrapper);
    }


    @Override
    public LambdaQueryWrapper<DisplayShelfPutReport> fillWrapper(ShelfPutReportMsg reportMsg) {
        LambdaQueryWrapper<DisplayShelfPutReport> wrapper = Wrappers.<DisplayShelfPutReport>lambdaQuery();
        if(reportMsg.getGroupDeptId() != null){
            wrapper.eq(DisplayShelfPutReport::getGroupDeptId,reportMsg.getGroupDeptId());
        }
        if(reportMsg.getServiceDeptId() != null){
            wrapper.eq(DisplayShelfPutReport::getServiceDeptId,reportMsg.getServiceDeptId());
        }
        if(reportMsg.getRegionDeptId() != null){
            wrapper.eq(DisplayShelfPutReport::getRegionDeptId,reportMsg.getRegionDeptId());
        }
        if(reportMsg.getBusinessDeptId() != null){
            wrapper.eq(DisplayShelfPutReport::getBusinessDeptId,reportMsg.getBusinessDeptId());
        }
        if(reportMsg.getHeadquartersDeptId() != null){
            wrapper.eq(DisplayShelfPutReport::getHeadquartersDeptId,reportMsg.getHeadquartersDeptId());
        }
        if(StringUtils.isNotEmpty(reportMsg.getCustomerName())){
            wrapper.like(DisplayShelfPutReport::getPutCustomerName,reportMsg.getCustomerName());
        }
        if(StringUtils.isNotEmpty(reportMsg.getCustomerNumber())){
            wrapper.eq(DisplayShelfPutReport::getPutCustomerNumber,reportMsg.getCustomerNumber());
        }
        if(StringUtils.isNotEmpty(reportMsg.getStartTime())){
            wrapper.ge(DisplayShelfPutReport::getCreateTime,reportMsg.getStartTime());
        }
        if(StringUtils.isNotEmpty(reportMsg.getEndTime())){
            wrapper.le(DisplayShelfPutReport::getCreateTime,reportMsg.getEndTime());
        }
        if(reportMsg.getPutStatus() != null){
            wrapper.eq(DisplayShelfPutReport::getPutStatus,reportMsg.getPutStatus());
        }
        wrapper.orderByDesc(DisplayShelfPutReport::getCreateTime);
        return wrapper;
    }


//    @Override
    public LambdaQueryWrapper<DisplayShelf> fillWrappers(ShelfPutReportMsg reportMsg) {
        LambdaQueryWrapper<DisplayShelf> wrapper = Wrappers.<DisplayShelf>lambdaQuery();
        /*if(reportMsg != null) {
            if (reportMsg.getDeptType() != null && reportMsg.getMarketAreaId() != null) {
                switch (reportMsg.getDeptType()) {
                    //deptType  1:服务处 2:大区 3:事业部 4:本部 5:组
                    case 1:
                        wrapper.eq(DisplayShelf::getServiceDeptId, reportMsg.getMarketAreaId());
                        break;
                    case 2:
                        wrapper.eq(DisplayShelf::getRegionDeptId, reportMsg.getMarketAreaId());
                        break;
                    case 3:
                        wrapper.eq(DisplayShelf::getBusinessDeptId, reportMsg.getMarketAreaId());
                        break;
                    case 4:
                        wrapper.eq(DisplayShelf::getHeadquartersDeptId, reportMsg.getMarketAreaId());
                        break;
                    case 5:
                        wrapper.eq(DisplayShelf::getGroupDeptId, reportMsg.getMarketAreaId());
                        break;
                    default:
                        break;
                }
            }
        }*/
        /*if(StringUtils.isNotEmpty(reportMsg.getCustomerName())){
            wrapper.like(DisplayShelfPutReport::getPutCustomerName,reportMsg.getCustomerName());
        }
        if(StringUtils.isNotEmpty(reportMsg.getCustomerNumber())){
            wrapper.eq(DisplayShelfPutReport::getPutCustomerNumber,reportMsg.getCustomerNumber());
        }
        if(StringUtils.isNotEmpty(reportMsg.getStartTime())){
            wrapper.ge(DisplayShelfPutReport::getCreateTime,reportMsg.getStartTime());
        }
        if(StringUtils.isNotEmpty(reportMsg.getEndTime())){
            wrapper.le(DisplayShelfPutReport::getCreateTime,reportMsg.getEndTime());
        }
        if(reportMsg.getPutStatus() != null){
            wrapper.eq(DisplayShelfPutReport::getPutStatus,reportMsg.getPutStatus());
        }
        wrapper.orderByDesc(DisplayShelfPutReport::getCreateTime);*/
        return wrapper;
    }
}
