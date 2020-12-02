package com.szeastroc.icebox.newprocess.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.exception.NormalOptionException;
import com.szeastroc.common.utils.ExecutorServiceFactory;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.commondb.config.redis.JedisClient;
import com.szeastroc.customer.client.FeignStoreClient;
import com.szeastroc.customer.client.FeignSupplierClient;
import com.szeastroc.customer.common.vo.StoreInfoDtoVo;
import com.szeastroc.customer.common.vo.SubordinateInfoVo;
import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.icebox.enums.ExamineStatusEnum;
import com.szeastroc.icebox.newprocess.consumer.common.IceBoxExamineExceptionReportMsg;
import com.szeastroc.icebox.newprocess.consumer.enums.OperateTypeEnum;
import com.szeastroc.icebox.newprocess.dao.IceBoxDao;
import com.szeastroc.icebox.newprocess.dao.IceBoxExamineExceptionReportDao;
import com.szeastroc.icebox.newprocess.dao.IceBoxExtendDao;
import com.szeastroc.icebox.newprocess.dao.IceExamineDao;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.entity.IceBoxExamineExceptionReport;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.entity.IceBoxExtend;
import com.szeastroc.icebox.newprocess.entity.IceExamine;
import com.szeastroc.icebox.newprocess.enums.*;
import com.szeastroc.icebox.newprocess.service.IceBoxService;
import com.szeastroc.icebox.newprocess.service.IceExamineService;
import com.szeastroc.icebox.newprocess.service.IcePutApplyService;
import com.szeastroc.icebox.newprocess.vo.IceExamineVo;
import com.szeastroc.icebox.newprocess.vo.request.IceExamineRequest;
import com.szeastroc.icebox.oldprocess.dao.IceEventRecordDao;
import com.szeastroc.icebox.oldprocess.entity.IceEventRecord;
import com.szeastroc.user.client.FeignCacheClient;
import com.szeastroc.user.client.FeignDeptClient;
import com.szeastroc.user.client.FeignDeptRuleClient;
import com.szeastroc.user.client.FeignUserClient;
import com.szeastroc.user.common.session.MatchRuleVo;
import com.szeastroc.user.common.vo.SessionDeptInfoVo;
import com.szeastroc.user.common.vo.SessionUserInfoVo;
import com.szeastroc.user.common.vo.SimpleUserInfoVo;
import com.szeastroc.user.common.vo.SysRuleIceDetailVo;
import com.szeastroc.visit.client.FeignBacklogClient;
import com.szeastroc.visit.client.FeignExamineClient;
import com.szeastroc.visit.common.IceBoxExamineModel;
import com.szeastroc.visit.common.SessionExamineCreateVo;
import com.szeastroc.visit.common.SessionExamineVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
public class IceExamineServiceImpl extends ServiceImpl<IceExamineDao, IceExamine> implements IceExamineService {

    @Autowired
    private IceExamineDao iceExamineDao;
    @Autowired
    private FeignUserClient feignUserClient;
    @Autowired
    private FeignStoreClient feignStoreClient;
    @Autowired
    private IceBoxDao iceBoxDao;
    @Autowired
    private IceBoxExtendDao iceBoxExtendDao;
    @Autowired
    private IceEventRecordDao iceEventRecordDao;
    @Autowired
    private FeignSupplierClient feignSupplierClient;
    @Autowired
    private FeignDeptClient feignDeptClient;
    @Autowired
    private FeignExamineClient feignExamineClient;
    @Autowired
    private FeignBacklogClient feignBacklogClient;
    @Autowired
    private FeignDeptRuleClient feignDeptRuleClient;
    @Autowired
    private FeignCacheClient feignCacheClient;
    @Autowired
    private JedisClient jedisClient;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private IceBoxExamineExceptionReportDao iceBoxExamineExceptionReportDao;
    @Autowired
    private IceBoxService iceBoxService;

    @Override
    @Transactional(rollbackFor = Exception.class, value = "transactionManager")
    public void doExamine(IceExamine iceExamine) {
        if (!iceExamine.validate()) {
            throw new NormalOptionException(Constants.API_CODE_FAIL, "参数不完整");
        }

        Integer iceBoxId = iceExamine.getIceBoxId();
        IceBoxExtend select = iceBoxExtendDao.selectById(iceBoxId);
        String assetId = select.getAssetId();
        Integer openTotal = select.getOpenTotal();
        iceExamine.setOpenCloseCount(openTotal);

        IceEventRecord iceEventRecord = iceEventRecordDao.selectOne(Wrappers.<IceEventRecord>lambdaQuery().eq(IceEventRecord::getAssetId, assetId).le(IceEventRecord::getOccurrenceTime, new Date()).last("limit 1"));

        if (iceEventRecord != null) {
            Double temperature = iceEventRecord.getTemperature();
            String lat = iceEventRecord.getLat();
            String lng = iceEventRecord.getLng();
            String detailAddress = iceEventRecord.getDetailAddress();
            iceExamine.setTemperature(temperature);
            iceExamine.setLatitude(lat);
            iceExamine.setLongitude(lng);
            iceExamine.setGpsAddress(detailAddress);
        }
        iceExamineDao.insert(iceExamine);

        IceBox iceBox = iceBoxDao.selectById(iceBoxId);
        //OA审批才能变更状态,由异常状态变更为正常状态不需要提报OA审批
        if(ExamineStatusEnum.IS_PASS.getStatus().equals(iceExamine.getExaminStatus())){
            if(IceBoxEnums.StatusEnum.NORMAL.getType().equals(iceExamine.getIceStatus())){
                iceBox.setStatus(IceBoxEnums.StatusEnum.NORMAL.getType());
            }
            if(IceBoxEnums.StatusEnum.SCRAP.getType().equals(iceExamine.getIceStatus())){
                iceBox.setStatus(IceBoxEnums.StatusEnum.IS_SCRAPING.getType());
            }
            if(IceBoxEnums.StatusEnum.LOSE.getType().equals(iceExamine.getIceStatus())){
                iceBox.setStatus(IceBoxEnums.StatusEnum.IS_LOSEING.getType());
            }
            iceBox.setUpdatedTime(new Date());
            iceBoxDao.updateById(iceBox);
        }

        Date now = new Date();
        Integer iceExamineId = iceExamine.getId();
        IceBoxExtend iceBoxExtend = new IceBoxExtend();
        iceBoxExtend.setLastExamineId(iceExamineId);
        iceBoxExtend.setLastExamineTime(now);

        iceBoxExtendDao.update(iceBoxExtend, Wrappers.<IceBoxExtend>lambdaUpdate().eq(IceBoxExtend::getId, iceBoxId));

        //发送mq消息,同步申请数据到报表
        CompletableFuture.runAsync(() -> {
            Integer examineExceptionStatus = ExamineExceptionStatusEnums.is_reporting.getStatus();
            if(iceExamine.getExaminStatus().equals(ExamineStatus.PASS_EXAMINE.getStatus())){
                examineExceptionStatus = ExamineExceptionStatusEnums.allow_report.getStatus();
            }
            if(iceExamine.getExaminStatus().equals(ExamineStatus.REJECT_EXAMINE.getStatus())){
                examineExceptionStatus = ExamineExceptionStatusEnums.is_unpass.getStatus();
            }
            buildReportAndSendMq(iceExamine,examineExceptionStatus,now, null);
        }, ExecutorServiceFactory.getInstance());
    }

    private void buildReportAndSendMq(IceExamine iceExamine, Integer status, Date now, Integer updateBy) {
        try {
            IceBoxExamineExceptionReport isExsit = iceBoxExamineExceptionReportDao.selectOne(Wrappers.<IceBoxExamineExceptionReport>lambdaQuery().eq(IceBoxExamineExceptionReport::getExamineNumber, iceExamine.getExamineNumber()));
            IceBoxExamineExceptionReportMsg report = new IceBoxExamineExceptionReportMsg();
            if(isExsit != null){
                if(updateBy != null){
                    SimpleUserInfoVo userInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findSimpleUserById(updateBy));
                    report.setExamineUserId(updateBy);
                    report.setExamineTime(now);
                    if(userInfoVo != null){
                        report.setExamineUserName(userInfoVo.getRealname());
                    }
                }
                report.setExamineNumber(iceExamine.getExamineNumber());
                report.setStatus(status);
                report.setOperateType(OperateTypeEnum.UPDATE.getType());
            }else {
                IceBox iceBox = iceBoxDao.selectById(iceExamine.getIceBoxId());
                report.setExamineNumber(iceExamine.getExamineNumber());
                report.setStatus(status);
                Map<Integer, SessionDeptInfoVo> deptInfoVoMap = FeignResponseUtil.getFeignData(feignCacheClient.getFiveLevelDept(iceBox.getDeptId()));
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
                report.setToOaType(iceExamine.getIceStatus());
                report.setDepositMoney(iceBox.getDepositMoney());
                report.setIceBoxModelId(iceBox.getModelId());
                report.setIceBoxModelName(iceBox.getModelName());
                report.setIceBoxAssetId(iceBox.getAssetId());
                SubordinateInfoVo supplier = FeignResponseUtil.getFeignData(feignSupplierClient.findSupplierBySupplierId(iceBox.getSupplierId()));
                report.setSupplierId(iceBox.getSupplierId());
                if (supplier != null) {
                    report.setSupplierNumber(supplier.getNumber());
                    report.setSupplierName(supplier.getName());
                }
                report.setPutCustomerNumber(iceExamine.getStoreNumber());
                if(iceExamine.getStoreNumber().startsWith("C0")){
                    StoreInfoDtoVo store = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(iceExamine.getStoreNumber()));
                    if(store != null){
                        report.setPutCustomerName(store.getStoreName());
                        report.setPutCustomerType(SupplierTypeEnum.IS_STORE.getType());
                    }
                }else {
                    SubordinateInfoVo customer = FeignResponseUtil.getFeignData(feignSupplierClient.findByNumber(iceExamine.getStoreNumber()));
                    if(customer != null){
                        report.setPutCustomerName(customer.getName());
                        report.setPutCustomerType(customer.getSupplierType());
                    }
                }
                SimpleUserInfoVo userInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findUserById(iceExamine.getCreateBy()));
                report.setSubmitterId(iceExamine.getCreateBy());
                if (userInfoVo != null) {
                    report.setSubmitterName(userInfoVo.getRealname());
                    report.setSubmitterPosion(userInfoVo.getPosion());
                }
                report.setSubmitTime(now);
                report.setOperateType(OperateTypeEnum.INSERT.getType());
            }
            log.info("发送巡检信息到巡检报表——》【{}】",JSON.toJSONString(report));
            rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.iceboxExceptionReportKey, report);
        } catch (Exception e) {
            log.info("捕获的buildReportAndSendMq异常信息-->[{}],具体信息-->[{}]",JSON.toJSONString(e), e.getMessage());
        }
    }

    @Override
    public IPage<IceExamineVo> findExamine(IceExamineRequest iceExamineRequest) {
        LambdaQueryWrapper<IceExamine> wrapper = Wrappers.<IceExamine>lambdaQuery();
        wrapper.orderByDesc(IceExamine::getCreateTime);

        Integer iceBoxId = iceExamineRequest.getIceBoxId();

        if (iceBoxId == null) {
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }

        wrapper.eq(IceExamine::getIceBoxId, iceBoxId);

        Integer createBy = iceExamineRequest.getCreateBy();

        if (createBy == null) {
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }

        wrapper.eq(IceExamine::getCreateBy, createBy);

        String storeNumber = iceExamineRequest.getStoreNumber();
        if (StringUtils.isBlank(storeNumber)) {
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }

        wrapper.eq(IceExamine::getStoreNumber, storeNumber);
        Date createTime = iceExamineRequest.getCreateTime();
        if (createTime != null) {
            Date date = new Date(createTime.getTime());
            date.setTime(date.getTime() + 24 * 60 * 60 * 1000);
            wrapper.ge(IceExamine::getCreateTime, createTime).le(IceExamine::getCreateTime, date);
        }

        IPage<IceExamine> iPage = iceExamineDao.selectPage(iceExamineRequest, wrapper);


        List<IceExamine> records = iPage.getRecords();

        IPage<IceExamineVo> page = new Page<>();

        if (CollectionUtil.isNotEmpty(records)) {

            List<Integer> collect = records.stream().map(IceExamine::getCreateBy).distinct().collect(Collectors.toList());

            Map<Integer, SessionUserInfoVo> map = FeignResponseUtil.getFeignData(feignUserClient.getSessionUserInfoVoByIds(collect));

            StoreInfoDtoVo storeInfoDtoVo = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(storeNumber));

            String storeName = "";
            if (null != storeInfoDtoVo && StringUtils.isNotBlank(storeInfoDtoVo.getStoreNumber())) {
                storeName = storeInfoDtoVo.getStoreName();
            } else {
                SubordinateInfoVo subordinateInfoVo = FeignResponseUtil.getFeignData(feignSupplierClient.findByNumber(storeNumber));
                if (null != subordinateInfoVo && StringUtils.isNotBlank(subordinateInfoVo.getNumber())) {
                    storeName = subordinateInfoVo.getName();
                }
            }

            String finalStoreName = storeName;
            page = iPage.convert(iceExamine -> {

                SessionUserInfoVo sessionUserInfoVo = map.get(createBy);

                return iceExamine.convert(iceExamine, sessionUserInfoVo.getRealname(), finalStoreName, storeNumber);

//                return IceExamineVo.builder()
//                        .id(iceExamine.getId())
//                        .createBy(iceExamine.getCreateBy())
//                        .createName(sessionUserInfoVo.getRealname())
//                        .displayImage(iceExamine.getDisplayImage())
//                        .exteriorImage(iceExamine.getExteriorImage())
//                        .createTime(iceExamine.getCreateTime())
//                        .storeName(storeInfoDtoVo.getStoreName())
//                        .storeNumber(storeNumber)
//                        .iceBoxId(iceExamine.getIceBoxId())
//                        .latitude(iceExamine.getLatitude())
//                        .longitude(iceExamine.getLongitude())
//                        .temperature(iceExamine.getTemperature())
//                        .openCloseCount(iceExamine.getOpenCloseCount())
//                        .build();

            });
        }
        return page;
    }


    @Override
    public IceExamineVo findOneExamine(IceExamineRequest iceExamineRequest) {
        String storeNumber = iceExamineRequest.getStoreNumber();
        Integer createBy = iceExamineRequest.getCreateBy();
        Integer type = iceExamineRequest.getType();
        Integer iceBoxId = iceExamineRequest.getIceBoxId();

        if (iceBoxId == null || createBy == null || StringUtils.isBlank(storeNumber) || type == null) {
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }

        LambdaQueryWrapper<IceExamine> wrapper = Wrappers.<IceExamine>lambdaQuery();

        wrapper.eq(IceExamine::getCreateBy, createBy).eq(IceExamine::getStoreNumber, storeNumber).eq(IceExamine::getIceBoxId, iceBoxId).last("limit 1");

        if (type.equals(ExamineEnums.ExamineTime.FIRST_TIME.getType())) {
            // 第一次巡检

            wrapper.orderByAsc(IceExamine::getCreateTime);
        } else {
            // 最后一次巡检
            wrapper.orderByDesc(IceExamine::getCreateTime);
        }

        IceExamine iceExamine = iceExamineDao.selectOne(wrapper);

        IceExamineVo iceExamineVo;

        if (iceExamine != null) {

            ArrayList<Integer> list = new ArrayList<>();
            list.add(createBy);
            Map<Integer, SessionUserInfoVo> map = FeignResponseUtil.getFeignData(feignUserClient.getSessionUserInfoVoByIds(list));
            SessionUserInfoVo sessionUserInfoVo = map.get(createBy);
            StoreInfoDtoVo storeInfoDtoVo = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(storeNumber));
            String storeName = "";
            if (null != storeInfoDtoVo && StringUtils.isNotBlank(storeInfoDtoVo.getStoreNumber())) {
                storeName = storeInfoDtoVo.getStoreName();
            } else {
                SubordinateInfoVo subordinateInfoVo = FeignResponseUtil.getFeignData(feignSupplierClient.findByNumber(storeNumber));
                if (null != subordinateInfoVo && StringUtils.isNotBlank(subordinateInfoVo.getNumber())) {
                    storeName = subordinateInfoVo.getName();
                }
            }
            iceExamineVo = iceExamine.convert(iceExamine, sessionUserInfoVo.getRealname(), storeName, storeNumber);

//            iceExamineVo = IceExamineVo.builder()
//                    .id(iceExamine.getId())
//                    .createBy(iceExamine.getCreateBy())
//                    .createName(sessionUserInfoVo.getRealname())
//                    .displayImage(iceExamine.getDisplayImage())
//                    .exteriorImage(iceExamine.getExteriorImage())
//                    .createTime(iceExamine.getCreateTime())
//                    .storeName(storeInfoDtoVo.getStoreName())
//                    .storeNumber(storeNumber)
//                    .iceBoxId(iceExamine.getIceBoxId())
//                    .latitude(iceExamine.getLatitude())
//                    .longitude(iceExamine.getLongitude())
//                    .temperature(iceExamine.getTemperature())
//                    .openCloseCount(iceExamine.getOpenCloseCount())
//                    .build();
        } else {
            iceExamineVo = null;
        }
        return iceExamineVo;
    }

    @Override
    public Map<String, Object> doExamineNew(IceExamineVo iceExamineVo) {

        IceBox iceBox = iceBoxDao.selectById(iceExamineVo.getIceBoxId());
        if(iceBox == null){
            throw new NormalOptionException(Constants.API_CODE_FAIL, "巡检的冰柜不存在！");
        }
        String examineNumber = UUID.randomUUID().toString().replace("-", "");
        iceExamineVo.setExamineNumber(examineNumber);
        IceExamine iceExamine = new IceExamine();
        BeanUtils.copyProperties(iceExamineVo,iceExamine);
        iceExamine.setExamineNumber(examineNumber);
        iceExamine.setIceStatus(iceExamineVo.getIceExamineStatus());
        Map<String, Object> map = new HashMap<>();
        map.put("examineNumber", examineNumber);
        //冰柜状态是正常，巡检也是正常，不需要审批
        if(IceBoxEnums.StatusEnum.NORMAL.getType().equals(iceExamineVo.getIceStatus()) && IceBoxEnums.StatusEnum.NORMAL.getType().equals(iceExamineVo.getIceExamineStatus())){
            iceExamine.setExaminStatus(ExamineStatus.PASS_EXAMINE.getStatus());
            map.put("isCheck", CommonIsCheckEnum.IS_CHECK.getStatus());
        }
        MatchRuleVo matchRuleVo = new MatchRuleVo();
        //冰柜状态是报废，巡检是正常，需要走与报废相同的审批
        if(IceBoxEnums.StatusEnum.SCRAP.getType().equals(iceExamineVo.getIceStatus()) && IceBoxEnums.StatusEnum.NORMAL.getType().equals(iceExamineVo.getIceExamineStatus())){
            matchRuleVo.setOpreateType(5);
            map = createExamineCheckProcess(iceExamineVo,map,matchRuleVo, iceExamine);
            iceBox.setStatus(IceBoxEnums.StatusEnum.IS_NORMALING_UNPASS.getType());
        }
        //冰柜状态是遗失，巡检是正常，需要走与遗失相同的审批
        if(IceBoxEnums.StatusEnum.LOSE.getType().equals(iceExamineVo.getIceStatus()) && IceBoxEnums.StatusEnum.NORMAL.getType().equals(iceExamineVo.getIceExamineStatus())){
            matchRuleVo.setOpreateType(6);
            map = createExamineCheckProcess(iceExamineVo,map,matchRuleVo, iceExamine);
            iceBox.setStatus(IceBoxEnums.StatusEnum.IS_NORMALING_UNPASS.getType());
        }
//        //冰柜状态是报修，巡检是正常，需要走与报修相同的审批   产品说报修没了，以后还要，所以保留代码
//        if(IceBoxEnums.StatusEnum.REPAIR.getType().equals(iceExamineVo.getIceStatus()) && IceBoxEnums.StatusEnum.NORMAL.getType().equals(iceExamineVo.getIceExamineStatus())){
//            matchRuleVo.setOpreateType(7);
//            map = createExamineCheckProcess(iceExamineVo,map,matchRuleVo);
//        }
        //冰柜状态不是报废，巡检是报废，需要走报废审批
        if(!IceBoxEnums.StatusEnum.SCRAP.getType().equals(iceExamineVo.getIceStatus()) && IceBoxEnums.StatusEnum.SCRAP.getType().equals(iceExamineVo.getIceExamineStatus())){
            matchRuleVo.setOpreateType(5);
            map = createExamineCheckProcess(iceExamineVo,map,matchRuleVo, iceExamine);
            iceBox.setStatus(IceBoxEnums.StatusEnum.IS_SCRAPING_UNPASS.getType());
        }

        //冰柜状态不是遗失，巡检是遗失，需要走遗失审批
        if(!IceBoxEnums.StatusEnum.LOSE.getType().equals(iceExamineVo.getIceStatus()) && IceBoxEnums.StatusEnum.LOSE.getType().equals(iceExamineVo.getIceExamineStatus())){
            matchRuleVo.setOpreateType(6);
            map = createExamineCheckProcess(iceExamineVo,map,matchRuleVo,iceExamine);
            iceBox.setStatus(IceBoxEnums.StatusEnum.IS_LOSEING_UNPASS.getType());
        }

        //冰柜状态不是报修，巡检是报修，需要走报修通知上级  产品说报修现在没了，以后还要，所以保留代码
//        if(!IceBoxEnums.StatusEnum.REPAIR.getType().equals(iceExamineVo.getIceStatus()) && IceBoxEnums.StatusEnum.REPAIR.getType().equals(iceExamineVo.getIceExamineStatus())){
//
////            SimpleUserInfoVo simpleUserInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findSimpleUserById(iceExamineVo.getCreateBy()));
//            Map<Integer, SessionUserInfoVo> sessionUserInfoMap = FeignResponseUtil.getFeignData(feignDeptClient.findLevelLeaderByDeptIdNew(iceExamineVo.getMarketAreaId()));
//            List<Integer> ids = new ArrayList<Integer>();
//            //获取上级部门领导
//            SessionUserInfoVo groupUser = new SessionUserInfoVo();
//            SessionUserInfoVo serviceUser = new SessionUserInfoVo();
//            Set<Integer> keySet = sessionUserInfoMap.keySet();
//            for (Integer key : keySet) {
//                SessionUserInfoVo userInfoVo = sessionUserInfoMap.get(key);
//                if(userInfoVo == null){
//                    continue;
//                }
//                if(DeptTypeEnum.GROUP.getType().equals(userInfoVo.getDeptType())){
//                    groupUser = userInfoVo;
//                    continue;
//                }
//                if(DeptTypeEnum.SERVICE.getType().equals(userInfoVo.getDeptType())){
//                    serviceUser = userInfoVo;
//                    continue;
//                }
//
//            }
//            if(groupUser.getId() != null){
//                ids.add(groupUser.getId());
//            }
//
//            if(serviceUser.getId() != null && !ids.contains(serviceUser.getId())){
//                ids.add(groupUser.getId());
//            }
//
//            if(CollectionUtil.isNotEmpty(ids)){
//                for(Integer id:ids){
//                    SessionVisitExamineBacklog backlog = new SessionVisitExamineBacklog();
//                    backlog.setBacklogName(iceExamineVo.getCreateName()+"冰柜报修通知信息");
//                    backlog.setCode(iceExamineVo.getAssetId());
//                    backlog.setExamineStatus(ExamineStatus.PASS_EXAMINE.getStatus());
//                    backlog.setExamineType(ExamineTypeEnum.ICEBOX_REPAIR.getType());
//                    backlog.setSendType(1);
//                    backlog.setSendUserId(id);
//                    backlog.setCreateBy(iceExamineVo.getCreateBy());
//                    feignBacklogClient.createBacklog(backlog);
//                }
//            }
//        }

        doExamine(iceExamine);
        //发送mq消息,同步申请数据到报表
//        CompletableFuture.runAsync(() -> {
//            buildReportAndSendMq(iceExamine,ExamineExceptionStatusEnums.is_reporting.getStatus(),new Date());
//        }, ExecutorServiceFactory.getInstance());
        iceBoxDao.updateById(iceBox);
        return map;
    }

    @Override
    public void dealIceExamineCheck(String redisKey, Integer status, Integer updateBy) {
        String str = jedisClient.get(redisKey);
        if(StringUtils.isBlank(str)){
            throw new NormalOptionException(Constants.API_CODE_FAIL, "审批失败,找不到冰柜巡检信息！");
        }
        IceBoxExamineModel iceBoxExamineModel = JSON.parseObject(str, IceBoxExamineModel.class);
        IceBox iceBox = iceBoxDao.selectOne(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getAssetId,iceBoxExamineModel.getAssetId()));
        if(iceBoxDao == null){
            throw new NormalOptionException(Constants.API_CODE_FAIL, "审批失败,找不到冰柜信息！");
        }
        //OA审批才能变更状态,由异常状态变更为正常状态不需要提报OA审批
        if(status.equals(ExamineStatusEnum.IS_PASS.getStatus())){
            if(IceBoxEnums.StatusEnum.NORMAL.getType().equals(iceBoxExamineModel.getIceExaminStatus())){
                iceBox.setStatus(IceBoxEnums.StatusEnum.NORMAL.getType());
            }
            if(IceBoxEnums.StatusEnum.SCRAP.getType().equals(iceBoxExamineModel.getIceExaminStatus())){
                iceBox.setStatus(IceBoxEnums.StatusEnum.IS_SCRAPING.getType());
            }
            if(IceBoxEnums.StatusEnum.LOSE.getType().equals(iceBoxExamineModel.getIceExaminStatus())){
                iceBox.setStatus(IceBoxEnums.StatusEnum.IS_LOSEING.getType());
            }
            iceBox.setUpdatedTime(new Date());
            iceBoxDao.updateById(iceBox);
        }
        IceExamine iceExamine = iceExamineDao.selectOne(Wrappers.<IceExamine>lambdaQuery().eq(IceExamine::getExamineNumber, iceBoxExamineModel.getExamineNumber()));
        if(iceExamine != null){
            iceExamine.setExaminStatus(status);
            iceExamine.setUpdateTime(new Date());
            iceExamineDao.updateById(iceExamine);
        }
        if(status.equals(ExamineStatusEnum.IS_DEFAULT.getStatus())){
            //发送mq消息,同步申请数据到报表
            CompletableFuture.runAsync(() -> {
                buildReportAndSendMq(iceExamine,ExamineExceptionStatusEnums.is_reporting.getStatus(),new Date(),updateBy);
            }, ExecutorServiceFactory.getInstance());
        }
        if(status.equals(ExamineStatusEnum.IS_PASS.getStatus())){
            //发送mq消息,同步申请数据到报表
            CompletableFuture.runAsync(() -> {
                buildReportAndSendMq(iceExamine,ExamineExceptionStatusEnums.allow_report.getStatus(),new Date(),updateBy);
            }, ExecutorServiceFactory.getInstance());
        }
        if(status.equals(ExamineStatusEnum.UN_PASS.getStatus())){
            iceBox.setStatus(iceBoxExamineModel.getIceStatus());
            iceBox.setUpdatedTime(new Date());
            iceBoxDao.updateById(iceBox);
            //发送mq消息,同步申请数据到报表
            CompletableFuture.runAsync(() -> {
                buildReportAndSendMq(iceExamine,ExamineExceptionStatusEnums.is_unpass.getStatus(),new Date(), updateBy);
            }, ExecutorServiceFactory.getInstance());
        }
//        IceExamine iceExamine = new IceExamine();
//        iceExamine.setIceBoxId(iceBox.getId());
//        iceExamine.setExamineNumber(iceBoxExamineModel.getExamineNumber());
//        iceExamine.setStoreNumber(iceBoxExamineModel.getStoreNumber());
//        iceExamine.setDisplayImage(iceBoxExamineModel.getDisplayImage());
//        iceExamine.setExteriorImage(iceBoxExamineModel.getExteriorImage());
//        iceExamine.setIceStatus(iceBoxExamineModel.getIceStatus());
//        iceExamine.setExaminMsg(iceBoxExamineModel.getExaminMsg());
//        iceExamine.setCreateBy(iceBoxExamineModel.getCreateBy());
//        iceExamine.setExaminStatus(status);
//        doExamine(iceExamine);
    }

    private Map<String, Object> createExamineCheckProcess(IceExamineVo iceExamineVo, Map<String, Object> map, MatchRuleVo matchRuleVo, IceExamine iceExamine) {

        matchRuleVo.setDeptId(iceExamineVo.getMarketAreaId());
        matchRuleVo.setType(2);
        SysRuleIceDetailVo ruleIceDetailVo = FeignResponseUtil.getFeignData(feignDeptRuleClient.matchIceRule(matchRuleVo));
        IceBox isExist = iceBoxDao.selectById(iceExamineVo.getIceBoxId());
        if(isExist == null ){
            throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败,找不到冰柜信息！");
        }
        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(iceExamineVo.getIceBoxId());
        if(iceBoxExtend == null ){
            throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败,找不到冰柜信息！");
        }
        SimpleUserInfoVo simpleUserInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findSimpleUserByIdAndDept(iceExamineVo.getCreateBy(),iceExamineVo.getUserMarketAreaId()));
        Map<Integer, SessionUserInfoVo> sessionUserInfoMap = FeignResponseUtil.getFeignData(feignDeptClient.findLevelLeaderByDeptIdNew(iceExamineVo.getUserMarketAreaId()));
        List<Integer> ids = new ArrayList<Integer>();
        //获取上级部门领导
        SessionUserInfoVo groupUser = new SessionUserInfoVo();
        SessionUserInfoVo serviceUser = new SessionUserInfoVo();
        SessionUserInfoVo regionUser = new SessionUserInfoVo();
        SessionUserInfoVo businessUser = new SessionUserInfoVo();
        Set<Integer> keySet = sessionUserInfoMap.keySet();
        for (Integer key : keySet) {
            SessionUserInfoVo userInfoVo = sessionUserInfoMap.get(key);
            if(userInfoVo == null){
                continue;
            }
            if(DeptTypeEnum.GROUP.getType().equals(userInfoVo.getDeptType())){
                groupUser = userInfoVo;
                if(userInfoVo.getId() == null){
                    groupUser = null;
                }
                continue;
            }
            if(DeptTypeEnum.SERVICE.getType().equals(userInfoVo.getDeptType())){
                serviceUser = userInfoVo;
                if(userInfoVo.getId() == null){
                    serviceUser = null;
                }
                continue;
            }
            if(DeptTypeEnum.LARGE_AREA.getType().equals(userInfoVo.getDeptType())){
                regionUser = userInfoVo;
                if(userInfoVo.getId() == null){
                    regionUser = null;
                }
                continue;
            }
            if(DeptTypeEnum.BUSINESS_UNIT.getType().equals(userInfoVo.getDeptType())){
                businessUser = userInfoVo;
                if(userInfoVo.getId() == null){
                    businessUser = null;
                }
                continue;
            }

        }
        if(ruleIceDetailVo != null){
            //规则设置：不需审批
            if(!ruleIceDetailVo.getIsApproval()){
//                IceExamine iceExamine = new IceExamine();
//                BeanUtils.copyProperties(iceExamineVo,iceExamine);
//                doExamine(iceExamine);
                iceExamine.setExaminStatus(ExamineStatus.PASS_EXAMINE.getStatus());
                return map;
            }

            //最高组审批
            if(ExamineLastApprovalEnum.GROUP.getType().equals(ruleIceDetailVo.getLastApprovalNode())){
                if(groupUser == null || groupUser.getId() == null){
                    throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败,找不到组长！");
                }
                //申请人组长本人或部门是高于组的，直接置为审核状态
                if (simpleUserInfoVo.getId().equals(groupUser.getId())
                        || simpleUserInfoVo.getDeptType().equals(DeptTypeEnum.SERVICE.getType())
                        || simpleUserInfoVo.getDeptType().equals(DeptTypeEnum.LARGE_AREA.getType())
                        || simpleUserInfoVo.getDeptType().equals(DeptTypeEnum.BUSINESS_UNIT.getType())
                        || simpleUserInfoVo.getDeptType().equals(DeptTypeEnum.THIS_PART.getType())) {
                    return checkExamine(iceExamineVo,map, iceExamine);
                }
                ids.add(groupUser.getId());
                if (CollectionUtil.isEmpty(ids)) {
                    throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败，找不到上级审批人！");
                }
                createExamineModel(iceExamineVo, map, isExist, iceBoxExtend, ids, iceExamine);
                return map;
            }

            List<String> skipNodeList = new ArrayList<>();
            String skipNode = ruleIceDetailVo.getSkipNode();
            if(StringUtils.isNotBlank(skipNode)){
                String[] skipNodeArr = skipNode.split(",");
                if(skipNodeArr != null){
                    skipNodeList = Arrays.asList(skipNodeArr);
                }
            }
            /**
             * 最高服务处审批
             * 1、是否只需上级审批
             * 1.1、是，判断是否跳过了审批节点。跳过节点找最接近的上级，没跳过取上级
             * 1.2、否，判断是否跳过了审批节点。跳过节点找低于最高节点的其他节点，没跳过正常取
             */

            if(ExamineLastApprovalEnum.SERVICE.getType().equals(ruleIceDetailVo.getLastApprovalNode())){


                //申请人服务处领导或者部门是高于服务处的，直接置为审核状态
                if (simpleUserInfoVo.getId().equals(serviceUser.getId())
                        || simpleUserInfoVo.getDeptType().equals(DeptTypeEnum.LARGE_AREA.getType())
                        || simpleUserInfoVo.getDeptType().equals(DeptTypeEnum.BUSINESS_UNIT.getType())
                        || simpleUserInfoVo.getDeptType().equals(DeptTypeEnum.THIS_PART.getType())) {

                    return checkExamine(iceExamineVo,map,iceExamine);
                }

                //规则设置：是否上级审批
                if(ruleIceDetailVo.getIsLeaderApproval()){
                    /**
                     * 需要上级审批
                     * 规则设置：需要跳过的节点
                     */
                    if(CollectionUtil.isEmpty(skipNodeList)){
                        /**
                         * 不存在需要跳过的节点
                         * 判断创建人是否和第一个领导为同一人，是：直接审批通过；否：第一个领导审批审批
                         */
                        SessionUserInfoVo userInfoVo = sessionUserInfoMap.get(0);

                        if(DeptTypeEnum.GROUP.getType().equals(simpleUserInfoVo.getDeptType())){
                            userInfoVo = groupUser;
                            if(simpleUserInfoVo.getIsLearder().equals(1)){
                                userInfoVo = serviceUser;
                            }
                        }
                        if(DeptTypeEnum.SERVICE.getType().equals(simpleUserInfoVo.getDeptType())){
                            userInfoVo = serviceUser;
                        }

                        if(userInfoVo == null || userInfoVo.getId() == null){
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败,找不到上级领导！");
                        }
                        if ((userInfoVo.getId() != null && userInfoVo.getId().equals(simpleUserInfoVo.getId()))) {
                            return checkExamine(iceExamineVo,map, iceExamine);
                        }
                        if(!ids.contains(userInfoVo.getId())){
                            ids.add(userInfoVo.getId());
                        }
                        createExamineModel(iceExamineVo, map, isExist, iceBoxExtend, ids, iceExamine);
                        return map;
                    }
                }


                /**
                 * 需要或者不需要上级审批，由于最高审批是服务处，所以跳过的只能是组，直接取服务处经理审批；没有跳过的就全部审批
                 */
                if(CollectionUtil.isNotEmpty(skipNodeList)){
                    if(serviceUser == null || serviceUser.getId() == null){
                        throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败,找不到服务处经理！");
                    }
                    if(!ids.contains(serviceUser.getId())){
                        ids.add(serviceUser.getId());
                    }
                    createExamineModel(iceExamineVo, map, isExist, iceBoxExtend, ids, iceExamine);
                    return map;
                }else {
                    if(groupUser == null || groupUser.getId() == null){
                        throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败,找不到组长！");
                    }
                    if(!ids.contains(groupUser.getId()) && !simpleUserInfoVo.getId().equals(groupUser.getId())){
                        ids.add(groupUser.getId());
                    }

                    if(serviceUser == null || serviceUser.getId() == null){
                        throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败,找不到服务处经理！");
                    }
                    if(!ids.contains(serviceUser.getId()) && !simpleUserInfoVo.getId().equals(serviceUser.getId())){
                        ids.add(serviceUser.getId());
                    }
                    createExamineModel(iceExamineVo, map, isExist, iceBoxExtend, ids, iceExamine);
                    return map;
                }

            }

            //最高大区审批
            if(ExamineLastApprovalEnum.LARGE_AREA.getType().equals(ruleIceDetailVo.getLastApprovalNode())){
                //申请人大区领导或者部门是高于大区的，直接置为审核状态
                if (simpleUserInfoVo.getId().equals(regionUser.getId())
                        || simpleUserInfoVo.getDeptType().equals(DeptTypeEnum.BUSINESS_UNIT.getType())
                        || simpleUserInfoVo.getDeptType().equals(DeptTypeEnum.THIS_PART.getType())) {
                    return checkExamine(iceExamineVo,map, iceExamine);
                }

                //规则设置：是否上级审批
                if(ruleIceDetailVo.getIsLeaderApproval()){
                    /**
                     * 需要上级审批
                     * 规则设置：需要跳过的节点
                     */
                    if(CollectionUtil.isEmpty(skipNodeList)){
                        /**
                         * 不存在需要跳过的节点
                         * 判断创建人是否和第一个领导为同一人，是：直接审批通过；否：第一个领导审批
                         */
                        SessionUserInfoVo userInfoVo = sessionUserInfoMap.get(0);
                        if(DeptTypeEnum.GROUP.getType().equals(simpleUserInfoVo.getDeptType())){
                            userInfoVo = groupUser;
                            if(simpleUserInfoVo.getIsLearder().equals(1)){
                                userInfoVo = serviceUser;
                            }
                        }
                        if(DeptTypeEnum.SERVICE.getType().equals(simpleUserInfoVo.getDeptType())){
                            userInfoVo = serviceUser;
                            if(simpleUserInfoVo.getIsLearder().equals(1)){
                                userInfoVo = regionUser;
                            }
                        }
                        if(DeptTypeEnum.LARGE_AREA.getType().equals(simpleUserInfoVo.getDeptType())){
                            userInfoVo = regionUser;
                        }
                        if(userInfoVo == null || userInfoVo.getId() == null){
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败,找不到上级领导！");
                        }
                        if ((userInfoVo.getId() != null && userInfoVo.getId().equals(simpleUserInfoVo.getId()))) {
                            return checkExamine(iceExamineVo,map, iceExamine);
                        }
                        if(!ids.contains(userInfoVo.getId())){
                            ids.add(userInfoVo.getId());
                        }
                        createExamineModel(iceExamineVo, map, isExist, iceBoxExtend, ids, iceExamine);
                        return map;
                    }else {
                        /**
                         * 存在需要跳过的节点
                         * 查找最近的领导，判断创建人是否和领导为同一人，是：直接审批通过；否：领导审批
                         * 允许跳过的节点(1-服务组 2-服务处 3-大区 4-事业部)
                         */
                        List<Integer> allNodes = new ArrayList<>();
                        allNodes.add(1);
                        allNodes.add(2);
                        allNodes.add(3);
                        Iterator<Integer> iterator = allNodes.iterator();

                       while (iterator.hasNext()){
                           Integer next = iterator.next();
                           if(skipNode.contains(next+"")){
                               iterator.remove();
                           }
                       }
                        SessionUserInfoVo userInfoVo = null;
                        if(DeptTypeEnum.GROUP.getType().equals(simpleUserInfoVo.getDeptType())){

                            if(simpleUserInfoVo.getIsLearder().equals(1)){
                                if(allNodes.contains(2)){
                                    userInfoVo = serviceUser;
                                }
                                if(!allNodes.contains(2) && allNodes.contains(3)){
                                    userInfoVo = regionUser;
                                }
                            }else {
                                if(allNodes.contains(1)){
                                    userInfoVo = groupUser;
                                }
                                if(!allNodes.contains(1) && allNodes.contains(2)){
                                    userInfoVo = serviceUser;
                                }
                                if(!allNodes.contains(1) && !allNodes.contains(2) && allNodes.contains(3)){
                                    userInfoVo = regionUser;
                                }
                            }
                        }

                        if(DeptTypeEnum.SERVICE.getType().equals(simpleUserInfoVo.getDeptType())){

                            if(simpleUserInfoVo.getIsLearder().equals(1)){
                                userInfoVo = regionUser;
                            }else {
                                if(allNodes.contains(2)){
                                    userInfoVo = serviceUser;
                                }
                                if(!allNodes.contains(2) && allNodes.contains(3)){
                                    userInfoVo = regionUser;
                                }
                            }
                        }

                        if(DeptTypeEnum.LARGE_AREA.getType().equals(simpleUserInfoVo.getDeptType())){
                            userInfoVo = regionUser;
                        }

                        if(userInfoVo == null || userInfoVo.getId() == null){
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败,找不到领导审批！");
                        }
                        if ((userInfoVo.getId().equals(simpleUserInfoVo.getId()))) {
                            return checkExamine(iceExamineVo,map, iceExamine);
                        }
                        if(!ids.contains(userInfoVo.getId())){
                            ids.add(userInfoVo.getId());
                        }
                        createExamineModel(iceExamineVo, map, isExist, iceBoxExtend, ids, iceExamine);
                        return map;

                    }
                }
                /**
                 * 不需要上级审批
                 * 不需要跳过，全部审批
                 * 需要跳过，剩下的审批
                 */
                if(CollectionUtil.isEmpty(skipNodeList)){
                    if(DeptTypeEnum.GROUP.getType().equals(simpleUserInfoVo.getDeptType())){
                        if(regionUser == null || regionUser.getId() == null){
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败,找不到大区经理！");
                        }

                        if(serviceUser == null || serviceUser.getId() == null){
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败,找不到服务处经理！");
                        }

                        if(simpleUserInfoVo.getIsLearder().equals(0)){
                            if(groupUser == null || groupUser.getId() == null){
                                throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败,找不到组长！");
                            }
                            if(!ids.contains(groupUser.getId()) && !simpleUserInfoVo.getId().equals(groupUser.getId())){
                                ids.add(groupUser.getId());
                            }
                        }


                        if(!ids.contains(serviceUser.getId()) && !simpleUserInfoVo.getId().equals(serviceUser.getId())){
                            ids.add(serviceUser.getId());
                        }

                        if(!ids.contains(regionUser.getId()) && !simpleUserInfoVo.getId().equals(regionUser.getId())){
                            ids.add(regionUser.getId());
                        }

                        if (CollectionUtil.isEmpty(ids)) {
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败，找不到上级审批人！");
                        }
                    }
                    if(DeptTypeEnum.SERVICE.getType().equals(simpleUserInfoVo.getDeptType())){

                        if(regionUser == null || regionUser.getId() == null){
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败,找不到大区经理！");
                        }

                        if(simpleUserInfoVo.getIsLearder().equals(0)){
                            if(serviceUser == null || serviceUser.getId() == null){
                                throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败,找不到服务处经理！");
                            }

                            if(!ids.contains(serviceUser.getId()) && !simpleUserInfoVo.getId().equals(serviceUser.getId())){
                                ids.add(serviceUser.getId());
                            }
                        }


                        if(!ids.contains(regionUser.getId()) && !simpleUserInfoVo.getId().equals(regionUser.getId())){
                            ids.add(regionUser.getId());
                        }

                        if (CollectionUtil.isEmpty(ids)) {
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败，找不到上级审批人！");
                        }
                    }

                    if(DeptTypeEnum.LARGE_AREA.getType().equals(simpleUserInfoVo.getDeptType())){

                        if(regionUser == null || regionUser.getId() == null){
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败,找不到大区经理！");
                        }

                        if(!ids.contains(regionUser.getId()) && !simpleUserInfoVo.getId().equals(regionUser.getId())){
                            ids.add(regionUser.getId());
                        }

                        if (CollectionUtil.isEmpty(ids)) {
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败，找不到上级审批人！");
                        }
                    }
                    createExamineModel(iceExamineVo, map, isExist, iceBoxExtend, ids, iceExamine);
                    return map;
                }else {
                    List<Integer> allNodes = new ArrayList<>();
                    allNodes.add(1);
                    allNodes.add(2);
                    allNodes.add(3);
                    Iterator<Integer> iterator = allNodes.iterator();

                    while (iterator.hasNext()){
                        Integer next = iterator.next();
                        if(skipNode.contains(next+"")){
                            iterator.remove();
                        }
                    }

                    if(DeptTypeEnum.GROUP.getType().equals(simpleUserInfoVo.getDeptType())){

                        if(simpleUserInfoVo.getIsLearder().equals(1)){
                            if(allNodes.contains(2) && !ids.contains(serviceUser.getId())){
                                ids.add(serviceUser.getId());
                            }
                        }else {
                            if(allNodes.contains(1) && !ids.contains(groupUser.getId())){
                                ids.add(groupUser.getId());
                            }
                            if(allNodes.contains(2) && !ids.contains(serviceUser.getId())){
                                ids.add(serviceUser.getId());
                            }
                        }
                    }

                    if(DeptTypeEnum.SERVICE.getType().equals(simpleUserInfoVo.getDeptType())){
                        if(!simpleUserInfoVo.getIsLearder().equals(1)){
                            if(allNodes.contains(2) && !ids.contains(serviceUser.getId())){
                                ids.add(serviceUser.getId());
                            }
                        }
                    }

                    if(allNodes.contains(3) && !ids.contains(regionUser.getId())){
                        ids.add(regionUser.getId());
                    }
                    createExamineModel(iceExamineVo, map, isExist, iceBoxExtend, ids, iceExamine);
                    return map;
                }

            }

            //最高事业部审批
            if(ExamineLastApprovalEnum.BUSINESS_UNIT.getType().equals(ruleIceDetailVo.getLastApprovalNode())){

                //申请人事业部领导或者部门是高于大区的，直接置为审核状态
                if (simpleUserInfoVo.getId().equals(businessUser.getId()) || simpleUserInfoVo.getDeptType().equals(DeptTypeEnum.THIS_PART.getType())) {
                    return checkExamine(iceExamineVo,map, iceExamine);
                }

                //规则设置：是否上级审批
                if(ruleIceDetailVo.getIsLeaderApproval()){
                    /**
                     * 需要上级审批
                     * 规则设置：需要跳过的节点
                     */
                    if(CollectionUtil.isEmpty(skipNodeList)){
                        /**
                         * 不存在需要跳过的节点
                         * 判断创建人是否和第一个领导为同一人，是：直接审批通过；否：第一个领导审批
                         */
                        SessionUserInfoVo userInfoVo = sessionUserInfoMap.get(0);
                        if(DeptTypeEnum.GROUP.getType().equals(simpleUserInfoVo.getDeptType())){
                            userInfoVo = groupUser;
                            if(simpleUserInfoVo.getIsLearder().equals(1)){
                                userInfoVo = serviceUser;
                            }
                        }
                        if(DeptTypeEnum.SERVICE.getType().equals(simpleUserInfoVo.getDeptType())){
                            userInfoVo = serviceUser;
                            if(simpleUserInfoVo.getIsLearder().equals(1)){
                                userInfoVo = regionUser;
                            }
                        }
                        if(DeptTypeEnum.LARGE_AREA.getType().equals(simpleUserInfoVo.getDeptType())){
                            userInfoVo = regionUser;
                            if(simpleUserInfoVo.getIsLearder().equals(1)){
                                userInfoVo = businessUser;
                            }
                        }
                        if(DeptTypeEnum.BUSINESS_UNIT.getType().equals(simpleUserInfoVo.getDeptType())){
                            userInfoVo = businessUser;
                        }
                        if(userInfoVo == null || userInfoVo.getId() == null){
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败,找不到上级领导！");
                        }
                        if ((userInfoVo.getId() != null && userInfoVo.getId().equals(simpleUserInfoVo.getId()))) {
                            return checkExamine(iceExamineVo,map, iceExamine);
                        }
                        if(!ids.contains(userInfoVo.getId())){
                            ids.add(userInfoVo.getId());
                        }
                        createExamineModel(iceExamineVo, map, isExist, iceBoxExtend, ids, iceExamine);
                        return map;
                    }else {
                        /**
                         * 存在需要跳过的节点
                         * 查找最近的领导，判断创建人是否和领导为同一人，是：直接审批通过；否：领导审批
                         * 允许跳过的节点(1-服务组 2-服务处 3-大区 4-事业部)
                         */
                        List<Integer> allNodes = new ArrayList<>();
                        allNodes.add(1);
                        allNodes.add(2);
                        allNodes.add(3);
                        allNodes.add(4);
                        Iterator<Integer> iterator = allNodes.iterator();

                        while (iterator.hasNext()){
                            Integer next = iterator.next();
                            if(skipNode.contains(next+"")){
                                iterator.remove();
                            }
                        }

                        SessionUserInfoVo userInfoVo = null;
                        if(DeptTypeEnum.GROUP.getType().equals(simpleUserInfoVo.getDeptType())){

                            if(simpleUserInfoVo.getIsLearder().equals(1)){
                                if(allNodes.contains(2)){
                                    userInfoVo = serviceUser;
                                }
                                if(!allNodes.contains(2) && allNodes.contains(3)){
                                    userInfoVo = regionUser;
                                }
                            }else {
                                if(allNodes.contains(1)){
                                    userInfoVo = groupUser;
                                }
                                if(!allNodes.contains(1) && allNodes.contains(2)){
                                    userInfoVo = serviceUser;
                                }
                                if(!allNodes.contains(1) && !allNodes.contains(2) && allNodes.contains(3)){
                                    userInfoVo = regionUser;
                                }
                                if(!allNodes.contains(1) && !allNodes.contains(2) && !allNodes.contains(3) && allNodes.contains(4)){
                                    userInfoVo = businessUser;
                                }
                            }
                        }

                        if(DeptTypeEnum.SERVICE.getType().equals(simpleUserInfoVo.getDeptType())){

                            if(simpleUserInfoVo.getIsLearder().equals(1)){
                                if(allNodes.contains(3)){
                                    userInfoVo = regionUser;
                                }
                                if(!allNodes.contains(3) && allNodes.contains(4)){
                                    userInfoVo = businessUser;
                                }

                            }else {
                                if(allNodes.contains(2)){
                                    userInfoVo = serviceUser;
                                }
                                if(!allNodes.contains(2) && allNodes.contains(3)){
                                    userInfoVo = regionUser;
                                }
                                if(!allNodes.contains(2) && !allNodes.contains(3) && allNodes.contains(4)){
                                    userInfoVo = businessUser;
                                }
                            }
                        }

                        if(DeptTypeEnum.LARGE_AREA.getType().equals(simpleUserInfoVo.getDeptType())){

                            if(simpleUserInfoVo.getIsLearder().equals(1)){
                                userInfoVo = businessUser;
                            }else {
                                if(allNodes.contains(3)){
                                    userInfoVo = regionUser;
                                }
                                if(!allNodes.contains(3) && allNodes.contains(4)){
                                    userInfoVo = businessUser;
                                }
                            }
                        }

                        if(DeptTypeEnum.BUSINESS_UNIT.getType().equals(simpleUserInfoVo.getDeptType())){
                            userInfoVo = businessUser;
                        }

                        if(userInfoVo == null || userInfoVo.getId() == null){
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败,找不到领导审批！");
                        }
                        if ((userInfoVo.getId().equals(simpleUserInfoVo.getId()))) {
                            return checkExamine(iceExamineVo,map, iceExamine);
                        }
                        if(!ids.contains(userInfoVo.getId())){
                            ids.add(userInfoVo.getId());
                        }
                        createExamineModel(iceExamineVo, map, isExist, iceBoxExtend, ids, iceExamine);
                        return map;
                    }
                }
                /**
                 * 不需要上级审批
                 * 不需要跳过，全部审批
                 * 需要跳过，剩下的审批
                 */
                if(CollectionUtil.isEmpty(skipNodeList)){
                    if(DeptTypeEnum.GROUP.getType().equals(simpleUserInfoVo.getDeptType())){
                        if(businessUser == null || businessUser.getId() == null){
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败,找不到事业部经理！");
                        }

                        if(regionUser == null || regionUser.getId() == null){
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败,找不到大区经理！");
                        }

                        if(serviceUser == null || serviceUser.getId() == null){
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败,找不到服务处经理！");
                        }

                        if(simpleUserInfoVo.getIsLearder().equals(0)){
                            if(groupUser == null || groupUser.getId() == null){
                                throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败,找不到组长！");
                            }
                            if(!ids.contains(groupUser.getId()) && !simpleUserInfoVo.getId().equals(groupUser.getId())){
                                ids.add(groupUser.getId());
                            }
                        }


                        if(!ids.contains(serviceUser.getId()) && !simpleUserInfoVo.getId().equals(serviceUser.getId())){
                            ids.add(serviceUser.getId());
                        }

                        if(!ids.contains(regionUser.getId()) && !simpleUserInfoVo.getId().equals(regionUser.getId())){
                            ids.add(regionUser.getId());
                        }
                        if(!ids.contains(businessUser.getId()) && !simpleUserInfoVo.getId().equals(businessUser.getId())){
                            ids.add(businessUser.getId());
                        }

                        if (CollectionUtil.isEmpty(ids)) {
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败，找不到上级审批人！");
                        }
                    }
                    if(DeptTypeEnum.SERVICE.getType().equals(simpleUserInfoVo.getDeptType())){
                        if(businessUser == null || businessUser.getId() == null){
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败,找不到事业部经理！");
                        }

                        if(regionUser == null || regionUser.getId() == null){
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败,找不到大区经理！");
                        }

                        if(simpleUserInfoVo.getIsLearder().equals(0)){
                            if(serviceUser == null || serviceUser.getId() == null){
                                throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败,找不到服务处经理！");
                            }

                            if(!ids.contains(serviceUser.getId()) && !simpleUserInfoVo.getId().equals(serviceUser.getId())){
                                ids.add(serviceUser.getId());
                            }
                        }


                        if(!ids.contains(regionUser.getId()) && !simpleUserInfoVo.getId().equals(regionUser.getId())){
                            ids.add(regionUser.getId());
                        }
                        if(!ids.contains(businessUser.getId()) && !simpleUserInfoVo.getId().equals(businessUser.getId())){
                            ids.add(businessUser.getId());
                        }

                        if (CollectionUtil.isEmpty(ids)) {
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败，找不到上级审批人！");
                        }
                    }

                    if(DeptTypeEnum.LARGE_AREA.getType().equals(simpleUserInfoVo.getDeptType())){
                        if(businessUser == null || businessUser.getId() == null){
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败,找不到事业部经理！");
                        }

                        if(simpleUserInfoVo.getIsLearder().equals(0)){
                            if(regionUser == null || regionUser.getId() == null){
                                throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败,找不到大区经理！");
                            }

                            if(!ids.contains(regionUser.getId()) && !simpleUserInfoVo.getId().equals(regionUser.getId())){
                                ids.add(regionUser.getId());
                            }
                        }

                        if(!ids.contains(businessUser.getId()) && !simpleUserInfoVo.getId().equals(businessUser.getId())){
                            ids.add(businessUser.getId());
                        }

                        if (CollectionUtil.isEmpty(ids)) {
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败，找不到上级审批人！");
                        }
                    }

                    if(DeptTypeEnum.BUSINESS_UNIT.getType().equals(simpleUserInfoVo.getDeptType())){
                        if(businessUser == null || businessUser.getId() == null){
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败,找不到事业部经理！");
                        }
                        if (CollectionUtil.isEmpty(ids)) {
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败，找不到上级审批人！");
                        }
                    }

                    createExamineModel(iceExamineVo, map, isExist, iceBoxExtend, ids, iceExamine);
                    return map;
                }else {
                    List<Integer> allNodes = new ArrayList<>();
                    allNodes.add(1);
                    allNodes.add(2);
                    allNodes.add(3);
                    allNodes.add(4);
                    Iterator<Integer> iterator = allNodes.iterator();

                    while (iterator.hasNext()){
                        Integer next = iterator.next();
                        if(skipNode.contains(next+"")){
                            iterator.remove();
                        }
                    }

                    if(DeptTypeEnum.GROUP.getType().equals(simpleUserInfoVo.getDeptType())){
                        if(simpleUserInfoVo.getIsLearder().equals(1)){
                            if(allNodes.contains(2) && !ids.contains(serviceUser.getId())){
                                ids.add(serviceUser.getId());
                            }
                            if(allNodes.contains(3) && !ids.contains(regionUser.getId())){
                                ids.add(regionUser.getId());
                            }
                        }else {
                            if(allNodes.contains(1) && !ids.contains(groupUser.getId())){
                                ids.add(groupUser.getId());
                            }
                            if(allNodes.contains(2) && !ids.contains(serviceUser.getId())){
                                ids.add(serviceUser.getId());
                            }
                            if(allNodes.contains(3) && !ids.contains(regionUser.getId())){
                                ids.add(regionUser.getId());
                            }
                        }
                    }

                    if(DeptTypeEnum.SERVICE.getType().equals(simpleUserInfoVo.getDeptType())){

                        if(!simpleUserInfoVo.getIsLearder().equals(1)){
                            if(allNodes.contains(2) && !ids.contains(serviceUser.getId())){
                                ids.add(serviceUser.getId());
                            }
                        }
                        if(allNodes.contains(3) && !ids.contains(regionUser.getId())){
                            ids.add(regionUser.getId());
                        }
                    }

                    if(allNodes.contains(4) && !ids.contains(businessUser.getId())){
                        ids.add(businessUser.getId());
                    }

                    createExamineModel(iceExamineVo, map, isExist, iceBoxExtend, ids, iceExamine);
                    return map;
                }
            }
        }else {
            //默认最高服务处审批
            if(serviceUser == null || serviceUser.getId() == null){
                throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败,找不到服务处经理！");
            }
            //申请人是服务处经理，直接置为审核状态
            if ((serviceUser.getId() != null && serviceUser.getId().equals(simpleUserInfoVo.getId()))) {
                return checkExamine(iceExamineVo,map, iceExamine);
            }

            if(groupUser == null || groupUser.getId() == null){
                throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败,找不到组长！");
            }
            if(!ids.contains(groupUser.getId()) && !simpleUserInfoVo.getId().equals(groupUser.getId())){
                ids.add(groupUser.getId());
            }

            if(!ids.contains(serviceUser.getId())){
                ids.add(serviceUser.getId());
            }

            if (CollectionUtil.isEmpty(ids)) {
                throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败，找不到上级审批人！");
            }
            createExamineModel(iceExamineVo, map, isExist, iceBoxExtend, ids, iceExamine);
            return map;
        }
        return map;
    }

    private void createExamineModel(IceExamineVo iceExamineVo, Map<String, Object> map, IceBox isExist, IceBoxExtend iceBoxExtend, List<Integer> ids, IceExamine iceExamine) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        IceBoxExamineModel examineModel = new IceBoxExamineModel();
        examineModel.setStoreNumber(isExist.getPutStoreNumber());
        StoreInfoDtoVo store = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(isExist.getPutStoreNumber()));
        if(store != null){
            examineModel.setStoreName(store.getStoreName());
        }
        examineModel.setExamineNumber(iceExamineVo.getExamineNumber());
        examineModel.setAssetId(isExist.getAssetId());
        examineModel.setDepositMoney(isExist.getDepositMoney());
        examineModel.setDisplayImage(iceExamineVo.getDisplayImage());
        examineModel.setExaminMsg(iceExamineVo.getExaminMsg());
        examineModel.setExteriorImage(iceExamineVo.getExteriorImage());
        examineModel.setIceBoxModel(isExist.getModelName());
        examineModel.setIceBoxName(isExist.getChestName());
        examineModel.setIceStatus(iceExamineVo.getIceStatus());
        examineModel.setIceExaminStatus(iceExamineVo.getIceExamineStatus());
        examineModel.setPutTime(dateFormat.format(isExist.getUpdatedTime()));
        if(iceBoxExtend.getReleaseTime() != null){
            examineModel.setReleaseTimeStr(dateFormat.format(iceBoxExtend.getReleaseTime()));
        }
        if(iceBoxExtend.getRepairBeginTime() != null){
            examineModel.setRepairBeginTime(dateFormat.format(iceBoxExtend.getRepairBeginTime()));
        }
        examineModel.setSignTime(1);
        examineModel.setCreateByName(iceExamineVo.getCreateName());
        examineModel.setCreateTimeStr(dateFormat.format(new Date()));

        SessionExamineVo sessionExamineVo = new SessionExamineVo();
        SessionExamineCreateVo sessionExamineCreateVo = SessionExamineCreateVo.builder()
                .code(iceExamineVo.getExamineNumber())
                .relateCode(iceExamineVo.getExamineNumber())
                .createBy(iceExamineVo.getCreateBy())
                .userIds(ids)
                .build();
        sessionExamineVo.setSessionExamineCreateVo(sessionExamineCreateVo);
        sessionExamineVo.setIceBoxExamineModel(examineModel);
        SessionExamineVo examineVo = FeignResponseUtil.getFeignData(feignExamineClient.createIceBoxExamine(sessionExamineVo));
        List<SessionExamineVo.VisitExamineNodeVo> visitExamineNodes = examineVo.getVisitExamineNodes();
        map.put("iceBoxExamineNodes",visitExamineNodes);
        iceExamine.setExaminStatus(ExamineStatus.DEFAULT_EXAMINE.getStatus());
    }

    private Map<String, Object> checkExamine(IceExamineVo iceExamineVo, Map<String, Object> map, IceExamine iceExamine) {
        IceBox iceBox = iceBoxDao.selectById(iceExamineVo.getIceBoxId());
        if(iceBox == null){
            throw new NormalOptionException(Constants.API_CODE_FAIL, "巡检的冰柜不存在！");
        }
        iceExamine.setExaminStatus(ExamineStatus.PASS_EXAMINE.getStatus());
//        iceBox.setStatus(iceExamineVo.getIceStatus());
//        iceBox.setUpdatedBy(iceExamineVo.getCreateBy());
//        iceBox.setUpdatedTime(new Date());
//        iceBoxDao.updateById(iceBox);

        map.put("isCheck", CommonIsCheckEnum.IS_CHECK.getStatus());
        return map;
    }


//    public static final String URL = "https://api.xdp8.cn/gps/getLocation";
//
//    private String getAddress(String longitude, String latitude) throws ImproperOptionException {
//
//        StringBuilder requestUrl = new StringBuilder(URL);
//        requestUrl.append("?type=5").append("&longitude=").append(longitude).append("&latitude=").append(latitude);
//        String result = "";
//        try {
//            result = HttpUtils.get(requestUrl.toString());
//        } catch (Exception e) {
//            log.error("请求东鹏定位接口异常", e);
//        }
//        JSONObject jsonObject = JSON.parseObject(result);
//        if ("1".equals(jsonObject.getString("code"))) {
//            JSONObject data = jsonObject.getJSONObject("data");
//
//            String province = data.getString("province");
//            String city = data.getString("city");
//            String area = data.getString("area");
//            String address = data.getString("address");
//            return city + address;
//        } else {
//            log.error("东鹏定位接口请求失败:{}", result);
//        }
//        return null;
//    }


    @Override
    public Integer getInspectionCount(List<Integer> userIds) {
        List<Integer> boxIds = new ArrayList<>();
        for (Integer userId : userIds) {
            List<Integer> list = iceBoxService.getPutBoxIds(userId);
            boxIds.addAll(list);
        }
        LambdaQueryWrapper<IceExamine> wrapper = Wrappers.<IceExamine>lambdaQuery();
        if(CollectionUtils.isEmpty(userIds)){
            return 0;
        }
        wrapper.eq(IceExamine::getExaminStatus,2)
                .in(IceExamine::getIceBoxId,boxIds)
                .apply("date_format(create_time,'%Y-%m') = '" + new DateTime().toString("yyyy-MM")+"'")
                .groupBy(IceExamine::getIceBoxId);
        return iceExamineDao.selectCount(wrapper);
    }

    @Override
    public List<IceExamine> getInspectionBoxes(Integer userId) {
        List<Integer> boxIds = iceBoxService.getPutBoxIds(userId);
        if(CollectionUtils.isEmpty(boxIds)){
            return Lists.newArrayList();
        }
        LambdaQueryWrapper<IceExamine> wrapper = Wrappers.<IceExamine>lambdaQuery();
        wrapper.eq(IceExamine::getExaminStatus,2)
                .in(IceExamine::getIceBoxId,boxIds)
                .apply("date_format(create_time,'%Y-%m') = '" + new DateTime().toString("yyyy-MM")+"'")
                .groupBy(IceExamine::getIceBoxId);
        return iceExamineDao.selectList(wrapper);
    }

    @Override
    public Integer getNoInspectionBoxes(Integer putCount, Integer userId) {
        List<Integer> boxIds = iceBoxService.getPutBoxIds(userId);
        if(CollectionUtils.isEmpty(boxIds)){
            return 0;
        }
        LambdaQueryWrapper<IceExamine> wrapper = Wrappers.<IceExamine>lambdaQuery();
        wrapper.eq(IceExamine::getExaminStatus,2)
                .eq(IceExamine::getCreateBy,userId)
                .in(IceExamine::getIceBoxId,boxIds)
                .apply("date_format(create_time,'%Y-%m') = '" + new DateTime().toString("yyyy-MM")+"'")
                .groupBy(IceExamine::getIceBoxId);
        int size = iceExamineDao.selectList(wrapper).size();
        return putCount-size;
    }

    @Override
    public IceExamineVo findExamineByNumber(String examineNumber) {
        IceExamine iceExamine = iceExamineDao.selectOne(Wrappers.<IceExamine>lambdaQuery().eq(IceExamine::getExamineNumber,examineNumber));

        IceExamineVo iceExamineVo;

        if (iceExamine != null) {

            ArrayList<Integer> list = new ArrayList<>();
            list.add(iceExamine.getCreateBy());
            Map<Integer, SessionUserInfoVo> map = FeignResponseUtil.getFeignData(feignUserClient.getSessionUserInfoVoByIds(list));
            SessionUserInfoVo sessionUserInfoVo = map.get(iceExamine.getCreateBy());
            StoreInfoDtoVo storeInfoDtoVo = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(iceExamine.getStoreNumber()));
            String storeName = "";
            if (null != storeInfoDtoVo && StringUtils.isNotBlank(storeInfoDtoVo.getStoreNumber())) {
                storeName = storeInfoDtoVo.getStoreName();
            } else {
                SubordinateInfoVo subordinateInfoVo = FeignResponseUtil.getFeignData(feignSupplierClient.findByNumber(iceExamine.getStoreNumber()));
                if (null != subordinateInfoVo && StringUtils.isNotBlank(subordinateInfoVo.getNumber())) {
                    storeName = subordinateInfoVo.getName();
                }
            }
            iceExamineVo = iceExamine.convert(iceExamine, sessionUserInfoVo.getRealname(), storeName, iceExamine.getStoreNumber());
            List<SessionExamineVo.VisitExamineNodeVo> examineNodeVos = FeignResponseUtil.getFeignData(feignExamineClient.getExamineNodesByRelateCode(examineNumber));
            iceExamineVo.setExamineNodeVos(examineNodeVos);
//            iceExamineVo = IceExamineVo.builder()
//                    .id(iceExamine.getId())
//                    .createBy(iceExamine.getCreateBy())
//                    .createName(sessionUserInfoVo.getRealname())
//                    .displayImage(iceExamine.getDisplayImage())
//                    .exteriorImage(iceExamine.getExteriorImage())
//                    .createTime(iceExamine.getCreateTime())
//                    .storeName(storeInfoDtoVo.getStoreName())
//                    .storeNumber(storeNumber)
//                    .iceBoxId(iceExamine.getIceBoxId())
//                    .latitude(iceExamine.getLatitude())
//                    .longitude(iceExamine.getLongitude())
//                    .temperature(iceExamine.getTemperature())
//                    .openCloseCount(iceExamine.getOpenCloseCount())
//                    .build();
        } else {
            iceExamineVo = null;
        }
        return iceExamineVo;
    }

    @Override
    public void syncExamineDataToReport(List<Integer> ids) {
        List<IceExamine> iceExamineList = null;
        if(CollectionUtil.isNotEmpty(ids)){
            iceExamineList = iceExamineDao.selectList(Wrappers.<IceExamine>lambdaQuery().in(IceExamine::getId,ids));
        }else {
            iceExamineList = iceExamineDao.selectList(null);
        }
        if(CollectionUtil.isEmpty(iceExamineList)){
            return;
        }
        for(IceExamine iceExamine:iceExamineList){
            IceBoxExamineExceptionReport report = new IceBoxExamineExceptionReport();
            report.setExamineNumber(iceExamine.getExamineNumber());
            if(ExamineStatus.DEFAULT_EXAMINE.getStatus().equals(iceExamine.getExaminStatus()) || ExamineStatus.DOING_EXAMINE.getStatus().equals(iceExamine.getExaminStatus())){
                report.setStatus(ExamineExceptionStatusEnums.is_reporting.getStatus());
            }
            if(ExamineStatus.PASS_EXAMINE.getStatus().equals(iceExamine.getExaminStatus())){
                report.setStatus(ExamineExceptionStatusEnums.allow_report.getStatus());
            }

            if(ExamineStatus.REJECT_EXAMINE.getStatus().equals(iceExamine.getExaminStatus())){
                report.setStatus(ExamineExceptionStatusEnums.is_unpass.getStatus());
            }
            if(StringUtils.isEmpty(iceExamine.getExamineNumber())){
                String examineNumber = UUID.randomUUID().toString().replace("-", "");
                iceExamine.setExamineNumber(examineNumber);
                iceExamineDao.updateById(iceExamine);
                report.setExamineNumber(examineNumber);
                report.setStatus(ExamineExceptionStatusEnums.is_repaired.getStatus());
            }
            IceBoxExamineExceptionReport isExsit = iceBoxExamineExceptionReportDao.selectOne(Wrappers.<IceBoxExamineExceptionReport>lambdaQuery().eq(IceBoxExamineExceptionReport::getExamineNumber, iceExamine.getExamineNumber()));
            if(isExsit != null){
                continue;
            }
            IceBox iceBox = iceBoxDao.selectById(iceExamine.getIceBoxId());

            Map<Integer, SessionDeptInfoVo> deptInfoVoMap = FeignResponseUtil.getFeignData(feignCacheClient.getFiveLevelDept(iceBox.getDeptId()));
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
            report.setToOaType(iceExamine.getIceStatus());
            report.setDepositMoney(iceBox.getDepositMoney());
            report.setIceBoxModelId(iceBox.getModelId());
            report.setIceBoxModelName(iceBox.getModelName());
            report.setIceBoxAssetId(iceBox.getAssetId());
            SubordinateInfoVo supplier = FeignResponseUtil.getFeignData(feignSupplierClient.findSupplierBySupplierId(iceBox.getSupplierId()));
            report.setSupplierId(iceBox.getSupplierId());
            if (supplier != null) {
                report.setSupplierNumber(supplier.getNumber());
                report.setSupplierName(supplier.getName());
            }
            report.setPutCustomerNumber(iceExamine.getStoreNumber());
            if(iceExamine.getStoreNumber().startsWith("C0")){
                StoreInfoDtoVo store = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(iceExamine.getStoreNumber()));
                if(store != null){
                    report.setPutCustomerName(store.getStoreName());
                    report.setPutCustomerType(SupplierTypeEnum.IS_STORE.getType());
                }
            }else {
                SubordinateInfoVo customer = FeignResponseUtil.getFeignData(feignSupplierClient.findByNumber(iceExamine.getStoreNumber()));
                if(customer != null){
                    report.setPutCustomerName(customer.getName());
                    report.setPutCustomerType(customer.getSupplierType());
                }
            }
            SimpleUserInfoVo userInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findUserById(iceExamine.getCreateBy()));
            report.setSubmitterId(iceExamine.getCreateBy());
            if (userInfoVo != null) {
                report.setSubmitterName(userInfoVo.getRealname());
                report.setSubmitterPosion(userInfoVo.getPosion());
            }
            report.setSubmitTime(iceExamine.getCreateTime());
            List<SessionExamineVo.VisitExamineNodeVo> visitExamineNodeVos = FeignResponseUtil.getFeignData(feignExamineClient.getExamineNodesByRelateCode(report.getExamineNumber()));
            if(CollectionUtil.isNotEmpty(visitExamineNodeVos)){
                for(SessionExamineVo.VisitExamineNodeVo examineNodeVo:visitExamineNodeVos){
                    if(examineNodeVo.getExamineStatus().equals(1)){
                        report.setExamineUserId(examineNodeVo.getUserId());
                        SimpleUserInfoVo userInfo = FeignResponseUtil.getFeignData(feignUserClient.findSimpleUserById(examineNodeVo.getUserId()));
                        if(userInfo != null){
                            report.setExamineUserName(userInfo.getRealname());
                        }
                        report.setExamineTime(examineNodeVo.getUpdateTime());
                    }
                }
            }
            iceBoxExamineExceptionReportDao.insert(report);
        }

    }
}
