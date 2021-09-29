package com.szeastroc.icebox.newprocess.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
import com.szeastroc.icebox.newprocess.dao.ShelfSignDao;
import com.szeastroc.icebox.newprocess.entity.*;
import com.szeastroc.icebox.newprocess.enums.*;
import com.szeastroc.icebox.newprocess.service.DisplayShelfPutApplyRelateService;
import com.szeastroc.icebox.newprocess.service.DisplayShelfPutApplyService;
import com.szeastroc.icebox.newprocess.service.DisplayShelfPutReportService;
import com.szeastroc.icebox.newprocess.service.DisplayShelfService;
import com.szeastroc.icebox.newprocess.vo.DisplayShelfPutApplyVo;
import com.szeastroc.icebox.newprocess.vo.SupplierDisplayShelfVO;
import com.szeastroc.icebox.newprocess.vo.request.InvalidShelfApplyRequest;
import com.szeastroc.icebox.newprocess.vo.request.SignShelfRequest;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
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
    @Autowired
    private ShelfSignDao shelfSignDao;
    @Autowired
    private DisplayShelfPutApplyService shelfPutApplyService;

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
                        displayShelfPutApply.setPutStatus(PutStatus.FINISH_PUT.getStatus());
                        displayShelfPutApply.setUpdateTime(new Date());
                        this.updateById(displayShelfPutApply);
                        List<ShelfSign> shelfSigns = shelfSignDao.selectList(Wrappers.<ShelfSign>lambdaQuery().eq(ShelfSign::getApplyNumber, displayShelfPutApply.getApplyNumber()));
                        shelfSigns.forEach(shelf -> {
                            shelfSignDao.update(shelf, Wrappers.<ShelfSign>lambdaUpdate()
                                    .eq(ShelfSign::getApplyNumber,displayShelfPutApply.getApplyNumber())
                                    .set(ShelfSign::getSignStatus,StoreSignStatus.ALREADY_SIGN.getStatus()));
                        });
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
        }else if(IceBoxStatus.NO_PUT.getStatus().equals(request.getStatus())){
            List<DisplayShelfPutApplyRelate> relates = shelfPutApplyRelateService.list(Wrappers.<DisplayShelfPutApplyRelate>lambdaQuery().eq(DisplayShelfPutApplyRelate::getApplyNumber, request.getApplyNumber()));
            List<Integer> collect = relates.stream().map(DisplayShelfPutApplyRelate::getShelfId).collect(Collectors.toList());
            Collection<DisplayShelf> displayShelves = displayShelfService.listByIds(collect);

            DisplayShelfPutApplyRelate displayShelfPutApplyRelateOne = shelfPutApplyRelateService.getOne(Wrappers.<DisplayShelfPutApplyRelate>lambdaQuery()
                    .in(DisplayShelfPutApplyRelate::getShelfId, collect).orderByDesc(DisplayShelfPutApplyRelate::getUpdateTime)
                    .last("limit 1"));

            shelfPutApplyService.update(Wrappers.<DisplayShelfPutApply>lambdaUpdate()
                    .eq(DisplayShelfPutApply::getApplyNumber, displayShelfPutApplyRelateOne.getApplyNumber())
                    .set(DisplayShelfPutApply::getPutStatus,PutStatus.NO_PASS.getStatus()));

            displayShelves.forEach(shelf -> {
                shelf.setPutStatus(PutStatus.NO_PUT.getStatus());
                shelf.setPutNumber("");
                shelf.setCustomerType(null);
                shelf.setPutName("");
                shelf.setResponseManId(null);
                shelf.setResponseMan("");
            });
            displayShelfService.updateBatchById(displayShelves);
        }
       /* //报表
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
        putReportService.updateById(putReport);*/
    }

    @Override
    @Transactional(rollbackFor = Exception.class, value = "transactionManager")
    public void sign(SignShelfRequest request) {
        DisplayShelfPutApply shelfPutApply = this.getOne(Wrappers.<DisplayShelfPutApply>lambdaQuery()
                .eq(DisplayShelfPutApply::getPutCustomerNumber, request.getCustomerNumber())
                .eq(DisplayShelfPutApply::getApplyNumber,request.getApplyNumber()));
        if (Objects.isNull(shelfPutApply)) {
            throw new NormalOptionException(Constants.API_CODE_FAIL, "门店暂无可签收货架");
        }
        List<DisplayShelfPutApplyRelate> relates = shelfPutApplyRelateService.list(Wrappers.<DisplayShelfPutApplyRelate>lambdaQuery().eq(DisplayShelfPutApplyRelate::getApplyNumber, request.getApplyNumber()));
        Collection<DisplayShelf> displayShelves = displayShelfService.listByIds(relates.stream().map(DisplayShelfPutApplyRelate::getShelfId).collect(Collectors.toList()));
        Map<String, List<DisplayShelf>> collect = displayShelves.stream().collect(Collectors.groupingBy(groups -> groups.getType()+"_"+groups.getSize()));
        for (SignShelfRequest.Shelf shelf : request.getShelfList()) {
            List<DisplayShelf> shelves = collect.get(shelf.getType()+ "_" + shelf.getSize());
            if (CollectionUtils.isNotEmpty(shelves)) {
                if (shelf.getCount() > shelves.size()) {
                    throw new NormalOptionException(Constants.API_CODE_FAIL, "签收失败，" + DisplayShelfTypeEnum.getByType(shelf.getType()).getDesc() + "只投放" + shelves.size() + "个");
                }
                int count = 0;
                /*for (DisplayShelf displayShelf : shelves) {
                    if(displayShelf.getSignStatus() == 1){
                        count = count +1;
                        continue;
                    }
                    for (int i = count; i < shelf.getCount() + count; i++) {
                        shelves.get(i).setPutStatus(PutStatus.FINISH_PUT.getStatus());
                        shelves.get(i).setSignStatus(StoreSignStatus.ALREADY_SIGN.getStatus());
                        ShelfSign shelfSign = new ShelfSign();
                        shelfSignDao.update(shelfSign,new LambdaUpdateWrapper<ShelfSign>().eq(ShelfSign::getShelfId, shelves.get(i).getId())
                                .set(ShelfSign::getSignStatus,StoreSignStatus.ALREADY_SIGN.getStatus()));
                        displayShelfService.updateById(shelves.get(i));
                    }
                }*/

                for (DisplayShelf displayShelf : shelves) {
                    if(displayShelf.getSignStatus() == 1){
//                        count = count +1;
                        continue;
                    }
//                    for (int i = count; i < shelf.getCount() + count; i++) {
                    displayShelf.setPutStatus(PutStatus.FINISH_PUT.getStatus());
                    displayShelf.setSignStatus(StoreSignStatus.ALREADY_SIGN.getStatus());
                    ShelfSign shelfSign = new ShelfSign();
                    shelfSignDao.update(shelfSign,new LambdaUpdateWrapper<ShelfSign>().eq(ShelfSign::getShelfId, displayShelf.getId())
                            .set(ShelfSign::getSignStatus,StoreSignStatus.ALREADY_SIGN.getStatus()));
                    displayShelfService.updateById(displayShelf);
//                    }
                }
            } else {
                throw new NormalOptionException(Constants.API_CODE_FAIL, "签收失败，门店未投放" + DisplayShelfTypeEnum.getByType(shelf.getType()).getDesc());
            }
        }
//        List<ShelfSign> shelfSigns = shelfSignDao.selectList(Wrappers.<ShelfSign>lambdaQuery().eq(ShelfSign::getApplyNumber, request.getApplyNumber()).eq(ShelfSign::getSignStatus, StoreSignStatus.DEFAULT_SIGN.getStatus()));
//        if(CollectionUtils.isEmpty(shelfSigns)){
            shelfPutApply.setSignStatus(StoreSignStatus.ALREADY_SIGN.getStatus()).setUpdateTime(new Date());
//        }
        this.updateById(shelfPutApply);
        /*DisplayShelfPutReport putReport = putReportService.getOne(Wrappers.<DisplayShelfPutReport>lambdaQuery().eq(DisplayShelfPutReport::getApplyNumber, shelfPutApply.getApplyNumber()));
        putReport.setSignTime(new Date());
        putReportService.updateById(putReport);*/
    }

    @Override
    public List<SupplierDisplayShelfVO> putList(String customerNumber) {
        List<DisplayShelf> list = displayShelfService.list(Wrappers.<DisplayShelf>lambdaQuery().eq(DisplayShelf::getPutNumber, customerNumber));
        List<SupplierDisplayShelfVO> shelfList = new ArrayList<>();
            DisplayShelfPutApplyVo vo = new DisplayShelfPutApplyVo();

            //获取已经签收的投放陈列架的id
            List<Integer> collect = list.stream().map(DisplayShelf::getId).collect(Collectors.toList());
            if(CollectionUtils.isEmpty(collect)){
                return shelfList;
            }
            List<DisplayShelf> displayShelves = displayShelfService.list(Wrappers.<DisplayShelf>lambdaQuery().eq(DisplayShelf::getSignStatus,StoreSignStatus.ALREADY_SIGN.getStatus()).in(DisplayShelf::getId, collect));
            //根据类型进行分组
            Map<String, List<DisplayShelf>> collect1 = displayShelves.stream().collect(Collectors.groupingBy(groups -> groups.getType()+"_"+groups.getSize()));
            shelfList = collect1.entrySet().stream().map(e -> {
                SupplierDisplayShelfVO supplierDisplayShelfVO = new SupplierDisplayShelfVO();
                supplierDisplayShelfVO.setCount(e.getValue().size());
                supplierDisplayShelfVO.setType(e.getValue().get(0).getType());
                supplierDisplayShelfVO.setName(e.getValue().get(0).getName());
                supplierDisplayShelfVO.setSize(e.getValue().get(0).getSize());
                supplierDisplayShelfVO.setServiceDeptId(e.getValue().get(0).getServiceDeptId());
                supplierDisplayShelfVO.setServiceDeptName(e.getValue().get(0).getServiceDeptName());
                return supplierDisplayShelfVO;
            }).collect(Collectors.toList());

           return shelfList;

    }

    @Override
    public List<DisplayShelfPutApplyVo> processing(String customerNumber) {
        List<DisplayShelfPutApply> list = this.list(Wrappers.<DisplayShelfPutApply>lambdaQuery()
                .eq(DisplayShelfPutApply::getPutCustomerNumber, customerNumber)
                .eq(DisplayShelfPutApply::getSignStatus, StoreSignStatus.DEFAULT_SIGN.getStatus())
                .in(DisplayShelfPutApply::getPutStatus,1,2));
        return list.stream().map(o -> {
            DisplayShelfPutApplyVo vo = new DisplayShelfPutApplyVo();
            vo.setApplyNumber(o.getApplyNumber());
            vo.setCreateTime(o.getCreatedTime());
            List<DisplayShelfPutApplyRelate> relates = shelfPutApplyRelateService.list(Wrappers.<DisplayShelfPutApplyRelate>lambdaQuery().eq(DisplayShelfPutApplyRelate::getApplyNumber, o.getApplyNumber()));
            List<Integer> collect = relates.stream().map(DisplayShelfPutApplyRelate::getShelfId).collect(Collectors.toList());
            if(CollectionUtils.isEmpty(collect)){
                return null;
            }
            List<DisplayShelf> displayShelves = displayShelfService.list(Wrappers.<DisplayShelf>lambdaQuery().in(DisplayShelf::getId, collect));
            Map<String, List<DisplayShelf>> listMap = displayShelves.stream().collect(Collectors.groupingBy(groups -> groups.getType()+"_"+groups.getSize()));
            List<SupplierDisplayShelfVO> shelfList = listMap.entrySet().stream().map(e -> {

                SupplierDisplayShelfVO supplierDisplayShelfVO = new SupplierDisplayShelfVO();

                supplierDisplayShelfVO.setCount(e.getValue().size());
                supplierDisplayShelfVO.setType(e.getValue().get(0).getType());
                supplierDisplayShelfVO.setName(e.getValue().get(0).getName());
                supplierDisplayShelfVO.setSize(e.getValue().get(0).getSize());
                supplierDisplayShelfVO.setServiceDeptId(e.getValue().get(0).getServiceDeptId());
                supplierDisplayShelfVO.setServiceDeptName(e.getValue().get(0).getServiceDeptName());
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
            shelf.setPutNumber("");
            shelf.setPutName("");
            shelf.setCustomerType(null);
        });
        displayShelfService.updateBatchById(displayShelves);
        this.update(Wrappers.<DisplayShelfPutApply>lambdaUpdate()
                .eq(DisplayShelfPutApply::getApplyNumber, request.getApplyNumber())
                .set(DisplayShelfPutApply::getExamineStatus, 0)
                .set(DisplayShelfPutApply::getRemark, request.getRemark())
                .set(DisplayShelfPutApply::getPutStatus,4));
        SessionVisitExamineBacklog log = new SessionVisitExamineBacklog();
        log.setCode(request.getApplyNumber());
        feignBacklogClient.deleteBacklogByCode(log);

        /*List<SessionExamineVo.VisitExamineNodeVo> examineNodeVos = FeignResponseUtil.getFeignData(feignExamineClient.getExamineNodesByRelateCode(request.getApplyNumber()));
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
        }*/
       /* DisplayShelfPutReport putReport = putReportService.getOne(Wrappers.<DisplayShelfPutReport>lambdaQuery().eq(DisplayShelfPutReport::getApplyNumber, request.getApplyNumber()));
        putReport.setExamineTime(new Date());
        putReport.setExamineUserId(request.getUserId());
        SimpleUserInfoVo userInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findUserById(request.getUserId()));
        putReport.setExamineUserName(userInfoVo.getRealname());
        putReport.setExamineUserPosion(userInfoVo.getPosion());
        putReport.setPutStatus(PutStatus.IS_CANCEL.getStatus());
        putReportService.updateById(putReport);*/
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
                .putStatus(PutStatus.LOCK_PUT.getStatus())
                .deptId(model.getMarketAreaId())
                .build();
        this.save(displayShelfPutApply);

        for (ShelfPutModel.ShelfModel shelfModel : model.getShelfModelList()) {
            List<DisplayShelf> shelves = displayShelfService.list(
                    Wrappers.<DisplayShelf>lambdaQuery()
                            //陈列架只能从服务处发出
                            .eq(DisplayShelf::getServiceDeptId, model.getServiceDeptId())
                            .eq(DisplayShelf::getType, shelfModel.getShelfType())
                            .eq(DisplayShelf::getStatus, IceBoxEnums.StatusEnum.NORMAL.getType())
                            .eq(DisplayShelf::getPutStatus, PutStatus.NO_PUT.getStatus())
                            .eq(DisplayShelf::getSize,shelfModel.getSize())
                            .last("limit " + shelfModel.getApplyCount())
            );
            if (CollectionUtils.isNotEmpty(shelves) && shelves.size() == shelfModel.getApplyCount()) {
                for (DisplayShelf displayShelf : shelves) {
                    displayShelf.setPutStatus(PutStatus.LOCK_PUT.getStatus());
                    displayShelf.setPutNumber(model.getCustomerNumber());
                    displayShelf.setCustomerType(customerType);
                    displayShelf.setPutName(model.getCustomerName());
                    displayShelf.setResponseManId(model.getCreateBy());
                    displayShelf.setResponseMan(model.getCreateByName());
                    displayShelf.setUpdateTime(new Date());
                    DisplayShelfPutApplyRelate displayShelfPutApplyRelate = new DisplayShelfPutApplyRelate();
                    displayShelfPutApplyRelate.setShelfId(displayShelf.getId()).setApplyNumber(applyNumber);
                    shelfPutApplyRelateService.save(displayShelfPutApplyRelate);
                    displayShelfService.updateById(displayShelf);
                }
            } else {
                throw new NormalOptionException(Constants.API_CODE_FAIL, "服务处可投放货架不足");
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

    @Override
    public void shelfSign(String applyNumber) {
        List<DisplayShelfPutApply> list = this.list(Wrappers.<DisplayShelfPutApply>lambdaQuery()
                .eq(DisplayShelfPutApply::getApplyNumber, applyNumber)
                .eq(DisplayShelfPutApply::getSignStatus, StoreSignStatus.DEFAULT_SIGN.getStatus()));
        for (DisplayShelfPutApply putApply : list) {
            List<DisplayShelfPutApplyRelate> relates = shelfPutApplyRelateService.list(Wrappers.<DisplayShelfPutApplyRelate>lambdaQuery()
                    .eq(DisplayShelfPutApplyRelate::getApplyNumber, putApply.getApplyNumber()));
            List<Integer> collect = relates.stream().map(DisplayShelfPutApplyRelate::getShelfId).collect(Collectors.toList());
            List<DisplayShelf> displayShelves = displayShelfService.list(Wrappers.<DisplayShelf>lambdaQuery().in(DisplayShelf::getId, collect));
            for (DisplayShelf displayShelf : displayShelves) {
                ShelfSign shelfSign = ShelfSign.builder()
                        .shelfId(displayShelf.getId())
                        .applyNumber(applyNumber)
                        .shelfType(displayShelf.getType())
                        .shelfName(displayShelf.getName())
                        .shelfSize(displayShelf.getSize())
                        .informName("陈列架签收")
                        .customerNumber(displayShelf.getPutNumber())
                        .customerName(displayShelf.getPutName())
                        .createDate(DateTime.now().toDate())
                        .signStatus(StoreSignStatus.DEFAULT_SIGN.getStatus())
                        .build();
                shelfSignDao.insert(shelfSign);
            }

        }

    }

    @Override
    public List<ShelfSign> signInform(String customerNumber) {
        List<ShelfSign> shelfSignInforms = shelfSignDao.selectList(Wrappers.<ShelfSign>lambdaQuery()
                .eq(ShelfSign::getCustomerNumber, customerNumber)
                .eq(ShelfSign::getSignStatus,StoreSignStatus.DEFAULT_SIGN.getStatus())
                .groupBy(ShelfSign::getApplyNumber, ShelfSign::getApplyNumber));
        return shelfSignInforms;
    }

    @Override
    public List<DisplayShelf.DisplayShelfType> customerTotal(String applyNumber) {
        List<DisplayShelf.DisplayShelfType> typeList = new ArrayList<>();
        //投放一次  就只有一条数据
        DisplayShelfPutApply displayShelfPutApply = this.getOne(Wrappers.<DisplayShelfPutApply>lambdaQuery()
                .eq(DisplayShelfPutApply::getApplyNumber, applyNumber));
                /*.eq(DisplayShelfPutApply::getSignStatus, StoreSignStatus.DEFAULT_SIGN.getStatus())
                .in(DisplayShelfPutApply::getPutStatus,1,2));*/


        if(displayShelfPutApply != null){
                List<DisplayShelfPutApplyRelate> relates = shelfPutApplyRelateService.list(Wrappers.<DisplayShelfPutApplyRelate>lambdaQuery().eq(DisplayShelfPutApplyRelate::getApplyNumber,applyNumber));
                if(CollectionUtils.isNotEmpty(relates)){
                    List<Integer> collect = relates.stream().map(DisplayShelfPutApplyRelate::getShelfId).collect(Collectors.toList());
                    List<DisplayShelf> displayShelves = displayShelfService.list(Wrappers.<DisplayShelf>lambdaQuery().in(DisplayShelf::getId, collect).groupBy(DisplayShelf::getSize,DisplayShelf::getType));
                    List<DisplayShelf> displays = displayShelfService.list(Wrappers.<DisplayShelf>lambdaQuery().in(DisplayShelf::getId, collect));
                    int signCount = 0;
                    int notSignCount = 0;
                    for (DisplayShelf display : displayShelves) {

                        for (DisplayShelf displayShelf : displays) {
                            if(displayShelf.getName().equals(display.getName()) && displayShelf.getSize().equals(display.getSize()) && displayShelf.getSignStatus() == 1){
                                signCount = signCount + 1;
                            }else if(displayShelf.getName().equals(display.getName()) && displayShelf.getSize().equals(display.getSize()) && displayShelf.getSignStatus() == 0){
                                notSignCount = notSignCount + 1;
                            }
                        }
                        DisplayShelf.DisplayShelfType displayShelfType = new DisplayShelf.DisplayShelfType();
                        DisplayShelf.DisplayShelfType displayShelfType1 = new DisplayShelf.DisplayShelfType();
                        if(signCount > 0){
                            displayShelfType.setType(display.getType());
                            displayShelfType.setTypeName(display.getName());
                            displayShelfType.setSize(display.getSize());
                            displayShelfType.setCount(signCount);
                            displayShelfType.setSignStatus(StoreSignStatus.ALREADY_SIGN.getStatus());
                            typeList.add(displayShelfType);
                            signCount = 0;
                        }
                        if(notSignCount > 0){
                            displayShelfType1.setType(display.getType());
                            displayShelfType1.setTypeName(display.getName());
                            displayShelfType1.setSize(display.getSize());
                            displayShelfType1.setCount(notSignCount);
                            displayShelfType1.setSignStatus(StoreSignStatus.DEFAULT_SIGN.getStatus());
                            typeList.add(displayShelfType1);
                            notSignCount = 0;
                        }

                    }
                }
        }
        return typeList;
    }
}
