package com.szeastroc.icebox.newprocess.service.impl;

import cn.hutool.core.util.IdUtil;
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
import com.szeastroc.common.entity.visit.NoticeBacklogRequestVo;
import com.szeastroc.common.entity.visit.SessionExamineVo;
import com.szeastroc.common.entity.visit.ShelfBackModel;
import com.szeastroc.common.entity.visit.enums.NoticeTypeEnum;
import com.szeastroc.common.exception.NormalOptionException;
import com.szeastroc.common.feign.customer.FeignStoreClient;
import com.szeastroc.common.feign.customer.FeignSupplierClient;
import com.szeastroc.common.feign.user.FeignDeptRuleClient;
import com.szeastroc.common.feign.user.FeignUserClient;
import com.szeastroc.common.feign.visit.FeignExamineClient;
import com.szeastroc.common.feign.visit.FeignOutBacklogClient;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.icebox.enums.ExamineStatusEnum;
import com.szeastroc.icebox.newprocess.dao.DisplayShelfBackApplyDao;
import com.szeastroc.icebox.newprocess.dao.ShelfBackDao;
import com.szeastroc.icebox.newprocess.entity.*;
import com.szeastroc.icebox.newprocess.enums.BackEnum;
import com.szeastroc.icebox.newprocess.enums.IceBoxEnums;
import com.szeastroc.icebox.newprocess.enums.PutStatus;
import com.szeastroc.icebox.newprocess.enums.StoreSignStatus;
import com.szeastroc.icebox.newprocess.service.*;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class DisplayShelfBackApplyServiceImpl extends ServiceImpl<DisplayShelfBackApplyDao, DisplayShelfBackApply> implements DisplayShelfBackApplyService {

    @Autowired
    private FeignStoreClient feignStoreClient;
    @Autowired
    private FeignSupplierClient feignSupplierClient;
    @Autowired
    private DisplayShelfService displayShelfService;
    @Autowired
    private FeignUserClient feignUserClient;
    @Autowired
    private FeignDeptRuleClient feignDeptRuleClient;
    @Autowired
    private FeignExamineClient feignExamineClient;
    @Autowired
    private DisplayShelfBackReportService backReportService;
    @Autowired
    private DisplayShelfPutApplyRelateService shelfPutApplyRelateService;
    @Autowired
    private FeignOutBacklogClient feignOutBacklogClient;
    @Autowired
    private ShelfBackDao shelfBackDao;
    @Autowired
    private DisplayShelfBackApplyService displayShelfBackApplyService;
    @Autowired
    private DisplayShelfPutApplyService displayShelfPutApplyService;


    @Override
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public List<SessionExamineVo.VisitExamineNodeVo> shelfBack(ShelfBackModel model) {
//        String applyNumber = "BACK" + IdUtil.simpleUUID().substring(0, 29);
        model.setApplyNumber(model.getApplyNumber());

        Integer customerType;
        StoreInfoDtoVo store = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(model.getCustomerNumber()));
        if (Objects.nonNull(store)) {
            customerType = 5;
        } else {
            SupplierInfoSessionVo supplier = FeignResponseUtil.getFeignData(feignSupplierClient.getSuppliserInfoByNumber(model.getCustomerNumber()));
            customerType = supplier.getSupplierType();
        }
        model.setCustomerType(customerType);

        DisplayShelfBackApply displayShelfBackApply = DisplayShelfBackApply.builder()
                .applyNumber(model.getApplyNumber())
                .putCustomerNumber(model.getCustomerNumber())
                .putCustomerType(customerType)
                .userId(model.getCreateBy())
                .createdBy(model.getCreateBy())
                .remark(model.getRemark())
                .deptId(model.getMarketAreaId())
                .backStatus(0)  //0????????? 1?????????  ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????"?????????"
                .build();
        this.save(displayShelfBackApply);
        //??????????????????????????????
        for (ShelfBackModel.ShelfModel shelfModel : model.getShelfModelList()){
            //??????????????????????????????????????????????????????????????????????????????
            List<DisplayShelf> shelves = displayShelfService.list(
                    Wrappers.<DisplayShelf>lambdaQuery()
                            .eq(DisplayShelf::getServiceDeptId, model.getServiceDeptId())
                            .eq(DisplayShelf::getType, shelfModel.getShelfType())
                            .eq(DisplayShelf::getPutNumber,model.getCustomerNumber())
                            .eq(DisplayShelf::getStatus, IceBoxEnums.StatusEnum.NORMAL.getType())
                            .eq(DisplayShelf::getPutStatus, PutStatus.DO_BACK.getStatus())
                            .eq(DisplayShelf::getSize,shelfModel.getSize())
                            .last("limit " + shelfModel.getApplyCount())
            );
            if (CollectionUtils.isNotEmpty(shelves) && shelves.size() == shelfModel.getApplyCount()) {
                for (DisplayShelf displayShelf : shelves) {
                    displayShelf.setPutStatus(PutStatus.DO_BACK.getStatus());
                    displayShelf.setUpdateTime(new Date());
                    displayShelfService.updateById(displayShelf);
                }
            } else {
                throw new NormalOptionException(Constants.API_CODE_FAIL, "?????????????????????????????????????????????????????????");
            }
        }
        SessionExamineVo sessionExamineVo = FeignResponseUtil.getFeignData(feignExamineClient.createShelfBack(model));
        List<SessionExamineVo.VisitExamineNodeVo> visitExamineNodes = sessionExamineVo.getVisitExamineNodes();
        if (CollectionUtils.isNotEmpty(visitExamineNodes)) {
            displayShelfBackApply.setExamineId(visitExamineNodes.get(0).getExamineId());
            this.updateById(displayShelfBackApply);
        }
        //????????????????????????????????????
       /* CompletableFuture.runAsync(()->{
            backReportService.buildBackReport(model);
        });*/
        return visitExamineNodes;
    }



    @Override
    @Transactional(rollbackFor = Exception.class, value = "transactionManager")
    public void updateBackStatus(IceBoxRequest request) {
        DisplayShelfBackApply displayShelfBackApply = this.getOne(Wrappers.<DisplayShelfBackApply>lambdaQuery().eq(DisplayShelfBackApply::getApplyNumber, request.getApplyNumber()));
        Integer updateBy = request.getUpdateBy();

        Optional.ofNullable(displayShelfBackApply).ifPresent(o -> {
            displayShelfBackApply.setExamineStatus(request.getExamineStatus());
            displayShelfBackApply.setUpdatedBy(updateBy);
            displayShelfBackApply.setUpdateTime(new Date());
            this.updateById(displayShelfBackApply);
        });

        if (IceBoxStatus.NO_PUT.getStatus().equals(request.getStatus())) {
            Optional.ofNullable(displayShelfBackApply).ifPresent(o -> {
                    List<ShelfBack> shelfs = shelfBackDao.selectList(Wrappers.<ShelfBack>lambdaQuery()
                            .eq(ShelfBack::getUuid, request.getApplyNumber())
                            .eq(ShelfBack::getSignStatus, 0));
                    List<Integer> collect = shelfs.stream().map(ShelfBack::getShelfId).collect(Collectors.toList());
                    //???????????????????????????????????????
                    List<DisplayShelf> displayShelfList = displayShelfService.list(Wrappers.<DisplayShelf>lambdaQuery()
                            .eq(DisplayShelf::getPutStatus, PutStatus.DO_BACK.getStatus())
                            .in(DisplayShelf::getId,collect));
                for (DisplayShelf displayShelf : displayShelfList) {
                    displayShelfService.update(Wrappers.<DisplayShelf>lambdaUpdate()
                            .eq(DisplayShelf::getId,displayShelf.getId())
                            .set(DisplayShelf::getPutStatus,PutStatus.NO_PUT.getStatus())
                            .set(DisplayShelf::getPutNumber,"")
                            .set(DisplayShelf::getPutName,"")
                            .set(DisplayShelf::getCustomerType,null)
                            .set(DisplayShelf::getResponseManId,null)
                            .set(DisplayShelf::getPutStatus,PutStatus.NO_PUT.getStatus())
                            .set(DisplayShelf::getSignStatus,StoreSignStatus.DEFAULT_SIGN.getStatus()));
                }



                    /*DisplayShelfPutApply applyServiceOne = displayShelfPutApplyService.getOne(Wrappers.<DisplayShelfPutApply>lambdaQuery().eq(DisplayShelfPutApply::getPutCustomerNumber, customerNumber));
                    //?????????
                    applyServiceOne.setSignStatus(3);
                    displayShelfPutApplyService.updateById(applyServiceOne);*/



                    /*List<ShelfBack> shelfBacks = shelfBackDao.selectList(Wrappers.<ShelfBack>lambdaQuery().in(ShelfBack::getCustomerNumber, applyServiceOne.getPutCustomerNumber()));
                    shelfBacks.forEach(shelfBack -> {
                        shelfBackDao.update(shelfBack,Wrappers.<ShelfBack>lambdaUpdate()
                                .set(ShelfBack::getSignStatus,StoreSignStatus.ALREADY_SIGN.getStatus())
                                .eq(ShelfBack::getSignStatus,StoreSignStatus.DEFAULT_SIGN.getStatus())
                                .eq(ShelfBack::getCustomerNumber,applyServiceOne.getPutCustomerNumber()));
                    });*/


            });
        }else if(IceBoxStatus.IS_PUTED.getStatus().equals(request.getStatus())){
            List<ShelfBack> shelfs = shelfBackDao.selectList(Wrappers.<ShelfBack>lambdaQuery()
                    .eq(ShelfBack::getUuid, request.getApplyNumber())
                    .eq(ShelfBack::getSignStatus, 0));
            List<Integer> collect = shelfs.stream().map(ShelfBack::getShelfId).collect(Collectors.toList());


            List<DisplayShelf> displayShelfList = displayShelfService.list(Wrappers.<DisplayShelf>lambdaQuery()
                    .eq(DisplayShelf::getPutStatus, PutStatus.DO_BACK.getStatus())
                    .in(DisplayShelf::getId,collect));

            for (DisplayShelf displayShelf : displayShelfList) {
                displayShelfService.update(Wrappers.<DisplayShelf>lambdaUpdate().eq(DisplayShelf::getId,displayShelf.getId())
                        .set(DisplayShelf::getPutStatus,PutStatus.FINISH_PUT.getStatus())
                        .set(DisplayShelf::getSignStatus,StoreSignStatus.ALREADY_SIGN.getStatus()));
            }

            /*displayShelfList.forEach(displayShelf -> {
                displayShelf.setPutStatus(PutStatus.FINISH_PUT.getStatus());
                displayShelf.setSignStatus(StoreSignStatus.ALREADY_SIGN.getStatus());
            });
            displayShelfService.updateBatchById(displayShelfList);*/
        }

        /*//??????
        DisplayShelfBackReport backReport = backReportService.getOne(Wrappers.<DisplayShelfBackReport>lambdaQuery()
                .eq(DisplayShelfBackReport::getApplyNumber, request.getApplyNumber()));
        backReport.setCheckDate(new Date());
        backReport.setReason(request.getExamineRemark());
        backReport.setCheckPersonId(updateBy);
        SimpleUserInfoVo userInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findUserById(updateBy));
        backReport.setCheckPerson(userInfoVo.getRealname());
        backReport.setCheckOfficeName(userInfoVo.getPosion());
        if(request.getExamineStatus().equals(ExamineStatusEnum.IS_DEFAULT.getStatus())){
            backReport.setExamineStatus(BackEnum.IS_DEFAULT.getStatus());
        }else if(request.getExamineStatus().equals(ExamineStatusEnum.IS_PASS.getStatus())){
            backReport.setExamineStatus(BackEnum.WAIT_ORDER.getStatus());
        }else if(request.getExamineStatus().equals(ExamineStatusEnum.UN_PASS.getStatus())){
            backReport.setExamineStatus(BackEnum.UN_PASS.getStatus());
        }*/
    }

    @Override
    @Transactional(rollbackFor = Exception.class, value = "transactionManager")
    public void shelfBacklog(ShelfBackModel model) {
        String uuid = UUID.randomUUID().toString();
        StoreInfoDtoVo store = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(model.getCustomerNumber()));
        for (ShelfBackModel.ShelfModel shelfModel : model.getShelfModelList()) {
            List<DisplayShelf> displayShelves = displayShelfService.list(Wrappers.<DisplayShelf>lambdaQuery()
                    .eq(DisplayShelf::getPutNumber, model.getCustomerNumber())
                    .eq(DisplayShelf::getSize,shelfModel.getSize())
                    .eq(DisplayShelf::getType,shelfModel.getShelfType())
                    .eq(DisplayShelf::getPutStatus,PutStatus.FINISH_PUT.getStatus())
                    .eq(DisplayShelf::getStatus,1)
                    .last("limit " + shelfModel.getApplyCount()));
            for (DisplayShelf displayShelf : displayShelves) {
                ShelfBack shelfBack = ShelfBack.builder()
                        .shelfId(displayShelf.getId())
                        .uuid(uuid)
                        .customerNumber(model.getCustomerNumber())
                        .customerName(store.getStoreName())
                        .shelfType(shelfModel.getShelfType())
                        .shelfName(shelfModel.getShelfName())
                        .shelfSize(shelfModel.getSize())
                        .applyCount(shelfModel.getApplyCount())
                        .marketArea(store.getMarketArea())
                        .serviceDeptId(store.getServiceDeptId())
                        .serviceDeptName(store.getServiceDeptName())
                        .backTime(DateTime.now().toDate())
                        .signStatus(0)
                        .build();
                shelfBackDao.insert(shelfBack);
                displayShelf.setPutStatus(PutStatus.DO_BACK.getStatus());
                displayShelf.setSignStatus(StoreSignStatus.DEFAULT_SIGN.getStatus());
                displayShelfService.updateById(displayShelf);
            }
        }

        // ????????????
        DateTime date = new DateTime();
        String prefix = date.toString("yyyyMMddHHmmss");
//        String relateCode = prefix + "_" + model.getCustomerNumber();
        String backlogName = "??????????????????????????????";
        NoticeBacklogRequestVo noticeBacklogRequestVo = NoticeBacklogRequestVo.builder()
                .backlogName(backlogName)
                .noticeTypeEnum(NoticeTypeEnum.SHELF_BACK_MSG)
                .relateCode(uuid)
                .sendUserId(store.getMainSaleManId())//???????????????
                .build();
        // ????????????
        feignOutBacklogClient.createNoticeBacklog(noticeBacklogRequestVo);
    }

    @Override
    public List<ShelfBack> shelfBackDetails(String uuid) {
        List<ShelfBack> shelfBacks = shelfBackDao.selectList(Wrappers.<ShelfBack>lambdaQuery()
                .eq(ShelfBack::getUuid, uuid)
                .eq(ShelfBack::getSignStatus, 0)
                .groupBy(ShelfBack::getShelfType,ShelfBack::getShelfSize));
        return shelfBacks;
    }

}
