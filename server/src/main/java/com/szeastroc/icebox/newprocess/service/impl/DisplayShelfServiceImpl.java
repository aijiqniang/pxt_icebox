package com.szeastroc.icebox.newprocess.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.entity.customer.vo.*;
import com.szeastroc.common.entity.user.session.MatchRuleVo;
import com.szeastroc.common.entity.user.session.UserManageVo;
import com.szeastroc.common.entity.user.vo.DeptInfoConnectParentVo;
import com.szeastroc.common.entity.user.vo.DeptNameRequest;
import com.szeastroc.common.entity.user.vo.SessionDeptInfoVo;
import com.szeastroc.common.entity.user.vo.SysRuleShelfDetailVo;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.exception.NormalOptionException;
import com.szeastroc.common.feign.customer.FeignStoreClient;
import com.szeastroc.common.feign.customer.FeignStoreRelateMemberClient;
import com.szeastroc.common.feign.customer.FeignSupplierClient;
import com.szeastroc.common.feign.user.FeignDeptClient;
import com.szeastroc.common.feign.user.FeignDeptRuleClient;
import com.szeastroc.common.feign.user.FeignUserClient;
import com.szeastroc.common.feign.visit.FeignExportRecordsClient;
import com.szeastroc.common.feign.visit.FeignIceboxQueryClient;
import com.szeastroc.common.utils.ExecutorServiceFactory;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.commondb.config.redis.JedisClient;
import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.icebox.constant.RedisConstant;
import com.szeastroc.icebox.enums.DisplayShelfTypeEnum;
import com.szeastroc.icebox.newprocess.consumer.common.ShelfPutDetailsMsg;
import com.szeastroc.icebox.newprocess.dao.DisplayShelfDao;
import com.szeastroc.icebox.newprocess.dao.DisplayShelfPutApplyDao;
import com.szeastroc.icebox.newprocess.entity.DisplayShelf;
import com.szeastroc.icebox.newprocess.entity.DisplayShelfInspectReport;
import com.szeastroc.icebox.newprocess.entity.DisplayShelfPutApply;
import com.szeastroc.icebox.newprocess.entity.DisplayShelfPutApplyRelate;
import com.szeastroc.icebox.newprocess.enums.*;
import com.szeastroc.icebox.newprocess.service.DisplayShelfInspectApplyService;
import com.szeastroc.icebox.newprocess.service.DisplayShelfPutApplyRelateService;
import com.szeastroc.icebox.newprocess.service.DisplayShelfPutApplyService;
import com.szeastroc.icebox.newprocess.service.DisplayShelfService;
import com.szeastroc.icebox.newprocess.vo.DisplayShelfPutApplyVo;
import com.szeastroc.icebox.newprocess.vo.SupplierDisplayShelfVO;
import com.szeastroc.icebox.newprocess.vo.request.DisplayShelfPage;
import com.szeastroc.icebox.newprocess.vo.request.ShelfStockRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
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
    private FeignStoreClient feignStoreClient;
    @Autowired
    private FeignDeptClient feignDeptClient;
    @Autowired
    private FeignIceboxQueryClient feignIceboxQueryClient;
    @Autowired
    FeignDeptRuleClient feignDeptRuleClient;
    @Autowired
    DisplayShelfPutApplyService putApplyService;
    @Autowired
    DisplayShelfInspectApplyService inspectApplyService;
    @Autowired
    DisplayShelfService displayShelfService;
    @Autowired
    FeignStoreRelateMemberClient storeRelateMemberClient;
    @Resource
    private DisplayShelfPutApplyDao displayShelfPutApplyDao;
    @Autowired
    private FeignUserClient feignUserClient;
    @Autowired
    JedisClient jedis;
    @Autowired
    private DisplayShelfDao displayShelfDao;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    FeignExportRecordsClient feignExportRecordsClient;

    @Override
    public IPage<DisplayShelf> selectDetails(DisplayShelfPage page) {
        IPage<DisplayShelf> selectDetails = this.baseMapper.selectDetails(page);
       /* List<DisplayShelf> records = selectDetails.getRecords();
        List<DisplayShelf> list = new ArrayList<>();
        Map<String, DisplayShelf> map = new HashMap<>();
        for (DisplayShelf record : records) {
            String v = record.getName() + record.getSize();
            if(map.containsKey(v)){
                DisplayShelf displayShelf = map.get(v);
                displayShelf.setCount(displayShelf.getCount() + record.getCount());
                map.put(v, displayShelf);
            }{
                map.put(v, record);
            }*/
//            for (DisplayShelf displayShelf : records) {
//                //如果投放陈列架的名称和大小和状态都一样  是同一条数据
//                if(record.getName().equals(displayShelf.getName()) && record.getSize().equals(displayShelf.getSize()) && record.getPutStatus().equals(displayShelf.getPutStatus())){
//                    continue;
//                //如果名称和大小一样 状态不一样  表示是同一种投放数据  需要合并
//                }else if(record.getName().equals(displayShelf.getName()) && record.getSize().equals(displayShelf.getSize()) && !record.getPutStatus().equals(displayShelf.getPutStatus())){
//                    record.setCount(record.getCount() + displayShelf.getCount());
//                    record.setPutCount(displayShelf.getCount());
//                    list.add(record);
//                    break;
//                }
//            }
        /*}
        records.addAll(list);
        selectDetails.setRecords(records);*/
       /* //--------------------------------
        LambdaQueryWrapper<DisplayShelf> wrapper = Wrappers.lambdaQuery();
        if(page != null) {
            if (page.getDeptType() != null && page.getMarketAreaId() != null) {
                switch (page.getDeptType()) {
                    //deptType  1:服务处 2:大区 3:事业部 4:本部
                    case 1:
                        wrapper.eq(DisplayShelf::getServiceDeptId, page.getMarketAreaId());
                        break;
                    case 2:
                        wrapper.eq(DisplayShelf::getRegionDeptId, page.getMarketAreaId());
                        break;
                    case 3:
                        wrapper.eq(DisplayShelf::getBusinessDeptId, page.getMarketAreaId());
                        break;
                    case 4:
                        wrapper.eq(DisplayShelf::getHeadquartersDeptId, page.getMarketAreaId());
                        break;
                }
            }
        }
        if(StringUtils.isNotEmpty(page.getShelfType())){
            wrapper.like(DisplayShelf::getName,page.getShelfType());
        }
        wrapper.groupBy(DisplayShelf::getName,DisplayShelf::getSize,DisplayShelf::getPutStatus,
                DisplayShelf::getServiceDeptId,DisplayShelf::getRegionDeptId,DisplayShelf::getBusinessDeptId,DisplayShelf::getHeadquartersDeptId)
                .in(DisplayShelf::getPutStatus,0,3);*/
//        IPage<DisplayShelf> selectDetails = displayShelfDao.selectPage(page, wrapper);
        //--------------------------------
        return selectDetails;
    }

    @Getter
    @AllArgsConstructor
    private enum ExportRecordTypeEnum {
        PROCESSING((byte) 0, "处理中"),
        COMPLETED((byte) 1, "已完成");

        private Byte type;
        private String desc;
    }


    @Override
    public IPage<DisplayShelf> selectPage(DisplayShelfPage page) {
        return this.baseMapper.selectPage(page);
    }

    @Transactional(rollbackFor = Exception.class, transactionManager = "transactionManager")
    @Override
    public void importData(MultipartFile file) {
        try {
            List<DisplayShelf.DisplayShelfData> list = EasyExcel.read(file.getInputStream()).head(DisplayShelf.DisplayShelfData.class).sheet().doReadSync();
            int repertoryCount = list.parallelStream().parallel().mapToInt(DisplayShelf.DisplayShelfData::getRepertoryCount).sum();
            if (repertoryCount > 10000) {
                throw new NormalOptionException(Constants.API_CODE_FAIL, "每次导入陈列货架总数不能超过10000");
            }

            for (DisplayShelf.DisplayShelfData o : list) {
                List<DisplayShelf> shelves = new ArrayList<>();
                DeptNameRequest dep = new DeptNameRequest();
                dep.setDeptName(o.getServiceDeptName());
                List<DeptInfoConnectParentVo> deptInfoVos = FeignResponseUtil.getFeignData(feignDeptClient.findFullDeptInfoByName(dep));
                if(DisplayShelfTypeEnum.ENERGY_FOUR.getDesc().equals(o.getShelfType())){
                    DisplayShelf displayShelf = buildData(o,deptInfoVos);
                    displayShelf.setSize(o.getSize());
                    displayShelf.setName(o.getShelfType());
                    displayShelf.setType(DisplayShelfTypeEnum.ENERGY_FOUR.getType());
                    for (int i = 0; i < o.getRepertoryCount(); i++) {
                        shelves.add(displayShelf);
                    }
                }else if(DisplayShelfTypeEnum.LEMON_TEA_FOUR.getDesc().equals(o.getShelfType())){
                    DisplayShelf displayShelf = buildData(o,deptInfoVos);
                    displayShelf.setSize(o.getSize());
                    displayShelf.setName(o.getShelfType());
                    displayShelf.setType(DisplayShelfTypeEnum.LEMON_TEA_FOUR.getType());
                    for (int i = 0; i < o.getRepertoryCount(); i++) {
                        shelves.add(displayShelf);
                    }
                }else if(DisplayShelfTypeEnum.SODA_FOUR.getDesc().equals(o.getShelfType())){
                    DisplayShelf displayShelf = buildData(o,deptInfoVos);
                    displayShelf.setSize(o.getSize());
                    displayShelf.setName(o.getShelfType());
                    displayShelf.setType(DisplayShelfTypeEnum.SODA_FOUR.getType());
                    for (int i = 0; i < o.getRepertoryCount(); i++) {
                        shelves.add(displayShelf);
                    }
                }else if(DisplayShelfTypeEnum.LARGE_DISPLAY_RACK.getDesc().equals(o.getShelfType())){
                    DisplayShelf displayShelf = buildData(o,deptInfoVos);
                    displayShelf.setSize(o.getSize());
                    displayShelf.setName(o.getShelfType());
                    displayShelf.setType(DisplayShelfTypeEnum.LARGE_DISPLAY_RACK.getType());
                    for (int i = 0; i < o.getRepertoryCount(); i++) {
                        shelves.add(displayShelf);
                    }
                }else if(DisplayShelfTypeEnum.MEDIUM_DISPLAY_SHELF.getDesc().equals(o.getShelfType())){
                    DisplayShelf displayShelf = buildData(o,deptInfoVos);
                    displayShelf.setSize(o.getSize());
                    displayShelf.setName(o.getShelfType());
                    displayShelf.setType(DisplayShelfTypeEnum.MEDIUM_DISPLAY_SHELF.getType());
                    for (int i = 0; i < o.getRepertoryCount(); i++) {
                        shelves.add(displayShelf);
                    }
                }
                this.saveBatch(shelves);
            }
        } catch (Exception e) {
            throw new NormalOptionException(Constants.API_CODE_FAIL, "导入陈列架失败");
        }
    }

    @Override
    public CommonResponse exportShelf(ShelfPutDetailsMsg shelfPutDetailsMsg) {
        // 获取当前用户相关信息
        UserManageVo userManageVo = FeignResponseUtil.getFeignData(feignUserClient.getSessionUserInfo());

        String key = String.format("%s%s", RedisConstant.SHELF_DETAILS_EXPORT_KEY, userManageVo.getSessionUserInfoVo().getId());
        if (null != jedis.get(key)){
            return new CommonResponse<>(Constants.API_CODE_FAIL, "请求导出操作频繁，请稍候操作");
        }
        LambdaQueryWrapper<DisplayShelf> shelfLambdaQueryWrapper = this.buildWrapper(shelfPutDetailsMsg);
        Integer count = Optional.ofNullable(displayShelfDao.selectByExportCount(shelfLambdaQueryWrapper)).orElse(0);
        if (0 == count) {
            return new CommonResponse<>(Constants.API_CODE_FAIL, "暂无可导出的数据");
        }
        /*// 生成下载任务编号
        String serialNum = String.format("shelf%s", System.currentTimeMillis());
        displayShelfDao.insertExportRecords(serialNum, "陈列架投放信息详情-导出", userManageVo.getSessionUserInfoVo().getId(),
                userManageVo.getSessionUserInfoVo().getRealname(), ExportRecordTypeEnum.PROCESSING.getType(), new Date(), JSON.toJSONString(page));
        // 发送消息
        CompletableFuture.runAsync(() -> rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.SHELF_PUT_DETAILS_K,
                new ShelfPutDetailsMsg().setShelfLambdaQueryWrapper(shelfLambdaQueryWrapper).setSerialNum(serialNum)
                        .setUserId(userManageVo.getSessionUserInfoVo().getId())
                        .setRealName(userManageVo.getSessionUserInfoVo().getRealname())));*/
        DisplayShelfPage page = new DisplayShelfPage();
        BeanUtils.copyProperties(shelfPutDetailsMsg, page);
        IPage<DisplayShelf> displayShelfIPage = displayShelfDao.selectDetails(page);
        // 生成下载任务
        Integer recordsId = FeignResponseUtil.getFeignData(feignExportRecordsClient.createExportRecords(userManageVo.getSessionUserInfoVo().getId(),
                userManageVo.getSessionUserInfoVo().getRealname(), JSON.toJSONString(shelfPutDetailsMsg), "陈列架投放信息详情-导出"));

        //发送mq消息,同步申请数据到报表

        CompletableFuture.runAsync(() -> {
            shelfPutDetailsMsg.setRecordsId(recordsId);
            shelfPutDetailsMsg.setDisplayShelfIPage(displayShelfIPage);
            rabbitTemplate.convertAndSend(MqConstant.E_EXCHANGE, MqConstant.SHELF_PUT_DETAILS_K, shelfPutDetailsMsg);
        });

        // 三分钟间隔
        jedis.set(key, "ex", 300, TimeUnit.SECONDS);

        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }

    //构建查询条件
    public LambdaQueryWrapper<DisplayShelf> buildWrapper(ShelfPutDetailsMsg shelfPutDetailsMsg) throws ImproperOptionException, NormalOptionException {
        LambdaQueryWrapper<DisplayShelf> wrapper = Wrappers.lambdaQuery();
        if(shelfPutDetailsMsg != null) {
            if (shelfPutDetailsMsg.getDeptType() != null && shelfPutDetailsMsg.getMarketAreaId() != null) {
                switch (shelfPutDetailsMsg.getDeptType()) {
                    //deptType  1:服务处 2:大区 3:事业部 4:本部
                    case 1:
                        wrapper.eq(DisplayShelf::getServiceDeptId, shelfPutDetailsMsg.getMarketAreaId());
                        break;
                    case 2:
                        wrapper.eq(DisplayShelf::getRegionDeptId, shelfPutDetailsMsg.getMarketAreaId());
                        break;
                    case 3:
                        wrapper.eq(DisplayShelf::getBusinessDeptId, shelfPutDetailsMsg.getMarketAreaId());
                        break;
                    case 4:
                        wrapper.eq(DisplayShelf::getHeadquartersDeptId, shelfPutDetailsMsg.getMarketAreaId());
                        break;
                }
            }
        }
        if(com.baomidou.mybatisplus.core.toolkit.StringUtils.isNotEmpty(shelfPutDetailsMsg.getShelfType())){
            wrapper.like(DisplayShelf::getName,shelfPutDetailsMsg.getShelfType());
        }
        return wrapper;
    }

    private DisplayShelf buildData(DisplayShelf.DisplayShelfData o, List<DeptInfoConnectParentVo> deptInfoVos) {
        DeptInfoConnectParentVo deptInfoConnectParentVo = deptInfoVos.get(0);
        SessionDeptInfoVo service = deptInfoConnectParentVo.getSessionDeptInfoVos().get(0);
        SessionDeptInfoVo region = deptInfoConnectParentVo.getSessionDeptInfoVos().get(1);
        SessionDeptInfoVo business = deptInfoConnectParentVo.getSessionDeptInfoVos().get(2);
        SessionDeptInfoVo headquarters = deptInfoConnectParentVo.getSessionDeptInfoVos().get(3);
        DisplayShelf displayShelf = new DisplayShelf();
        BeanUtils.copyProperties(o, displayShelf);
        displayShelf.setHeadquartersDeptId(headquarters.getId());
        displayShelf.setHeadquartersDeptName(headquarters.getName());
        displayShelf.setBusinessDeptId(business.getId());
        displayShelf.setRegionDeptId(region.getId());
        displayShelf.setServiceDeptId(service.getId());
        return displayShelf;
    }




    @Override
    public List<SupplierDisplayShelfVO> canPut(ShelfStockRequest request) {
        //查询货架投放规则
        MatchRuleVo matchRuleVo = new MatchRuleVo();
        matchRuleVo.setOpreateType(11);
        matchRuleVo.setDeptId(request.getMarketAreaId());
        matchRuleVo.setType(3);
        SysRuleShelfDetailVo putRule = FeignResponseUtil.getFeignData(feignDeptRuleClient.matchShelfRule(matchRuleVo));
        if (Objects.isNull(putRule)) {
            throw new NormalOptionException(Constants.API_CODE_FAIL, "未设置货架投放规则");
        }
        String shelfType = putRule.getShelfType();
        if (StringUtils.isBlank(shelfType)) {
            throw new NormalOptionException(Constants.API_CODE_FAIL, "货架投放规则未设置货架类型");
        }
        Integer serviceId = FeignResponseUtil.getFeignData(feignDeptClient.getServiceId(request.getMarketAreaId()));
        String[] typeArr = shelfType.split(",");
        List<DisplayShelf> shelfList = this.baseMapper.noPutShelves(serviceId, typeArr);
        String customerLevel;
        String customerName;
        String customerLinkMobile;
        String customerLinkAddress;
        String customerLinkMan;
        StoreInfoDtoVo store = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(request.getCustomerNumber()));
        if(Objects.nonNull(store)){
            customerLevel = store.getStoreLevel();
            customerLinkAddress = store.getAddress();
            customerName = store.getStoreName();
            MemberInfoVo shopKeeper = FeignResponseUtil.getFeignData(storeRelateMemberClient.getShopKeeper(request.getCustomerNumber()));
            if(Objects.isNull(shopKeeper)){
                MemberInfoVo member = FeignResponseUtil.getFeignData(storeRelateMemberClient.getMemberByStoreNumber(request.getCustomerNumber()));
                if(Objects.isNull(member)){
                    throw new NormalOptionException(Constants.API_CODE_FAIL, "该门店没有联系人");
                }
                customerLinkMan = member.getName();
                customerLinkMobile = member.getMobile();
            }else{
                customerLinkMan = shopKeeper.getName();
                customerLinkMobile = shopKeeper.getMobile();
            }

        }else{
            SupplierInfoSessionVo supplier = FeignResponseUtil.getFeignData(feignSupplierClient.getSuppliserInfoByNumber(request.getCustomerNumber()));
            if(Objects.nonNull(supplier)){
                customerLevel = supplier.getLevel();
                customerName = supplier.getName();;
                customerLinkAddress = supplier.getAddress();
                customerLinkMan = supplier.getLinkMan();
                customerLinkMobile = supplier.getLinkManMobile();
            }else{
                throw new NormalOptionException(Constants.API_CODE_FAIL, "当前投放客户找不到");
            }
        }
        return shelfList.stream().map(o -> {
            SupplierDisplayShelfVO vo = new SupplierDisplayShelfVO();
            BeanUtils.copyProperties(o, vo);
            /*SupplierInfoSessionVo supplier = FeignResponseUtil.getFeignData(feignSupplierClient.getSuppliserInfoByNumber(o.getSupplierNumber()));
            vo.setLinkMobile(supplier.getLinkManMobile());
            vo.setLinkMan(supplier.getLinkMan());
            vo.setLinkAddress(supplier.getAddress());*/
            vo.setVisitTypeName(VisitCycleEnum.getDescByCode(FeignResponseUtil.getFeignData(feignIceboxQueryClient.selectVisitTypeForReport(request.getCustomerNumber()))));
            vo.setCustomerLevel(customerLevel);
            vo.setCustomerLinkAddress(customerLinkAddress);
            vo.setCustomerLinkMan(customerLinkMan);
            vo.setCustomerName(customerName);
            vo.setCustomerLinkMobile(customerLinkMobile);
            return vo;
        }).collect(Collectors.toList());
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

    @Override
    public List<DisplayShelf.DisplayShelfType> customerTotalCount(String customerNumber) {
        return this.baseMapper.typeCount(customerNumber);
    }


    @Override
    public List<DisplayShelf.DisplayShelfType> customerDetail(String customerNumber) {
        return this.baseMapper.customerDetail(customerNumber);
    }

    @Override
    public List<DisplayShelfPutApplyVo> examineDetails(String code) {
        DisplayShelfPutApplyVo vo = new DisplayShelfPutApplyVo();
        List<DisplayShelfPutApplyVo> list = new ArrayList<>();
        DisplayShelfPutApply displayShelfPutApply = displayShelfPutApplyDao.selectOne(Wrappers.<DisplayShelfPutApply>lambdaQuery()
                .eq(DisplayShelfPutApply::getApplyNumber, code)
                .eq(DisplayShelfPutApply::getSignStatus, StoreSignStatus.DEFAULT_SIGN.getStatus()));
        if(Objects.nonNull(displayShelfPutApply)){
            vo.setApplyNumber(displayShelfPutApply.getApplyNumber());
            vo.setCreateTime(displayShelfPutApply.getCreatedTime());
        }
        List<DisplayShelfPutApplyRelate> relates = shelfPutApplyRelateService.list(Wrappers.<DisplayShelfPutApplyRelate>lambdaQuery().eq(DisplayShelfPutApplyRelate::getApplyNumber, code));
        Collection<DisplayShelf> displayShelves = displayShelfService.listByIds(relates.stream().map(DisplayShelfPutApplyRelate::getShelfId).collect(Collectors.toList()));
        Map<String, List<DisplayShelf>> listMap = displayShelves.stream().collect(Collectors.groupingBy(groups -> groups.getType()+"_"+groups.getSize()));
        List<SupplierDisplayShelfVO> shelfList = listMap.entrySet().stream().map(e -> {
            String[] temp = e.getKey().split("_");
            String type = temp[0];
            String size = temp[1];
            SupplierDisplayShelfVO supplierDisplayShelfVO = new SupplierDisplayShelfVO();
            supplierDisplayShelfVO.setCount(e.getValue().size());
            supplierDisplayShelfVO.setType(e.getValue().get(0).getType());
            supplierDisplayShelfVO.setName(e.getValue().get(0).getName());
            supplierDisplayShelfVO.setSize(e.getValue().get(0).getSize());
            supplierDisplayShelfVO.setServiceDeptId(e.getValue().get(0).getServiceDeptId());
            supplierDisplayShelfVO.setServiceDeptName(e.getValue().get(0).getServiceDeptName());
            return supplierDisplayShelfVO;
        }).collect(Collectors.toList());
        vo.setShelfList(shelfList);
        list.add(vo);
        return list;
    }

    @Override
    public Integer selectByExportCount(LambdaQueryWrapper<DisplayShelf> wrapper) {
        return this.count(wrapper);
    }


}
