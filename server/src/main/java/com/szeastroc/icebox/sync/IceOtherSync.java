package com.szeastroc.icebox.sync;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import com.szeastroc.common.entity.customer.vo.StoreInfoDtoVo;
import com.szeastroc.common.entity.customer.vo.SubordinateInfoVo;
import com.szeastroc.common.entity.customer.vo.SupplierInfoSessionVo;
import com.szeastroc.common.entity.user.vo.SessionDeptInfoVo;
import com.szeastroc.common.feign.customer.FeignStoreClient;
import com.szeastroc.common.feign.customer.FeignSupplierClient;
import com.szeastroc.common.feign.user.FeignCacheClient;
import com.szeastroc.common.feign.user.FeignDeptClient;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.icebox.enums.OrderStatus;
import com.szeastroc.icebox.newprocess.dao.IceBoxDao;
import com.szeastroc.icebox.newprocess.entity.*;
import com.szeastroc.icebox.newprocess.enums.DeptTypeEnum;
import com.szeastroc.icebox.newprocess.service.*;
import com.szeastroc.icebox.oldprocess.entity.OrderInfo;
import com.szeastroc.icebox.oldprocess.entity.PactRecord;
import com.szeastroc.icebox.oldprocess.entity.WechatTransferOrder;
import com.szeastroc.icebox.oldprocess.service.OrderInfoService;
import com.szeastroc.icebox.oldprocess.service.PactRecordService;
import com.szeastroc.icebox.oldprocess.service.WechatTransferOrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.crypto.hash.Hash;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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
    private FeignStoreClient feignStoreClient;
    @Autowired
    FeignDeptClient feignDeptClient;
    @Autowired
    private IceBoxExamineExceptionReportService iceBoxExamineExceptionReportService;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private FeignCacheClient feignCacheClient;

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

    private ThreadPoolExecutor executor = new ThreadPoolExecutor(10,20,0, TimeUnit.SECONDS,new ArrayBlockingQueue<>(1000), Executors.defaultThreadFactory(),new ThreadPoolExecutor.CallerRunsPolicy());

    public void syncIceBoxDept() {
        log.info("开始更新部门信息");
//        List<IceBox> iceBoxes = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().isNotNull(IceBox::getSupplierId).groupBy(IceBox::getSupplierId));
//
//        List<Integer> collect = iceBoxes.stream().map(IceBox::getSupplierId).collect(Collectors.toList());
//
//        Map<Integer, SubordinateInfoVo> map = FeignResponseUtil.getFeignData(feignSupplierClient.findByIds(collect));
//
//        map.forEach((key,value) -> {
//            log.info("key------>[{}]",key);
//            Integer marketAreaId = value.getMarketAreaId();
//            if (null != marketAreaId) {
//                iceBoxDao.update(null,Wrappers.<IceBox>lambdaUpdate()
//                        .set(IceBox::getDeptId, marketAreaId)
//                        .eq(IceBox::getSupplierId,key));
//            }
//        });

        Page<IceBox> page = new Page<>();
        page.setSearchCount(false);
        page.setSize(500);
        List<IceBox> iceBoxes = new ArrayList<>();
        while (true){
            IPage<IceBox> iceBoxIPage = iceBoxDao.selectPage(page, Wrappers.<IceBox>lambdaQuery().eq(IceBox::getSupplierId, 0));
            iceBoxes = iceBoxIPage.getRecords();
            if(CollectionUtils.isEmpty(iceBoxes)){
                break;
            }
            page.setCurrent(page.getCurrent()+1);
            for (IceBox iceBox : iceBoxes) {
                executor.execute(()->{
                    String putStoreNumber = iceBox.getPutStoreNumber();
                    String value = redisTemplate.opsForValue().get("pxt_ice_box_dept_" + putStoreNumber);
                    if(StringUtils.isNotBlank(value)){
                        iceBox.setDeptId(Integer.valueOf(value));
                        iceBoxDao.updateById(iceBox);
                    }else{
                        SupplierInfoSessionVo supplier = FeignResponseUtil.getFeignData(feignSupplierClient.getSuppliserInfoByNumber(putStoreNumber));
                        Integer marketAreaId = null;
                        if(Objects.nonNull(supplier)){
                            marketAreaId = supplier.getMarketAreaId();
                        }else{
                            StoreInfoDtoVo store = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(putStoreNumber));
                            if(Objects.nonNull(store)){
                                marketAreaId = store.getMarketArea();
                            }
                        }
                        Optional.ofNullable(marketAreaId).ifPresent(i->{
                            SessionDeptInfoVo dept = FeignResponseUtil.getFeignData(feignDeptClient.findSessionDeptById(i));
                            Integer deptType = dept.getDeptType();
                            if(DeptTypeEnum.SERVICE.getType().equals(deptType)){
                                iceBox.setDeptId(i);
                            }else if (DeptTypeEnum.GROUP.getType().equals(deptType)){
                                iceBox.setDeptId(dept.getParentId());
                            }
                            iceBoxDao.updateById(iceBox);
                            redisTemplate.opsForValue().set("pxt_ice_box_dept_"+putStoreNumber,String.valueOf(iceBox.getDeptId()),30,TimeUnit.MINUTES);
                        });
                    }
                });
            }
        }

        Page<IceBoxExamineExceptionReport> reportPage = new Page<>();
        reportPage.setSearchCount(false);
        reportPage.setSize(500);
        List<IceBoxExamineExceptionReport> reports = new ArrayList<>();
        while (true){
            IPage<IceBoxExamineExceptionReport> reportIPage = iceBoxExamineExceptionReportService.page(reportPage, Wrappers.<IceBoxExamineExceptionReport>lambdaQuery().eq(IceBoxExamineExceptionReport::getSupplierId, 0));
            reports = reportIPage.getRecords();
            if(CollectionUtils.isEmpty(reports)){
                break;
            }
            reportPage.setCurrent(reportPage.getCurrent()+1);
            for (IceBoxExamineExceptionReport report : reports) {
                executor.execute(()->{
                    String putStoreNumber = report.getPutCustomerNumber();
                    String s = redisTemplate.opsForValue().get("pxt_ice_box_level_dept_" + putStoreNumber);
                    Map<Integer, SessionDeptInfoVo> deptInfoVoMap = new HashMap<>();
                    if(StringUtils.isNotBlank(s)){
                        deptInfoVoMap = JSON.parseObject(s,new TypeReference<HashMap<Integer, SessionDeptInfoVo>>(){});
                    }else{
                        String value = redisTemplate.opsForValue().get("pxt_ice_box_dept_" + putStoreNumber);
                        if(StringUtils.isNotBlank(value)){
                            deptInfoVoMap = FeignResponseUtil.getFeignData(feignCacheClient.getFiveLevelDept(Integer.valueOf(value)));
                            redisTemplate.opsForValue().set("pxt_ice_box_level_dept_"+putStoreNumber, JSONObject.toJSONString(deptInfoVoMap),30,TimeUnit.MINUTES);
                        }else{
                            SupplierInfoSessionVo supplier = FeignResponseUtil.getFeignData(feignSupplierClient.getSuppliserInfoByNumber(putStoreNumber));
                            Integer marketAreaId = null;
                            if(Objects.nonNull(supplier)){
                                marketAreaId = supplier.getMarketAreaId();
                            }else{
                                StoreInfoDtoVo store = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(putStoreNumber));
                                if(Objects.nonNull(store)){
                                    marketAreaId = store.getMarketArea();
                                }
                            }
                            if(Objects.nonNull(marketAreaId)){
                                SessionDeptInfoVo dept = FeignResponseUtil.getFeignData(feignDeptClient.findSessionDeptById(marketAreaId));
                                Integer deptType = dept.getDeptType();
                                Integer deptId = 0;
                                if(DeptTypeEnum.SERVICE.getType().equals(deptType)){
                                    deptId = marketAreaId;
                                }else if (DeptTypeEnum.GROUP.getType().equals(deptType)){
                                    deptId = dept.getParentId();
                                }
                                redisTemplate.opsForValue().set("pxt_ice_box_dept_"+putStoreNumber,String.valueOf(deptId),30,TimeUnit.MINUTES);
                                deptInfoVoMap = FeignResponseUtil.getFeignData(feignCacheClient.getFiveLevelDept(Integer.valueOf(deptId)));
                                redisTemplate.opsForValue().set("pxt_ice_box_level_dept_"+putStoreNumber, JSONObject.toJSONString(deptInfoVoMap),30,TimeUnit.MINUTES);
                            }

                        }
                    }
                    SessionDeptInfoVo group = deptInfoVoMap.get(1);
                    if (group != null) {
                        report.setGroupDeptId(group.getId());
                        report.setGroupDeptName(group.getName());
                    }
                    SessionDeptInfoVo service = deptInfoVoMap.get(2);
                    if (service != null) {
                        report.setServiceDeptId(service.getId());
                        report.setServiceDeptName(service.getName());
                    }
                    SessionDeptInfoVo region = deptInfoVoMap.get(3);
                    if (region != null) {
                        report.setRegionDeptId(region.getId());
                        report.setRegionDeptName(region.getName());
                    }

                    SessionDeptInfoVo business = deptInfoVoMap.get(4);
                    if (business != null) {
                        report.setBusinessDeptId(business.getId());
                        report.setBusinessDeptName(business.getName());
                    }

                    SessionDeptInfoVo headquarters = deptInfoVoMap.get(5);
                    if (headquarters != null) {
                        report.setHeadquartersDeptId(headquarters.getId());
                        report.setHeadquartersDeptName(headquarters.getName());
                    }
                    iceBoxExamineExceptionReportService.updateById(report);
                });
            }
        }
        log.info("更新部门信息结束");
    }
}
