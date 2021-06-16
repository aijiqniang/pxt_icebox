package com.szeastroc.icebox.newprocess.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sun.org.apache.regexp.internal.RE;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.entity.customer.vo.StoreInfoDtoVo;
import com.szeastroc.common.entity.customer.vo.SupplierInfoSessionVo;
import com.szeastroc.common.entity.icebox.enums.IceBoxStatus;
import com.szeastroc.common.entity.icebox.vo.IceBoxRequest;
import com.szeastroc.common.entity.user.session.MatchRuleVo;
import com.szeastroc.common.entity.user.vo.SimpleUserInfoVo;
import com.szeastroc.common.entity.user.vo.SysRuleShelfDetailVo;
import com.szeastroc.common.entity.visit.SessionExamineVo;
import com.szeastroc.common.entity.visit.SessionVisitExamineBacklog;
import com.szeastroc.common.entity.visit.ShelfPutModel;
import com.szeastroc.common.exception.NormalOptionException;
import com.szeastroc.common.feign.customer.FeignStoreClient;
import com.szeastroc.common.feign.customer.FeignSupplierClient;
import com.szeastroc.common.feign.user.FeignDeptRuleClient;
import com.szeastroc.common.feign.user.FeignUserClient;
import com.szeastroc.common.feign.visit.FeignBacklogClient;
import com.szeastroc.common.feign.visit.FeignExamineClient;
import com.szeastroc.common.feign.visit.FeignIceboxQueryClient;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.icebox.enums.DisplayShelfTypeEnum;
import com.szeastroc.icebox.enums.ExamineStatusEnum;
import com.szeastroc.icebox.newprocess.dao.DisplayShelfPutApplyDao;
import com.szeastroc.icebox.newprocess.entity.DisplayShelf;
import com.szeastroc.icebox.newprocess.entity.DisplayShelfPutApply;
import com.szeastroc.icebox.newprocess.entity.DisplayShelfPutApplyRelate;
import com.szeastroc.icebox.newprocess.entity.DisplayShelfPutReport;
import com.szeastroc.icebox.newprocess.enums.ExamineNodeStatusEnum;
import com.szeastroc.icebox.newprocess.enums.ExamineTypeEnum;
import com.szeastroc.icebox.newprocess.enums.IceBoxEnums;
import com.szeastroc.icebox.newprocess.enums.PutStatus;
import com.szeastroc.icebox.newprocess.enums.StoreSignStatus;
import com.szeastroc.icebox.newprocess.enums.VisitCycleEnum;
import com.szeastroc.icebox.newprocess.service.DisplayShelfPutApplyRelateService;
import com.szeastroc.icebox.newprocess.service.DisplayShelfPutApplyService;
import com.szeastroc.icebox.newprocess.service.DisplayShelfPutReportService;
import com.szeastroc.icebox.newprocess.service.DisplayShelfService;
import com.szeastroc.icebox.newprocess.vo.DisplayShelfPutApplyVo;
import com.szeastroc.icebox.newprocess.vo.ExamineNodeVo;
import com.szeastroc.icebox.newprocess.vo.SupplierDisplayShelfVO;
import com.szeastroc.icebox.newprocess.vo.request.InvalidShelfApplyRequest;
import com.szeastroc.icebox.newprocess.vo.request.SignShelfRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * <p>
 * 业务员申请表  服务实现类
 * </p>
 *
 * @author 陈超
 * @since 2021-06-01
 */
@Service
public class DisplayDisplayShelfPutApplyServiceImpl extends ServiceImpl<DisplayShelfPutApplyDao, DisplayShelfPutApply> implements DisplayShelfPutApplyService {
    @Autowired
    private FeignDeptRuleClient feignDeptRuleClient;
    @Autowired
    private DisplayShelfService displayShelfService;
    @Autowired
    private DisplayShelfPutApplyRelateService shelfPutApplyRelateService;
    @Autowired
    private FeignExamineClient feignExamineClient;
    @Autowired
    private FeignSupplierClient feignSupplierClient;
    @Autowired
    private FeignStoreClient feignStoreClient;
    @Autowired
    private FeignIceboxQueryClient feignIceboxQueryClient;
    @Autowired
    private FeignBacklogClient feignBacklogClient;
    @Autowired
    private DisplayShelfPutReportService putReportService;
    @Autowired
    private FeignUserClient feignUserClient;

    @Override
    @Transactional(rollbackFor = Exception.class, value = "transactionManager")
    public void updateStatus(IceBoxRequest request) {
        DisplayShelfPutApply displayShelfPutApply = this.getOne(Wrappers.<DisplayShelfPutApply>lambdaQuery().eq(DisplayShelfPutApply::getApplyNumber, request.getApplyNumber()));
        Integer updateBy = request.getUpdateBy();
        Optional.ofNullable(displayShelfPutApply).ifPresent(o -> {
            displayShelfPutApply.setExamineStatus(request.getExamineStatus());
            displayShelfPutApply.setUpdatedBy(updateBy);
            displayShelfPutApply.setUpdateTime(new Date());
            this.updateById(displayShelfPutApply);
        });
        //审批通过将货架置为投放中状态，商户签收将状态置为已投放
        if (IceBoxStatus.IS_PUTING.getStatus().equals(request.getStatus())) {
            Optional.ofNullable(displayShelfPutApply).ifPresent(o -> {
                //查询货架投放规则
                MatchRuleVo matchRuleVo = new MatchRuleVo();
                matchRuleVo.setOpreateType(11);
                matchRuleVo.setDeptId(displayShelfPutApply.getDeptId());
                matchRuleVo.setType(3);
                SysRuleShelfDetailVo approvalRule = FeignResponseUtil.getFeignData(feignDeptRuleClient.matchShelfRule(matchRuleVo));
                Optional.ofNullable(approvalRule).ifPresent(rule -> {
                    Boolean unableRegister = false;
                    Boolean isBind = false;
                    //无法注册门店 自动签收
                    if (5 == o.getPutCustomerType()) {
                        unableRegister = FeignResponseUtil.getFeignData(feignStoreClient.isUnableRegister(o.getPutCustomerNumber()));
                        isBind = FeignResponseUtil.getFeignData(feignStoreClient.isBind(o.getPutCustomerNumber()));
                    }
                    //不需要签收
                    if (!rule.getIsSign() || unableRegister || !isBind) {
                        displayShelfPutApply.setSignStatus(StoreSignStatus.ALREADY_SIGN.getStatus());
                        displayShelfPutApply.setUpdateTime(new Date());
                        this.updateById(displayShelfPutApply);
                        //更改货架状态
                        displayShelfService.doPut(displayShelfPutApply.getApplyNumber());
                    } else {
                        List<DisplayShelfPutApplyRelate> relates = shelfPutApplyRelateService.list(Wrappers.<DisplayShelfPutApplyRelate>lambdaQuery().eq(DisplayShelfPutApplyRelate::getApplyNumber, request.getApplyNumber()));
                        Collection<DisplayShelf> displayShelves = displayShelfService.listByIds(relates.stream().map(DisplayShelfPutApplyRelate::getShelfId).collect(Collectors.toList()));
                        displayShelves.forEach(shelf -> {
                            shelf.setPutStatus(PutStatus.DO_PUT.getStatus());
                        });
                        displayShelfService.updateBatchById(displayShelves);
                    }
                });
            });
        }
        //报表
        DisplayShelfPutReport putReport = putReportService.getOne(Wrappers.<DisplayShelfPutReport>lambdaQuery().eq(DisplayShelfPutReport::getApplyNumber, request.getApplyNumber()));
        putReport.setExamineTime(new Date());
        putReport.setExamineRemark(request.getExamineRemark());
        putReport.setExamineUserId(updateBy);
        SimpleUserInfoVo userInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findUserById(updateBy));
        putReport.setExamineUserName(userInfoVo.getRealname());
        putReport.setExamineUserPosion(userInfoVo.getPosion());
        if(request.getExamineStatus().equals(ExamineStatusEnum.IS_DEFAULT.getStatus())){
            putReport.setPutStatus(PutStatus.DO_PUT.getStatus());
        }else if(request.getExamineStatus().equals(ExamineStatusEnum.IS_PASS.getStatus())){
            putReport.setPutStatus(PutStatus.FINISH_PUT.getStatus());
        }else if(request.getExamineStatus().equals(ExamineStatusEnum.UN_PASS.getStatus())){
            putReport.setPutStatus(PutStatus.NO_PASS.getStatus());
        }
        putReportService.updateById(putReport);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, value = "transactionManager")
    public void sign(SignShelfRequest request) {
        DisplayShelfPutApply shelfPutApply = this.getOne(Wrappers.<DisplayShelfPutApply>lambdaQuery().eq(DisplayShelfPutApply::getPutCustomerNumber, request.getCustomerNumber()).eq(DisplayShelfPutApply::getSignStatus, StoreSignStatus.DEFAULT_SIGN));
        if (Objects.isNull(shelfPutApply)) {
            throw new NormalOptionException(Constants.API_CODE_FAIL, "门店暂无可签收货架");
        }
        List<DisplayShelfPutApplyRelate> relates = shelfPutApplyRelateService.list(Wrappers.<DisplayShelfPutApplyRelate>lambdaQuery().eq(DisplayShelfPutApplyRelate::getApplyNumber, shelfPutApply.getApplyNumber()));
        Collection<DisplayShelf> displayShelves = displayShelfService.listByIds(relates.stream().map(DisplayShelfPutApplyRelate::getShelfId).collect(Collectors.toList()));
        Map<Integer, List<DisplayShelf>> collect = displayShelves.stream().collect(Collectors.groupingBy(DisplayShelf::getType));
        for (SignShelfRequest.Shelf shelf : request.getShelfList()) {
            List<DisplayShelf> shelves = collect.get(shelf.getType());
            if (CollectionUtils.isNotEmpty(shelves)) {
                if (shelf.getCount() > shelves.size()) {
                    throw new NormalOptionException(Constants.API_CODE_FAIL, "签收失败，" + DisplayShelfTypeEnum.getByType(shelf.getType()).getDesc() + "已投放" + shelves.size() + "个");
                }
                for (int i = 0; i < shelf.getCount(); i++) {
                    shelves.get(i).setPutStatus(PutStatus.DO_PUT.getStatus());
                    displayShelfService.updateById(shelves.get(i));
                }
            } else {
                throw new NormalOptionException(Constants.API_CODE_FAIL, "签收失败，门店未投放" + DisplayShelfTypeEnum.getByType(shelf.getType()).getDesc());
            }
        }
        shelfPutApply.setSignStatus(StoreSignStatus.ALREADY_SIGN.getStatus()).setUpdateTime(new Date());
        this.updateById(shelfPutApply);
        DisplayShelfPutReport putReport = putReportService.getOne(Wrappers.<DisplayShelfPutReport>lambdaQuery().eq(DisplayShelfPutReport::getApplyNumber, shelfPutApply.getApplyNumber()));
        putReport.setSignTime(new Date());
        putReportService.updateById(putReport);
    }

    @Override
    public List<DisplayShelfPutApplyVo> putList(String customerNumber) {
        List<DisplayShelfPutApply> list = this.list(Wrappers.<DisplayShelfPutApply>lambdaQuery().eq(DisplayShelfPutApply::getPutCustomerNumber, customerNumber).eq(DisplayShelfPutApply::getSignStatus, StoreSignStatus.ALREADY_SIGN.getStatus()));
        return list.stream().map(o -> {
            DisplayShelfPutApplyVo vo = new DisplayShelfPutApplyVo();
            vo.setApplyNumber(o.getApplyNumber());
            vo.setCreateTime(o.getCreatedTime());
            List<DisplayShelfPutApplyRelate> relates = shelfPutApplyRelateService.list(Wrappers.<DisplayShelfPutApplyRelate>lambdaQuery().eq(DisplayShelfPutApplyRelate::getApplyNumber, o.getApplyNumber()));
            Collection<DisplayShelf> displayShelves = displayShelfService.listByIds(relates.stream().map(DisplayShelfPutApplyRelate::getShelfId).collect(Collectors.toList()));
            Map<Integer, List<DisplayShelf>> collect = displayShelves.stream().collect(Collectors.groupingBy(DisplayShelf::getType));
            List<SupplierDisplayShelfVO> shelfList = collect.entrySet().stream().map(e -> {
                SupplierDisplayShelfVO supplierDisplayShelfVO = new SupplierDisplayShelfVO();
                supplierDisplayShelfVO.setCount(e.getValue().size());
                supplierDisplayShelfVO.setType(e.getKey());
                supplierDisplayShelfVO.setName(DisplayShelfTypeEnum.getByType(e.getKey()).getDesc());
                return supplierDisplayShelfVO;
            }).collect(Collectors.toList());
            vo.setShelfList(shelfList);
            vo.setCustomerNumber(customerNumber);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<DisplayShelfPutApplyVo> processing(String customerNumber) {
        List<DisplayShelfPutApply> list = this.list(Wrappers.<DisplayShelfPutApply>lambdaQuery().eq(DisplayShelfPutApply::getPutCustomerNumber, customerNumber).eq(DisplayShelfPutApply::getSignStatus, StoreSignStatus.DEFAULT_SIGN.getStatus()));
        return list.stream().map(o -> {
            DisplayShelfPutApplyVo vo = new DisplayShelfPutApplyVo();
            vo.setApplyNumber(o.getApplyNumber());
            vo.setCreateTime(o.getCreatedTime());
            List<DisplayShelfPutApplyRelate> relates = shelfPutApplyRelateService.list(Wrappers.<DisplayShelfPutApplyRelate>lambdaQuery().eq(DisplayShelfPutApplyRelate::getApplyNumber, o.getApplyNumber()));
            Collection<DisplayShelf> displayShelves = displayShelfService.listByIds(relates.stream().map(DisplayShelfPutApplyRelate::getShelfId).collect(Collectors.toList()));
            Map<Integer, List<DisplayShelf>> collect = displayShelves.stream().collect(Collectors.groupingBy(DisplayShelf::getType));
            List<SupplierDisplayShelfVO> shelfList = collect.entrySet().stream().map(e -> {
                SupplierDisplayShelfVO supplierDisplayShelfVO = new SupplierDisplayShelfVO();
                supplierDisplayShelfVO.setCount(e.getValue().size());
                supplierDisplayShelfVO.setType(e.getKey());
                supplierDisplayShelfVO.setName(DisplayShelfTypeEnum.getByType(e.getKey()).getDesc());
                DisplayShelf displayShelf = e.getValue().get(0);
                SupplierInfoSessionVo supplier = FeignResponseUtil.getFeignData(feignSupplierClient.getSuppliserInfoByNumber(displayShelf.getSupplierNumber()));
                supplierDisplayShelfVO.setLinkMobile(supplier.getLinkManMobile());
                supplierDisplayShelfVO.setLinkMan(supplier.getLinkMan());
                supplierDisplayShelfVO.setLinkAddress(supplier.getAddress());
                supplierDisplayShelfVO.setVisitTypeName(VisitCycleEnum.getDescByCode(FeignResponseUtil.getFeignData(feignIceboxQueryClient.selectVisitTypeForReport(customerNumber))));
                return supplierDisplayShelfVO;
            }).collect(Collectors.toList());
            vo.setShelfList(shelfList);
            List<SessionExamineVo.VisitExamineNodeVo> examineNodeVos = FeignResponseUtil.getFeignData(feignExamineClient.getExamineNodesByRelateCode(o.getApplyNumber()));
            vo.setVisitExamineNodes(examineNodeVos);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class, transactionManager = "transactionManager")
    public void invalid(InvalidShelfApplyRequest request) {
        List<DisplayShelfPutApplyRelate> relates = shelfPutApplyRelateService.list(Wrappers.<DisplayShelfPutApplyRelate>lambdaQuery().eq(DisplayShelfPutApplyRelate::getApplyNumber, request.getApplyNumber()));
        if(CollectionUtils.isEmpty(relates)){
            throw new NormalOptionException(Constants.API_CODE_FAIL, "本次申请不包含陈列架");
        }
        Collection<DisplayShelf> displayShelves = displayShelfService.listByIds(relates.stream().map(DisplayShelfPutApplyRelate::getShelfId).collect(Collectors.toList()));
        displayShelves.forEach(shelf -> {
            shelf.setPutStatus(PutStatus.NO_PUT.getStatus());
            shelf.setPutNumber(null);
            shelf.setPutName(null);
        });
        displayShelfService.updateBatchById(displayShelves);
        this.update(Wrappers.<DisplayShelfPutApply>lambdaUpdate().eq(DisplayShelfPutApply::getApplyNumber, request.getApplyNumber()).set(DisplayShelfPutApply::getExamineStatus, 0).set(DisplayShelfPutApply::getRemark, request.getRemark()));
        SessionVisitExamineBacklog log = new SessionVisitExamineBacklog();
        log.setCode(request.getApplyNumber());
        feignBacklogClient.deleteBacklogByCode(log);
        List<SessionExamineVo.VisitExamineNodeVo> examineNodeVos = FeignResponseUtil.getFeignData(feignExamineClient.getExamineNodesByRelateCode(request.getApplyNumber()));
        for (SessionExamineVo.VisitExamineNodeVo nodeVo : examineNodeVos) {
            if (ExamineNodeStatusEnum.IS_PASS.getStatus().equals(nodeVo.getExamineStatus())) {
                SessionVisitExamineBacklog backlog = new SessionVisitExamineBacklog();
                backlog.setBacklogName(request.getUserName() + "作废陈列架申请通知信息");
                backlog.setCode(request.getApplyNumber());
                backlog.setExamineId(nodeVo.getExamineId());
                backlog.setExamineStatus(nodeVo.getExamineStatus());
                backlog.setExamineType(ExamineTypeEnum.ICEBOX_PUT.getType());
                backlog.setSendType(1);
                backlog.setSendUserId(nodeVo.getUserId());
                backlog.setCreateBy(request.getUserId());
                feignBacklogClient.createBacklog(backlog);
            }
        }
        DisplayShelfPutReport putReport = putReportService.getOne(Wrappers.<DisplayShelfPutReport>lambdaQuery().eq(DisplayShelfPutReport::getApplyNumber, request.getApplyNumber()));
        putReport.setExamineTime(new Date());
        putReport.setExamineUserId(request.getUserId());
        SimpleUserInfoVo userInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findUserById(request.getUserId()));
        putReport.setExamineUserName(userInfoVo.getRealname());
        putReport.setExamineUserPosion(userInfoVo.getPosion());
        putReport.setPutStatus(PutStatus.IS_CANCEL.getStatus());
        putReportService.updateById(putReport);
    }

    @Override
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public List<SessionExamineVo.VisitExamineNodeVo> shelfPut(ShelfPutModel model) {
        String applyNumber = "PUT" + IdUtil.simpleUUID().substring(0, 29);
        model.setApplyNumber(applyNumber);
        Integer customerType;
        StoreInfoDtoVo store = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(model.getCustomerNumber()));
        if(Objects.nonNull(store)){
            customerType = 5;
        }else{
            SupplierInfoSessionVo supplier = FeignResponseUtil.getFeignData(feignSupplierClient.getSuppliserInfoByNumber(model.getCustomerNumber()));
            customerType = supplier.getSupplierType();
        }
        model.setCustomerType(customerType);
        DisplayShelfPutApply displayShelfPutApply = DisplayShelfPutApply.builder()
                .applyNumber(applyNumber)
                .putCustomerNumber(model.getCustomerNumber())
                .putCustomerType(customerType)
                .userId(model.getCreateBy())
                .createdBy(model.getCreateBy())
                .remark(model.getRemark())
                .deptId(model.getMarketAreaId())
                .build();
        this.save(displayShelfPutApply);
        for (ShelfPutModel.ShelfModel shelfModel : model.getShelfModelList()) {
            List<DisplayShelf> shelves = displayShelfService.list(
                    Wrappers.<DisplayShelf>lambdaQuery()
                            .eq(DisplayShelf::getSupplierNumber, model.getSupplierNumber())
                            .eq(DisplayShelf::getType, shelfModel.getShelfType())
                            .eq(DisplayShelf::getStatus, IceBoxEnums.StatusEnum.NORMAL.getType())
                            .eq(DisplayShelf::getPutStatus, PutStatus.NO_PUT.getStatus())
                            .last("limit " + shelfModel.getApplyCount())
            );
            if (CollectionUtils.isNotEmpty(shelves) && shelves.size() == shelfModel.getApplyCount()) {
                for (DisplayShelf displayShelf : shelves) {
                    displayShelf.setPutStatus(PutStatus.LOCK_PUT.getStatus());
                    displayShelf.setPutNumber(model.getCustomerNumber());
                    displayShelf.setCustomerType(customerType);
                    displayShelf.setPutName(model.getCustomerName());
                    displayShelf.setUpdateTime(new Date());
                    DisplayShelfPutApplyRelate displayShelfPutApplyRelate = new DisplayShelfPutApplyRelate();
                    displayShelfPutApplyRelate.setShelfId(displayShelf.getId()).setApplyNumber(applyNumber);
                    shelfPutApplyRelateService.save(displayShelfPutApplyRelate);
                    displayShelfService.updateById(displayShelf);
                }
            } else {
                throw new NormalOptionException(Constants.API_CODE_FAIL, "经销商可投放货架不足");
            }
        }
        SessionExamineVo sessionExamineVo = FeignResponseUtil.getFeignData(feignExamineClient.createShelfPut(model));
        List<SessionExamineVo.VisitExamineNodeVo> visitExamineNodes = sessionExamineVo.getVisitExamineNodes();
        if (CollectionUtils.isNotEmpty(visitExamineNodes)) {
            displayShelfPutApply.setExamineId(visitExamineNodes.get(0).getExamineId());
            this.updateById(displayShelfPutApply);
        }
        CompletableFuture.runAsync(()->{
            putReportService.build(model);
        });
        return visitExamineNodes;
    }
}
