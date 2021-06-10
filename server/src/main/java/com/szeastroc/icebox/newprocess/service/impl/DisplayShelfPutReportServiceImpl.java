package com.szeastroc.icebox.newprocess.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.entity.customer.vo.BaseDistrictVO;
import com.szeastroc.common.entity.customer.vo.StoreInfoDtoVo;
import com.szeastroc.common.entity.customer.vo.SupplierInfoSessionVo;
import com.szeastroc.common.entity.user.vo.SessionDeptInfoVo;
import com.szeastroc.common.entity.user.vo.SimpleUserInfoVo;
import com.szeastroc.common.entity.visit.ShelfPutModel;
import com.szeastroc.common.feign.customer.FeignDistrictClient;
import com.szeastroc.common.feign.customer.FeignStoreClient;
import com.szeastroc.common.feign.customer.FeignSupplierClient;
import com.szeastroc.common.feign.user.FeignCacheClient;
import com.szeastroc.common.feign.user.FeignUserClient;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.icebox.newprocess.consumer.common.ShelfPutReportMsg;
import com.szeastroc.icebox.newprocess.dao.DisplayShelfPutReportDao;
import com.szeastroc.icebox.newprocess.entity.DisplayShelfPutReport;
import com.szeastroc.icebox.newprocess.enums.DeptTypeEnum;
import com.szeastroc.icebox.newprocess.enums.SupplierTypeEnum;
import com.szeastroc.icebox.newprocess.service.DisplayShelfPutApplyRelateService;
import com.szeastroc.icebox.newprocess.service.DisplayShelfPutReportService;
import com.szeastroc.icebox.newprocess.service.DisplayShelfService;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

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
        if(customerType.equals(SupplierTypeEnum.IS_STORE.getType())){
            StoreInfoDtoVo store = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(model.getCustomerNumber()));
            customerAddress = store.getAddress();
            provinceCode = store.getProvinceCode();
            cityCode = store.getCityCode();
            districtCode = store.getDistrictCode();
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
                .linkmanMobile(model.getLinkMobile())
                .linkmanName(model.getLinkMan())
                .visitTypeName(model.getVisitTypeName())
                .putCustomerLevel(model.getCustomerLevel())
                .putCustomerName(model.getCustomerName())
                .putCustomerNumber(model.getCustomerNumber())
                .putCustomerType(customerType)
                .submitterId(model.getCreateBy())
                .submitterName(model.getCreateByName())
                .submitTime(new DateTime(model.getCreateTimeStr()).toDate())
                .submitterMobile(user.getMobile())
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
    public Object detail(String applyNumber) {
//        displayShelfPutApplyRelateService.
        return null;
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
    
}
