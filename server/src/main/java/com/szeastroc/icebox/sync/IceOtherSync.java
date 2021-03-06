package com.szeastroc.icebox.sync;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import com.szeastroc.common.entity.customer.vo.SubordinateInfoVo;
import com.szeastroc.common.entity.user.vo.SessionDeptInfoVo;
import com.szeastroc.common.entity.user.vo.SessionUserInfoVo;
import com.szeastroc.common.entity.user.vo.SimpleUserInfoVo;
import com.szeastroc.common.feign.customer.FeignStoreClient;
import com.szeastroc.common.feign.customer.FeignSupplierClient;
import com.szeastroc.common.feign.user.FeignCacheClient;
import com.szeastroc.common.feign.user.FeignDeptClient;
import com.szeastroc.common.feign.user.FeignUserClient;
import com.szeastroc.common.feign.visit.FeignExamineClient;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.icebox.enums.OrderStatus;
import com.szeastroc.icebox.newprocess.dao.IceBoxDao;
import com.szeastroc.icebox.newprocess.entity.IceBackApply;
import com.szeastroc.icebox.newprocess.entity.IceBackApplyRelateBox;
import com.szeastroc.icebox.newprocess.entity.IceBackApplyReport;
import com.szeastroc.icebox.newprocess.entity.IceBackOrder;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.entity.IceBoxExtend;
import com.szeastroc.icebox.newprocess.entity.IceInspectionReport;
import com.szeastroc.icebox.newprocess.entity.IcePutApply;
import com.szeastroc.icebox.newprocess.entity.IcePutOrder;
import com.szeastroc.icebox.newprocess.entity.IcePutPactRecord;
import com.szeastroc.icebox.newprocess.enums.DeptTypeEnum;
import com.szeastroc.icebox.newprocess.enums.PutStatus;
import com.szeastroc.icebox.newprocess.service.IceBackApplyRelateBoxService;
import com.szeastroc.icebox.newprocess.service.IceBackApplyReportService;
import com.szeastroc.icebox.newprocess.service.IceBackApplyService;
import com.szeastroc.icebox.newprocess.service.IceBackOrderService;
import com.szeastroc.icebox.newprocess.service.IceBoxExtendService;
import com.szeastroc.icebox.newprocess.service.IceBoxService;
import com.szeastroc.icebox.newprocess.service.IceExamineService;
import com.szeastroc.icebox.newprocess.service.IceInspectionReportService;
import com.szeastroc.icebox.newprocess.service.IcePutApplyService;
import com.szeastroc.icebox.newprocess.service.IcePutOrderService;
import com.szeastroc.icebox.newprocess.service.IcePutPactRecordService;
import com.szeastroc.icebox.oldprocess.entity.OrderInfo;
import com.szeastroc.icebox.oldprocess.entity.PactRecord;
import com.szeastroc.icebox.oldprocess.entity.WechatTransferOrder;
import com.szeastroc.icebox.oldprocess.service.OrderInfoService;
import com.szeastroc.icebox.oldprocess.service.PactRecordService;
import com.szeastroc.icebox.oldprocess.service.WechatTransferOrderService;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
public class IceOtherSync {

    @Autowired
    private OrderInfoService orderInfoService;
    @Autowired
    private PactRecordService pactRecordService;
    @Autowired
    private WechatTransferOrderService wechatTransferOrderService;

    @Autowired
    private IcePutOrderService icePutOrderService;
    @Autowired
    private IcePutPactRecordService icePutPactRecordService;
    @Autowired
    private IceBackOrderService iceBackOrderService;

    @Autowired
    private IcePutApplyService icePutApplyService;
    @Autowired
    private IceBackApplyService iceBackApplyService;
    @Autowired
    private CommonConvert commonConvert;

    @Autowired
    private IceBoxExtendService iceBoxExtendService;
    @Autowired
    private FeignSupplierClient feignSupplierClient;
    @Autowired
    private IceBoxDao iceBoxDao;
    @Autowired
    private IceBackApplyRelateBoxService iceBackApplyRelateBoxService;
    @Autowired
    private IceBackApplyReportService iceBackApplyReportService;
    @Autowired
    FeignExamineClient feignExamineClient;
    @Autowired
    FeignUserClient feignUserClient;

    @Autowired
    FeignStoreClient feignStoreClient;
    @Autowired
    private IceInspectionReportService iceInspectionReportService;
    @Autowired
    FeignDeptClient feignDeptClient;
    @Autowired
    FeignCacheClient feignCacheClient;
    @Autowired
    IceExamineService iceExamineService;
    @Autowired
    IceBoxService iceBoxService;

    /**
     * ??????????????????
     */
    public void syncPutOrder() {
        // ???????????????
        List<OrderInfo> orderInfos = orderInfoService.list();
        // ???????????????
        List<IcePutOrder> icePutOrders = buildPutOrders(orderInfos);
        // ??????
        icePutOrderService.saveOrUpdateBatch(icePutOrders);
    }

    private List<IcePutOrder> buildPutOrders(List<OrderInfo> orderInfos) {
        List<IcePutOrder> list = Lists.newArrayList();
        for (OrderInfo orderInfo : orderInfos) {
            // ??????????????????????????????
            if (orderInfo.getChestId() == 1 || orderInfo.getChestId() == 2 || orderInfo.getChestId() == 3) {
                continue;
            }
            if (!orderInfo.getStatus().equals(OrderStatus.IS_FINISH.getStatus())) {
                continue;
            }
            IcePutOrder icePutOrder = new IcePutOrder();
            icePutOrder.setId(orderInfo.getId());
            icePutOrder.setChestId(orderInfo.getChestId());
            icePutOrder.setApplyNumber(getPutApplyNumberByOldPutId(orderInfo.getChestPutRecordId()));
            icePutOrder.setOrderNum(orderInfo.getOrderNum());
            icePutOrder.setOpenid(orderInfo.getOpenid());
            icePutOrder.setTotalMoney(orderInfo.getTotalMoney());
            icePutOrder.setPayMoney(orderInfo.getPayMoney());
            icePutOrder.setPrayId(orderInfo.getPrayId());
            icePutOrder.setTransactionId(orderInfo.getTransactionId());
            icePutOrder.setTradeState(orderInfo.getTradeState());
            icePutOrder.setTradeStateDesc(orderInfo.getTradeStateDesc());
            icePutOrder.setPayTime(orderInfo.getPayTime());
            icePutOrder.setStatus(orderInfo.getStatus());
            icePutOrder.setCreatedBy(0);
            icePutOrder.setCreatedTime(orderInfo.getCreateTime());
            icePutOrder.setUpdatedBy(0);
            icePutOrder.setUpdatedTime(orderInfo.getUpdateTime());
            list.add(icePutOrder);
        }
        return list;
    }

    private String getPutApplyNumberByOldPutId(Integer chestPutRecordId) {
        IcePutApply icePutApply = icePutApplyService.getOne(Wrappers.<IcePutApply>lambdaQuery()
                .eq(IcePutApply::getOldPutId, chestPutRecordId));
        return icePutApply == null ? null : icePutApply.getApplyNumber();
    }

    /**
     * ????????????????????????
     */
    public void syncPutPack() throws Exception {
        // ???????????????
        List<PactRecord> pactRecords = pactRecordService.list();
        // ???????????????
        List<IcePutPactRecord> icePutPactRecords = buildPutPacks(pactRecords);
        // ??????
        icePutPactRecordService.saveOrUpdateBatch(icePutPactRecords);
    }

    private List<IcePutPactRecord> buildPutPacks(List<PactRecord> pactRecords) throws Exception {
        List<IcePutPactRecord> list = Lists.newArrayList();
        for (PactRecord pactRecord : pactRecords) {
            // ??????????????????????????????
            if (pactRecord.getChestId() == 1 || pactRecord.getChestId() == 2 || pactRecord.getChestId() == 3) {
                continue;
            }
            IcePutPactRecord icePutPactRecord = new IcePutPactRecord();
            icePutPactRecord.setId(pactRecord.getId());
            icePutPactRecord.setStoreNumber(commonConvert.getStoreNumberByClientId(pactRecord.getClientId()));
            icePutPactRecord.setBoxId(pactRecord.getChestId());
            String applyNumber = getPutApplyNumberByOldPutId(pactRecord.getPutId());
            if(applyNumber == null){
                continue;
            }
            icePutPactRecord.setApplyNumber(applyNumber);
            icePutPactRecord.setPutTime(pactRecord.getPutTime());
            icePutPactRecord.setPutExpireTime(pactRecord.getPutExpireTime());
            icePutPactRecord.setCreatedBy(0);
            icePutPactRecord.setCreatedTime(pactRecord.getCreateTime());
            icePutPactRecord.setUpdatedBy(0);
            icePutPactRecord.setUpdatedTime(pactRecord.getUpdateTime());
            list.add(icePutPactRecord);
        }
        return list;
    }

    /**
     * ??????????????????
     */
    public void syncBackOrder() {
        // ???????????????
        List<WechatTransferOrder> wechatTransferOrders = wechatTransferOrderService.list();
        // ???????????????
        List<IceBackOrder> iceBackOrders = buildBackOrders(wechatTransferOrders);
        // ??????
        iceBackOrderService.saveOrUpdateBatch(iceBackOrders);
    }

    private List<IceBackOrder> buildBackOrders(List<WechatTransferOrder> wechatTransferOrders) {
        List<IceBackOrder> list = Lists.newArrayList();
        for (WechatTransferOrder wechatTransferOrder : wechatTransferOrders) {
            // ??????????????????????????????
            if (wechatTransferOrder.getChestId() == 1 || wechatTransferOrder.getChestId() == 2 || wechatTransferOrder.getChestId() == 3) {
                continue;
            }
            IceBackOrder iceBackOrder = new IceBackOrder();
            iceBackOrder.setId(wechatTransferOrder.getId());
            iceBackOrder.setResourceKey(wechatTransferOrder.getResourceKey());
            iceBackOrder.setBoxId(wechatTransferOrder.getChestId());
            iceBackOrder.setApplyNumber(getBackApplyNumberByOldPutId(wechatTransferOrder.getChestPutRecordId()));
            iceBackOrder.setPutOrderId(wechatTransferOrder.getOrderId());
            iceBackOrder.setPartnerTradeNo(wechatTransferOrder.getPartnerTradeNo());
            iceBackOrder.setOpenid(wechatTransferOrder.getOpenid());
            iceBackOrder.setAmount(wechatTransferOrder.getAmount());
            iceBackOrder.setCreatedBy(0);
            iceBackOrder.setCreatedTime(wechatTransferOrder.getCreateTime());
            iceBackOrder.setUpdatedBy(0);
            iceBackOrder.setUpdatedTime(wechatTransferOrder.getUpdateTime());
            list.add(iceBackOrder);
        }
        return list;
    }

    private String getBackApplyNumberByOldPutId(Integer chestPutRecordId) {
        IceBackApply iceBackApply = iceBackApplyService.getOne(Wrappers.<IceBackApply>lambdaQuery()
                .eq(IceBackApply::getOldPutId, chestPutRecordId).ne(IceBackApply::getExamineStatus,3));
        return iceBackApply.getApplyNumber();
    }

    /**
     * ??????????????????????????????????????????
     */
    public void syncIceBoxExtendFromApplyName() {
        List<IceBoxExtend> updateIceBoxExtends = new ArrayList<>();
        List<IceBoxExtend> iceBoxExtends = iceBoxExtendService.list();
        for (IceBoxExtend iceBoxExtend : iceBoxExtends) {
            Integer lastPutId = iceBoxExtend.getLastPutId();
            if(lastPutId == null || lastPutId == 0){
                continue;
            }
            String applyNumber = getPutApplyNumberByOldPutId(lastPutId);
            if(applyNumber == null){
                applyNumber = getBackApplyNumberByOldPutId(lastPutId);
            }

            IceBoxExtend updateIceBoxExtend = new IceBoxExtend();
            updateIceBoxExtend.setId(iceBoxExtend.getId());
            updateIceBoxExtend.setLastApplyNumber(applyNumber);
            updateIceBoxExtends.add(updateIceBoxExtend);
        }
        iceBoxExtendService.updateBatchById(updateIceBoxExtends);
    }


    public void syncIceBoxDept() {
        log.info("????????????????????????");
        List<IceBox> iceBoxes = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().isNotNull(IceBox::getSupplierId).groupBy(IceBox::getSupplierId));

        List<Integer> collect = iceBoxes.stream().map(IceBox::getSupplierId).collect(Collectors.toList());

        Map<Integer, SubordinateInfoVo> map = FeignResponseUtil.getFeignData(feignSupplierClient.findByIds(collect));

        map.forEach((key,value) -> {
            log.info("key------>[{}]",key);
            Integer marketAreaId = value.getMarketAreaId();
            if (null != marketAreaId) {
                iceBoxDao.update(null,Wrappers.<IceBox>lambdaUpdate()
                        .set(IceBox::getDeptId, marketAreaId)
                        .eq(IceBox::getSupplierId,key));
            }
        });
        log.info("????????????????????????");
    }

    public void syncIceBackApplyReport() {
        List<IceBackApply> list = iceBackApplyService.list(Wrappers.<IceBackApply>lambdaQuery().gt(IceBackApply::getId, 4));
        for (IceBackApply iceBackApply : list) {
            String applyNumber = iceBackApply.getApplyNumber();
            IceBackApplyRelateBox one = iceBackApplyRelateBoxService.getOne(Wrappers.<IceBackApplyRelateBox>lambdaQuery().eq(IceBackApplyRelateBox::getApplyNumber, applyNumber));
            IceBox iceBox = iceBoxDao.selectById(one.getBoxId());
            IceBackApplyReport backApplyReport = iceBackOrderService.generateBackReport(iceBox, applyNumber, iceBackApply.getBackStoreNumber(), one.getFreeType(),null,null);
            SubordinateInfoVo supplier = FeignResponseUtil.getFeignData(feignSupplierClient.readId(one.getBackSupplierId()));
            backApplyReport.setDealerName(supplier.getName());
            backApplyReport.setDealerNumber(supplier.getNumber());
            backApplyReport.setExamineStatus(iceBackApply.getExamineStatus());
            backApplyReport.setExamineId(iceBackApply.getExamineId());
            backApplyReport.setCheckDate(iceBackApply.getUpdatedTime());
            SessionUserInfoVo checkPerson = FeignResponseUtil.getFeignData(feignExamineClient.findLastCheckPerson(iceBackApply.getExamineId()));
            if(Objects.nonNull(checkPerson)){
                backApplyReport.setCheckOfficeName(checkPerson.getOfficeName());
                backApplyReport.setCheckPerson(checkPerson.getRealname());
                backApplyReport.setCheckPersonId(checkPerson.getId());
            }
            if(1==backApplyReport.getFreeType()){
                IceBackOrder iceBackOrder = iceBackOrderService.getOne(Wrappers.<IceBackOrder>lambdaQuery().eq(IceBackOrder::getApplyNumber, applyNumber));
                backApplyReport.setDepositMoney(iceBackOrder.getAmount());
            }
            backApplyReport.setCreatedTime(iceBackApply.getCreatedTime());
            backApplyReport.setBackDate(iceBackApply.getCreatedTime());
            SimpleUserInfoVo submitter = FeignResponseUtil.getFeignData(feignUserClient.findUserById(iceBackApply.getCreatedBy()));
            backApplyReport.setSubmitterMobile(submitter.getMobile());
            backApplyReport.setSubmitterName(submitter.getRealname());
            backApplyReport.setSubmitterId(submitter.getId());
            iceBackApplyReportService.updateById(backApplyReport);
        }
    }

    public void syncIceInspectionReport() {
        iceInspectionReportService.truncate();
        Page<IceBox> page = new Page<>();
        page.setSize(5000);
        List<IceBox> list;
        while(true){
            IPage<IceBox> iceBoxIPage = iceBoxDao.selectPage(page, new LambdaQueryWrapper<IceBox>().eq(IceBox::getPutStatus, PutStatus.FINISH_PUT.getStatus()).isNotNull(IceBox::getPutStoreNumber));
            list = iceBoxIPage.getRecords();
            if(CollectionUtils.isEmpty(list)){
                break;
            }
            page.setCurrent(page.getCurrent()+1);
            for (IceBox iceBox : list) {
                String number = iceBox.getPutStoreNumber();
                Integer userId = FeignResponseUtil.getFeignData(feignStoreClient.getMainSaleManId(number));
                if(Objects.isNull(userId)){
                    userId = FeignResponseUtil.getFeignData(feignSupplierClient.getMainSaleManId(number));
                }
                if(Objects.nonNull(userId)){
                    IceInspectionReport currentMonthReport = iceInspectionReportService.getCurrentMonthReport(userId);
                    if(Objects.isNull(currentMonthReport)){
                        Integer deptId = FeignResponseUtil.getFeignData(feignDeptClient.getMainDeptByUserId(userId));
                        if(Objects.isNull(deptId)){
                            continue;
                        }
                        SimpleUserInfoVo userInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findUserById(userId));
                        currentMonthReport = new IceInspectionReport();
                        currentMonthReport.setInspectionDate(new DateTime().toString("yyyy-MM"))
                                .setUserId(userId)
                                .setUserName(userInfoVo.getRealname());
                        Map<Integer, SessionDeptInfoVo> deptMap = FeignResponseUtil.getFeignData(feignCacheClient.getFiveLevelDept(deptId));
                        SessionDeptInfoVo headquarter = deptMap.get(5);
                        SessionDeptInfoVo business = deptMap.get(4);
                        if(Objects.nonNull(business)&&!DeptTypeEnum.BUSINESS_UNIT.getType().equals(business.getDeptType())){
                            business = null;
                            headquarter = deptMap.get(4);
                        }
                        SessionDeptInfoVo region = deptMap.get(3);
                        SessionDeptInfoVo service = deptMap.get(2);
                        SessionDeptInfoVo group = deptMap.get(1);
                        if(Objects.nonNull(headquarter)){
                            currentMonthReport.setHeadquartersDeptId(headquarter.getId()).setHeadquartersDeptName(headquarter.getName());
                        }
                        if(Objects.nonNull(business)){
                            currentMonthReport.setBusinessDeptId(business.getId()).setBusinessDeptName(business.getName());
                        }
                        if(Objects.nonNull(region)){
                            currentMonthReport.setRegionDeptId(region.getId()).setRegionDeptName(region.getName());
                        }
                        if(Objects.nonNull(service)){
                            currentMonthReport.setServiceDeptId(service.getId()).setServiceDeptName(service.getName());
                        }
                        if(Objects.nonNull(group)){
                            currentMonthReport.setGroupDeptId(group.getId()).setGroupDeptName(group.getName());
                        }
                        List<Integer> putBoxIds = iceBoxService.getPutBoxIds(userId);
                        Integer inspectionCount = iceExamineService.getInspectionBoxes(putBoxIds).size();
                        int lostCount = iceBoxService.getLostScrapCount(putBoxIds);
                        currentMonthReport.setInspectionCount(inspectionCount).setLostScrapCount(lostCount).setPutCount(putBoxIds.size());
                        iceInspectionReportService.save(currentMonthReport);
                    }
                }
            }
        }
    }
}
