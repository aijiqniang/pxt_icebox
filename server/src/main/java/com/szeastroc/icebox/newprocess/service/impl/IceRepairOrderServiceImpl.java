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
import com.szeastroc.icebox.newprocess.dao.IceBackApplyDao;
import com.szeastroc.icebox.newprocess.dao.IceBoxExtendDao;
import com.szeastroc.icebox.newprocess.dao.IcePutApplyRelateBoxDao;
import com.szeastroc.icebox.newprocess.dao.IceRepairOrderDao;
import com.szeastroc.icebox.newprocess.entity.IceBackApply;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.entity.IceBoxExtend;
import com.szeastroc.icebox.newprocess.entity.IcePutApplyRelateBox;
import com.szeastroc.icebox.newprocess.entity.IceRepairOrder;
import com.szeastroc.icebox.newprocess.enums.SupplierTypeEnum;
import com.szeastroc.icebox.newprocess.service.IceBoxService;
import com.szeastroc.icebox.newprocess.service.IceRepairOrderService;
import com.szeastroc.icebox.newprocess.vo.IceRepairOrderVO;
import com.szeastroc.icebox.newprocess.vo.request.IceRepairRequest;
import com.szeastroc.icebox.newprocess.vo.request.IceRepairStatusRequest;
import com.szeastroc.icebox.newprocess.webservice.JaxbUtil;
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

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * ?????????????????????(IceRepairOrder)??????????????????
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
    @Resource
    private IcePutApplyRelateBoxDao icePutApplyRelateBoxDao;
    @Resource
    private IceBackApplyDao iceBackApplyDao;
    @Resource
    private IceBoxExtendDao iceBoxExtendDao;

    @Transactional(rollbackFor = Exception.class, transactionManager = "transactionManager")
    @RedisLock(key = "#iceRepairRequest.boxId")
    @Override
    public CommonResponse createOrder(IceRepairRequest iceRepairRequest) {
        Integer count = this.getUnfinishOrderCount(iceRepairRequest.getBoxId());
        if (count > 0) {
            return new CommonResponse(Constants.API_CODE_FAIL, null, "??????????????????????????????????????????????????????");
        }
        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(iceRepairRequest.getBoxId());
        IcePutApplyRelateBox relateBox = icePutApplyRelateBoxDao.selectOne(
                Wrappers.<IcePutApplyRelateBox>lambdaQuery()
                .eq(IcePutApplyRelateBox::getApplyNumber, iceBoxExtend.getLastApplyNumber())
                .eq(IcePutApplyRelateBox::getBoxId, iceRepairRequest.getBoxId()));
        if (relateBox != null) {
            IceBackApply iceBackApply = iceBackApplyDao.selectOne(Wrappers.<IceBackApply>lambdaQuery().eq(IceBackApply::getOldPutId, relateBox.getId())
                    .ne(IceBackApply::getExamineStatus, 3));
            if(Objects.nonNull(iceBackApply)){
                return new CommonResponse(Constants.API_CODE_FAIL, null, "??????????????????????????????");
            }
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
            if(Objects.isNull(store)){
                return new CommonResponse(Constants.API_CODE_FAIL, null, "???????????????????????????");
            }
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
            if(Objects.isNull(supplier)){
                return new CommonResponse(Constants.API_CODE_FAIL, null, "??????????????????????????????");
            }
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
        String orderNumber = "REP" + new DateTime().toString("yyyyMMddHHmmss") + RandomUtil.randomNumbers(8);
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
                .finishStatus("?????????").createdTime(new Date()).build();
        iceRepairRequest.setSaleOrderId(orderNumber);
        log.info("???????????????????????????,{}", JSONObject.toJSONString(iceRepairRequest));
        WbSiteRequestVO wbSiteRequestVO = iceRepairRequest.convertToWbSite();
        WebSite webSite = new WebSite();
        WebSitePortType httpEndpoint = webSite.getWebSiteHttpSoap12Endpoint();
        WbSiteResponseVO responseVO = httpEndpoint.getWBSite(wbSiteRequestVO);
        try {
            log.info("???????????????????????????,{}", JaxbUtil.convertToXml(wbSiteRequestVO));
            log.info("???????????????????????????,{}", JaxbUtil.convertToXml(responseVO));
        } catch (Exception e) {
            log.info("???????????????????????????????????????");
        }
        String value = responseVO.getResultCode().getValue();
        if (!"1".equals(value)) {
            msg = responseVO.getResultMsg().getValue();
            return new CommonResponse(Constants.API_CODE_FAIL, "??????" + msg);
        }
        this.baseMapper.insert(repairOrder);
        return new CommonResponse(Constants.API_CODE_SUCCESS, null);
    }

    public static void main(String[] args) throws Exception{
        String json = "{\"area\":\"?????????\",\"areaCode\":\"441907\",\"assetId\":\"2100518103426\",\"bookingRange\":\"??????\",\"boxId\":64985,\"city\":\"?????????\",\"cityCode\":\"441900\",\"customerAddress\":\"???????????????????????????100???(????????????)\",\"customerName\":\"????????????????????????????????????\",\"customerNumber\":\"C02431183\",\"customerType\":5,\"description\":\"?????????\",\"linkMan\":\"??????\",\"linkMobile\":\"18046909386\",\"modelId\":1,\"modelName\":\"SC-518WYSL/HP\",\"originFlag\":\"DP\",\"phoneAreaCode\":\"0769\",\"province\":\"?????????\",\"provinceCode\":\"440000\",\"psnAccount\":\"website\",\"psnPwd\":\"Aa666666\",\"remark\":\"???????????????\",\"requireServiceDate\":\"2021-04-14\",\"saleOrderId\":\"REP2021041411351166291548\",\"serviceTypeId\":\"WX\"}";
        IceRepairRequest iceRepairRequest = JSONObject.parseObject(json, IceRepairRequest.class);
        WbSiteRequestVO wbSiteRequestVO = iceRepairRequest.convertToWbSite();
        System.out.println(JaxbUtil.convertToXml(wbSiteRequestVO));
    }

    @Override
    public IPage<IceRepairOrder> findByPage(IceRepairOrderMsg msg) {
        LambdaQueryWrapper<IceRepairOrder> wrapper = fillWrapper(msg);
        IPage<IceRepairOrder> page = this.baseMapper.selectPage(msg, wrapper);
        page.convert(one->{
            if(StringUtils.isNotBlank(one.getRemark())){
                one.setRemark("null".equals(one.getRemark().trim())?"":one.getRemark().trim());
            }
            if(StringUtils.isNotBlank(one.getFeedback())){
                one.setFeedback("null".equals(one.getFeedback().trim())?"":one.getFeedback().trim());
            }
            if(StringUtils.isNotBlank(one.getCustomerNumber())){
                StoreInfoDtoVo storeInfoDtoVo = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(one.getCustomerNumber()));
                if(storeInfoDtoVo != null && StringUtils.isNotEmpty(storeInfoDtoVo.getMerchantNumber())){
                    one.setMerchantNumber(storeInfoDtoVo.getMerchantNumber());
                }
            }
            return one;
        });
        return page;
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
        if (StringUtils.isNotBlank(msg.getFinishStatus())) {
            wrapper.eq(IceRepairOrder::getFinishStatus, msg.getFinishStatus());
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
        if(StringUtils.isNotBlank(msg.getStartTime())){
            wrapper.ge(IceRepairOrder::getCreatedTime,msg.getStartTime());
        }
        if(StringUtils.isNotBlank(msg.getEndTime())){
            wrapper.le(IceRepairOrder::getCreatedTime,msg.getEndTime());
        }
        wrapper.orderByDesc(IceRepairOrder::getCreatedTime);
        return wrapper;
    }

    @Override
    public CommonResponse<Void> sendExportMsg(IceRepairOrderMsg reportMsg) {
        // ??????????????????????????????
        UserManageVo userManageVo = FeignResponseUtil.getFeignData(feignUserClient.getSessionUserInfo());
        String key = String.format("%s%s", RedisConstant.ICE_BOX_REPAIR_ORDER_KEY,userManageVo.getSessionUserInfoVo().getId());
        if (null != jedis.get(key)) {
            return new CommonResponse<>(Constants.API_CODE_FAIL, "??????????????????????????????????????????");
        }
        LambdaQueryWrapper<IceRepairOrder> wrapper = fillWrapper(reportMsg);
        Integer count = Optional.ofNullable(this.selectByExportCount(wrapper)).orElse(0);
        if (0 == count) {
            return new CommonResponse<>(Constants.API_CODE_FAIL, "?????????????????????");
        }
        // ??????????????????
        Integer recordsId = FeignResponseUtil.getFeignData(feignExportRecordsClient.createExportRecords(userManageVo.getSessionUserInfoVo().getId(),
                userManageVo.getSessionUserInfoVo().getRealname(), JSON.toJSONString(reportMsg), "??????????????????-??????"));

        //??????mq??????,???????????????????????????
        CompletableFuture.runAsync(() -> {
            reportMsg.setRecordsId(recordsId);
            rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.iceRepairOrderKey, reportMsg);
        }, ExecutorServiceFactory.getInstance());
        // ???????????????
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
        queryWrapper.eq(IceRepairOrder::getBoxId, boxId).notIn(IceRepairOrder::getStatus,7,8,9,10,20,50,51,52);;
        return this.baseMapper.selectCount(queryWrapper);
    }

    @Transactional(rollbackFor = Exception.class, transactionManager = "transactionManager")
    @Override
    public CommonResponse changeStatus(IceRepairStatusRequest request) {
        LambdaQueryWrapper<IceRepairOrder> queryWrapper = Wrappers.<IceRepairOrder>lambdaQuery();
        queryWrapper.eq(IceRepairOrder::getOrderNumber, request.getOrderNumber());
        IceRepairOrder iceRepairOrder = this.baseMapper.selectOne(queryWrapper);
        if(Objects.isNull(iceRepairOrder)){
            return new CommonResponse(Constants.API_CODE_FAIL, null, "??????????????????");
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