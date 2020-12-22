package com.szeastroc.icebox.sync;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Lists;
import com.szeastroc.common.entity.customer.vo.SubordinateInfoVo;
import com.szeastroc.common.entity.user.vo.SessionUserInfoVo;
import com.szeastroc.common.entity.user.vo.SimpleUserInfoVo;
import com.szeastroc.common.entity.visit.SessionExamineVo;
import com.szeastroc.common.feign.customer.FeignSupplierClient;
import com.szeastroc.common.feign.user.FeignUserClient;
import com.szeastroc.common.feign.visit.FeignExamineClient;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.icebox.enums.OrderStatus;
import com.szeastroc.icebox.newprocess.dao.IceBackApplyRelateBoxDao;
import com.szeastroc.icebox.newprocess.dao.IceBoxDao;
import com.szeastroc.icebox.newprocess.entity.*;
import com.szeastroc.icebox.newprocess.service.*;
import com.szeastroc.icebox.oldprocess.entity.OrderInfo;
import com.szeastroc.icebox.oldprocess.entity.PactRecord;
import com.szeastroc.icebox.oldprocess.entity.WechatTransferOrder;
import com.szeastroc.icebox.oldprocess.service.OrderInfoService;
import com.szeastroc.icebox.oldprocess.service.PactRecordService;
import com.szeastroc.icebox.oldprocess.service.WechatTransferOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
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

    /**
     * 同步投放订单
     */
    public void syncPutOrder() {
        // 查询旧数据
        List<OrderInfo> orderInfos = orderInfoService.list();
        // 构建新数据
        List<IcePutOrder> icePutOrders = buildPutOrders(orderInfos);
        // 保存
        icePutOrderService.saveOrUpdateBatch(icePutOrders);
    }

    private List<IcePutOrder> buildPutOrders(List<OrderInfo> orderInfos) {
        List<IcePutOrder> list = Lists.newArrayList();
        for (OrderInfo orderInfo : orderInfos) {
            // 需要跳过一些测试数据
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
     * 同步投放电子协议
     */
    public void syncPutPack() throws Exception {
        // 查询旧数据
        List<PactRecord> pactRecords = pactRecordService.list();
        // 构建新数据
        List<IcePutPactRecord> icePutPactRecords = buildPutPacks(pactRecords);
        // 保存
        icePutPactRecordService.saveOrUpdateBatch(icePutPactRecords);
    }

    private List<IcePutPactRecord> buildPutPacks(List<PactRecord> pactRecords) throws Exception {
        List<IcePutPactRecord> list = Lists.newArrayList();
        for (PactRecord pactRecord : pactRecords) {
            // 需要跳过一些测试数据
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
     * 同步退还订单
     */
    public void syncBackOrder() {
        // 查询旧数据
        List<WechatTransferOrder> wechatTransferOrders = wechatTransferOrderService.list();
        // 构建新数据
        List<IceBackOrder> iceBackOrders = buildBackOrders(wechatTransferOrders);
        // 保存
        iceBackOrderService.saveOrUpdateBatch(iceBackOrders);
    }

    private List<IceBackOrder> buildBackOrders(List<WechatTransferOrder> wechatTransferOrders) {
        List<IceBackOrder> list = Lists.newArrayList();
        for (WechatTransferOrder wechatTransferOrder : wechatTransferOrders) {
            // 需要跳过一些测试数据
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
     * 同步冰柜扩展表的最近申请编号
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
        log.info("开始更新部门信息");
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
        log.info("更新部门信息结束");
    }

    public void syncIceBackApplyReport() {
        List<IceBackApply> list = iceBackApplyService.list(Wrappers.<IceBackApply>lambdaQuery().gt(IceBackApply::getId, 4));
        for (IceBackApply iceBackApply : list) {
            String applyNumber = iceBackApply.getApplyNumber();
            IceBackApplyRelateBox one = iceBackApplyRelateBoxService.getOne(Wrappers.<IceBackApplyRelateBox>lambdaQuery().eq(IceBackApplyRelateBox::getApplyNumber, applyNumber));
            IceBox iceBox = iceBoxDao.selectById(one.getBoxId());
            IceBackApplyReport backApplyReport = iceBackOrderService.generateBackReport(iceBox, applyNumber, iceBackApply.getBackStoreNumber(), one.getFreeType());
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
            iceBackApplyReportService.updateById(backApplyReport);
        }
    }
}
