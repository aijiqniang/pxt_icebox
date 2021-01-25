package com.szeastroc.icebox.newprocess.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.entity.customer.vo.StoreInfoDtoVo;
import com.szeastroc.common.entity.customer.vo.SupplierInfoSessionVo;
import com.szeastroc.common.entity.user.session.UserManageVo;
import com.szeastroc.common.feign.customer.FeignDistrictExtensionClient;
import com.szeastroc.common.feign.customer.FeignStoreClient;
import com.szeastroc.common.feign.customer.FeignSupplierClient;
import com.szeastroc.common.feign.user.FeignUserClient;
import com.szeastroc.common.feign.visit.FeignExportRecordsClient;
import com.szeastroc.common.utils.ExecutorServiceFactory;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.commondb.config.annotation.RedisLock;
import com.szeastroc.commondb.config.redis.JedisClient;
import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.icebox.constant.RedisConstant;
import com.szeastroc.icebox.newprocess.consumer.common.IceRepairOrderMsg;
import com.szeastroc.icebox.newprocess.dao.IceRepairOrderDao;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.entity.IceRepairOrder;
import com.szeastroc.icebox.newprocess.enums.SupplierTypeEnum;
import com.szeastroc.icebox.newprocess.service.IceBoxService;
import com.szeastroc.icebox.newprocess.service.IceRepairOrderService;
import com.szeastroc.icebox.newprocess.vo.IceRepairOrderVO;
import com.szeastroc.icebox.newprocess.vo.request.IceRepairRequest;
import com.szeastroc.icebox.newprocess.vo.request.IceRepairStatusRequest;
import com.szeastroc.icebox.newprocess.webservice.WbSiteRequestVO;
import com.szeastroc.icebox.newprocess.webservice.WbSiteResponseVO;
import com.szeastroc.icebox.newprocess.webservice.WebSite;
import com.szeastroc.icebox.newprocess.webservice.WebSitePortType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 冰柜维修订单表(IceRepairOrder)表服务实现类
 *
 * @author chenchao
 * @since 2021-01-12 15:58:24
 */
@Slf4j
@Service
public class IceRepairOrderServiceImpl extends ServiceImpl<IceRepairOrderDao, IceRepairOrder> implements IceRepairOrderService {
    @Autowired
    private FeignStoreClient feignStoreClient;
    @Autowired
    private FeignSupplierClient feignSupplierClient;
    @Autowired
    private FeignUserClient feignUserClient;

    @Value("${hisense.repair.account}")
    private String account;
    @Value("${hisense.repair.password}")
    private String password;
    @Autowired
    private JedisClient jedis;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private FeignExportRecordsClient feignExportRecordsClient;
    @Autowired
    private FeignDistrictExtensionClient districtExtensionClient;
    @Autowired
    private IceBoxService iceBoxService;

    @Transactional(rollbackFor = Exception.class, transactionManager = "transactionManager")
    @RedisLock(key = "#iceRepairRequest.boxId")
    @Override
    public CommonResponse createOrder(IceRepairRequest iceRepairRequest) {
        Integer count = this.getUnfinishOrderCount(iceRepairRequest.getBoxId());
        if (count > 0) {
            return new CommonResponse(Constants.API_CODE_FAIL, null, "冰柜报修失败，该冰柜已存在未完成订单");
        }
        String msg = null;
        Integer businessDeptId = null;
        Integer headquartersDeptId = null;
        Integer serviceDeptId = null;
        Integer groupDeptId = null;
        Integer regionDeptId = null;
        String headquartersDeptName = null;
        String businessDeptName = null;
        String regionDeptName = null;
        String serviceDeptName = null;
        String groupDeptName = null;
        if (SupplierTypeEnum.IS_STORE.getType().equals(iceRepairRequest.getCustomerType())) {
            StoreInfoDtoVo store = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(iceRepairRequest.getCustomerNumber()));
            businessDeptId = store.getBusinessDeptId();
            headquartersDeptId = store.getHeadquartersDeptId();
            regionDeptId = store.getRegionDeptId();
            serviceDeptId = store.getServiceDeptId();
            groupDeptId = store.getGroupDeptId();
            businessDeptName = store.getBusinessDeptName();
            headquartersDeptName = store.getHeadquartersDeptName();
            regionDeptName = store.getRegionDeptName();
            serviceDeptName = store.getServiceDeptName();
            groupDeptName = store.getGroupDeptName();
        } else {
            SupplierInfoSessionVo supplier = FeignResponseUtil.getFeignData(feignSupplierClient.getSuppliserInfoByNumber(iceRepairRequest.getCustomerNumber()));
            businessDeptId = supplier.getBusinessDeptId();
            headquartersDeptId = supplier.getHeadquartersDeptId();
            regionDeptId = supplier.getRegionDeptId();
            serviceDeptId = supplier.getServiceDeptId();
            groupDeptId = supplier.getGroupDeptId();
            businessDeptName = supplier.getBusinessDeptName();
            headquartersDeptName = supplier.getHeadquartersDeptName();
            regionDeptName = supplier.getRegionDeptName();
            serviceDeptName = supplier.getServiceDeptName();
            groupDeptName = supplier.getGroupDeptName();
        }
        String phoneAreaCode = FeignResponseUtil.getFeignData(districtExtensionClient.getPhoneAreaCodeByCode(iceRepairRequest.getCityCode()));
        iceRepairRequest.setServiceTypeId("WX");
        iceRepairRequest.setOriginFlag("DP");
        iceRepairRequest.setPsnAccount(account);
        iceRepairRequest.setPsnPwd(password);
        iceRepairRequest.setPhoneAreaCode(phoneAreaCode);
        String orderNumber = "REP" + new DateTime().toString("yyyyMMddHHmmss") + RandomUtil.randomNumbers(4);
        IceRepairOrder repairOrder = IceRepairOrder.builder().orderNumber(orderNumber).boxId(iceRepairRequest.getBoxId())
                .businessDeptId(businessDeptId).businessDeptName(businessDeptName)
                .headquartersDeptId(headquartersDeptId).headquartersDeptName(headquartersDeptName)
                .regionDeptId(regionDeptId).regionDeptName(regionDeptName)
                .serviceDeptId(serviceDeptId).serviceDeptName(serviceDeptName)
                .groupDeptId(groupDeptId).groupDeptName(groupDeptName)
                .customerNumber(iceRepairRequest.getCustomerNumber()).customerName(iceRepairRequest.getCustomerName())
                .customerAddress(iceRepairRequest.getCustomerAddress()).customerType(iceRepairRequest.getCustomerType()).assetId(iceRepairRequest.getAssetId())
                .linkMan(iceRepairRequest.getLinkMan()).linkMobile(iceRepairRequest.getLinkMobile())
                .modelName(iceRepairRequest.getModelName()).modelId(iceRepairRequest.getModelId())
                .remark(iceRepairRequest.getRemark()).description(iceRepairRequest.getDescription())
                .province(iceRepairRequest.getProvince()).city(iceRepairRequest.getCity()).area(iceRepairRequest.getArea())
                .phoneAreaCode(phoneAreaCode).requireServiceDate(iceRepairRequest.getRequireServiceDate()).bookingRange(iceRepairRequest.getBookingRange())
                .finishStatus("未完成").createdTime(new Date()).build();
        iceRepairRequest.setSaleOrderId(orderNumber);
        log.info("请求海信创建报修单,{}", JSONObject.toJSONString(iceRepairRequest));
        WbSiteRequestVO wbSiteRequestVO = iceRepairRequest.convertToWbSite();
        WebSite webSite = new WebSite();
        WebSitePortType httpEndpoint = webSite.getWebSiteHttpSoap12Endpoint();
        WbSiteResponseVO responseVO = httpEndpoint.getWBSite(wbSiteRequestVO);
        String value = responseVO.getResultCode().getValue();
        if (!"1".equals(value)) {
            msg = responseVO.getResultMsg().getValue();
            return new CommonResponse(Constants.API_CODE_FAIL, "海信" + msg);
        }
        this.baseMapper.insert(repairOrder);
        return new CommonResponse(Constants.API_CODE_SUCCESS, null);
    }

    @Override
    public IPage<IceRepairOrder> findByPage(IceRepairOrderMsg msg) {
        LambdaQueryWrapper<IceRepairOrder> wrapper = fillWrapper(msg);
        return this.baseMapper.selectPage(msg, wrapper);
    }

    @Override
    public LambdaQueryWrapper<IceRepairOrder> fillWrapper(IceRepairOrderMsg msg) {
        LambdaQueryWrapper<IceRepairOrder> wrapper = Wrappers.<IceRepairOrder>lambdaQuery();
        if (StringUtils.isNotBlank(msg.getOrderNumber())) {
            wrapper.eq(IceRepairOrder::getOrderNumber, msg.getOrderNumber());
        }
        if (StringUtils.isNotBlank(msg.getCustomerName())) {
            wrapper.like(IceRepairOrder::getCustomerName, msg.getCustomerName());
        }
        if (StringUtils.isNotBlank(msg.getAssetId())) {
            wrapper.eq(IceRepairOrder::getAssetId, msg.getAssetId());
        }
        if (Objects.nonNull(msg.getStatus())) {
            wrapper.eq(IceRepairOrder::getStatus, msg.getStatus());
        }
        if (Objects.nonNull(msg.getHeadquartersDeptId())) {
            wrapper.eq(IceRepairOrder::getHeadquartersDeptId, msg.getHeadquartersDeptId());
        }
        if (Objects.nonNull(msg.getBusinessDeptId())) {
            wrapper.eq(IceRepairOrder::getBusinessDeptId, msg.getBusinessDeptId());
        }
        if (Objects.nonNull(msg.getRegionDeptId())) {
            wrapper.eq(IceRepairOrder::getRegionDeptId, msg.getRegionDeptId());
        }
        if (Objects.nonNull(msg.getServiceDeptId())) {
            wrapper.eq(IceRepairOrder::getServiceDeptId, msg.getServiceDeptId());
        }
        if (Objects.nonNull(msg.getGroupDeptId())) {
            wrapper.eq(IceRepairOrder::getGroupDeptId, msg.getGroupDeptId());
        }
        wrapper.orderByDesc(IceRepairOrder::getCreatedTime);
        return wrapper;
    }

    @Override
    public CommonResponse<Void> sendExportMsg(IceRepairOrderMsg reportMsg) {
        // 获取当前用户相关信息
        UserManageVo userManageVo = FeignResponseUtil.getFeignData(feignUserClient.getSessionUserInfo());
        String key = String.format("%s%s", RedisConstant.ICE_BOX_REPAIR_ORDER_KEY, userManageVo.getSessionUserInfoVo().getId());
//        if (null != jedis.get(key)) {
//            return new CommonResponse<>(Constants.API_CODE_FAIL, "请求导出操作频繁，请稍候操作");
//        }
        LambdaQueryWrapper<IceRepairOrder> wrapper = fillWrapper(reportMsg);
        Integer count = Optional.ofNullable(this.selectByExportCount(wrapper)).orElse(0);
        if (0 == count) {
            return new CommonResponse<>(Constants.API_CODE_FAIL, "暂无可下载数据");
        }
        // 生成下载任务
        Integer recordsId = FeignResponseUtil.getFeignData(feignExportRecordsClient.createExportRecords(userManageVo.getSessionUserInfoVo().getId(),
                userManageVo.getSessionUserInfoVo().getRealname(), JSON.toJSONString(reportMsg), "冰柜报修订单-导出"));

        //发送mq消息,同步申请数据到报表
        CompletableFuture.runAsync(() -> {
            reportMsg.setRecordsId(recordsId);
            rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.iceRepairOrderKey, reportMsg);
        }, ExecutorServiceFactory.getInstance());
        // 三分钟间隔
        jedis.set(key, "ex", 300, TimeUnit.SECONDS);

        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }

    @Override
    public Integer selectByExportCount(LambdaQueryWrapper<IceRepairOrder> wrapper) {
        return this.count(wrapper);
    }


    @Override
    public List<IceRepairOrder> getOrders(String customerNumber) {
        LambdaQueryWrapper<IceRepairOrder> queryWrapper = Wrappers.<IceRepairOrder>lambdaQuery();
        queryWrapper.eq(IceRepairOrder::getCustomerNumber, customerNumber);
        return this.baseMapper.selectList(queryWrapper);
    }

    @Override
    public IceRepairOrderVO getDetail(String orderNumber) {
        IceRepairOrderVO iceRepairOrderVO = new IceRepairOrderVO();
        LambdaQueryWrapper<IceRepairOrder> queryWrapper = Wrappers.<IceRepairOrder>lambdaQuery();
        queryWrapper.eq(IceRepairOrder::getOrderNumber, orderNumber);
        IceRepairOrder order = this.baseMapper.selectOne(queryWrapper);
        BeanUtils.copyProperties(order, iceRepairOrderVO);
        IceBox iceBox = iceBoxService.getById(order.getBoxId());
        iceRepairOrderVO.setChestName(iceBox.getChestName());
        iceRepairOrderVO.setBrandName(iceBox.getBrandName());
        iceRepairOrderVO.setChestNorm(iceBox.getChestNorm());
        return iceRepairOrderVO;
    }

    @Override
    public Integer getUnfinishOrderCount(Integer boxId) {
        LambdaQueryWrapper<IceRepairOrder> queryWrapper = Wrappers.<IceRepairOrder>lambdaQuery();
        queryWrapper.eq(IceRepairOrder::getBoxId, boxId);
        return this.baseMapper.selectCount(queryWrapper);
    }

    @Transactional(rollbackFor = Exception.class, transactionManager = "transactionManager")
    @Override
    public CommonResponse changeStatus(IceRepairStatusRequest request) {
        LambdaQueryWrapper<IceRepairOrder> queryWrapper = Wrappers.<IceRepairOrder>lambdaQuery();
        queryWrapper.eq(IceRepairOrder::getOrderNumber, request.getOrderNumber());
        IceRepairOrder iceRepairOrder = this.baseMapper.selectOne(queryWrapper);
        if(Objects.isNull(iceRepairOrder)){
            return new CommonResponse(Constants.API_CODE_FAIL, null, "该订单不存在");
        }
        iceRepairOrder.setStatus(request.getStatus()).setFinishStatus(request.getFinishStatus())
        .setAcceptTime(request.getAcceptTime()).setCause(request.getCause()).setFinishTime(request.getFinishTime())
        .setFactServiceMethod(request.getFactServiceMethod()).setFactServiceType(request.getFactServiceType())
        .setEngineer(request.getEngineer()).setServiceProviderCode(request.getServiceProviderCode())
        .setServiceProviderName(request.getServiceProviderName()).setFeedback(request.getFeedback())
        .setRepairMethod(request.getRepairMethod()).setResult(request.getResult()).setUpdatedTime(new Date());
        this.baseMapper.updateById(iceRepairOrder);
        return new CommonResponse(Constants.API_CODE_SUCCESS, null);
    }
}