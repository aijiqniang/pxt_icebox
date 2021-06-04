package com.szeastroc.icebox.newprocess.service.impl;

import cn.hutool.core.util.IdUtil;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.entity.customer.vo.StoreInfoDtoVo;
import com.szeastroc.common.entity.customer.vo.SupplierInfoSessionVo;
import com.szeastroc.common.entity.user.session.MatchRuleVo;
import com.szeastroc.common.entity.user.vo.SysRuleShelfDetailVo;
import com.szeastroc.common.entity.visit.SessionExamineVo;
import com.szeastroc.common.entity.visit.ShelfPutModel;
import com.szeastroc.common.exception.NormalOptionException;
import com.szeastroc.common.feign.customer.FeignStoreClient;
import com.szeastroc.common.feign.customer.FeignSupplierClient;
import com.szeastroc.common.feign.user.FeignDeptClient;
import com.szeastroc.common.feign.user.FeignDeptRuleClient;
import com.szeastroc.common.feign.visit.FeignExamineClient;
import com.szeastroc.common.feign.visit.FeignIceboxQueryClient;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.icebox.enums.DisplayShelfTypeEnum;
import com.szeastroc.icebox.newprocess.dao.DisplayShelfDao;
import com.szeastroc.icebox.newprocess.entity.DisplayShelf;
import com.szeastroc.icebox.newprocess.entity.DisplayShelfPutApply;
import com.szeastroc.icebox.newprocess.entity.DisplayShelfPutApplyRelate;
import com.szeastroc.icebox.newprocess.enums.PutStatus;
import com.szeastroc.icebox.newprocess.enums.SupplierTypeEnum;
import com.szeastroc.icebox.newprocess.enums.VisitCycleEnum;
import com.szeastroc.icebox.newprocess.service.DisplayShelfService;
import com.szeastroc.icebox.newprocess.service.DisplayShelfPutApplyRelateService;
import com.szeastroc.icebox.newprocess.service.DisplayShelfPutApplyService;
import com.szeastroc.icebox.newprocess.vo.SupplierDisplayShelfVO;
import com.szeastroc.icebox.newprocess.vo.request.DisplayShelfPage;
import com.szeastroc.icebox.newprocess.vo.request.ShelfStockRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * (DisplayShelf)表服务实现类
 *
 * @author chenchao
 * @since 2021-05-28 09:36:31
 */
@Slf4j
@Service
public class DisplayShelfServiceImpl extends ServiceImpl<DisplayShelfDao, DisplayShelf> implements DisplayShelfService {
    @Autowired
    private FeignSupplierClient feignSupplierClient;
    @Autowired
    private DisplayShelfPutApplyService shelfPutApplyService;
    @Autowired
    private DisplayShelfPutApplyRelateService shelfPutApplyRelateService;
    @Autowired
    private FeignExamineClient feignExamineClient;
    @Autowired
    private FeignStoreClient feignStoreClient;
    @Autowired
    private FeignDeptClient feignDeptClient;
    @Autowired
    private FeignIceboxQueryClient feignIceboxQueryClient;
    @Autowired
    FeignDeptRuleClient feignDeptRuleClient;

    @Override
    public IPage<DisplayShelf> selectPage(DisplayShelfPage page) {
        IPage<DisplayShelf> selectPage = this.baseMapper.selectPage(page);
        selectPage.convert(o -> {
            List<DisplayShelf.DisplayShelfType> shelfTypes = this.baseMapper.selectType(o.getSupplierNumber());
            o.setList(shelfTypes);
            return o;
        });
        return selectPage;
    }

    @Override
    public IPage<DisplayShelf> selectDetails(DisplayShelfPage page) {
        return this.baseMapper.selectDetails(page);
    }

    @Transactional(rollbackFor = Exception.class, transactionManager = "transactionManager")
    @Override
    public void importData(MultipartFile file) {
        try {
            List<DisplayShelf.DisplayShelfData> list = EasyExcel.read(file.getInputStream()).head(DisplayShelf.DisplayShelfData.class).sheet().doReadSync();
            int energyTotalCount = list.parallelStream().parallel().mapToInt(DisplayShelf.DisplayShelfData::getEnergyCount).sum();
            int lemonTeaTotalCount = list.parallelStream().parallel().mapToInt(DisplayShelf.DisplayShelfData::getLemonTeaCount).sum();
            int sodaTotalCount = list.parallelStream().parallel().mapToInt(DisplayShelf.DisplayShelfData::getSodaCount).sum();
            if (energyTotalCount + lemonTeaTotalCount + sodaTotalCount > 10000) {
                throw new NormalOptionException(Constants.API_CODE_FAIL, "每次导入陈列货架总数不能超过5000");
            }
            for (DisplayShelf.DisplayShelfData o : list) {
                List<DisplayShelf> shelves = new ArrayList<>();
                SupplierInfoSessionVo supplier = FeignResponseUtil.getFeignData(feignSupplierClient.getSuppliserInfoByNumber(o.getSupplierNumber()));
                Optional.ofNullable(o.getEnergyCount()).ifPresent(count -> {
                    if (count > 0) {
                        DisplayShelf displayShelf = buildData(o, supplier);
                        displayShelf.setName(DisplayShelfTypeEnum.ENERGY_FOUR.getDesc());
                        displayShelf.setType(DisplayShelfTypeEnum.ENERGY_FOUR.getType());
                        for (int i = 0; i < count; i++) {
                            shelves.add(displayShelf);
                        }
                    }

                });
                Optional.ofNullable(o.getLemonTeaCount()).ifPresent(count -> {
                    if (count > 0) {
                        DisplayShelf displayShelf = buildData(o, supplier);
                        displayShelf.setName(DisplayShelfTypeEnum.LEMON_TEA_FOUR.getDesc());
                        displayShelf.setType(DisplayShelfTypeEnum.LEMON_TEA_FOUR.getType());
                        for (int i = 0; i < count; i++) {
                            shelves.add(displayShelf);
                        }
                    }

                });
                Optional.ofNullable(o.getSodaCount()).ifPresent(count -> {
                    if (count > 0) {
                        DisplayShelf displayShelf = buildData(o, supplier);
                        displayShelf.setName(DisplayShelfTypeEnum.SODA_FOUR.getDesc());
                        displayShelf.setType(DisplayShelfTypeEnum.SODA_FOUR.getType());
                        for (int i = 0; i < count; i++) {
                            shelves.add(displayShelf);
                        }
                    }

                });
                this.saveBatch(shelves);
            }
        } catch (Exception e) {
            throw new NormalOptionException(Constants.API_CODE_FAIL, "导入陈列架失败");
        }
    }

    private DisplayShelf buildData(DisplayShelf.DisplayShelfData o, SupplierInfoSessionVo supplier) {
        DisplayShelf displayShelf = new DisplayShelf();
        BeanUtils.copyProperties(o, displayShelf);
        displayShelf.setHeadquartersDeptId(supplier.getHeadquartersDeptId());
        displayShelf.setHeadquartersDeptName(supplier.getHeadquartersDeptName());
        displayShelf.setBusinessDeptId(supplier.getBusinessDeptId());
        displayShelf.setRegionDeptId(supplier.getRegionDeptId());
        displayShelf.setServiceDeptId(supplier.getServiceDeptId());
        displayShelf.setSupplierType(supplier.getType());
        return displayShelf;
    }

    @Override
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public List<SessionExamineVo.VisitExamineNodeVo> shelfPut(ShelfPutModel model) {
        String applyNumber = "PUT" + IdUtil.simpleUUID().substring(0, 29);
        model.setApplyNumber(applyNumber);
        DisplayShelfPutApply displayShelfPutApply = DisplayShelfPutApply.builder()
                .applyNumber(applyNumber)
                .putCustomerNumber(model.getCustomerNumber())
                .putCustomerType(model.getCustomerType())
                .userId(model.getCreateBy())
                .createdBy(model.getCreateBy())
                .remark(model.getRemark())
                .deptId(model.getMarketAreaId())
                .build();
        shelfPutApplyService.save(displayShelfPutApply);
        for (ShelfPutModel.ShelfModel shelfModel : model.getShelfModelList()) {
            List<DisplayShelf> shelves = this.list(
                    Wrappers.<DisplayShelf>lambdaQuery()
                            .eq(DisplayShelf::getSupplierNumber, model.getSupplierNumber())
                            .eq(DisplayShelf::getType, shelfModel.getShelfType())
                            .eq(DisplayShelf::getStatus, 1)
                            .eq(DisplayShelf::getPutStatus, PutStatus.NO_PUT.getStatus())
                            .last("limit " + shelfModel.getApplyCount())
            );
            if (CollectionUtils.isNotEmpty(shelves) && shelves.size() == shelfModel.getApplyCount()) {
                for (DisplayShelf displayShelf : shelves) {
                    displayShelf.setPutStatus(PutStatus.LOCK_PUT.getStatus());
                    displayShelf.setPutNumber(model.getCustomerNumber());
                    displayShelf.setPutName(model.getCustomerName());
                    displayShelf.setUpdateTime(new Date());
                    DisplayShelfPutApplyRelate displayShelfPutApplyRelate = new DisplayShelfPutApplyRelate();
                    displayShelfPutApplyRelate.setShelfId(displayShelf.getId()).setApplyNumber(applyNumber);
                    shelfPutApplyRelateService.save(displayShelfPutApplyRelate);
                    this.updateById(displayShelf);
                }
            } else {
                throw new NormalOptionException(Constants.API_CODE_FAIL, "经销商可投放货架不足");
            }
        }
        SessionExamineVo sessionExamineVo = FeignResponseUtil.getFeignData(feignExamineClient.createShelfPut(model));
        List<SessionExamineVo.VisitExamineNodeVo> visitExamineNodes = sessionExamineVo.getVisitExamineNodes();
        if(CollectionUtils.isNotEmpty(visitExamineNodes)){
            displayShelfPutApply.setExamineId(visitExamineNodes.get(0).getExamineId());
            shelfPutApplyService.updateById(displayShelfPutApply);
        }
        return visitExamineNodes;
    }


    @Override
    public List<SupplierDisplayShelfVO> canPut(ShelfStockRequest request) {
        //查询货架投放规则
        MatchRuleVo matchRuleVo = new MatchRuleVo();
        matchRuleVo.setOpreateType(11);
        matchRuleVo.setDeptId(request.getMarketAreaId());
        matchRuleVo.setType(3);
        SysRuleShelfDetailVo putRule = FeignResponseUtil.getFeignData(feignDeptRuleClient.matchShelfRule(matchRuleVo));
        if(Objects.isNull(putRule)){
            throw new NormalOptionException(Constants.API_CODE_FAIL,"未设置货架投放规则");
        }
        String shelfType = putRule.getShelfType();
        if(StringUtils.isBlank(shelfType)){
            throw new NormalOptionException(Constants.API_CODE_FAIL,"货架投放规则未设置货架类型");
        }
        Integer serviceId = FeignResponseUtil.getFeignData(feignDeptClient.getServiceId(request.getMarketAreaId()));
        String[] typeArr = shelfType.split(",");
        List<DisplayShelf> shelfList = this.baseMapper.noPutShelves(serviceId,typeArr);
        List<SupplierDisplayShelfVO> list = shelfList.stream().map(o -> {
            SupplierDisplayShelfVO vo = new SupplierDisplayShelfVO();
            BeanUtils.copyProperties(o, vo);
            SupplierInfoSessionVo supplier = FeignResponseUtil.getFeignData(feignSupplierClient.getSuppliserInfoByNumber(o.getSupplierNumber()));
            vo.setLinkMobile(supplier.getLinkManMobile());
            vo.setLinkMan(supplier.getLinkMan());
            vo.setLinkAddress(supplier.getAddress());
            vo.setVisitTypeName(VisitCycleEnum.getDescByCode(FeignResponseUtil.getFeignData(feignIceboxQueryClient.selectVisitTypeForReport(request.getCustomerNumber()))));
            return vo;
        }).collect(Collectors.toList());

        return list;
    }


    @Override
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public void doPut(String applyNumber) {
        DisplayShelfPutApply apply = shelfPutApplyService.getOne(Wrappers.<DisplayShelfPutApply>lambdaQuery().eq(DisplayShelfPutApply::getApplyNumber, applyNumber));
        List<DisplayShelfPutApplyRelate> relates = shelfPutApplyRelateService.list(Wrappers.<DisplayShelfPutApplyRelate>lambdaQuery().eq(DisplayShelfPutApplyRelate::getApplyNumber, applyNumber));
        Collection<DisplayShelf> displayShelves = this.listByIds(relates.stream().map(DisplayShelfPutApplyRelate::getShelfId).collect(Collectors.toList()));
        Integer headquartersDeptId;
        String headquartersDeptName;
        Integer businessDeptId;
        String businessDeptName;
        Integer regionDeptId;
        String regionDeptName;
        Integer serviceDeptId;
        String serviceDeptName;
        Integer groupDeptId;
        String groupDeptName;
        if (apply.getPutCustomerType().equals(SupplierTypeEnum.IS_STORE.getType())) {
            StoreInfoDtoVo store = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(apply.getPutCustomerNumber()));
            headquartersDeptId = store.getHeadquartersDeptId();
            headquartersDeptName = store.getHeadquartersDeptName();
            businessDeptId = store.getBusinessDeptId();
            businessDeptName = store.getBusinessDeptName();
            regionDeptId = store.getRegionDeptId();
            regionDeptName = store.getRegionDeptName();
            serviceDeptId = store.getServiceDeptId();
            serviceDeptName = store.getServiceDeptName();
            groupDeptId = store.getGroupDeptId();
            groupDeptName = store.getGroupDeptName();
        } else {
            SupplierInfoSessionVo supplier = FeignResponseUtil.getFeignData(feignSupplierClient.getSuppliserInfoByNumber(apply.getPutCustomerNumber()));
            headquartersDeptId = supplier.getHeadquartersDeptId();
            headquartersDeptName = supplier.getHeadquartersDeptName();
            businessDeptId = supplier.getBusinessDeptId();
            businessDeptName = supplier.getBusinessDeptName();
            regionDeptId = supplier.getRegionDeptId();
            regionDeptName = supplier.getRegionDeptName();
            serviceDeptId = supplier.getServiceDeptId();
            serviceDeptName = supplier.getServiceDeptName();
            groupDeptId = supplier.getGroupDeptId();
            groupDeptName = supplier.getGroupDeptName();
        }
        displayShelves.forEach(o -> {
            o.setPutStatus(PutStatus.FINISH_PUT.getStatus());
            o.setHeadquartersDeptId(headquartersDeptId);
            o.setHeadquartersDeptName(headquartersDeptName);
            o.setBusinessDeptName(businessDeptName);
            o.setBusinessDeptId(businessDeptId);
            o.setRegionDeptName(regionDeptName);
            o.setRegionDeptId(regionDeptId);
            o.setServiceDeptName(serviceDeptName);
            o.setServiceDeptId(serviceDeptId);
            o.setGroupDeptName(groupDeptName);
            o.setGroupDeptId(groupDeptId);
        });
        this.updateBatchById(displayShelves);

    }


}
