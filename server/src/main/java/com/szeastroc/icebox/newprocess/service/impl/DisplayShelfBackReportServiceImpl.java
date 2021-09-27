package com.szeastroc.icebox.newprocess.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.entity.customer.vo.BaseDistrictVO;
import com.szeastroc.common.entity.customer.vo.StoreInfoDtoVo;
import com.szeastroc.common.entity.customer.vo.SupplierInfoSessionVo;
import com.szeastroc.common.entity.user.vo.SessionDeptInfoVo;
import com.szeastroc.common.entity.user.vo.SessionUserInfoVo;
import com.szeastroc.common.entity.user.vo.SimpleUserInfoVo;
import com.szeastroc.common.entity.visit.ShelfBackModel;
import com.szeastroc.common.exception.NormalOptionException;
import com.szeastroc.common.feign.customer.FeignDistrictClient;
import com.szeastroc.common.feign.customer.FeignStoreClient;
import com.szeastroc.common.feign.customer.FeignSupplierClient;
import com.szeastroc.common.feign.user.FeignCacheClient;
import com.szeastroc.common.feign.user.FeignUserClient;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.icebox.enums.DisplayShelfTypeEnum;
import com.szeastroc.icebox.newprocess.dao.DisplayShelfBackReportDao;
import com.szeastroc.icebox.newprocess.entity.DisplayShelfBackReport;
import com.szeastroc.icebox.newprocess.entity.DisplayShelfPutReport;
import com.szeastroc.icebox.newprocess.enums.DeptTypeEnum;
import com.szeastroc.icebox.newprocess.enums.SupplierTypeEnum;
import com.szeastroc.icebox.newprocess.service.DisplayShelfBackReportService;
import com.szeastroc.icebox.newprocess.service.DisplayShelfService;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@Slf4j
public class DisplayShelfBackReportServiceImpl extends ServiceImpl<DisplayShelfBackReportDao, DisplayShelfBackReport> implements DisplayShelfBackReportService {

    @Autowired
    private FeignCacheClient feignCacheClient;
    @Autowired
    private FeignUserClient feignUserClient;
    @Autowired
    private FeignStoreClient feignStoreClient;
    @Autowired
    FeignDistrictClient feignDistrictClient;
    @Autowired
    private FeignSupplierClient feignSupplierClient;
    @Autowired
    private DisplayShelfService displayShelfService;


    /**
     * 构建回退报表的相关信息，并将信息插入到报表中
     * */
    @Override
    @Transactional(rollbackFor = Exception.class, transactionManager = "transactionManager")
    public void buildBackReport(ShelfBackModel model) {
        Integer groupId          = null;
        String  groupName        = null;
        Integer serviceId        = null;
        String  serviceName      = null;
        Integer regionId         = null;
        String  regionName       = null;
        Integer businessId       = null;
        String  businessName     = null;
        Integer headquartersId   = null;
        String  headquartersName = null;
        //获取用户的信息
        SimpleUserInfoVo user = FeignResponseUtil.getFeignData(feignUserClient.findUserById(model.getCreateBy()));
        //获取五级上级部门的信息 组、服务处、大区、事业部、本部
        Map<Integer, SessionDeptInfoVo> deptMap = FeignResponseUtil.getFeignData(feignCacheClient.getFiveLevelDept(model.getMarketAreaId()));
        Set<Integer> keySet = deptMap.keySet();
        for (Integer key : keySet) {
            SessionDeptInfoVo deptInfoVo = deptMap.get(key);
            if (deptInfoVo == null) {
                continue;
            }

            if (DeptTypeEnum.GROUP.getType().equals(deptInfoVo.getDeptType())) {
                groupId = deptInfoVo.getId();
                groupName = deptInfoVo.getName();
                continue;
            }
            if (DeptTypeEnum.SERVICE.getType().equals(deptInfoVo.getDeptType())) {
                serviceId = deptInfoVo.getId();
                serviceName = deptInfoVo.getName();
                continue;
            }
            if (DeptTypeEnum.LARGE_AREA.getType().equals(deptInfoVo.getDeptType())) {
                regionId = deptInfoVo.getId();
                regionName = deptInfoVo.getName();
                continue;
            }
            if (DeptTypeEnum.BUSINESS_UNIT.getType().equals(deptInfoVo.getDeptType())) {
                businessId = deptInfoVo.getId();
                businessName = deptInfoVo.getName();
                continue;
            }
            if (DeptTypeEnum.THIS_PART.getType().equals(deptInfoVo.getDeptType())) {
                headquartersId = deptInfoVo.getId();
                headquartersName = deptInfoVo.getName();
                continue;
            }
        }

        Integer customerType = model.getCustomerType();
        String  provinceName     = null;
        String  cityName         = null;
        String  districtName     = null;
        String  provinceCode     = null;
        String  cityCode         = null;
        String  districtCode     = null;
        String  customerAddress  = null;
        String  shNumber         = null;
        Integer dptyShelf        = null;
        Integer ygnmcShelf       = null;
        Integer dpjqShelf        = null;
        //客户类型是5的话是门店，其它的属于经销商和分销商等类型
        if(customerType.equals(SupplierTypeEnum.IS_STORE.getType())){
            StoreInfoDtoVo store = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(model.getCustomerNumber()));
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
        for (ShelfBackModel.ShelfModel shelfModel : model.getShelfModelList()) {
            // 有三种类型 每种类型有多条数据 货架类型 1东鹏特饮四层陈列架 2由柑柠檬茶四层陈列架 3东鹏加気四层陈列架
            if(DisplayShelfTypeEnum.ENERGY_FOUR.getDesc().equals(shelfModel.getShelfName())){
                dptyShelf = shelfModel.getApplyCount();
            }else if(DisplayShelfTypeEnum.LEMON_TEA_FOUR.getDesc().equals(shelfModel.getShelfName())){
                ygnmcShelf = shelfModel.getApplyCount();
            }else if(DisplayShelfTypeEnum.SODA_FOUR.getDesc().equals(shelfModel.getShelfName())){
                dpjqShelf = shelfModel.getApplyCount();
            }

        }
        //这里还缺少了审核人、审核人职位、审核日期（可以直接使用表里的更新日期）
        DisplayShelfBackReport report = DisplayShelfBackReport.builder()
                .headquartersDeptId(headquartersId).headquartersDeptName(headquartersName)
                .businessDeptId(businessId).businessDeptName(businessName)
                .regionDeptName(regionName).regionDeptId(regionId)
                .serviceDeptId(serviceId).serviceDeptName(serviceName)
                .groupDeptName(groupName).groupDeptId(groupId)
                .customerAddress(customerAddress)
                .province(provinceName).city(cityName).area(districtName)
                .applyNumber(model.getApplyNumber())
                .dealerNumber(model.getSupplierNumber())
                .dealerName(model.getSupplierName())
                .linkMobile(model.getLinkMobile())
                .linkMan(model.getLinkMan())
                .customerName(model.getCustomerName())
                .customerNumber(model.getCustomerNumber())
                .customerType(customerType)
                .merchantNumber(shNumber) //商户编号
                .submitterId(model.getCreateBy())
                .submitterName(model.getCreateByName())
                .backDate(new DateTime(model.getCreateTimeStr()).toDate())
                .submitterMobile(user.getMobile())
                .backRemarks(model.getRemark())
                .build();
        if(dptyShelf != null){
            report.setDptyShelf(dptyShelf);
        }
        if(ygnmcShelf != null){
            report.setYgnmcShelf(ygnmcShelf);
        }
        if(dpjqShelf != null){
            report.setDpjqShelf(dpjqShelf);
        }
        this.save(report);
    }
}
