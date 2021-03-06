package com.szeastroc.icebox.newprocess.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.entity.customer.dto.CustomerLabelDetailDto;
import com.szeastroc.common.entity.customer.vo.*;
import com.szeastroc.common.entity.customer.vo.label.CusLabelDetailVo;
import com.szeastroc.common.entity.icebox.enums.IceBoxStatus;
import com.szeastroc.common.entity.icebox.vo.IceBoxRequest;
import com.szeastroc.common.entity.icebox.vo.IceBoxTransferHistoryVo;
import com.szeastroc.common.entity.icebox.vo.IceInspectionReportMsg;
import com.szeastroc.common.entity.user.session.MatchRuleVo;
import com.szeastroc.common.entity.user.session.UserManageVo;
import com.szeastroc.common.entity.user.vo.*;
import com.szeastroc.common.entity.visit.*;
import com.szeastroc.common.enums.CommonStatus;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.exception.NormalOptionException;
import com.szeastroc.common.feign.customer.FeignCusLabelClient;
import com.szeastroc.common.feign.customer.FeignStoreClient;
import com.szeastroc.common.feign.customer.FeignSupplierClient;
import com.szeastroc.common.feign.customer.FeignSupplierRelateUserClient;
import com.szeastroc.common.feign.user.*;
import com.szeastroc.common.feign.visit.FeignBacklogClient;
import com.szeastroc.common.feign.visit.FeignExamineClient;
import com.szeastroc.common.feign.visit.FeignExportRecordsClient;
import com.szeastroc.common.feign.visit.FeignIceboxQueryClient;
import com.szeastroc.common.utils.ExecutorServiceFactory;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.common.utils.ImageUploadUtil;
import com.szeastroc.common.utils.Streams;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.commondb.config.redis.JedisClient;
import com.szeastroc.icebox.config.DmsUrlConfig;
import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.icebox.constant.IceBoxConstant;
import com.szeastroc.icebox.constant.RedisConstant;
import com.szeastroc.icebox.enums.RecordStatus;
import com.szeastroc.icebox.enums.ServiceType;
import com.szeastroc.icebox.enums.*;
import com.szeastroc.icebox.newprocess.consumer.common.IceBoxPutReportMsg;
import com.szeastroc.icebox.newprocess.consumer.enums.OperateTypeEnum;
import com.szeastroc.icebox.newprocess.convert.IceBoxConverter;
import com.szeastroc.icebox.newprocess.dao.*;
import com.szeastroc.icebox.newprocess.entity.*;
import com.szeastroc.icebox.newprocess.enums.PutStatus;
import com.szeastroc.icebox.newprocess.enums.ResultEnum;
import com.szeastroc.icebox.newprocess.enums.*;
import com.szeastroc.icebox.newprocess.service.*;
import com.szeastroc.icebox.newprocess.vo.*;
import com.szeastroc.icebox.newprocess.vo.request.IceBoxPage;
import com.szeastroc.icebox.newprocess.vo.request.IceBoxRequestVo;
import com.szeastroc.icebox.newprocess.vo.request.IceExaminePage;
import com.szeastroc.icebox.newprocess.vo.request.IceTransferRecordPage;
import com.szeastroc.icebox.oldprocess.dao.IceEventRecordDao;
import com.szeastroc.icebox.oldprocess.entity.IceEventRecord;
import com.szeastroc.icebox.rabbitMQ.MethodNameOfMQ;
import com.szeastroc.icebox.util.CreatePathUtil;
import com.szeastroc.icebox.util.SendRequestUtils;
import com.szeastroc.icebox.util.redis.RedisLockUtil;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.hutool.core.date.DateTime.now;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @_(@Autowired))
public class IceBoxServiceImpl extends ServiceImpl<IceBoxDao, IceBox> implements IceBoxService {


    private final String FWCJL = "???????????????";
    private final String FWCFJL = "??????????????????";
    private final String DQZJ = "????????????";
    private final String DQFZJ = "???????????????";

    private final IceBoxDao iceBoxDao;
    private final IceBoxExtendDao iceBoxExtendDao;
    private final IceModelDao iceModelDao;
    private final FeignDeptClient feignDeptClient;
    private final FeignSupplierClient feignSupplierClient;
    private final IcePutApplyDao icePutApplyDao;
    private final IceBackApplyDao iceBackApplyDao;
    private final IcePutApplyRelateBoxDao icePutApplyRelateBoxDao;
    private final IceBackApplyRelateBoxDao iceBackApplyRelateBoxDao;
    private final FeignExamineClient feignExamineClient;
    private final RedisTemplate redisTemplate;
    private final IceExamineDao iceExamineDao;
    private final FeignUserClient feignUserClient;
    private final IcePutPactRecordDao icePutPactRecordDao;
    private final IceTransferRecordDao iceTransferRecordDao;
    private final FeignStoreClient feignStoreClient;
    private final IceEventRecordDao iceEventRecordDao;
    private final IcePutOrderDao icePutOrderDao;
    private final FeignCacheClient feignCacheClient;
    private final FeignXcxBaseClient feignXcxBaseClient;
    private final PutStoreRelateModelDao putStoreRelateModelDao;
    private final ApplyRelatePutStoreModelDao applyRelatePutStoreModelDao;
    private final JedisClient jedis;
    private final FeignCusLabelClient feignCusLabelClient;
    private final ImageUploadUtil imageUploadUtil;
    private final FeignExportRecordsClient feignExportRecordsClient;
    private final FeignBacklogClient feignBacklogClient;
    private final IceBoxTransferHistoryDao iceBoxTransferHistoryDao;
    private final OldIceBoxSignNoticeDao oldIceBoxSignNoticeDao;
    private final RabbitTemplate rabbitTemplate;
    private final IceBoxChangeHistoryDao iceBoxChangeHistoryDao;
    private final IceBoxExamineExceptionReportDao iceBoxExamineExceptionReportDao;
    private final IceBoxPutReportDao iceBoxPutReportDao;
    private final FeignDeptRuleClient feignDeptRuleClient;
    private final FeignIceBoxExamineUserClient feignIceBoxExamineUserClient;
    private final IceBoxRelateDmsDao iceBoxRelateDmsDao;
    private final DmsUrlConfig dmsUrlConfig;
    private final IceBackApplyReportService iceBackApplyReportService;
    private final IceAlarmMapper iceAlarmMapper;
    private final ExamineErrorMapper examineErrorMapper;
    @Autowired
    private IceBoxService iceBoxService;
    @Autowired
    private IcePutOrderService icePutOrderService;
    @Autowired
    private FeignSupplierRelateUserClient feignSupplierRelateUserClient;
    @Autowired
    private FeignIceboxQueryClient feignIceboxQueryClient;
    @Autowired
    private IceRepairOrderService iceRepairOrderService;
    @Autowired
    private IceTransferRecordService iceTransferRecordService;


    @Override
    public List<IceBoxVo> findIceBoxList(IceBoxRequestVo requestVo) {

        List<IceBoxVo> iceBoxVos = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //?????????
        if (XcxType.IS_PUTED.getStatus().equals(requestVo.getType())) {
            List<IceBox> iceBoxes = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getPutStoreNumber, requestVo.getStoreNumber()).eq(IceBox::getPutStatus, PutStatus.FINISH_PUT.getStatus()));
            if (CollectionUtil.isEmpty(iceBoxes)) {
                return iceBoxVos;
            }
            for (IceBox iceBox : iceBoxes) {
                IceBoxVo boxVo = buildIceBoxVo(dateFormat, iceBox);
                iceBoxVos.add(boxVo);
            }
        }
        //?????????
        if (XcxType.NO_PUT.getStatus().equals(requestVo.getType())) {
            if (requestVo.getMarketAreaId() == null) {
                throw new ImproperOptionException("?????????????????????????????????");
            }
            Integer serviceId = FeignResponseUtil.getFeignData(feignDeptClient.getServiceId(requestVo.getMarketAreaId()));
            List<SimpleSupplierInfoVo> supplierInfoVos = FeignResponseUtil.getFeignData(feignSupplierClient.findByDeptId(serviceId));
            if (CollectionUtil.isEmpty(supplierInfoVos)) {
                return iceBoxVos;
            }
            Set<Integer> supplierIds = supplierInfoVos.stream().map(x -> x.getId()).collect(Collectors.toSet());
            Map<Integer, SimpleSupplierInfoVo> supplierInfoVoMap = supplierInfoVos.stream().collect(Collectors.toMap(SimpleSupplierInfoVo::getId, x -> x));
            LambdaQueryWrapper<IceBox> wrapper = Wrappers.<IceBox>lambdaQuery();
            wrapper.in(IceBox::getSupplierId, supplierIds).eq(IceBox::getPutStatus, PutStatus.NO_PUT.getStatus());
            if (StringUtils.isNotEmpty(requestVo.getSearchContent())) {
                List<IceModel> iceModels = iceModelDao.selectList(Wrappers.<IceModel>lambdaQuery().like(IceModel::getChestModel, requestVo.getSearchContent()));
                if (CollectionUtil.isNotEmpty(iceModels)) {
                    Set<Integer> iceModelIds = iceModels.stream().map(x -> x.getId()).collect(Collectors.toSet());
                    wrapper.and(x -> x.like(IceBox::getChestName, requestVo.getSearchContent()).or().in(IceBox::getModelId, iceModelIds));
                } else {
                    wrapper.like(IceBox::getChestName, requestVo.getSearchContent());
                }

            }
            List<IceBox> iceBoxes = iceBoxDao.selectList(wrapper);
            if (CollectionUtil.isEmpty(iceBoxes)) {
                return iceBoxVos;
            }
            Map<Integer, List<IceBox>> iceGroupMap = iceBoxes.stream().collect(Collectors.groupingBy(IceBox::getSupplierId));
            for (Integer supplierId : iceGroupMap.keySet()) {
                List<IceBoxVo> iceBoxVoList = new ArrayList<>();
                List<IceBox> iceBoxList = iceGroupMap.get(supplierId);
                Map<Integer, Integer> iceBoxCountMap = new HashMap<>();
                for (IceBox iceBox : iceBoxList) {
                    Integer count = iceBoxCountMap.get(iceBox.getModelId());
                    if (count != null) {
                        count = count + 1;
                        iceBoxCountMap.put(iceBox.getModelId(), count);
                        continue;
                    }
                    IceBoxVo boxVo = buildIceBoxVo(dateFormat, iceBox);
                    SimpleSupplierInfoVo simpleSupplierInfoVo = supplierInfoVoMap.get(iceBox.getSupplierId());
                    if (simpleSupplierInfoVo != null) {
                        boxVo.setSupplierName(simpleSupplierInfoVo.getName());
                        boxVo.setSupplierAddress(simpleSupplierInfoVo.getAddress());
                        boxVo.setLinkman(simpleSupplierInfoVo.getLinkMan());
                        boxVo.setLinkmanMobile(simpleSupplierInfoVo.getLinkManMobile());
                    }
                    iceBoxCountMap.put(iceBox.getModelId(), 1);
                    iceBoxVoList.add(boxVo);
                }
                if (CollectionUtil.isNotEmpty(iceBoxVoList)) {
                    for (IceBoxVo iceBoxVo : iceBoxVoList) {
                        Integer count = iceBoxCountMap.get(iceBoxVo.getModelId());
                        iceBoxVo.setIceBoxCount(count);
                    }
                    iceBoxVos.addAll(iceBoxVoList);
                }
            }
        }
        return iceBoxVos;
    }

    @Override
    public IceBoxVo findBySupplierIdAndModelId(Integer supplierId, Integer modelId) {
        SubordinateInfoVo subordinateInfoVo = FeignResponseUtil.getFeignData(feignSupplierClient.findSupplierBySupplierId(supplierId));
        if (subordinateInfoVo == null) {
            throw new ImproperOptionException("???????????????????????????");
        }
        List<IceBox> iceBoxes = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getModelId, modelId).eq(IceBox::getSupplierId, supplierId).eq(IceBox::getPutStatus, PutStatus.NO_PUT.getStatus()));
        if (CollectionUtil.isNotEmpty(iceBoxes)) {
            IceBox iceBox = iceBoxes.get(0);
            IceBoxVo iceBoxVo = new IceBoxVo();
            BeanUtils.copyProperties(iceBox, iceBoxVo);
            iceBoxVo.setSupplierName(subordinateInfoVo.getName());
            iceBoxVo.setSupplierAddress(subordinateInfoVo.getAddress());
            iceBoxVo.setLinkman(subordinateInfoVo.getLinkman());
            iceBoxVo.setLinkmanMobile(subordinateInfoVo.getLinkmanMobile());
            return iceBoxVo;
        }
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, value = "transactionManager")
    public Map<String, Object> submitApply(List<IceBoxRequestVo> iceBoxRequestVos) throws InterruptedException {
        Map<String, Object> map = new HashMap<>();
        IceBoxRequestVo iceBoxRequestVo = iceBoxRequestVos.get(0);
        String applyNumber = "PUT" + IdUtil.simpleUUID().substring(0, 29);
        IcePutApply icePutApply = IcePutApply.builder()
                .applyNumber(applyNumber)
                .putStoreNumber(iceBoxRequestVo.getStoreNumber())
                .userId(iceBoxRequestVo.getUserId())
                .createdBy(iceBoxRequestVo.getUserId())
                .applyPit(iceBoxRequestVo.getApplyPit())
                .build();
        icePutApplyDao.insert(icePutApply);
        iceBoxRequestVo.setVisitTypeName(VisitCycleEnum.getDescByCode(FeignResponseUtil.getFeignData(feignIceboxQueryClient.selectVisitTypeForReport(iceBoxRequestVo.getStoreNumber()))));
        List<IceBoxPutModel.IceBoxModel> iceBoxModels = new ArrayList<>();
        BigDecimal totalMoney = new BigDecimal(0);
        //????????????????????????
        MatchRuleVo matchRuleVo = new MatchRuleVo();
        matchRuleVo.setOpreateType(3);
        matchRuleVo.setDeptId(iceBoxRequestVo.getMarketAreaId());
        matchRuleVo.setType(2);
        SysRuleIceDetailVo ruleIceDetailVo = FeignResponseUtil.getFeignData(feignDeptRuleClient.matchIceRule(matchRuleVo));
        Integer freeType = null;
        if (ruleIceDetailVo != null) {
            freeType = FreePayTypeEnum.UN_FREE.getType();
            if (ruleIceDetailVo.getIsNoDeposit().equals(1)) {
                freeType = FreePayTypeEnum.IS_FREE.getType();
            }
        }
        for (IceBoxRequestVo requestVo : iceBoxRequestVos) {
            for (int i = 0; i < requestVo.getApplyCount(); i++) {
                List<IceBox> iceBoxes = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getModelId, requestVo.getModelId()).eq(IceBox::getSupplierId, iceBoxRequestVo.getSupplierId()).eq(IceBox::getPutStatus, PutStatus.NO_PUT.getStatus()));
                IceBox iceBox = null;
                if (CollectionUtil.isNotEmpty(iceBoxes)) {
                    iceBox = iceBoxes.get(0);

                } else {
                    throw new ImproperOptionException("??????????????????");
                }
                RedisLockUtil lock = new RedisLockUtil(redisTemplate, RedisConstant.ICE_BOX_LOCK + iceBox.getId(), 5000, 10000);
                try {
                    if (lock.lock()) {
                        log.info("????????????????????????-->" + JSON.toJSONString(iceBox));
                        iceBox.setPutStoreNumber(requestVo.getStoreNumber()); //
                        iceBox.setPutStatus(PutStatus.LOCK_PUT.getStatus());
                        iceBox.setUpdatedTime(new Date());
                        iceBoxDao.updateById(iceBox);
                        totalMoney = totalMoney.add(iceBox.getDepositMoney());

                        IceBoxExtend iceBoxExtend = new IceBoxExtend();
                        iceBoxExtend.setId(iceBox.getId());
                        iceBoxExtend.setLastApplyNumber(applyNumber);
                        iceBoxExtend.setLastPutId(icePutApply.getId());
                        iceBoxExtend.setLastPutTime(new Date());
                        iceBoxExtendDao.updateById(iceBoxExtend);

                        IcePutApplyRelateBox relateBox = new IcePutApplyRelateBox();
                        relateBox.setApplyNumber(applyNumber);
                        relateBox.setBoxId(iceBox.getId());
                        relateBox.setModelId(iceBox.getModelId());
                        relateBox.setFreeType(requestVo.getFreeType());
                        icePutApplyRelateBoxDao.insert(relateBox);

                        //?????????????????????????????????????????????????????????????????????
//                        DateTime dateTime = new DateTime(System.currentTimeMillis()).plusYears(1);
//                        IcePutPactRecord icePutPactRecord = IcePutPactRecord.builder()
//                                .applyNumber(applyNumber)
//                                .boxId(iceBox.getId())
//                                .storeNumber(requestVo.getStoreNumber())
//                                .putTime(new Date())
//                                .putExpireTime(dateTime.toDate())
//                                .build();
//                        icePutPactRecordDao.insert(icePutPactRecord);

                        IceTransferRecord iceTransferRecord = IceTransferRecord.builder()
                                .applyNumber(applyNumber)
                                .applyTime(new Date())
                                .applyUserId(requestVo.getUserId())
                                .boxId(iceBox.getId())
                                .createTime(new Date())
                                .recordStatus(RecordStatus.APPLY_ING.getStatus())
                                .serviceType(ServiceType.IS_PUT.getType())
                                .storeNumber(requestVo.getStoreNumber())
                                .supplierId(requestVo.getSupplierId())
                                .build();
                        iceTransferRecord.setTransferMoney(new BigDecimal(0));
                        if (FreePayTypeEnum.UN_FREE.getType() == requestVo.getFreeType().intValue()) {
                            iceTransferRecord.setTransferMoney(iceBox.getDepositMoney());
                        }
                        iceTransferRecordDao.insert(iceTransferRecord);

                    }
                } catch (Exception e) {
                    throw e;
                } finally {
                    lock.unlock();
                }
            }
            SubordinateInfoVo supplier = FeignResponseUtil.getFeignData(feignSupplierClient.findSupplierBySupplierId(requestVo.getSupplierId()));
            if (supplier == null) {
                log.info("???????????????id--??????{}??????????????????????????????", iceBoxRequestVo.getSupplierId());
                throw new ImproperOptionException("???????????????????????????");
            }
            IceBoxPutModel.IceBoxModel iceBoxModel = new IceBoxPutModel.IceBoxModel(requestVo.getChestModel(), requestVo.getChestName(), requestVo.getDepositMoney(), requestVo.getApplyCount(),
                    requestVo.getFreeType(), requestVo.getSupplierName(), supplier.getAddress(), supplier.getLinkman(), supplier.getLinkmanMobile());
            iceBoxModels.add(iceBoxModel);
        }
//        String orderNum = CommonUtil.generateOrderNumber();
//        IcePutOrder icePutOrder = IcePutOrder.builder()
//                .applyNumber(applyNumber)
//                .orderNum(orderNum)
//                .totalMoney(totalMoney)
//                .status(OrderStatus.IS_PAY_ING.getStatus())
//                .createdBy(iceBoxRequestVo.getUserId())
//                .createdTime(new Date())
//                .build();
//        icePutOrderDao.insert(icePutOrder);
        boolean regionLeaderCheck = false;
        List<Integer> freeTypes = iceBoxRequestVos.stream().map(x -> x.getFreeType()).collect(Collectors.toList());
        if (freeTypes.contains(FreePayTypeEnum.IS_FREE.getType())) {
            regionLeaderCheck = true;
        }
        map.put("isCheck", 0);
        map = createIceBoxPutExamine(iceBoxRequestVo, applyNumber, iceBoxModels, regionLeaderCheck, ruleIceDetailVo);
        List<SessionExamineVo.VisitExamineNodeVo> iceBoxPutExamine = (List<SessionExamineVo.VisitExamineNodeVo>) map.get("iceBoxPutExamine");
        if (CollectionUtil.isNotEmpty(iceBoxPutExamine)) {
            SessionExamineVo.VisitExamineNodeVo visitExamineNodeVo = iceBoxPutExamine.get(0);
            icePutApply.setExamineId(visitExamineNodeVo.getExamineId());
            icePutApplyDao.updateById(icePutApply);
        }
        return map;
    }

    private Map<String, Object> createIceBoxPutExamine(IceBoxRequestVo iceBoxRequestVo, String applyNumber, List<IceBoxPutModel.IceBoxModel> iceBoxModels, boolean regionLeaderCheck, SysRuleIceDetailVo ruleIceDetailVo) {
        Map<String, Object> map = new HashMap<>();
        map.put("iceBoxPutExamine", new ArrayList<>());

        SimpleUserInfoVo simpleUserInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findSimpleUserByIdAndDept(iceBoxRequestVo.getUserId(), iceBoxRequestVo.getUserMarketAreaId()));
        Map<Integer, SessionUserInfoVo> sessionUserInfoMap = FeignResponseUtil.getFeignData(feignDeptClient.findLevelLeaderByDeptIdNew(iceBoxRequestVo.getUserMarketAreaId()));
        List<Integer> ids = new ArrayList<Integer>();
        //????????????????????????
        SessionUserInfoVo groupUser = new SessionUserInfoVo();
        SessionUserInfoVo serviceUser = new SessionUserInfoVo();
        SessionUserInfoVo regionUser = new SessionUserInfoVo();
        SessionUserInfoVo businessUser = new SessionUserInfoVo();
        Set<Integer> keySet = sessionUserInfoMap.keySet();
        for (Integer key : keySet) {
            SessionUserInfoVo userInfoVo = sessionUserInfoMap.get(key);
            if (userInfoVo == null) {
                continue;
            }
            if (DeptTypeEnum.GROUP.getType().equals(userInfoVo.getDeptType())) {
                groupUser = userInfoVo;
                if (userInfoVo.getId() == null) {
                    groupUser = null;
                }
                continue;
            }
            if (DeptTypeEnum.SERVICE.getType().equals(userInfoVo.getDeptType())) {
                serviceUser = userInfoVo;
                if (userInfoVo.getId() == null) {
                    serviceUser = null;
                }
                continue;
            }
            if (DeptTypeEnum.LARGE_AREA.getType().equals(userInfoVo.getDeptType())) {
                regionUser = userInfoVo;
                if (userInfoVo.getId() == null) {
                    regionUser = null;
                }
                continue;
            }
            if (DeptTypeEnum.BUSINESS_UNIT.getType().equals(userInfoVo.getDeptType())) {
                businessUser = userInfoVo;
                if (userInfoVo.getId() == null) {
                    businessUser = null;
                }
                continue;
            }

        }

        Integer examineUserId = FeignResponseUtil.getFeignData(feignIceBoxExamineUserClient.getExamineUserIdByDeptId(iceBoxRequestVo.getUserMarketAreaId()));

        if (ruleIceDetailVo != null) {
            //???????????????????????????
            if (!ruleIceDetailVo.getIsApproval()) {
                log.info("?????????????????????????????????----??????{}???", applyNumber);
                IceBoxRequest iceBoxRequest = new IceBoxRequest();
                iceBoxRequest.setApplyNumber(applyNumber);
                iceBoxRequest.setUpdateBy(serviceUser.getId());
                iceBoxRequest.setMarketAreaId(iceBoxRequestVo.getMarketAreaId());
                dealCheckPassIceBox(iceBoxRequest);
                map.put("isCheck", 1);
                return map;
            }

            //???????????????
            if (ExamineLastApprovalEnum.GROUP.getType().equals(ruleIceDetailVo.getLastApprovalNode())) {
                if (groupUser == null || groupUser.getId() == null) {
                    throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,??????????????????");
                }
                //????????????????????????????????????????????????????????????????????????
                if (simpleUserInfoVo.getId().equals(groupUser.getId())
                        || simpleUserInfoVo.getDeptType().equals(DeptTypeEnum.SERVICE.getType())
                        || simpleUserInfoVo.getDeptType().equals(DeptTypeEnum.LARGE_AREA.getType())
                        || simpleUserInfoVo.getDeptType().equals(DeptTypeEnum.BUSINESS_UNIT.getType())
                        || simpleUserInfoVo.getDeptType().equals(DeptTypeEnum.THIS_PART.getType())) {
                    IceBoxRequest iceBoxRequest = new IceBoxRequest();
                    iceBoxRequest.setApplyNumber(applyNumber);
                    iceBoxRequest.setUpdateBy(serviceUser.getId());
                    iceBoxRequest.setMarketAreaId(iceBoxRequestVo.getMarketAreaId());
                    dealCheckPassIceBox(iceBoxRequest);
                    map.put("isCheck", 1);
                    return map;
                }
                ids.add(groupUser.getId());
                if (CollectionUtil.isEmpty(ids)) {
                    throw new NormalOptionException(Constants.API_CODE_FAIL, "??????????????????????????????????????????");
                }
                createIceBoxPutModel(iceBoxRequestVo, applyNumber, iceBoxModels, map, simpleUserInfoVo, ids);
                return map;
            }

            List<String> skipNodeList = new ArrayList<>();
            String skipNode = ruleIceDetailVo.getSkipNode();
            if (StringUtils.isNotBlank(skipNode)) {
                String[] skipNodeArr = skipNode.split(",");
                if (skipNodeArr != null) {
                    skipNodeList = Arrays.asList(skipNodeArr);
                }
            }
            /**
             * ?????????????????????
             * 1???????????????????????????
             * 1.1???????????????????????????????????????????????????????????????????????????????????????????????????
             * 1.2??????????????????????????????????????????????????????????????????????????????????????????????????????????????????
             */

            if (ExamineLastApprovalEnum.SERVICE.getType().equals(ruleIceDetailVo.getLastApprovalNode())) {


                //????????????????????????????????????????????????????????????????????????????????????
                if (simpleUserInfoVo.getId().equals(serviceUser.getId())
                        || simpleUserInfoVo.getDeptType().equals(DeptTypeEnum.LARGE_AREA.getType())
                        || simpleUserInfoVo.getDeptType().equals(DeptTypeEnum.BUSINESS_UNIT.getType())
                        || simpleUserInfoVo.getDeptType().equals(DeptTypeEnum.THIS_PART.getType())) {

                    IceBoxRequest iceBoxRequest = new IceBoxRequest();
                    iceBoxRequest.setApplyNumber(applyNumber);
                    iceBoxRequest.setUpdateBy(serviceUser.getId());
                    iceBoxRequest.setMarketAreaId(iceBoxRequestVo.getMarketAreaId());
                    dealCheckPassIceBox(iceBoxRequest);
                    map.put("isCheck", 1);
                    return map;
                }

                //?????????????????????????????????
                if (ruleIceDetailVo.getIsLeaderApproval()) {
                    /**
                     * ??????????????????
                     * ????????????????????????????????????
                     */
                    if (CollectionUtil.isEmpty(skipNodeList)) {
                        /**
                         * ??????????????????????????????
                         * ??????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                         */
                        SessionUserInfoVo userInfoVo = sessionUserInfoMap.get(0);
                        Boolean flag = false;
                        if (DeptTypeEnum.GROUP.getType().equals(simpleUserInfoVo.getDeptType())) {
                            userInfoVo = groupUser;
                            if (simpleUserInfoVo.getIsLearder().equals(1)) {
                                userInfoVo = serviceUser;
                                flag = true;
                            }
                        }
                        if (DeptTypeEnum.SERVICE.getType().equals(simpleUserInfoVo.getDeptType())) {
                            userInfoVo = serviceUser;
                            flag = true;
                        }

                        if (userInfoVo == null || userInfoVo.getId() == null) {
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,????????????????????????");
                        }
                        if ((userInfoVo.getId() != null && userInfoVo.getId().equals(simpleUserInfoVo.getId()))) {
                            IceBoxRequest iceBoxRequest = new IceBoxRequest();
                            iceBoxRequest.setApplyNumber(applyNumber);
                            iceBoxRequest.setUpdateBy(serviceUser.getId());
                            iceBoxRequest.setMarketAreaId(iceBoxRequestVo.getMarketAreaId());
                            dealCheckPassIceBox(iceBoxRequest);
                            map.put("isCheck", 1);
                            return map;
                        }
                        if (!ids.contains(userInfoVo.getId())) {
                            ids.add(userInfoVo.getId());
                        }
                       if(flag){
                           if (examineUserId == null) {
                               throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,??????????????????????????????");
                           }
                           if (!ids.contains(examineUserId)) {
                               ids.add(examineUserId);
                           }
                       }
                        createIceBoxPutModel(iceBoxRequestVo, applyNumber, iceBoxModels, map, simpleUserInfoVo, ids);
                        return map;
                    }
                }


                /**
                 * ??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                 */
                if (CollectionUtil.isNotEmpty(skipNodeList)) {
                    if (serviceUser == null || serviceUser.getId() == null) {
                        throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,???????????????????????????");
                    }
                    if (!ids.contains(serviceUser.getId())) {
                        ids.add(serviceUser.getId());

                        if (examineUserId == null) {
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,??????????????????????????????");
                        }
                        if (!ids.contains(examineUserId)) {
                            ids.add(examineUserId);
                        }
                    }
                    createIceBoxPutModel(iceBoxRequestVo, applyNumber, iceBoxModels, map, simpleUserInfoVo, ids);
                    return map;
                } else {
                    if (groupUser == null || groupUser.getId() == null) {
                        throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,??????????????????");
                    }
                    if (!ids.contains(groupUser.getId()) && !simpleUserInfoVo.getId().equals(groupUser.getId())) {
                        ids.add(groupUser.getId());
                    }

                    if (serviceUser == null || serviceUser.getId() == null) {
                        throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,???????????????????????????");
                    }
                    if (!ids.contains(serviceUser.getId()) && !simpleUserInfoVo.getId().equals(serviceUser.getId())) {
                        ids.add(serviceUser.getId());

                        if (examineUserId == null) {
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,??????????????????????????????");
                        }
                        if (!ids.contains(examineUserId)) {
                            ids.add(examineUserId);
                        }
                    }
                    createIceBoxPutModel(iceBoxRequestVo, applyNumber, iceBoxModels, map, simpleUserInfoVo, ids);
                    return map;
                }

            }

            //??????????????????
            if (ExamineLastApprovalEnum.LARGE_AREA.getType().equals(ruleIceDetailVo.getLastApprovalNode())) {
                //??????????????????????????????????????????????????????????????????????????????
                if (simpleUserInfoVo.getId().equals(regionUser.getId())
                        || simpleUserInfoVo.getDeptType().equals(DeptTypeEnum.BUSINESS_UNIT.getType())
                        || simpleUserInfoVo.getDeptType().equals(DeptTypeEnum.THIS_PART.getType())) {
                    IceBoxRequest iceBoxRequest = new IceBoxRequest();
                    iceBoxRequest.setApplyNumber(applyNumber);
                    iceBoxRequest.setUpdateBy(serviceUser.getId());
                    iceBoxRequest.setMarketAreaId(iceBoxRequestVo.getMarketAreaId());
                    dealCheckPassIceBox(iceBoxRequest);
                    map.put("isCheck", 1);
                    return map;
                }

                //?????????????????????????????????
                if (ruleIceDetailVo.getIsLeaderApproval()) {
                    /**
                     * ??????????????????
                     * ????????????????????????????????????
                     */
                    if (CollectionUtil.isEmpty(skipNodeList)) {
                        /**
                         * ??????????????????????????????
                         * ????????????????????????????????????????????????????????????????????????????????????????????????????????????
                         */
                        SessionUserInfoVo userInfoVo = sessionUserInfoMap.get(0);
                        Boolean flag = false;
                        if (DeptTypeEnum.GROUP.getType().equals(simpleUserInfoVo.getDeptType())) {
                            userInfoVo = groupUser;
                            if (simpleUserInfoVo.getIsLearder().equals(1)) {
                                userInfoVo = serviceUser;
                                flag = true;
                            }
                        }
                        if (DeptTypeEnum.SERVICE.getType().equals(simpleUserInfoVo.getDeptType())) {
                            userInfoVo = serviceUser;
                            flag = true;
                            if (simpleUserInfoVo.getIsLearder().equals(1)) {
                                userInfoVo = regionUser;
                            }
                        }
                        if (DeptTypeEnum.LARGE_AREA.getType().equals(simpleUserInfoVo.getDeptType())) {
                            userInfoVo = regionUser;
                        }
                        if (userInfoVo == null || userInfoVo.getId() == null) {
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,????????????????????????");
                        }
                        if ((userInfoVo.getId() != null && userInfoVo.getId().equals(simpleUserInfoVo.getId()))) {
                            IceBoxRequest iceBoxRequest = new IceBoxRequest();
                            iceBoxRequest.setApplyNumber(applyNumber);
                            iceBoxRequest.setUpdateBy(serviceUser.getId());
                            iceBoxRequest.setMarketAreaId(iceBoxRequestVo.getMarketAreaId());
                            dealCheckPassIceBox(iceBoxRequest);
                            map.put("isCheck", 1);
                            return map;
                        }
                        if (!ids.contains(userInfoVo.getId())) {
                            ids.add(userInfoVo.getId());
                        }
                        if (flag) {
                            if (examineUserId == null) {
                                throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,??????????????????????????????");
                            }
                            if (!ids.contains(examineUserId)) {
                                ids.add(examineUserId);
                            }
                        }
                        createIceBoxPutModel(iceBoxRequestVo, applyNumber, iceBoxModels, map, simpleUserInfoVo, ids);
                        return map;
                    } else {
                        /**
                         * ???????????????????????????
                         * ??????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                         * ?????????????????????(1-????????? 2-????????? 3-?????? 4-?????????)
                         */
                        List<Integer> allNodes = new ArrayList<>();
                        allNodes.add(1);
                        allNodes.add(2);
                        allNodes.add(3);
                        Iterator<Integer> iterator = allNodes.iterator();

                        while (iterator.hasNext()) {
                            Integer next = iterator.next();
                            if (skipNode.contains(next + "")) {
                                iterator.remove();
                            }
                        }
                        SessionUserInfoVo userInfoVo = null;

                        Boolean flag = false;
                        if (DeptTypeEnum.GROUP.getType().equals(simpleUserInfoVo.getDeptType())) {

                            if (simpleUserInfoVo.getIsLearder().equals(1)) {
                                if (allNodes.contains(2)) {
                                    userInfoVo = serviceUser;
                                    flag = true;
                                }
                                if (!allNodes.contains(2) && allNodes.contains(3)) {
                                    userInfoVo = regionUser;
                                }
                            } else {
                                if (allNodes.contains(1)) {
                                    userInfoVo = groupUser;
                                }
                                if (!allNodes.contains(1) && allNodes.contains(2)) {
                                    userInfoVo = serviceUser;
                                    flag = true;
                                }
                                if (!allNodes.contains(1) && !allNodes.contains(2) && allNodes.contains(3)) {
                                    userInfoVo = regionUser;
                                }
                            }
                        }

                        if (DeptTypeEnum.SERVICE.getType().equals(simpleUserInfoVo.getDeptType())) {

                            if (simpleUserInfoVo.getIsLearder().equals(1)) {
                                userInfoVo = regionUser;
                            } else {
                                if (allNodes.contains(2)) {
                                    userInfoVo = serviceUser;
                                    flag = true;
                                }
                                if (!allNodes.contains(2) && allNodes.contains(3)) {
                                    userInfoVo = regionUser;
                                }
                            }
                        }

                        if (DeptTypeEnum.LARGE_AREA.getType().equals(simpleUserInfoVo.getDeptType())) {
                            userInfoVo = regionUser;
                        }

                        if (userInfoVo == null || userInfoVo.getId() == null) {
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,????????????????????????");
                        }
                        if ((userInfoVo.getId().equals(simpleUserInfoVo.getId()))) {
                            IceBoxRequest iceBoxRequest = new IceBoxRequest();
                            iceBoxRequest.setApplyNumber(applyNumber);
                            iceBoxRequest.setUpdateBy(serviceUser.getId());
                            iceBoxRequest.setMarketAreaId(iceBoxRequestVo.getMarketAreaId());
                            dealCheckPassIceBox(iceBoxRequest);
                            map.put("isCheck", 1);
                            return map;
                        }
                        if (!ids.contains(userInfoVo.getId())) {
                            ids.add(userInfoVo.getId());
                        }
                        if(flag){
                            if (examineUserId == null) {
                                throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,??????????????????????????????");
                            }
                            if (!ids.contains(examineUserId)) {
                                ids.add(examineUserId);
                            }
                        }
                        createIceBoxPutModel(iceBoxRequestVo, applyNumber, iceBoxModels, map, simpleUserInfoVo, ids);
                        return map;

                    }
                }
                /**
                 * ?????????????????????
                 * ??????????????????????????????
                 * ??????????????????????????????
                 */
                if (CollectionUtil.isEmpty(skipNodeList)) {
                    if (DeptTypeEnum.GROUP.getType().equals(simpleUserInfoVo.getDeptType())) {
                        if (regionUser == null || regionUser.getId() == null) {
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,????????????????????????");
                        }

                        if (serviceUser == null || serviceUser.getId() == null) {
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,???????????????????????????");
                        }

                        if (simpleUserInfoVo.getIsLearder().equals(0)) {
                            if (groupUser == null || groupUser.getId() == null) {
                                throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,??????????????????");
                            }
                            if (!ids.contains(groupUser.getId()) && !simpleUserInfoVo.getId().equals(groupUser.getId())) {
                                ids.add(groupUser.getId());
                            }
                        }


                        if (!ids.contains(serviceUser.getId()) && !simpleUserInfoVo.getId().equals(serviceUser.getId())) {
                            ids.add(serviceUser.getId());
                        }

                        if (examineUserId == null) {
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,??????????????????????????????");
                        }
                        if (!ids.contains(examineUserId)) {
                            ids.add(examineUserId);
                        }

                        if (!ids.contains(regionUser.getId()) && !simpleUserInfoVo.getId().equals(regionUser.getId())) {
                            ids.add(regionUser.getId());
                        }

                        if (CollectionUtil.isEmpty(ids)) {
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "??????????????????????????????????????????");
                        }
                    }
                    if (DeptTypeEnum.SERVICE.getType().equals(simpleUserInfoVo.getDeptType())) {

                        if (regionUser == null || regionUser.getId() == null) {
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,????????????????????????");
                        }

                        if (simpleUserInfoVo.getIsLearder().equals(0)) {
                            if (serviceUser == null || serviceUser.getId() == null) {
                                throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,???????????????????????????");
                            }

                            if (!ids.contains(serviceUser.getId()) && !simpleUserInfoVo.getId().equals(serviceUser.getId())) {
                                ids.add(serviceUser.getId());
                            }

                            if (examineUserId == null) {
                                throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,??????????????????????????????");
                            }
                            if (!ids.contains(examineUserId)) {
                                ids.add(examineUserId);
                            }
                        }


                        if (!ids.contains(regionUser.getId()) && !simpleUserInfoVo.getId().equals(regionUser.getId())) {
                            ids.add(regionUser.getId());
                        }

                        if (CollectionUtil.isEmpty(ids)) {
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "??????????????????????????????????????????");
                        }
                    }

                    if (DeptTypeEnum.LARGE_AREA.getType().equals(simpleUserInfoVo.getDeptType())) {

                        if (regionUser == null || regionUser.getId() == null) {
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,????????????????????????");
                        }

                        if (!ids.contains(regionUser.getId()) && !simpleUserInfoVo.getId().equals(regionUser.getId())) {
                            ids.add(regionUser.getId());
                        }

                        if (CollectionUtil.isEmpty(ids)) {
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "??????????????????????????????????????????");
                        }
                    }
                    createIceBoxPutModel(iceBoxRequestVo, applyNumber, iceBoxModels, map, simpleUserInfoVo, ids);
                    return map;
                } else {
                    List<Integer> allNodes = new ArrayList<>();
                    allNodes.add(1);
                    allNodes.add(2);
                    allNodes.add(3);
                    Iterator<Integer> iterator = allNodes.iterator();

                    while (iterator.hasNext()) {
                        Integer next = iterator.next();
                        if (skipNode.contains(next + "")) {
                            iterator.remove();
                        }
                    }

                    if (DeptTypeEnum.GROUP.getType().equals(simpleUserInfoVo.getDeptType())) {

                        if (simpleUserInfoVo.getIsLearder().equals(1)) {
                            if (allNodes.contains(2) && !ids.contains(serviceUser.getId())) {
                                ids.add(serviceUser.getId());

                                if (examineUserId == null) {
                                    throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,??????????????????????????????");
                                }
                                if (!ids.contains(examineUserId)) {
                                    ids.add(examineUserId);
                                }
                            }
                        } else {
                            if (allNodes.contains(1) && !ids.contains(groupUser.getId())) {
                                ids.add(groupUser.getId());
                            }
                            if (allNodes.contains(2) && !ids.contains(serviceUser.getId())) {
                                ids.add(serviceUser.getId());

                                if (examineUserId == null) {
                                    throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,??????????????????????????????");
                                }
                                if (!ids.contains(examineUserId)) {
                                    ids.add(examineUserId);
                                }
                            }
                        }
                    }

                    if (DeptTypeEnum.SERVICE.getType().equals(simpleUserInfoVo.getDeptType())) {
                        if (!simpleUserInfoVo.getIsLearder().equals(1)) {
                            if (allNodes.contains(2) && !ids.contains(serviceUser.getId())) {
                                ids.add(serviceUser.getId());

                                if (examineUserId == null) {
                                    throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,??????????????????????????????");
                                }
                                if (!ids.contains(examineUserId)) {
                                    ids.add(examineUserId);
                                }
                            }
                        }
                    }

                    if (allNodes.contains(3) && !ids.contains(regionUser.getId())) {
                        ids.add(regionUser.getId());
                    }
                    createIceBoxPutModel(iceBoxRequestVo, applyNumber, iceBoxModels, map, simpleUserInfoVo, ids);
                    return map;
                }

            }

            //?????????????????????
            if (ExamineLastApprovalEnum.BUSINESS_UNIT.getType().equals(ruleIceDetailVo.getLastApprovalNode())) {

                //?????????????????????????????????????????????????????????????????????????????????
                if (simpleUserInfoVo.getId().equals(businessUser.getId()) || simpleUserInfoVo.getDeptType().equals(DeptTypeEnum.THIS_PART.getType())) {
                    IceBoxRequest iceBoxRequest = new IceBoxRequest();
                    iceBoxRequest.setApplyNumber(applyNumber);
                    iceBoxRequest.setUpdateBy(serviceUser.getId());
                    iceBoxRequest.setMarketAreaId(iceBoxRequestVo.getMarketAreaId());
                    dealCheckPassIceBox(iceBoxRequest);
                    map.put("isCheck", 1);
                    return map;
                }

                //?????????????????????????????????
                if (ruleIceDetailVo.getIsLeaderApproval()) {
                    /**
                     * ??????????????????
                     * ????????????????????????????????????
                     */
                    if (CollectionUtil.isEmpty(skipNodeList)) {
                        /**
                         * ??????????????????????????????
                         * ????????????????????????????????????????????????????????????????????????????????????????????????????????????
                         */
                        Boolean flag = false;
                        SessionUserInfoVo userInfoVo = sessionUserInfoMap.get(0);
                        if (DeptTypeEnum.GROUP.getType().equals(simpleUserInfoVo.getDeptType())) {
                            userInfoVo = groupUser;
                            if (simpleUserInfoVo.getIsLearder().equals(1)) {
                                userInfoVo = serviceUser;
                                flag = true;
                            }
                        }
                        if (DeptTypeEnum.SERVICE.getType().equals(simpleUserInfoVo.getDeptType())) {
                            userInfoVo = serviceUser;
                            if (simpleUserInfoVo.getIsLearder().equals(1)) {
                                userInfoVo = regionUser;
                            }
                        }
                        if (DeptTypeEnum.LARGE_AREA.getType().equals(simpleUserInfoVo.getDeptType())) {
                            userInfoVo = regionUser;
                            if (simpleUserInfoVo.getIsLearder().equals(1)) {
                                userInfoVo = businessUser;
                            }
                        }
                        if (DeptTypeEnum.BUSINESS_UNIT.getType().equals(simpleUserInfoVo.getDeptType())) {
                            userInfoVo = businessUser;
                        }
                        if (userInfoVo == null || userInfoVo.getId() == null) {
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,????????????????????????");
                        }
                        if ((userInfoVo.getId() != null && userInfoVo.getId().equals(simpleUserInfoVo.getId()))) {
                            IceBoxRequest iceBoxRequest = new IceBoxRequest();
                            iceBoxRequest.setApplyNumber(applyNumber);
                            iceBoxRequest.setUpdateBy(serviceUser.getId());
                            iceBoxRequest.setMarketAreaId(iceBoxRequestVo.getMarketAreaId());
                            dealCheckPassIceBox(iceBoxRequest);
                            map.put("isCheck", 1);
                            return map;
                        }
                        if (!ids.contains(userInfoVo.getId())) {
                            ids.add(userInfoVo.getId());
                        }
                        if(flag){
                            if (examineUserId == null) {
                                throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,??????????????????????????????");
                            }
                            if (!ids.contains(examineUserId)) {
                                ids.add(examineUserId);
                            }
                        }
                        createIceBoxPutModel(iceBoxRequestVo, applyNumber, iceBoxModels, map, simpleUserInfoVo, ids);
                        return map;
                    } else {
                        /**
                         * ???????????????????????????
                         * ??????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                         * ?????????????????????(1-????????? 2-????????? 3-?????? 4-?????????)
                         */
                        List<Integer> allNodes = new ArrayList<>();
                        allNodes.add(1);
                        allNodes.add(2);
                        allNodes.add(3);
                        allNodes.add(4);
                        Iterator<Integer> iterator = allNodes.iterator();

                        while (iterator.hasNext()) {
                            Integer next = iterator.next();
                            if (skipNode.contains(next + "")) {
                                iterator.remove();
                            }
                        }

                        SessionUserInfoVo userInfoVo = null;
                        Boolean flag = false;
                        if (DeptTypeEnum.GROUP.getType().equals(simpleUserInfoVo.getDeptType())) {

                            if (simpleUserInfoVo.getIsLearder().equals(1)) {
                                if (allNodes.contains(2)) {
                                    userInfoVo = serviceUser;
                                    flag = true;
                                }
                                if (!allNodes.contains(2) && allNodes.contains(3)) {
                                    userInfoVo = regionUser;
                                }
                            } else {
                                if (allNodes.contains(1)) {
                                    userInfoVo = groupUser;
                                }
                                if (!allNodes.contains(1) && allNodes.contains(2)) {
                                    userInfoVo = serviceUser;
                                    flag = true;
                                }
                                if (!allNodes.contains(1) && !allNodes.contains(2) && allNodes.contains(3)) {
                                    userInfoVo = regionUser;
                                }
                                if (!allNodes.contains(1) && !allNodes.contains(2) && !allNodes.contains(3) && allNodes.contains(4)) {
                                    userInfoVo = businessUser;
                                }
                            }
                        }

                        if (DeptTypeEnum.SERVICE.getType().equals(simpleUserInfoVo.getDeptType())) {

                            if (simpleUserInfoVo.getIsLearder().equals(1)) {
                                if (allNodes.contains(3)) {
                                    userInfoVo = regionUser;
                                }
                                if (!allNodes.contains(3) && allNodes.contains(4)) {
                                    userInfoVo = businessUser;
                                }

                            } else {
                                if (allNodes.contains(2)) {
                                    userInfoVo = serviceUser;
                                    flag = true;
                                }
                                if (!allNodes.contains(2) && allNodes.contains(3)) {
                                    userInfoVo = regionUser;
                                }
                                if (!allNodes.contains(2) && !allNodes.contains(3) && allNodes.contains(4)) {
                                    userInfoVo = businessUser;
                                }
                            }
                        }

                        if (DeptTypeEnum.LARGE_AREA.getType().equals(simpleUserInfoVo.getDeptType())) {

                            if (simpleUserInfoVo.getIsLearder().equals(1)) {
                                userInfoVo = businessUser;
                            } else {
                                if (allNodes.contains(3)) {
                                    userInfoVo = regionUser;
                                }
                                if (!allNodes.contains(3) && allNodes.contains(4)) {
                                    userInfoVo = businessUser;
                                }
                            }
                        }

                        if (DeptTypeEnum.BUSINESS_UNIT.getType().equals(simpleUserInfoVo.getDeptType())) {
                            userInfoVo = businessUser;
                        }

                        if (userInfoVo == null || userInfoVo.getId() == null) {
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,????????????????????????");
                        }
                        if ((userInfoVo.getId().equals(simpleUserInfoVo.getId()))) {
                            IceBoxRequest iceBoxRequest = new IceBoxRequest();
                            iceBoxRequest.setApplyNumber(applyNumber);
                            iceBoxRequest.setUpdateBy(serviceUser.getId());
                            iceBoxRequest.setMarketAreaId(iceBoxRequestVo.getMarketAreaId());
                            dealCheckPassIceBox(iceBoxRequest);
                            map.put("isCheck", 1);
                            return map;
                        }
                        if (!ids.contains(userInfoVo.getId())) {
                            ids.add(userInfoVo.getId());
                        }

                        if(flag){
                            if (examineUserId == null) {
                                throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,??????????????????????????????");
                            }
                            if (!ids.contains(examineUserId)) {
                                ids.add(examineUserId);
                            }
                        }
                        createIceBoxPutModel(iceBoxRequestVo, applyNumber, iceBoxModels, map, simpleUserInfoVo, ids);
                        return map;
                    }
                }
                /**
                 * ?????????????????????
                 * ??????????????????????????????
                 * ??????????????????????????????
                 */
                if (CollectionUtil.isEmpty(skipNodeList)) {
                    if (DeptTypeEnum.GROUP.getType().equals(simpleUserInfoVo.getDeptType())) {
                        if (businessUser == null || businessUser.getId() == null) {
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,???????????????????????????");
                        }

                        if (regionUser == null || regionUser.getId() == null) {
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,????????????????????????");
                        }

                        if (serviceUser == null || serviceUser.getId() == null) {
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,???????????????????????????");
                        }

                        if (simpleUserInfoVo.getIsLearder().equals(0)) {
                            if (groupUser == null || groupUser.getId() == null) {
                                throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,??????????????????");
                            }
                            if (!ids.contains(groupUser.getId()) && !simpleUserInfoVo.getId().equals(groupUser.getId())) {
                                ids.add(groupUser.getId());
                            }
                        }


                        if (!ids.contains(serviceUser.getId()) && !simpleUserInfoVo.getId().equals(serviceUser.getId())) {
                            ids.add(serviceUser.getId());

                            if (examineUserId == null) {
                                throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,??????????????????????????????");
                            }
                            if (!ids.contains(examineUserId)) {
                                ids.add(examineUserId);
                            }
                        }

                        if (!ids.contains(regionUser.getId()) && !simpleUserInfoVo.getId().equals(regionUser.getId())) {
                            ids.add(regionUser.getId());
                        }
                        if (!ids.contains(businessUser.getId()) && !simpleUserInfoVo.getId().equals(businessUser.getId())) {
                            ids.add(businessUser.getId());
                        }

                        if (CollectionUtil.isEmpty(ids)) {
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "??????????????????????????????????????????");
                        }
                    }
                    if (DeptTypeEnum.SERVICE.getType().equals(simpleUserInfoVo.getDeptType())) {
                        if (businessUser == null || businessUser.getId() == null) {
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,???????????????????????????");
                        }

                        if (regionUser == null || regionUser.getId() == null) {
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,????????????????????????");
                        }

                        if (simpleUserInfoVo.getIsLearder().equals(0)) {
                            if (serviceUser == null || serviceUser.getId() == null) {
                                throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,???????????????????????????");
                            }

                            if (!ids.contains(serviceUser.getId()) && !simpleUserInfoVo.getId().equals(serviceUser.getId())) {
                                ids.add(serviceUser.getId());

                                if (examineUserId == null) {
                                    throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,??????????????????????????????");
                                }
                                if (!ids.contains(examineUserId)) {
                                    ids.add(examineUserId);
                                }
                            }
                        }


                        if (!ids.contains(regionUser.getId()) && !simpleUserInfoVo.getId().equals(regionUser.getId())) {
                            ids.add(regionUser.getId());
                        }
                        if (!ids.contains(businessUser.getId()) && !simpleUserInfoVo.getId().equals(businessUser.getId())) {
                            ids.add(businessUser.getId());
                        }

                        if (CollectionUtil.isEmpty(ids)) {
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "??????????????????????????????????????????");
                        }
                    }

                    if (DeptTypeEnum.LARGE_AREA.getType().equals(simpleUserInfoVo.getDeptType())) {
                        if (businessUser == null || businessUser.getId() == null) {
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,???????????????????????????");
                        }

                        if (simpleUserInfoVo.getIsLearder().equals(0)) {
                            if (regionUser == null || regionUser.getId() == null) {
                                throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,????????????????????????");
                            }

                            if (!ids.contains(regionUser.getId()) && !simpleUserInfoVo.getId().equals(regionUser.getId())) {
                                ids.add(regionUser.getId());
                            }
                        }

                        if (!ids.contains(businessUser.getId()) && !simpleUserInfoVo.getId().equals(businessUser.getId())) {
                            ids.add(businessUser.getId());
                        }

                        if (CollectionUtil.isEmpty(ids)) {
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "??????????????????????????????????????????");
                        }
                    }

                    if (DeptTypeEnum.BUSINESS_UNIT.getType().equals(simpleUserInfoVo.getDeptType())) {
                        if (businessUser == null || businessUser.getId() == null) {
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,???????????????????????????");
                        }
                        if (CollectionUtil.isEmpty(ids)) {
                            throw new NormalOptionException(Constants.API_CODE_FAIL, "??????????????????????????????????????????");
                        }
                    }

                    createIceBoxPutModel(iceBoxRequestVo, applyNumber, iceBoxModels, map, simpleUserInfoVo, ids);
                    return map;
                } else {
                    List<Integer> allNodes = new ArrayList<>();
                    allNodes.add(1);
                    allNodes.add(2);
                    allNodes.add(3);
                    allNodes.add(4);
                    Iterator<Integer> iterator = allNodes.iterator();

                    while (iterator.hasNext()) {
                        Integer next = iterator.next();
                        if (skipNode.contains(next + "")) {
                            iterator.remove();
                        }
                    }

                    if (DeptTypeEnum.GROUP.getType().equals(simpleUserInfoVo.getDeptType())) {
                        if (simpleUserInfoVo.getIsLearder().equals(1)) {
                            if (allNodes.contains(2) && !ids.contains(serviceUser.getId())) {
                                ids.add(serviceUser.getId());

                                if (examineUserId == null) {
                                    throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,??????????????????????????????");
                                }
                                if (!ids.contains(examineUserId)) {
                                    ids.add(examineUserId);
                                }
                            }
                            if (allNodes.contains(3) && !ids.contains(regionUser.getId())) {
                                ids.add(regionUser.getId());
                            }
                        } else {
                            if (allNodes.contains(1) && !ids.contains(groupUser.getId())) {
                                ids.add(groupUser.getId());
                            }
                            if (allNodes.contains(2) && !ids.contains(serviceUser.getId())) {
                                ids.add(serviceUser.getId());

                                if (examineUserId == null) {
                                    throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,??????????????????????????????");
                                }
                                if (!ids.contains(examineUserId)) {
                                    ids.add(examineUserId);
                                }
                            }
                            if (allNodes.contains(3) && !ids.contains(regionUser.getId())) {
                                ids.add(regionUser.getId());
                            }
                        }
                    }

                    if (DeptTypeEnum.SERVICE.getType().equals(simpleUserInfoVo.getDeptType())) {

                        if (!simpleUserInfoVo.getIsLearder().equals(1)) {
                            if (allNodes.contains(2) && !ids.contains(serviceUser.getId())) {
                                ids.add(serviceUser.getId());

                                if (examineUserId == null) {
                                    throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,??????????????????????????????");
                                }
                                if (!ids.contains(examineUserId)) {
                                    ids.add(examineUserId);
                                }
                            }
                        }
                        if (allNodes.contains(3) && !ids.contains(regionUser.getId())) {
                            ids.add(regionUser.getId());
                        }
                    }

                    if (allNodes.contains(4) && !ids.contains(businessUser.getId())) {
                        ids.add(businessUser.getId());
                    }

                    createIceBoxPutModel(iceBoxRequestVo, applyNumber, iceBoxModels, map, simpleUserInfoVo, ids);
                    return map;
                }
            }
        } else {
            if (!regionLeaderCheck && simpleUserInfoVo != null && DeptTypeEnum.SERVICE.getType().equals(simpleUserInfoVo.getDeptType()) && simpleUserInfoVo.getIsLearder().equals(1)) {
                IceBoxRequest iceBoxRequest = new IceBoxRequest();
                iceBoxRequest.setApplyNumber(applyNumber);
                iceBoxRequest.setUpdateBy(serviceUser.getId());
                iceBoxRequest.setMarketAreaId(iceBoxRequestVo.getMarketAreaId());
                dealCheckPassIceBox(iceBoxRequest);
                map.put("isCheck", 1);
                return map;
            }
            if (serviceUser == null) {
                throw new NormalOptionException(Constants.API_CODE_FAIL, "?????????????????????????????????????????????");
            }
            ids.add(serviceUser.getId());

            if (examineUserId == null) {
                throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,??????????????????????????????");
            }
            if (!ids.contains(examineUserId)) {
                ids.add(examineUserId);
            }

            if (regionLeaderCheck) {
                if (regionUser == null) {
                    throw new NormalOptionException(Constants.API_CODE_FAIL, "??????????????????????????????????????????");
                }
                if (!ids.contains(regionUser.getId())) {
                    ids.add(regionUser.getId());
                }
            }
            if (CollectionUtil.isEmpty(ids)) {
                throw new NormalOptionException(Constants.API_CODE_FAIL, "??????????????????????????????????????????");
            }
            createIceBoxPutModel(iceBoxRequestVo, applyNumber, iceBoxModels, map, simpleUserInfoVo, ids);
            return map;
        }
        return map;
    }

    private void createIceBoxPutModel(IceBoxRequestVo iceBoxRequestVo, String applyNumber, List<IceBoxPutModel.IceBoxModel> iceBoxModels, Map<String, Object> map, SimpleUserInfoVo simpleUserInfoVo, List<Integer> userIds) {
        SessionExamineVo sessionExamineVo = new SessionExamineVo();
        IceBoxPutModel iceBoxPutModel = new IceBoxPutModel();

        iceBoxPutModel.setApplyNumber(applyNumber);
        SubordinateInfoVo supplier = FeignResponseUtil.getFeignData(feignSupplierClient.findSupplierBySupplierId(iceBoxRequestVo.getSupplierId()));
        if (supplier == null) {
            log.info("???????????????id--??????{}??????????????????????????????", iceBoxRequestVo.getSupplierId());
            throw new ImproperOptionException("???????????????????????????");
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        iceBoxPutModel.setAddress(supplier.getAddress());
        iceBoxPutModel.setLinkman(supplier.getLinkman());
        iceBoxPutModel.setLinkmanMobile(supplier.getLinkmanMobile());
        iceBoxPutModel.setSupplierName(supplier.getName());
        iceBoxPutModel.setCreateByName(simpleUserInfoVo.getRealname());
        iceBoxPutModel.setCreateTimeStr(dateFormat.format(new Date()));
        iceBoxPutModel.setIceBoxModelList(iceBoxModels);
        iceBoxPutModel.setApplyStoreNumber(iceBoxRequestVo.getStoreNumber());
        iceBoxPutModel.setApplyStoreName(iceBoxRequestVo.getStoreName());
        iceBoxPutModel.setApplyStoreLevel(iceBoxRequestVo.getStoreLevel());

        iceBoxPutModel.setApplyPit(iceBoxRequestVo.getApplyPit());
        iceBoxPutModel.setVisitTypeName(iceBoxRequestVo.getVisitTypeName());
        SessionExamineCreateVo sessionExamineCreateVo = SessionExamineCreateVo.builder()
                .code(applyNumber)
                .relateCode(applyNumber)
                .createBy(iceBoxRequestVo.getUserId())
                .userIds(userIds)
                .build();
        sessionExamineVo.setSessionExamineCreateVo(sessionExamineCreateVo);
        sessionExamineVo.setIceBoxPutModel(iceBoxPutModel);
        SessionExamineVo examineVo = FeignResponseUtil.getFeignData(feignExamineClient.createIceBoxPut(sessionExamineVo));
        List<SessionExamineVo.VisitExamineNodeVo> visitExamineNodes = examineVo.getVisitExamineNodes();
        map.put("iceBoxPutExamine", visitExamineNodes);
    }

    private List<IceBoxVo> getIceBoxVosByBackApplys(List<IceBackApply> iceBackApplies) {
        List<IceBoxVo> iceBoxVos = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Map<String, IceBackApply> iceBackApplyMap = iceBackApplies.stream().collect(Collectors.toMap(IceBackApply::getApplyNumber, x -> x));
        List<Integer> examineIds = iceBackApplies.stream().map(x -> x.getExamineId()).collect(Collectors.toList());
        RequestExamineVo examineVo = new RequestExamineVo();
        examineVo.setExamineInfoIds(examineIds);
        List<SessionExamineVo> sessionExamineVos = FeignResponseUtil.getFeignData(feignExamineClient.getExamineNodesByList(examineVo));
        if (CollectionUtil.isEmpty(sessionExamineVos)) {
            log.info("?????????????????????????????????");
            return iceBoxVos;
        }
        Map<Integer, SessionExamineVo> sessionExamineVoMap = sessionExamineVos.stream().collect(Collectors.toMap(SessionExamineVo::getExamineInfoId, x -> x));
        Set<String> applyNumbers = iceBackApplies.stream().map(x -> x.getApplyNumber()).collect(Collectors.toSet());
        List<IceBackApplyRelateBox> iceBackApplyRelateBoxes = iceBackApplyRelateBoxDao.selectList(Wrappers.<IceBackApplyRelateBox>lambdaQuery().in(IceBackApplyRelateBox::getApplyNumber, applyNumbers));
        if (CollectionUtil.isEmpty(iceBackApplyRelateBoxes)) {
            log.info("??????????????????????????????????????????????????????");
            return iceBoxVos;
        }
        Map<Integer, IceBackApplyRelateBox> relateBoxMap = iceBackApplyRelateBoxes.stream().collect(Collectors.toMap(IceBackApplyRelateBox::getBoxId, x -> x));

        Set<Integer> boxIds = iceBackApplyRelateBoxes.stream().map(x -> x.getBoxId()).collect(Collectors.toSet());
        List<IceBox> iceBoxes = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().in(IceBox::getId, boxIds));
        if (CollectionUtil.isEmpty(iceBoxes)) {
            log.info("???????????????????????????????????????????????????");
            return iceBoxVos;
        }
        for (IceBox iceBox : iceBoxes) {
            IceBoxVo boxVo = buildIceBoxVo(dateFormat, iceBox);
            boxVo.setStatusStr(IceBoxConstant.IS_BACKING);
            IceBackApplyRelateBox iceBackApplyRelateBox = relateBoxMap.get(iceBox.getId());
            if (iceBackApplyRelateBox == null) {
                continue;
            }
            boxVo.setApplyNumber(iceBackApplyRelateBox.getApplyNumber());
            boxVo.setFreeType(iceBackApplyRelateBox.getFreeType());
            IceBackApply backApply = iceBackApplyMap.get(iceBackApplyRelateBox.getApplyNumber());
            if (backApply == null) {
                continue;
            }
            boxVo.setApplyTimeStr(dateFormat.format(backApply.getCreatedTime()));
            SessionExamineVo sessionExamineVo = sessionExamineVoMap.get(backApply.getExamineId());
            if (sessionExamineVo == null) {
                continue;
            }
            List<SessionExamineVo.VisitExamineNodeVo> visitExamineNodes = sessionExamineVo.getVisitExamineNodes();
            if (CollectionUtil.isNotEmpty(visitExamineNodes)) {
                List<ExamineNodeVo> nodeVos = new ArrayList<>();
                for (SessionExamineVo.VisitExamineNodeVo sessionVisitExamineNodeVo : visitExamineNodes) {
                    ExamineNodeVo nodeVo = new ExamineNodeVo();
                    BeanUtils.copyProperties(sessionVisitExamineNodeVo, nodeVo);
                    nodeVos.add(nodeVo);
                }
                boxVo.setExamineNodeVoList(nodeVos);
            }
            iceBoxVos.add(boxVo);
        }
        return iceBoxVos;
    }

    private List<IceBoxVo> getIceBoxVosByPutApplys(List<IcePutApply> icePutApplies) {
        List<IceBoxVo> iceBoxVos = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Map<String, IcePutApply> icePutApplyMap = icePutApplies.stream().collect(Collectors.toMap(IcePutApply::getApplyNumber, x -> x));
        List<Integer> examineIds = icePutApplies.stream().map(x -> x.getExamineId()).collect(Collectors.toList());
        RequestExamineVo examineVo = new RequestExamineVo();
        examineVo.setExamineInfoIds(examineIds);
        List<SessionExamineVo> sessionExamineVos = FeignResponseUtil.getFeignData(feignExamineClient.getExamineNodesByList(examineVo));
        if (CollectionUtil.isEmpty(sessionExamineVos)) {
            log.info("?????????????????????????????????");
            return iceBoxVos;
        }
        Map<Integer, SessionExamineVo> sessionExamineVoMap = sessionExamineVos.stream().collect(Collectors.toMap(SessionExamineVo::getExamineInfoId, x -> x));
        Set<String> applyNumbers = icePutApplies.stream().map(x -> x.getApplyNumber()).collect(Collectors.toSet());
        List<IcePutApplyRelateBox> icePutApplyRelateBoxes = icePutApplyRelateBoxDao.selectList(Wrappers.<IcePutApplyRelateBox>lambdaQuery().in(IcePutApplyRelateBox::getApplyNumber, applyNumbers));
        if (CollectionUtil.isEmpty(icePutApplyRelateBoxes)) {
            log.info("??????????????????????????????????????????????????????");
            return iceBoxVos;
        }
        Map<Integer, IcePutApplyRelateBox> relateBoxMap = icePutApplyRelateBoxes.stream().collect(Collectors.toMap(IcePutApplyRelateBox::getBoxId, x -> x));

        Set<Integer> boxIds = icePutApplyRelateBoxes.stream().map(x -> x.getBoxId()).collect(Collectors.toSet());
        List<IceBox> iceBoxes = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().in(IceBox::getId, boxIds));
        if (CollectionUtil.isEmpty(iceBoxes)) {
            log.info("???????????????????????????????????????????????????");
            return iceBoxVos;
        }
        for (IceBox iceBox : iceBoxes) {
            IceBoxVo boxVo = buildIceBoxVo(dateFormat, iceBox);
            boxVo.setStatusStr(IceBoxConstant.IS_APPLYING);
            IcePutApplyRelateBox icePutApplyRelateBox = relateBoxMap.get(iceBox.getId());
            if (icePutApplyRelateBox == null) {
                continue;
            }
            boxVo.setApplyNumber(icePutApplyRelateBox.getApplyNumber());
            boxVo.setFreeType(icePutApplyRelateBox.getFreeType());
            IcePutApply putApply = icePutApplyMap.get(icePutApplyRelateBox.getApplyNumber());
            if (putApply == null) {
                continue;
            }
            boxVo.setApplyTimeStr(dateFormat.format(putApply.getCreatedTime()));
            SessionExamineVo sessionExamineVo = sessionExamineVoMap.get(putApply.getExamineId());
            if (sessionExamineVo == null) {
                continue;
            }
            List<SessionExamineVo.VisitExamineNodeVo> visitExamineNodes = sessionExamineVo.getVisitExamineNodes();
            if (CollectionUtil.isNotEmpty(visitExamineNodes)) {
                List<ExamineNodeVo> nodeVos = new ArrayList<>();
                for (SessionExamineVo.VisitExamineNodeVo sessionVisitExamineNodeVo : visitExamineNodes) {
                    ExamineNodeVo nodeVo = new ExamineNodeVo();
                    BeanUtils.copyProperties(sessionVisitExamineNodeVo, nodeVo);
                    nodeVos.add(nodeVo);
                }
                boxVo.setExamineNodeVoList(nodeVos);
            }
            iceBoxVos.add(boxVo);
        }
        return iceBoxVos;
    }

    private List<IceBoxVo> getIceBoxVosByPutApplysNew(List<PutStoreRelateModel> relateModelList) {
        List<IceBoxVo> iceBoxVos = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Map<Integer, PutStoreRelateModel> relateModelMap = relateModelList.stream().collect(Collectors.toMap(PutStoreRelateModel::getId, x -> x));
        Set<Integer> relateModelIds = relateModelList.stream().map(x -> x.getId()).collect(Collectors.toSet());
        List<ApplyRelatePutStoreModel> putStoreModels = applyRelatePutStoreModelDao.selectList(Wrappers.<ApplyRelatePutStoreModel>lambdaQuery().in(ApplyRelatePutStoreModel::getStoreRelateModelId, relateModelIds));
        if (CollectionUtil.isEmpty(putStoreModels)) {
            log.info("??????????????????????????????????????????????????????");
            return iceBoxVos;
        }
        Map<String, List<ApplyRelatePutStoreModel>> putStoreModelByApplyNumberMap = putStoreModels.stream().collect(Collectors.groupingBy(ApplyRelatePutStoreModel::getApplyNumber));

        Set<String> applyNumbers = putStoreModels.stream().map(x -> x.getApplyNumber()).collect(Collectors.toSet());
        List<IcePutApply> icePutApplies = icePutApplyDao.selectList(Wrappers.<IcePutApply>lambdaQuery().in(IcePutApply::getApplyNumber, applyNumbers));
        if (CollectionUtil.isEmpty(icePutApplies)) {
            log.info("??????????????????????????????????????????????????????");
            return iceBoxVos;
        }
//        Map<String, IcePutApply> icePutApplyMap = icePutApplies.stream().collect(Collectors.toMap(IcePutApply::getApplyNumber, x -> x));
        List<Integer> examineIds = icePutApplies.stream().map(x -> x.getExamineId()).collect(Collectors.toList());
        RequestExamineVo examineVo = new RequestExamineVo();
        examineVo.setExamineInfoIds(examineIds);
        List<SessionExamineVo> sessionExamineVos = new ArrayList<>();
        try {
            sessionExamineVos = FeignResponseUtil.getFeignData(feignExamineClient.getExamineNodesByList(examineVo));
        } catch (Exception e) {
            log.info("?????????????????????????????????");
        }
        String putStoreNumber = relateModelList.get(0).getPutStoreNumber();
        if (CollectionUtil.isEmpty(sessionExamineVos)) {
            for (String applyNumber : putStoreModelByApplyNumberMap.keySet()) {
                List<ApplyRelatePutStoreModel> applyRelatePutStoreModels = putStoreModelByApplyNumberMap.get(applyNumber);
                if (CollectionUtil.isEmpty(applyRelatePutStoreModels)) {
                    continue;
                }
                Map<String, Integer> countMap = new HashMap<>();
                for (ApplyRelatePutStoreModel storeModel : applyRelatePutStoreModels) {
                    PutStoreRelateModel relateModel = relateModelMap.get(storeModel.getStoreRelateModelId());
                    Integer value = countMap.get(relateModel.getModelId() + "_" + relateModel.getSupplierId());
                    if (value == null) {
                        countMap.put(relateModel.getModelId() + "_" + relateModel.getSupplierId(), 1);
                    } else {
                        countMap.put(relateModel.getModelId() + "_" + relateModel.getSupplierId(), Integer.sum(value, 1));
                    }
                }
                List<Integer> storeRelateModelIds = applyRelatePutStoreModels.stream().map(x -> x.getStoreRelateModelId()).collect(Collectors.toList());
                List<PutStoreRelateModel> relateModels = putStoreRelateModelDao.selectBatchIds(storeRelateModelIds);
                if (CollectionUtil.isEmpty(relateModels)) {
                    continue;
                }
                List<String> existList = new ArrayList<>();
                for (PutStoreRelateModel relateModel : relateModels) {

                    Integer modelId = relateModel.getModelId();
                    Integer supplierId = relateModel.getSupplierId();
                    String key = modelId + "_" + supplierId;
                    if (existList.contains(key)) {
                        continue;
                    }
                    IceBox iceBox = iceBoxDao.selectOne(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getModelId, modelId)
                            .eq(IceBox::getSupplierId, supplierId)
                            .last("limit 1"));
                    Map<Integer, ApplyRelatePutStoreModel> putStoreModelMap = applyRelatePutStoreModels.stream().collect(Collectors.toMap(ApplyRelatePutStoreModel::getStoreRelateModelId, x -> x));

                    ApplyRelatePutStoreModel storeModel = putStoreModelMap.get(relateModel.getId());
                    IceBoxVo boxVo = new IceBoxVo();

                    boxVo.setStatusStr(IceBoxConstant.IS_APPLYING);
                    boxVo.setApplyNumber(applyNumber);
                    boxVo.setApplyCount(countMap.get(key));
                    boxVo.setFreeType(storeModel.getFreeType());
                    boxVo.setApplyTimeStr(dateFormat.format(relateModel.getCreateTime()));
                    boxVo.setChestModel(iceBox.getModelName());
                    boxVo.setChestName(iceBox.getChestName());
                    boxVo.setDepositMoney(iceBox.getDepositMoney());

                    SubordinateInfoVo supplierInfoVo = FeignResponseUtil.getFeignData(feignSupplierClient.readById(supplierId));
                    if (supplierInfoVo != null) {
                        boxVo.setSupplierName(supplierInfoVo.getName());
                    }
                    iceBoxVos.add(boxVo);

                    existList.add(key);
                }
            }
        } else {
            for (SessionExamineVo sessionExamineVo : sessionExamineVos) {
                //String applyInfoStr = jedis.get(sessionExamineVo.getVisitExamineInfoVo().getRedisKey());
                String applyInfoStr = feignExamineClient.getByKey(sessionExamineVo.getVisitExamineInfoVo().getRedisKey());
                JSONObject applyInfo = JSON.parseObject(applyInfoStr);
                JSONArray iceBoxModelList = applyInfo.getJSONArray("iceBoxModelList");
                for (Object object : iceBoxModelList) {
                    IceBoxVo boxVo = new IceBoxVo();
                    JSONObject jsonObject = JSONObject.parseObject(object.toString());
                    boxVo.setStatusStr(IceBoxConstant.IS_APPLYING);
                    boxVo.setApplyNumber(applyInfo.getString("applyNumber"));
                    boxVo.setApplyCount(jsonObject.getInteger("applyCount"));
                    boxVo.setFreeType(jsonObject.getInteger("isFree"));
                    boxVo.setApplyTimeStr(applyInfo.getString("createTimeStr"));
                    boxVo.setChestModel(jsonObject.getString("iceBoxModel"));
                    boxVo.setChestName(jsonObject.getString("iceBoxName"));
                    boxVo.setDepositMoney(jsonObject.getBigDecimal("depositMoney"));
                    String supplierName = jsonObject.getString("supplierName");
                    boxVo.setSupplierName(supplierName);
                    List<SessionExamineVo.VisitExamineNodeVo> visitExamineNodes = sessionExamineVo.getVisitExamineNodes();
                    if (CollectionUtil.isNotEmpty(visitExamineNodes)) {
                        List<ExamineNodeVo> nodeVos = new ArrayList<>();
                        for (SessionExamineVo.VisitExamineNodeVo sessionVisitExamineNodeVo : visitExamineNodes) {
                            ExamineNodeVo nodeVo = new ExamineNodeVo();
                            BeanUtils.copyProperties(sessionVisitExamineNodeVo, nodeVo);
                            nodeVos.add(nodeVo);
                        }
                        boxVo.setExamineNodeVoList(nodeVos);
                    }
                    iceBoxVos.add(boxVo);
                }

            }
        }


//        Map<Integer, SessionExamineVo> sessionExamineVoMap = sessionExamineVos.stream().collect(Collectors.toMap(SessionExamineVo::getExamineInfoId, x -> x));
//
//
//        List<IceBox> iceBoxes = new ArrayList<>();
//        Set<Integer> modelIds = relateModelList.stream().map(x -> x.getModelId()).collect(Collectors.toSet());
//        Set<Integer> supplierIds = relateModelList.stream().map(x -> x.getSupplierId()).collect(Collectors.toSet());
//        for(Integer modelId:modelIds){
//            for(Integer supplierId:supplierIds){
//                List<IceBox> iceBoxeList = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getModelId, modelId).eq(IceBox::getSupplierId, supplierId));
//                if(CollectionUtil.isNotEmpty(iceBoxeList)){
//                    iceBoxes.add(iceBoxeList.get(0));
//                }
//            }
//        }
//        if (CollectionUtil.isEmpty(iceBoxes)) {
//            log.error("???????????????????????????????????????????????????");
//            return iceBoxVos;
//        }
//        for (IceBox iceBox : iceBoxes) {
//            IceBoxVo boxVo = buildIceBoxVo(dateFormat, iceBox);
//            boxVo.setStatusStr(IceBoxConstant.IS_APPLYING);
//            List<PutStoreRelateModel> relateModelList1 = relateModelGroup.get(iceBox.getModelId());
//            if (relateModelList1 == null) {
//                continue;
//            }
//            PutStoreRelateModel storeRelateModel = null;
//            for(PutStoreRelateModel relateModel:relateModelList1){
//                if(relateModel.getSupplierId().equals(iceBox.getSupplierId())){
//                    storeRelateModel = relateModel;
//                }
//            }
//            if(storeRelateModel == null){
//                continue;
//            }
//            ApplyRelatePutStoreModel applyRelatePutStoreModel = putStoreModelMap.get(storeRelateModel.getId());
//            if(applyRelatePutStoreModel == null){
//                continue;
//            }
//            boxVo.setApplyNumber(applyRelatePutStoreModel.getApplyNumber());
//            boxVo.setFreeType(applyRelatePutStoreModel.getFreeType());
//            IcePutApply putApply = icePutApplyMap.get(applyRelatePutStoreModel.getApplyNumber());
//            if (putApply == null) {
//                continue;
//            }
//            boxVo.setApplyTimeStr(dateFormat.format(putApply.getCreatedTime()));
//            SessionExamineVo sessionExamineVo = sessionExamineVoMap.get(putApply.getExamineId());
//            if (sessionExamineVo == null) {
//                continue;
//            }
//            String s = jedis.get(sessionExamineVo.getVisitExamineInfoVo().getRedisKey());
//            JSONObject o = JSON.parseObject(s);
//            String supplierName = o.get("supplierName").toString();
//            JSONArray iceBoxModelList = o.getJSONArray("iceBoxModelList");
//            boxVo.setSupplierName(supplierName);
//            List<SessionExamineVo.VisitExamineNodeVo> visitExamineNodes = sessionExamineVo.getVisitExamineNodes();
//            if (CollectionUtil.isNotEmpty(visitExamineNodes)) {
//                List<ExamineNodeVo> nodeVos = new ArrayList<>();
//                for (SessionExamineVo.VisitExamineNodeVo sessionVisitExamineNodeVo : visitExamineNodes) {
//                    ExamineNodeVo nodeVo = new ExamineNodeVo();
//                    BeanUtils.copyProperties(sessionVisitExamineNodeVo, nodeVo);
//                    nodeVos.add(nodeVo);
//                }
//                boxVo.setExamineNodeVoList(nodeVos);
//            }
//            iceBoxVos.add(boxVo);
//        }
        return iceBoxVos;
    }

    private IceBoxVo buildIceBoxVo(SimpleDateFormat dateFormat, IceBox iceBox) {
        IceBoxVo boxVo = new IceBoxVo();
        BeanUtils.copyProperties(iceBox, boxVo);
        boxVo.setIceBoxId(iceBox.getId());
        IceModel iceModel = iceModelDao.selectById(iceBox.getModelId());
        if (iceModel != null) {
            boxVo.setChestModel(iceModel.getChestModel());
        }
        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(iceBox.getId());
        if (iceBoxExtend != null) {
            if (iceBoxExtend.getLastPutTime() != null) {
                boxVo.setLastPutTimeStr(dateFormat.format(iceBoxExtend.getLastPutTime()));
            }
            IcePutApplyRelateBox relateBox = icePutApplyRelateBoxDao.selectOne(Wrappers.<IcePutApplyRelateBox>lambdaQuery().eq(IcePutApplyRelateBox::getApplyNumber, iceBoxExtend.getLastApplyNumber()).eq(IcePutApplyRelateBox::getBoxId, iceBox.getId()));
            if (relateBox != null) {
                boxVo.setFreeType(relateBox.getFreeType());
            }
        }
        return boxVo;
    }

    @Override
    public IceBoxDetailVo findIceBoxById(Integer id) {

        IceBox iceBox = iceBoxDao.selectById(id);

        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(id);

        Integer modelId = iceBox.getModelId();

        IceModel iceModel = iceModelDao.selectById(modelId);


        String storeNumber = iceBox.getPutStoreNumber();

        StoreInfoDtoVo storeInfoDtoVo = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(storeNumber));

        String storeAddress = "";

        String storeName = "";

        if (null != storeInfoDtoVo && StringUtils.isNotBlank(storeInfoDtoVo.getStoreNumber())) {
            storeAddress = storeInfoDtoVo.getAddress();
            storeName = storeInfoDtoVo.getStoreName();
        } else {
            SubordinateInfoVo subordinateInfoVo = FeignResponseUtil.getFeignData(feignSupplierClient.findByNumber(storeNumber));
            if (null != subordinateInfoVo && StringUtils.isNotBlank(subordinateInfoVo.getNumber())) {
                storeAddress = subordinateInfoVo.getAddress();
                storeName = subordinateInfoVo.getName();
            }
        }

        // ????????????????????????id ??????????????????????????????????????????????????????
//        IcePutPactRecord record = icePutPactRecordDao.selectOne(Wrappers.<IcePutPactRecord>lambdaQuery()
//                .eq(IcePutPactRecord::getStoreNumber, storeNumber)
//                .eq(IcePutPactRecord::getBoxId, id)
//                .eq(IcePutPactRecord::getApplyNumber, iceBoxExtend.getLastApplyNumber()));
//
//        if (record == null) {
//            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
//        }
//
//        Date putTime = record.getPutTime();
//        Date putExpireTime = record.getPutExpireTime();

        IceBoxDetailVo iceBoxDetailVo = IceBoxDetailVo.builder()
                .id(id)
                .assetId(iceBox.getAssetId())
                .chestModel(iceModel.getChestModel())
                .chestName(iceModel.getChestName())
                .depositMoney(iceBox.getDepositMoney())
                .lastPutTime(iceBoxExtend.getLastPutTime())
                .openTotal(iceBoxExtend.getOpenTotal())
                .putStoreNumber(storeNumber)
                .repairBeginTime(iceBoxExtend.getRepairBeginTime())
                .storeAddress(storeAddress)
                .releaseTime(iceBoxExtend.getReleaseTime())
                .iceBoxType(iceBox.getIceBoxType())
                .status(iceBox.getStatus())
                .build();


        IceExamine firstExamine = iceExamineDao.selectOne(Wrappers.<IceExamine>lambdaQuery().eq(IceExamine::getStoreNumber, storeNumber).eq(IceExamine::getIceBoxId, id).orderByAsc(IceExamine::getCreateTime).last("limit 1"));
        IceExamine lastExamine = iceExamineDao.selectOne(Wrappers.<IceExamine>lambdaQuery().eq(IceExamine::getStoreNumber, storeNumber).eq(IceExamine::getIceBoxId, id).orderByDesc(IceExamine::getCreateTime).last("limit 1"));

        if (firstExamine != null && lastExamine != null) {
            List<Integer> list = new ArrayList<>();
            Integer firstExamineCreateBy = firstExamine.getCreateBy();
            Integer lastExamineCreateBy = lastExamine.getCreateBy();
            list.add(firstExamineCreateBy);
            list.add(lastExamineCreateBy);

            Map<Integer, SessionUserInfoVo> map = FeignResponseUtil.getFeignData(feignUserClient.getSessionUserInfoVoByIds(list));

            IceExamineVo firstExamineVo = firstExamine.convert(firstExamine, map.get(firstExamineCreateBy).getRealname(), storeName, storeNumber);
            IceExamineVo lastExamineVo = firstExamine.convert(lastExamine, map.get(lastExamineCreateBy).getRealname(), storeName, storeNumber);
            iceBoxDetailVo.setFirstExamine(firstExamineVo);
            iceBoxDetailVo.setLastExamine(lastExamineVo);
        }
        return iceBoxDetailVo;
    }

    @Override
    public List<SimpleSupplierInfoVo> findSupplierByDeptId(Integer deptId,String assetId) {
        // ????????????id ?????????????????????????????????supplier_id ???????????? t_ice_box???
        List<SimpleSupplierInfoVo> supplierInfoVoList = new ArrayList<>();

        Integer serviceDeptId = FeignResponseUtil.getFeignData(feignDeptClient.findServiceDeptIdByDeptId(deptId));
        if(StringUtils.isNotEmpty(assetId)){
            IceBox iceBox = iceBoxDao.selectOne(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getAssetId,assetId));
            if(iceBox.getSupplierId() == 134496 || iceBox.getSupplierId() == 134494 || iceBox.getSupplierId() == 134508){
                /**
                 * ??????e???????????????  ????????????  ???????????????????????????
                 */
                Set<Integer> gxSet = new HashSet<>();
                //???????????? 10925
                gxSet.add(10926);
                gxSet.add(10931);
                gxSet.add(10934);
                gxSet.add(10938);
                gxSet.add(10942);
                gxSet.add(13160);
                gxSet.add(13162);
                gxSet.add(13178);
                //???????????? 10946
                gxSet.add(10947);
                gxSet.add(10952);
                gxSet.add(10955);
                gxSet.add(10959);
                gxSet.add(10963);
                gxSet.add(10966);
                gxSet.add(12368);
                //???????????? 10904
                gxSet.add(10905);
                gxSet.add(10909);
                gxSet.add(10911);
                gxSet.add(10913);
                gxSet.add(10916);
                gxSet.add(10920);
                gxSet.add(10923);
                gxSet.add(12363);

                Set<Integer> hnSet = new HashSet<>();
                //???????????? 13250
                hnSet.add(13252);
                hnSet.add(13268);
                hnSet.add(13274);
                hnSet.add(13278);
                hnSet.add(13281);
                //???????????? 13290
                hnSet.add(13291);
                hnSet.add(13299);
                hnSet.add(13306);
                hnSet.add(13309);
                hnSet.add(13315);
                hnSet.add(13326);
                hnSet.add(13332);

                Set<Integer> zjSet = new HashSet<>();
                //???????????? 8314
                zjSet.add(8318);
                zjSet.add(8322);
                zjSet.add(8325);
                zjSet.add(8329);
                zjSet.add(10332);
                zjSet.add(10334);
                zjSet.add(10335);
                zjSet.add(11178);
                zjSet.add(12221);
                zjSet.add(13099);
                //???????????? 8145
                zjSet.add(8146);
                zjSet.add(8157);
                zjSet.add(8164);
                zjSet.add(8172);
                zjSet.add(8178);
                zjSet.add(8182);
                zjSet.add(8193);
                zjSet.add(8200);
                zjSet.add(8201);
                zjSet.add(10308);
                zjSet.add(10309);
                zjSet.add(11161);
                zjSet.add(13114);
                zjSet.add(13120);
                //???????????? 13015
                zjSet.add(13016);
                zjSet.add(13021);
                zjSet.add(13026);
                zjSet.add(13034);
                zjSet.add(13039);
                zjSet.add(13044);
                zjSet.add(13054);

                int supId = 0;
                if(hnSet.contains(serviceDeptId)){
                    //134496	2298
                    supId = 134496;
                }
                if(gxSet.contains(serviceDeptId)){
                    //id134494	number2297
                    supId = 134494;
                }
                if(zjSet.contains(serviceDeptId)){
                    // 134508	2310
                    supId = 134508;
                }
                if(supId > 0){
                    SubordinateInfoVo res = FeignResponseUtil.getFeignData(feignSupplierClient.findSupplierBySupplierId(supId));
                    SimpleSupplierInfoVo simpleSupplierInfoVo = new SimpleSupplierInfoVo();
                    BeanUtils.copyProperties(res,simpleSupplierInfoVo);
                    supplierInfoVoList.add(simpleSupplierInfoVo);
                    return supplierInfoVoList;
                }
            }
        }

        if (null != serviceDeptId) {
            List<SimpleSupplierInfoVo> simpleSupplierInfoVoList = FeignResponseUtil.getFeignData(feignSupplierClient.findByDeptId(serviceDeptId));

            if (CollectionUtil.isNotEmpty(simpleSupplierInfoVoList)) {

                Map<Integer, SimpleSupplierInfoVo> map = simpleSupplierInfoVoList.stream().collect(Collectors.toMap(SimpleSupplierInfoVo::getId, Function.identity()));

                List<Integer> list = simpleSupplierInfoVoList.stream().map(SimpleSupplierInfoVo::getId).collect(Collectors.toList());

                List<IceBox> iceBoxList = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().in(IceBox::getSupplierId, list).groupBy(IceBox::getSupplierId));

                if (CollectionUtil.isNotEmpty(iceBoxList)) {
                    Set<Integer> collect = iceBoxList.stream().map(IceBox::getSupplierId).collect(Collectors.toSet());
                    collect.forEach(supplierId -> {
                        SimpleSupplierInfoVo simpleSupplierInfoVo = map.get(supplierId);
                        if (null != simpleSupplierInfoVo) {
                            supplierInfoVoList.add(simpleSupplierInfoVo);
                        }
                    });
                }
            }
        }


        return supplierInfoVoList;


    }


    /**
     * ?????? ???????????????(??????) ????????????????????????????????????, ?????????Vo??????
     *
     * @param pxtNumber
     * @return
     */
    @Override
    public List<IceBoxStoreVo> getIceBoxStoreVoByPxtNumber(String pxtNumber) {
        List<IceBox> iceBoxes = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getPutStoreNumber, pxtNumber));
        return buildIceBoxStoreVos(iceBoxes);
    }

    private List<IceBoxStoreVo> buildIceBoxStoreVos(List<IceBox> iceBoxes) {
        List<IceBoxStoreVo> iceBoxStoreVos = Lists.newArrayList();
        DateTime now = new DateTime();
        Date todayStart = now.withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0).toDate();
        Date todayEnd = now.withHourOfDay(23).withMinuteOfHour(59).withSecondOfMinute(59).toDate();
        for (IceBox iceBox : iceBoxes) {

            IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(iceBox.getId());
            IceEventRecord iceEventRecord = iceEventRecordDao.selectOne(Wrappers.<IceEventRecord>lambdaQuery()
                    .eq(IceEventRecord::getAssetId, iceBoxExtend.getAssetId())
                    .between(IceEventRecord::getOccurrenceTime,todayStart,todayEnd)
                    .orderByDesc(IceEventRecord::getCreateTime)
                    .last("limit 1"));
            IcePutApplyRelateBox icePutApplyRelateBox = icePutApplyRelateBoxDao.selectOne(Wrappers.<IcePutApplyRelateBox>lambdaQuery()
                    .eq(IcePutApplyRelateBox::getApplyNumber, iceBoxExtend.getLastApplyNumber())
                    .eq(IcePutApplyRelateBox::getBoxId, iceBox.getId()));
            IceModel iceModel = iceModelDao.selectById(iceBox.getModelId());

            IcePutApply icePutApply = icePutApplyDao.selectOne(Wrappers.<IcePutApply>lambdaQuery().eq(IcePutApply::getApplyNumber, iceBoxExtend.getLastApplyNumber()));

            IceBackApply iceBackApply = iceBackApplyDao.selectOne(Wrappers.<IceBackApply>lambdaQuery().eq(IceBackApply::getOldPutId, icePutApplyRelateBox.getId()).ne(IceBackApply::getExamineStatus, 3));

            IceBoxStoreVo iceBoxStoreVo = IceBoxConverter.convertToStoreVo(iceBox, iceBoxExtend, iceModel, icePutApplyRelateBox, iceEventRecord, iceBackApply);
            iceBoxStoreVos.add(iceBoxStoreVo);
        }
        return iceBoxStoreVos;
    }

    /**
     * ????????????????????????
     * 1. ???????????????
     * 2. ??????????????????????????????????????????
     * 3. ?????????????????????????????????
     *
     * @param qrcode
     * @param pxtNumber
     * @return
     */
    @Override
    public IceBoxStatusVo checkBoxByQrcode(String qrcode, String pxtNumber) {

        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectOne(Wrappers.<IceBoxExtend>lambdaQuery().eq(IceBoxExtend::getQrCode, qrcode));
        if (Objects.isNull(iceBoxExtend)) {
            // ???????????????(??????????????????)
            IceBoxStatusVo iceBoxStatusVo = new IceBoxStatusVo();
            iceBoxStatusVo.setSignFlag(false);
            iceBoxStatusVo.setStatus(5);
            iceBoxStatusVo.setMessage("???????????????(??????????????????)");
            return iceBoxStatusVo;
        }

        IceBox iceBox = iceBoxDao.selectById(iceBoxExtend.getId());
        return switchIceBoxStatus(iceBoxExtend.getLastApplyNumber(), pxtNumber, iceBox);
    }

    private IceBoxStatusVo switchIceBoxStatus(String applyNumber, String pxtNumber, IceBox iceBox) {
        IceBoxStatusVo iceBoxStatusVo = new IceBoxStatusVo();
        iceBoxStatusVo.setIceBoxId(iceBox.getId());
        switch (Objects.requireNonNull(PutStatus.convertEnum(iceBox.getPutStatus()))) {
            case NO_PUT:
                // ???????????????
                iceBoxStatusVo.setSignFlag(false);
                iceBoxStatusVo.setStatus(3);
                iceBoxStatusVo.setMessage("??????????????????????????????");
                break;
            case LOCK_PUT:
                // ??????????????????, ??????????????????
                iceBoxStatusVo.setSignFlag(false);
                iceBoxStatusVo.setStatus(4);
                iceBoxStatusVo.setMessage("?????????????????????");
                break;
            case DO_PUT:
                // ?????????????????????, ?????????????????????
                iceBoxStatusVo = checkPutApplyByApplyNumber(applyNumber, pxtNumber);
                break;
            case FINISH_PUT:
                if (iceBox.getPutStoreNumber().equals(pxtNumber)) {
                    // ????????????????????????
                    iceBoxStatusVo.setSignFlag(false);
                    iceBoxStatusVo.setStatus(6);
                    iceBoxStatusVo.setMessage("??????????????????????????????");
                    break;
                }
                // ????????????, ????????????
                iceBoxStatusVo.setSignFlag(false);
                iceBoxStatusVo.setStatus(2);
                iceBoxStatusVo.setMessage("???????????????????????????");
                break;
        }
        return iceBoxStatusVo;
    }

    /**
     * ???????????????????????????????????????
     *
     * @param applyNumber
     * @param pxtNumber
     * @return
     */
    private IceBoxStatusVo checkPutApplyByApplyNumber(String applyNumber, String pxtNumber) {
        IceBoxStatusVo iceBoxStatusVo = new IceBoxStatusVo();

        IcePutApply icePutApply = icePutApplyDao.selectOne(Wrappers.<IcePutApply>lambdaQuery().eq(IcePutApply::getApplyNumber, applyNumber));
        if (!icePutApply.getPutStoreNumber().equals(pxtNumber)) {
            // ?????????????????????????????????, ????????????????????????
            iceBoxStatusVo.setSignFlag(false);
            iceBoxStatusVo.setStatus(2);
            iceBoxStatusVo.setMessage("???????????????????????????");
            return iceBoxStatusVo;
        }
        // ?????????????????????????????????, ????????????????????????, ??????????????????
        iceBoxStatusVo.setSignFlag(true);
        iceBoxStatusVo.setStatus(1);
        return iceBoxStatusVo;
    }

    @Override
    public IceBoxVo getIceBoxByQrcode(String qrcode) {
        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectOne(Wrappers.<IceBoxExtend>lambdaQuery().eq(IceBoxExtend::getQrCode, qrcode));
        IceBox iceBox = iceBoxDao.selectById(Objects.requireNonNull(iceBoxExtend).getId());
        IceModel iceModel = iceModelDao.selectById(Objects.requireNonNull(iceBox).getModelId());
        IcePutApplyRelateBox icePutApplyRelateBox = icePutApplyRelateBoxDao.selectOne(Wrappers.<IcePutApplyRelateBox>lambdaQuery()
                .eq(IcePutApplyRelateBox::getApplyNumber, iceBoxExtend.getLastApplyNumber())
                .eq(IcePutApplyRelateBox::getBoxId, iceBox.getId()));
        return IceBoxConverter.convertToVo(Objects.requireNonNull(iceBox),
                Objects.requireNonNull(iceBoxExtend),
                Objects.requireNonNull(iceModel),
                Objects.isNull(icePutApplyRelateBox) ? FreePayTypeEnum.UN_FREE : FreePayTypeEnum.convertVo(icePutApplyRelateBox.getFreeType()));
    }

    @Override
    public boolean judgeRecordTime(Integer id) {

        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectOne(Wrappers.<IceBoxExtend>lambdaQuery().eq(IceBoxExtend::getId, id));

        Integer lastPutId = iceBoxExtend.getLastPutId();
        IcePutPactRecord icePutPactRecord = icePutPactRecordDao.selectById(lastPutId);

        if (icePutPactRecord == null) {
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }

        Date putExpireTime = icePutPactRecord.getPutExpireTime();
        Date date = new Date();
        return date.after(putExpireTime);
    }

    @Override
    public void checkIceBox(IceBoxRequest iceBoxRequest) {
        //??????
        if (IceBoxStatus.NO_PUT.getStatus().equals(iceBoxRequest.getStatus())) {
            IcePutApply icePutApply = icePutApplyDao.selectOne(Wrappers.<IcePutApply>lambdaQuery().eq(IcePutApply::getApplyNumber, iceBoxRequest.getApplyNumber()));
            if (icePutApply != null) {
                icePutApply.setExamineStatus(ExamineStatusEnum.UN_PASS.getStatus());
                icePutApply.setUpdatedBy(iceBoxRequest.getUpdateBy());
                icePutApply.setUpdateTime(new Date());
                icePutApplyDao.updateById(icePutApply);
            }

            List<IcePutApplyRelateBox> icePutApplyRelateBoxes = icePutApplyRelateBoxDao.selectList(Wrappers.<IcePutApplyRelateBox>lambdaQuery().eq(IcePutApplyRelateBox::getApplyNumber, iceBoxRequest.getApplyNumber()));
            Set<Integer> iceBoxIds = Streams.toStream(icePutApplyRelateBoxes).map(x -> x.getBoxId()).collect(Collectors.toSet());
            if (CollectionUtil.isNotEmpty(iceBoxIds)) {
                for (Integer iceBoxId : iceBoxIds) {
                    IceBox iceBox = new IceBox();
                    iceBox.setId(iceBoxId);
                    iceBox.setPutStatus(IceBoxStatus.NO_PUT.getStatus());
                    iceBox.setPutStoreNumber("");
                    iceBox.setUpdatedTime(new Date());
                    iceBox.setCreatedBy(0);
                    iceBoxDao.updateById(iceBox);
                }
            }
            IcePutOrder icePutOrder = icePutOrderDao.selectOne(Wrappers.<IcePutOrder>lambdaQuery().eq(IcePutOrder::getApplyNumber, iceBoxRequest.getApplyNumber()));
            if (icePutOrder != null) {
                icePutOrder.setStatus(OrderStatus.IS_CANCEL.getStatus());
                icePutOrder.setUpdatedTime(new Date());
                icePutOrder.setCreatedBy(0);
                icePutOrderDao.updateById(icePutOrder);
            }
        }
        //?????????
//        if (IceBoxStatus.IS_PUTING.getStatus().equals(iceBoxRequest.getStatus())) {
//            IcePutApply icePutApply = icePutApplyDao.selectOne(Wrappers.<IcePutApply>lambdaQuery().eq(IcePutApply::getApplyNumber, iceBoxRequest.getApplyNumber()));
//            if (icePutApply != null) {
//                icePutApply.setExamineStatus(ExamineStatusEnum.IS_DEFAULT.getStatus());
//                icePutApply.setUpdatedBy(0);
//                icePutApply.setUpdateTime(new Date());
//                icePutApplyDao.updateById(icePutApply);
//            }
//
//            List<IcePutApplyRelateBox> icePutApplyRelateBoxes = icePutApplyRelateBoxDao.selectList(Wrappers.<IcePutApplyRelateBox>lambdaQuery().eq(IcePutApplyRelateBox::getApplyNumber, iceBoxRequest.getApplyNumber()));
//            Set<Integer> iceBoxIds = Streams.toStream(icePutApplyRelateBoxes).map(x -> x.getBoxId()).collect(Collectors.toSet());
//            if (CollectionUtil.isNotEmpty(iceBoxIds)) {
//                for (Integer iceBoxId : iceBoxIds) {
//                    IceBox iceBox = new IceBox();
//                    iceBox.setId(iceBoxId);
//                    iceBox.setPutStatus(IceBoxStatus.IS_PUTING.getStatus());
//                    iceBox.setUpdatedTime(new Date());
//                    iceBox.setCreatedBy(0);
//                    iceBoxDao.updateById(iceBox);
//                }
//            }
//        }
        //?????????????????????????????????????????????????????????????????????????????????
        if (IceBoxStatus.IS_PUTING.getStatus().equals(iceBoxRequest.getStatus())) {
            IcePutApply icePutApply = icePutApplyDao.selectOne(Wrappers.<IcePutApply>lambdaQuery().eq(IcePutApply::getApplyNumber, iceBoxRequest.getApplyNumber()));
            if (icePutApply != null) {
                icePutApply.setExamineStatus(ExamineStatusEnum.IS_PASS.getStatus());
                icePutApply.setUpdatedBy(0);
                icePutApply.setUpdateTime(new Date());
                icePutApplyDao.updateById(icePutApply);
            }

            List<IcePutApplyRelateBox> icePutApplyRelateBoxes = icePutApplyRelateBoxDao.selectList(Wrappers.<IcePutApplyRelateBox>lambdaQuery().eq(IcePutApplyRelateBox::getApplyNumber, iceBoxRequest.getApplyNumber()));
            Set<Integer> iceBoxIds = Streams.toStream(icePutApplyRelateBoxes).map(x -> x.getBoxId()).collect(Collectors.toSet());
            if (CollectionUtil.isNotEmpty(iceBoxIds)) {
                for (Integer iceBoxId : iceBoxIds) {
                    IceBox iceBox = iceBoxDao.selectById(iceBoxId);
                    if (iceBox == null) {
                        continue;
                    }
                    iceBox.setPutStatus(IceBoxStatus.IS_PUTING.getStatus());
                    iceBox.setUpdatedTime(new Date());
                    iceBox.setCreatedBy(0);
                    iceBoxDao.updateById(iceBox);
                    if (IceBoxEnums.TypeEnum.OLD_ICE_BOX.getType().equals(iceBox.getIceBoxType())) {
                        OldIceBoxSignNotice oldIceBoxSignNotice = new OldIceBoxSignNotice();
                        oldIceBoxSignNotice.setApplyNumber(iceBoxRequest.getApplyNumber());
                        oldIceBoxSignNotice.setAssetId(iceBox.getAssetId());
                        oldIceBoxSignNotice.setIceBoxId(iceBox.getId());
                        oldIceBoxSignNotice.setPutStoreNumber(iceBox.getPutStoreNumber());
                        oldIceBoxSignNotice.setCreateTime(new Date());
                        oldIceBoxSignNoticeDao.insert(oldIceBoxSignNotice);
                    }
                }
            }
        }
    }

    //@RoutingDataSource(value = Datasources.SLAVE_DB)
    @Override
    public IPage findPage(IceBoxPage iceBoxPage) {

        // ???????????????????????????????????????
        List<Integer> deptIdList = FeignResponseUtil.getFeignData(feignDeptClient.findDeptInfoIdsBySessionUser());
        log.info("findDeptInfoIdsBySessionUser???success");
        iceBoxPage.setDeptIdList(deptIdList);
        // ??????????????????
        if (dealIceBoxPage(iceBoxPage)) {
            return new Page();
        }
        List<IceBox> iceBoxList = iceBoxDao.findPage(iceBoxPage);
        if (CollectionUtils.isEmpty(iceBoxList)) {
            return new Page();
        }
        List<Integer> deptIds = iceBoxList.stream().map(IceBox::getDeptId).collect(Collectors.toList());
        // ???????????????????????????  ?????????->??????->?????????
        Map<Integer, String> deptMap = null;
        if (CollectionUtils.isNotEmpty(deptIds)) {
            deptMap = FeignResponseUtil.getFeignData(feignCacheClient.getForMarketAreaName(deptIds));
            log.info("getForMarketAreaName:success");
        }
        // ????????????
        List<IceModel> iceModels = iceModelDao.selectList(Wrappers.<IceModel>lambdaQuery()
                .in(IceModel::getId, iceBoxList.stream().map(IceBox::getModelId).collect(Collectors.toSet())));
        Map<Integer, IceModel> modelMap = new HashMap<>();
        Optional.ofNullable(iceModels).ifPresent(list -> {
            list.forEach(i -> {
                modelMap.put(i.getId(), i);
            });
        });

        // ????????? ??????
        List<Integer> suppIds = iceBoxList.stream().map(IceBox::getSupplierId).collect(Collectors.toList());
        Map<Integer, Map<String, String>> suppMaps = null;
        if (CollectionUtils.isNotEmpty(suppIds)) {
            suppMaps = FeignResponseUtil.getFeignData(feignSupplierClient.getSimpledataByIdList(suppIds));
            log.info("getSimpledataByIdList:success");
        }
        // ????????????-- ??????/?????????/????????? ??????      ?????????????????????,???????????? ??????/?????????/????????????
        Map<String, Map<String, String>> storeMaps = null;
        List<String> storeNumbers = iceBoxList.stream().map(IceBox::getPutStoreNumber).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(storeNumbers)) {
            storeMaps = FeignResponseUtil.getFeignData(feignStoreClient.getSimpledataByNumber(storeNumbers));
            log.info("getSimpledataByNumber:success");
        }
        // ?????????????????????,???????????????  t_cus_supplier_info  ???
        storeMaps = getSuppMap(storeMaps, storeNumbers);

        List<Map<String, Object>> list = new ArrayList<>();
        for (IceBox iceBox : iceBoxList) {
            log.info("iceboxid:"+iceBox.getId());

            // t_ice_box ???id ??? t_ice_box_extend ???id??????????????????
            IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(iceBox.getId());
            Map<String, Object> map = new HashMap<>(32);
            map.put("statusStr", IceBoxEnums.StatusEnum.getDesc(iceBox.getStatus())); // ????????????
            map.put("putStatusStr", PutStatus.convertEnum(iceBox.getPutStatus()).getDesc());
            if(iceBox != null && StringUtils.isNotEmpty(iceBox.getResponseMan())){
                map.put("mainSaleMan",iceBox.getResponseMan());
            }
            String deptStr = null;
            if (deptMap != null) {
                deptStr = deptMap.get(iceBox.getDeptId()); // ????????????
            }
            map.put("deptStr", deptStr); // ????????????
            map.put("assetId", iceBoxExtend.getAssetId()); // ???????????? --????????????id
            map.put("chestName", iceBox.getChestName()); // ????????????
            map.put("brandName", iceBox.getBrandName()); // ??????
            IceModel iceModel = modelMap.get(iceBox.getModelId());
            map.put("chestModel", iceModel == null ? null : iceModel.getChestModel()); // ????????????
            map.put("chestNorm", iceBox.getChestNorm()); // ??????
            map.put("lastPutTime", iceBoxExtend.getLastPutTime()); // ??????????????????
            map.put("lastExamineTime", iceBoxExtend.getLastExamineTime()); // ??????????????????
            String lastApplyNumber = iceBoxExtend.getLastApplyNumber(); // ????????????????????????
            IcePutApplyRelateBox icePutApplyRelateBox = null;
            Date signTime  = null;
            if (StringUtils.isNotBlank(lastApplyNumber)) {
                icePutApplyRelateBox = icePutApplyRelateBoxDao.selectOne(Wrappers.<IcePutApplyRelateBox>lambdaQuery()
                        .eq(IcePutApplyRelateBox::getApplyNumber, lastApplyNumber).last(" limit 1"));

                LambdaQueryWrapper<IcePutApply> icePutApplyWrapper = Wrappers.<IcePutApply>lambdaQuery();
                icePutApplyWrapper.eq(IcePutApply::getStoreSignStatus,StoreSignStatus.ALREADY_SIGN.getStatus());
                icePutApplyWrapper.eq(IcePutApply::getApplyNumber, lastApplyNumber).last(" limit 1");
                IcePutApply icePutApply = icePutApplyDao.selectOne(icePutApplyWrapper);
                if(icePutApply != null){
                    signTime  = icePutApply.getUpdateTime();
                }
            }
            map.put("signTime", signTime); // ????????????
            map.put("freeTypeStr", icePutApplyRelateBox == null ? null : FreePayTypeEnum.getDesc(icePutApplyRelateBox.getFreeType())); // ????????????

            String name = null;
            String number = null;
            String level = null;
            String belongObjStr = null;
            String belongDealer = null;
            String businessDeptName = null;
            String regionDeptName = null;
            String serviceDeptName = null;
            String groupDeptName = null;
            if (suppMaps != null) {
                Map<String, String> suppMap = suppMaps.get(iceBox.getSupplierId());
                if (PutStatus.NO_PUT.getStatus().equals(iceBox.getPutStatus()) && suppMap != null) { // ?????????
                    name = suppMap.get("suppName");
                    number = suppMap.get("suppNumber");
                    level = suppMap.get("level");
                    belongObjStr = suppMap.get("suppTypeName"); // ????????????
                }
                belongDealer = suppMap == null ? null : suppMap.get("suppName"); // ???????????????
            }
            map.put("belongDealer", belongDealer); // ???????????????

            if (!PutStatus.NO_PUT.getStatus().equals(iceBox.getPutStatus()) && storeMaps != null) { // ??????/?????????/??????/?????????
                Map<String, String> storeMap = storeMaps.get(iceBox.getPutStoreNumber());
                name = storeMap == null ? null : storeMap.get("storeName");
                number = storeMap == null ? null : storeMap.get("storeNumber");
                level = storeMap == null ? null : storeMap.get("storeLevel");
                belongObjStr = storeMap == null ? null : storeMap.get("storeTypeName");

                businessDeptName = storeMap == null ? null : storeMap.get("businessDeptName");
                regionDeptName = storeMap == null ? null : storeMap.get("regionDeptName");
                serviceDeptName = storeMap == null ? null : storeMap.get("serviceDeptName");
                groupDeptName = storeMap == null ? null : storeMap.get("groupDeptName");
            }
            map.put("number", number); // ????????????
            map.put("name", name); // ????????????
            map.put("level", level); // ????????????
            map.put("belongObjStr", belongObjStr); // ????????????
            map.put("id", iceBox.getId());
            if(StringUtils.isNotEmpty(number)){
                StoreInfoDtoVo storeInfoDtoVo = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(number));
                log.info("getByStoreNumber:success");
                if(storeInfoDtoVo != null && storeInfoDtoVo.getMerchantNumber() != null){
                    map.put("merchantNumber",storeInfoDtoVo.getMerchantNumber());
                }
            }
            List<CusLabelDetailVo> labelDetailVos = new ArrayList<>();
            if(map.get("number") != null) {
                labelDetailVos = FeignResponseUtil.getFeignData(feignCusLabelClient.queryLabelsByCustomerNumber((String) map.get("number")));
            }
            String label = "";
            if(labelDetailVos != null){
                //?????????????????????
                List<CusLabelDetailVo> autoDetails = labelDetailVos.stream().filter(x->x.getLabelFlag()==1).collect(Collectors.toList());
                if(autoDetails != null){
                    for(CusLabelDetailVo detailVo : autoDetails){
                        if(StringUtils.isNotBlank(label)){
                            label += "," + detailVo.getLabelName();
                        }else {
                            label = detailVo.getLabelName();
                        }
                    }
                }
            }
            map.put("label",label);
            String fullDept = "";
            if(StringUtils.isNoneBlank(businessDeptName)){
                fullDept = businessDeptName;
            }
            if(StringUtils.isNoneBlank(regionDeptName)){
                fullDept = fullDept + "/" + regionDeptName;
            }
            if(StringUtils.isNoneBlank(serviceDeptName)){
                fullDept = fullDept + "/" + serviceDeptName;
            }
            if(StringUtils.isNoneBlank(groupDeptName)){
                fullDept = fullDept + "/" + groupDeptName;
            }

            if(StringUtils.isNoneBlank(fullDept)){
                map.put("deptStr", fullDept); // ????????????
            }
//            map.put("belongObjStr", iceBox.getPutStatus().equals(0) ? "?????????" : "??????"); // ??????????????????
            list.add(map);
        }
        return new Page(iceBoxPage.getCurrent(), iceBoxPage.getSize(), iceBoxPage.getTotal()).setRecords(list);
    }

    @Override
    public boolean dealIceBoxPage(IceBoxPage iceBoxPage) {

        // ?????????????????????????????????????????????????????????,???????????????????????????
        if ((StringUtils.isNotBlank(iceBoxPage.getBelongObjNumber()) || StringUtils.isNotBlank(iceBoxPage.getBelongObjName()))
                && iceBoxPage.getBelongObj() == null) {
            throw new NormalOptionException(Constants.API_CODE_FAIL, "???????????????????????????");
        }
        List<Integer> deptIdList = iceBoxPage.getDeptIdList();
        if (CollectionUtils.isEmpty(deptIdList)) {
            log.info("?????????????????????????????????");
            return true;
        }
        Set<Integer> deptIdSet = deptIdList.stream().collect(Collectors.toSet());
        Integer deptId = iceBoxPage.getDeptId(); // ????????????id
        if (deptId != null) {
            // ???????????????????????????????????????
            List<Integer> searchDeptIdList = FeignResponseUtil.getFeignData(feignDeptClient.findDeptIdsByParentIds(Ints.asList(deptId)));
            log.info("findDeptIdsByParentIds???success");
            if (CollectionUtils.isEmpty(searchDeptIdList)) {
                return true;
            }
            Set<Integer> searchDeptIdSet = searchDeptIdList.stream().collect(Collectors.toSet());
            if(deptIdSet.contains(1)){
                deptIdSet=searchDeptIdSet;
            }else {
                deptIdSet = Sets.intersection(deptIdSet, searchDeptIdSet);
            }
        }
        iceBoxPage.setDeptIdList(null);
         iceBoxPage.setDeptIds(deptIdSet.contains(1) ? null : deptIdSet); // ????????????????????? ??????????????????????????????????????????,???????????????????????????

        Set<Integer> supplierIdList = new HashSet<>(); // ?????????????????????
        // ????????????  (put_status  ???????????? 0: ????????? 1:?????????(??????????????????) 2:????????? 3:?????????; ?????????????????? 0-?????????;?????????????????????????????????;)
        String belongObjNumber = iceBoxPage.getBelongObjNumber();
        String belongObjName = iceBoxPage.getBelongObjName();
        String limit = " limit 30";
        // ??????????????? ?????????
        if (iceBoxPage.getBelongObj() != null && PutStatus.NO_PUT.getStatus().equals(iceBoxPage.getBelongObj())) {
            // supplier_type ???????????????1-????????????2-????????????3-?????????4-?????????
            // status ?????????0-?????????1-??????
            if (StringUtils.isNotBlank(belongObjNumber)) { // ??? number ??????
                List<SubordinateInfoVo> infoVoList = FeignResponseUtil.getFeignData(feignSupplierClient.getByNameOrNumber(null, belongObjNumber, null, 1, limit));
                log.info("getByNameOrNumber???success");
                Optional.ofNullable(infoVoList).ifPresent(list -> {
                    list.forEach(i -> {
                        supplierIdList.add(i.getId());
                    });
                });
                if (CollectionUtils.isEmpty(supplierIdList)) {
                    return true;
                }
            }
            if (StringUtils.isNotBlank(belongObjName)) { // ??? name ??????
                List<SubordinateInfoVo> infoVoList = FeignResponseUtil.getFeignData(feignSupplierClient.getByNameOrNumber(belongObjName, null, null, 1, limit));
                Optional.ofNullable(infoVoList).ifPresent(list -> {
                    list.forEach(i -> {
                        supplierIdList.add(i.getId());
                    });
                });
                if (CollectionUtils.isEmpty(supplierIdList)) {
                    return true;
                }
            }
        }
        Set<String> putStoreNumberList = new HashSet<>(); // ???????????????number
        // ??????????????? ??????
        if (iceBoxPage.getBelongObj() != null && !PutStatus.NO_PUT.getStatus().equals(iceBoxPage.getBelongObj())) {
            if (StringUtils.isNotBlank(belongObjNumber)) { // ??? number ??????
                List<SimpleStoreVo> storeVoList = FeignResponseUtil.getFeignData(feignStoreClient.getByNameOrNumber(belongObjNumber, null, null, null, limit));
                Optional.ofNullable(storeVoList).ifPresent(list -> {
                    list.forEach(i -> {
                        putStoreNumberList.add(i.getStoreNumber());
                    });
                });
                if (CollectionUtils.isEmpty(putStoreNumberList)) {
                    return true;
                }
            }
            if (StringUtils.isNotBlank(belongObjName)) { // ??? name ??????
                List<SimpleStoreVo> storeVoList = FeignResponseUtil.getFeignData(feignStoreClient.getByNameOrNumber(null, belongObjName, null, null, limit));
                Optional.ofNullable(storeVoList).ifPresent(list -> {
                    list.forEach(i -> {
                        putStoreNumberList.add(i.getStoreNumber());
                    });
                });
                if (CollectionUtils.isEmpty(putStoreNumberList)) {
                    return true;
                }
            }
        }
        iceBoxPage.setSupplierIdList(supplierIdList);
        iceBoxPage.setPutStoreNumberList(putStoreNumberList);
        return false;
    }

    @Override
    public Map<String, Object> readBasic(Integer id) {

        IceBox iceBox = iceBoxDao.selectById(id);
        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(id);
        Map<String, Object> map = new HashMap<>(32);
        map.put("iceBoxId", iceBox.getId()); // ???????????? --????????????id
        map.put("assetId", iceBoxExtend.getAssetId()); // ???????????? --????????????id
        map.put("oldAssetId", iceBox.getOldAssetId()); // ???????????????
        map.put("chestName", iceBox.getChestName()); // ??????
        map.put("modelId", iceBox.getModelId()); // ??????Id
        IceModel iceModel = iceModelDao.selectOne(Wrappers.<IceModel>lambdaQuery().eq(IceModel::getId, iceBox.getModelId()).last(" limit 1"));
        map.put("chestModel", iceModel == null ? null : iceModel.getChestModel()); // ??????
        map.put("chestNorm", iceBox.getChestNorm()); // ??????
        map.put("brandName", iceBox.getBrandName()); // ??????
        map.put("chestMoney", iceBox.getChestMoney()); // ??????
        map.put("depositMoney", iceBox.getDepositMoney()); // ????????????
        map.put("releaseTime", iceBoxExtend.getReleaseTime()); // ????????????
        map.put("repairBeginTime", iceBoxExtend.getRepairBeginTime()); // ??????????????????
        map.put("remark", iceBox.getRemark()); // ??????????????????
        map.put("supplierId", iceBox.getSupplierId());
        // ???????????????????????????  ?????????->??????->?????????

        if (iceBox.getDeptId() != null) {
            List<Integer> deptIds = new ArrayList<>();
            deptIds.add(iceBox.getDeptId());
            Map<Integer, String> data = FeignResponseUtil.getFeignData(feignCacheClient.getForMarketAreaName(deptIds));
            map.put("deptStr", data.get(iceBox.getDeptId())); // ????????????
        }
        map.put("iceBoxType", iceBox.getIceBoxType());
        map.put("deptId", iceBox.getDeptId());


        String khName = null;
        String khAddress = null;
        String khGrade = null;
        String khStatusStr = null;
        String khContactPerson = null;
        String khContactNumber = null;
        String putStatusStr = null; // ????????????
        SubordinateInfoVo suppInfoVo = FeignResponseUtil.getFeignData(feignSupplierClient.readById(iceBox.getSupplierId()));
        if (PutStatus.NO_PUT.getStatus().equals(iceBox.getPutStatus())) { // ?????????
            if (suppInfoVo != null) {
                khName = suppInfoVo.getName();
                khAddress = suppInfoVo.getAddress();
                khGrade = suppInfoVo.getLevel();
                // ?????????0-?????????1-??????
                khStatusStr = (suppInfoVo.getStatus() != null && suppInfoVo.getStatus().equals(1)) ? "??????" : "??????";
                khContactPerson = suppInfoVo.getLinkman();
                khContactNumber = suppInfoVo.getLinkmanMobile();
                putStatusStr = suppInfoVo.getTypeName();
            }
        } else {
            // ??????/?????????/??????/?????????
            // ??????
            StoreInfoDtoVo dtoVo = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(iceBox.getPutStoreNumber()));
            if (dtoVo != null) {
                Map<String, SessionStoreInfoVo> storeInfoVoMap = FeignResponseUtil.getFeignData(feignStoreClient.getSessionStoreInfoVo(Lists.newArrayList(iceBox.getPutStoreNumber())));
                if (dtoVo != null) {
                    khName = dtoVo.getStoreName();
                    khAddress = dtoVo.getAddress();
                    khGrade = dtoVo.getStoreLevel();
                    putStatusStr = dtoVo.getStoreTypeName();
                    // ?????????0-?????????1-??????
                    khStatusStr = (dtoVo.getStatus() != null && dtoVo.getStatus().equals(1)) ? "??????" : "??????";
                    if (storeInfoVoMap != null && storeInfoVoMap.get(iceBox.getPutStoreNumber()) != null) {
                        SessionStoreInfoVo infoVo = storeInfoVoMap.get(iceBox.getPutStoreNumber());
                        khContactPerson = infoVo.getMemberName();
                        khContactNumber = infoVo.getMemberMobile();
                    }
                }
            }
            if (dtoVo == null) { // ??????????????????
                SubordinateInfoVo infoVo = FeignResponseUtil.getFeignData(feignSupplierClient.findByNumber(iceBox.getPutStoreNumber()));
                if (infoVo != null) {
                    khName = infoVo.getName();
                    khAddress = infoVo.getAddress();
                    khGrade = infoVo.getLevel();
                    // ?????????0-?????????1-??????
                    khStatusStr = (infoVo.getStatus() != null && infoVo.getStatus().equals(1)) ? "??????" : "??????";
                    khContactPerson = infoVo.getLinkman();
                    khContactNumber = infoVo.getLinkmanMobile();
                    putStatusStr = infoVo.getTypeName();
                }
            }
        }
        map.put("khName", khName); // ????????????
        map.put("khAddress", khAddress); // ????????????
        map.put("khGrade", khGrade); // ????????????
        map.put("khStatusStr", khStatusStr); // ????????????
        map.put("khContactPerson", khContactPerson); // ?????????
        map.put("khContactNumber", khContactNumber); // ????????????
        map.put("putStatusStr", putStatusStr); // ????????????
        String belongDealer = null;
        if (suppInfoVo != null && suppInfoVo.getName() != null) {
            map.put("supplierNumber", suppInfoVo.getNumber());
            belongDealer = suppInfoVo.getName();
        }
        map.put("belongDealer", belongDealer); // ???????????????

        Integer status = iceBox.getStatus();
        map.put("status", status); // ??????
        String statusStr = "";
        if (IceBoxEnums.StatusEnum.REPAIR.getType() >= status ) {
            statusStr =  IceBoxEnums.StatusEnum.getDesc(status); // ??????
        }else {
            // ???????????????????????????
            IceExamine iceExamine = iceExamineDao.selectOne(Wrappers.<IceExamine>lambdaQuery().eq(IceExamine::getIceBoxId, iceBox.getId()).orderByDesc(IceExamine::getId).last("limit 1"));
            if (null != iceExamine) {
                Integer iceStatus = iceExamine.getIceStatus();
                Integer examinStatus = iceExamine.getExaminStatus();
                if (ExamineStatus.PASS_EXAMINE.getStatus().equals(examinStatus)) {
                    statusStr = "?????????" + IceBoxEnums.StatusEnum.getDesc(iceStatus);
                } else if (ExamineStatus.DEFAULT_EXAMINE.getStatus().equals(examinStatus) || ExamineStatus.DOING_EXAMINE.getStatus().equals(examinStatus)) {
                    statusStr = "??????" + IceBoxEnums.StatusEnum.getDesc(iceStatus) + "???";
                } else if (ExamineStatus.REJECT_EXAMINE.getStatus().equals(examinStatus)) {
                    statusStr = IceBoxEnums.StatusEnum.getDesc(status);
                }
            }
        }
        map.put("statusStr",statusStr);

        return map;
    }

    @Override
    public Map<String, Object> readModule(Integer id) {

        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(id);
        Map<String, Object> map = Maps.newHashMap();
        map.put("bluetoothId", iceBoxExtend.getBluetoothId()); // ????????????ID
        map.put("bluetoothMac", iceBoxExtend.getBluetoothMac()); // ??????????????????
        map.put("gpsMac", iceBoxExtend.getGpsMac()); // GPS????????????
        map.put("qrCode", iceBoxExtend.getQrCode()); // ???????????????

        return map;
    }

    @Override
    public Map<String, Object> readEquipNews(Integer id) {
        DateTime now = new DateTime();
        Date todayStart = now.withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0).toDate();
        Date todayEnd = now.withHourOfDay(23).withMinuteOfHour(59).withSecondOfMinute(59).toDate();

        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(id);
        IceEventRecord iceEventRecord = iceEventRecordDao.selectOne(Wrappers.<IceEventRecord>lambdaQuery()
                .eq(IceEventRecord::getAssetId, iceBoxExtend.getAssetId()).between(IceEventRecord::getOccurrenceTime,todayStart,todayEnd).orderByDesc(IceEventRecord::getId)
                .last(" limit 1"));
        Map<String, Object> map = Maps.newHashMap();
        Optional.ofNullable(iceEventRecord).ifPresent(info -> {
            map.put("temperature", info.getTemperature()); // ??????
            String assetId = iceBoxExtend.getAssetId();
            // ??????????????? -- ????????????
            //Integer totalSum = iceEventRecordDao.sumTotalOpenCloseCount(assetId);

            // ??????????????? -- ?????????
            Date monStart = now.dayOfMonth().withMinimumValue().withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0).toDate();
            Date monEnd = now.dayOfMonth().withMaximumValue().withHourOfDay(23).withMinuteOfHour(59).withSecondOfMinute(59).toDate();
            Integer monthSum = iceEventRecordDao.sumOpenCloseCount(assetId, monStart, monEnd);

            // ??????????????? -- ????????????

            Integer todaySum = iceEventRecordDao.sumOpenCloseCount(assetId, todayStart, todayEnd);

            //map.put("totalSum", totalSum);
            map.put("monthSum", monthSum);
            map.put("todaySum", todaySum);
            String address = info.getDetailAddress();
            if (StringUtils.isBlank(address)) {
                if (StringUtils.isNotBlank(info.getLng()) && StringUtils.isNotBlank(info.getLat())) {
                    map.put("lat",info.getLat());
                    map.put("lng",info.getLng());
                    AddressVo addressVo = FeignResponseUtil.getFeignData(feignXcxBaseClient.getAddressBylatAndLng(info.getLng(), info.getLat()));
                    if (addressVo != null) {
                        address = addressVo.getAddress();
                    }
                }
            }
            map.put("address", address); // ????????????
            map.put("occurrenceTime", info.getOccurrenceTime()); // ??????????????????
        });

        return map;
    }

    @Override
    public IPage readTransferRecord(IceTransferRecordPage iceTransferRecordPage) {

        IPage page = iceTransferRecordDao.selectPage(iceTransferRecordPage, Wrappers.<IceTransferRecord>lambdaQuery()
                .eq(IceTransferRecord::getBoxId, iceTransferRecordPage.getIceBoxId())
                .orderByDesc(IceTransferRecord::getId));
        List<IceTransferRecord> iceTransferRecordList = page.getRecords();
        if (CollectionUtils.isEmpty(iceTransferRecordList)) {
            return page;
        }

        // ???????????????  map
        Map<String, SimpleStoreVo> storeVoMap = null;
        Set<String> storeNumbers = iceTransferRecordList.stream().map(IceTransferRecord::getStoreNumber).collect(Collectors.toSet());
        if (CollectionUtils.isNotEmpty(storeNumbers)) {
            storeVoMap = FeignResponseUtil.getFeignData(feignStoreClient.getSimpleStoreByNumberList(Lists.newArrayList(storeNumbers)));
        }
        // ?????????????????? map
        Map<Integer, SubordinateInfoVo> supplierMap = null;
        Set<Integer> supplierIds = iceTransferRecordList.stream().map(IceTransferRecord::getSupplierId).collect(Collectors.toSet());
        if (CollectionUtils.isNotEmpty(supplierIds)) {
            supplierMap = FeignResponseUtil.getFeignData(feignSupplierClient.findByIds(Lists.newArrayList(supplierIds)));
        }
        // ?????????  map
        Map<Integer, SessionUserInfoVo> userInfoVoMap = null;
        Set<Integer> userIds = iceTransferRecordList.stream().map(IceTransferRecord::getApplyUserId).collect(Collectors.toSet());
        if (CollectionUtils.isNotEmpty(userIds)) {
            userInfoVoMap = FeignResponseUtil.getFeignData(feignUserClient.getSessionUserInfoVoByIds(Lists.newArrayList(userIds)));
        }

        List<Map<String, Object>> transferRecordList = Lists.newArrayList(); // ????????????
        for (IceTransferRecord transferRecord : iceTransferRecordList) {
            Map<String, Object> map = Maps.newHashMap();
            Integer serviceType = transferRecord.getServiceType(); //  ???????????? 0:?????? 1:?????? 2:??????
            map.put("serviceTypeStr", com.szeastroc.icebox.newprocess.enums.ServiceType.getDesc(serviceType)); // ????????????
            Integer supplierId = transferRecord.getSupplierId(); // ??????????????????ID
            String storeNumber = transferRecord.getStoreNumber(); // ???????????????Number
            String sendName = null;
            String receiveName = null;
            if (com.szeastroc.icebox.newprocess.enums.ServiceType.ENTER_WAREHOUSE.getType().equals(serviceType)) { // ??????  (??????-->?????????)
                if (supplierMap != null && supplierId != null) {
                    SubordinateInfoVo infoVo = supplierMap.get(supplierId);
                    receiveName = infoVo == null ? null : infoVo.getName();
                }
            } else if (com.szeastroc.icebox.newprocess.enums.ServiceType.IS_PUT.getType().equals(serviceType)) { // ??????  (?????????-->??????)
                if (supplierMap != null && supplierId != null) {
                    SubordinateInfoVo infoVo = supplierMap.get(supplierId);
                    sendName = infoVo == null ? null : infoVo.getName();
                }
                if (storeVoMap != null && StringUtils.isNotBlank(storeNumber)) {
                    SimpleStoreVo storeVo = storeVoMap.get(storeNumber);
                    receiveName = storeVo == null ? null : storeVo.getStoreName();
                }
            } else if (com.szeastroc.icebox.newprocess.enums.ServiceType.IS_RETURN.getType().equals(serviceType)) { // ??????  (??????-->?????????)
                if (storeVoMap != null && StringUtils.isNotBlank(storeNumber)) {
                    SimpleStoreVo storeVo = storeVoMap.get(storeNumber);
                    sendName = storeVo == null ? null : storeVo.getStoreName();
                }
                if (supplierMap != null && supplierId != null) {
                    SubordinateInfoVo infoVo = supplierMap.get(supplierId);
                    receiveName = infoVo == null ? null : infoVo.getName();
                }
            }

            map.put("sendName", sendName); // ?????????
            map.put("sendTime", transferRecord.getSendTime()); // ????????????
            map.put("receiveName", receiveName); // ?????????
            map.put("receiveTime", transferRecord.getReceiveTime()); // ????????????
            SessionUserInfoVo userInfoVo = null;
            if (userInfoVoMap != null && transferRecord.getApplyUserId() != null) {
                userInfoVo = userInfoVoMap.get(transferRecord.getApplyUserId());
            }
            map.put("applyUser", userInfoVo == null ? null : userInfoVo.getRealname()); // ?????????
            map.put("applyTime", transferRecord.getApplyTime()); // ????????????
            map.put("transferMoney", transferRecord.getTransferMoney()); // ????????????
            map.put("applyNumber", transferRecord.getApplyNumber()); // ????????????
            map.put("recordStatusStr", com.szeastroc.icebox.newprocess.enums.RecordStatus.getDesc(transferRecord.getRecordStatus())); // ??????
//            map.put("supplierId", transferRecord.getSupplierId()); // ??????

            transferRecordList.add(map);
        }

        return page.setRecords(transferRecordList);
    }

    @Override
    public IPage readExamine(IceExaminePage iceExaminePage) {

        IPage page = iceExamineDao.selectPage(iceExaminePage, Wrappers.<IceExamine>lambdaQuery()
                .eq(IceExamine::getIceBoxId, iceExaminePage.getIceBoxId())
                .orderByDesc(IceExamine::getId));
        List<IceExamine> iceExamineList = page.getRecords();
        if (CollectionUtils.isEmpty(iceExamineList)) {
            return page;
        }
        Set<String> storeNumbers = iceExamineList.stream().map(IceExamine::getStoreNumber).collect(Collectors.toSet());
        Set<Integer> userIds = iceExamineList.stream().map(IceExamine::getCreateBy).collect(Collectors.toSet());
        Map<String, SimpleStoreVo> storeVoMap = null; // ????????????
        if (CollectionUtils.isNotEmpty(storeNumbers)) {
            storeVoMap = FeignResponseUtil.getFeignData(feignStoreClient.getSimpleStoreByNumberList(Lists.newArrayList(storeNumbers)));
        }
        Map<Integer, SessionUserInfoVo> userInfoVoMap = null; // ??????????????????
        if (CollectionUtils.isNotEmpty(userIds)) {
            userInfoVoMap = FeignResponseUtil.getFeignData(feignUserClient.getSessionUserInfoVoByIds(Lists.newArrayList(userIds)));
        }

        List<Map<String, Object>> examineList = Lists.newArrayList(); // ????????????
        for (IceExamine i : iceExamineList) {
            Map<String, Object> map = Maps.newHashMap();
            map.put("storeNumber", i.getStoreNumber()); // ????????????
            SimpleStoreVo storeVo = null;
            if (storeVoMap != null) {
                storeVo = storeVoMap.get(i.getStoreNumber());
            }
            map.put("storeName", storeVo == null ? null : storeVo.getStoreName()); // ????????????
            SessionUserInfoVo userInfoVo = null;
            if (userInfoVoMap != null) {
                userInfoVo = userInfoVoMap.get(i.getCreateBy());
            }
            map.put("realname", userInfoVo == null ? null : userInfoVo.getRealname()); // ????????????
            map.put("createTime", i.getCreateTime()); // ????????????
            map.put("displayImage", i.getDisplayImage()); // ????????????
            map.put("exteriorImage", i.getExteriorImage()); // ???????????????URL
            map.put("assetImage", i.getAssetImage()); // ???????????????URL
//            map.put("storeNumber", i.getStoreNumber()); // ??????
            examineList.add(map);
        }

        return page.setRecords(examineList);
    }

    @Transactional(rollbackFor = Exception.class, value = "transactionManager")
    @Override
    public List<JSONObject> importByEasyExcel(MultipartFile mfile) throws Exception {

        /**
         * @Date: 2020/5/20 9:19 xiao
         *  ????????????????????????????????????????????????????????????????????????????????????
         */
        // ?????? ????????????????????????class??????????????????????????????sheet ?????????????????????finish
        List<ImportIceBoxVo> importDataList = EasyExcel.read(mfile.getInputStream()).head(ImportIceBoxVo.class).sheet().doReadSync();
        if (CollectionUtils.isEmpty(importDataList)) {
            throw new NormalOptionException(Constants.API_CODE_FAIL, "??????????????????");
        }

        // ????????????????????????
        List<IceModel> iceModels = iceModelDao.selectList(Wrappers.<IceModel>lambdaQuery());
        Map<String, Integer> iceModelMap = Maps.newHashMap();
        Optional.ofNullable(iceModels).ifPresent(list -> {
            list.forEach(i -> {
                iceModelMap.put(i.getChestModel(), i.getId());
            });
        });
        Map<String, SubordinateInfoVo> supplierNumberMap = Maps.newHashMap(); // ????????????????????????id

        int importSize = importDataList.size();
        List<JSONObject> lists = Lists.newArrayList();

        for (ImportIceBoxVo boxVo : importDataList) {

            Integer serialNumber = boxVo.getSerialNumber(); // ??????
            if (serialNumber == null) {
                throw new NormalOptionException(Constants.API_CODE_FAIL, "?????? ????????????");
            }
            String externalId = boxVo.getExternalId();  // ???????????????ID
            String assetId = boxVo.getAssetId();// ????????????
            // ?????? ????????????--????????????id ????????????????????????????????????
            if (StringUtils.isBlank(assetId)) {
                throw new NormalOptionException(Constants.API_CODE_FAIL, "???" + boxVo.getSerialNumber() + "???:???????????? ??????");
            }
            String qrCode = boxVo.getQrCode();// ???????????????
            if (StringUtils.isBlank(qrCode)) {
                throw new NormalOptionException(Constants.API_CODE_FAIL, "???" + boxVo.getSerialNumber() + "???:????????????????????? ??????");
            }

            String chestName = boxVo.getChestName();// ????????????
            if (StringUtils.isBlank(chestName)) {
                throw new NormalOptionException(Constants.API_CODE_FAIL, "???" + boxVo.getSerialNumber() + "???:???????????? ??????");
            }
            String brandName = boxVo.getBrandName();// ????????????
            if (StringUtils.isBlank(brandName)) {
                throw new NormalOptionException(Constants.API_CODE_FAIL, "???" + boxVo.getSerialNumber() + "???:???????????? ??????");
            }
            String modelStr = boxVo.getModelStr();// ????????????
            if (StringUtils.isBlank(modelStr)) {
                throw new NormalOptionException(Constants.API_CODE_FAIL, "???" + boxVo.getSerialNumber() + "???:???????????? ??????");
            }
            String chestNorm = boxVo.getChestNorm();// ????????????
            if (StringUtils.isBlank(chestNorm)) {
                throw new NormalOptionException(Constants.API_CODE_FAIL, "???" + boxVo.getSerialNumber() + "???:???????????? ??????");
            }
            Long chestMoney = boxVo.getChestMoney();// ????????????
            if (chestMoney == null) {
                throw new NormalOptionException(Constants.API_CODE_FAIL, "???" + boxVo.getSerialNumber() + "???:???????????? ??????");
            }
            Long depositMoney = boxVo.getDepositMoney();// ????????????
            if (depositMoney == null) {
                throw new NormalOptionException(Constants.API_CODE_FAIL, "???" + boxVo.getSerialNumber() + "???:???????????? ??????");
            }
            String supplierNumber = boxVo.getSupplierNumber();
            if (StringUtils.isBlank(supplierNumber)) {
                throw new NormalOptionException(Constants.API_CODE_FAIL, "???" + boxVo.getSerialNumber() + "???:???????????????????????? ??????");
            }
            SupplierInfoSessionVo supplierInfoSessionVo = FeignResponseUtil.getFeignData(feignSupplierClient.getSuppliserInfoByNumber(supplierNumber));
            if(supplierInfoSessionVo == null){
                throw new NormalOptionException(Constants.API_CODE_FAIL, "???" + boxVo.getSerialNumber() + "???:??????????????????");
            }
            String supplierName = boxVo.getSupplierName(); // ???????????????
            if (StringUtils.isBlank(supplierName)) {
                throw new NormalOptionException(Constants.API_CODE_FAIL, "???" + boxVo.getSerialNumber() + "???:??????????????? ??????");
            }
            String deptName = boxVo.getDeptName(); // ???????????????
            if (StringUtils.isBlank(deptName)) {
                throw new NormalOptionException(Constants.API_CODE_FAIL, "???" + boxVo.getSerialNumber() + "???:??????????????? ??????");
            }
            Date releaseTime = boxVo.getReleaseTime();// ????????????
            if (releaseTime == null) {
                throw new NormalOptionException(Constants.API_CODE_FAIL, "???" + boxVo.getSerialNumber() + "???:???????????? ??????");
            }
            Date repairBeginTime = boxVo.getRepairBeginTime();// ??????????????????
            if (repairBeginTime == null) {
                throw new NormalOptionException(Constants.API_CODE_FAIL, "???" + boxVo.getSerialNumber() + "???:?????????????????? ??????");
            }


            IceBox iceBox = iceBoxDao.selectOne(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getAssetId, assetId).last(" limit 1"));
            IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectOne(Wrappers.<IceBoxExtend>lambdaQuery().eq(IceBoxExtend::getAssetId, assetId).last(" limit 1"));
            if ((iceBox == null && iceBoxExtend != null) || (iceBox != null && iceBoxExtend == null)) { // ????????????????????????,?????????????????????
                throw new NormalOptionException(Constants.API_CODE_FAIL, "???" + boxVo.getSerialNumber() + "???:????????????????????????");
            }

            String bluetoothId = boxVo.getBluetoothId();// ????????????ID
            String bluetoothMac = boxVo.getBluetoothMac();// ??????????????????
            String gpsMac = boxVo.getGpsMac();// gps??????MAC

            if (iceModelMap == null || iceModelMap.get(modelStr) == null) {
                throw new NormalOptionException(Constants.API_CODE_FAIL, "???" + boxVo.getSerialNumber() + "???:?????????????????????????????????");
            }
            Integer modelId = iceModelMap.get(modelStr); // ????????????


            Integer supplierId = null;  // ?????????id
            String suppName = null; // ???????????????
            SubordinateInfoVo subordinateInfoVo = supplierNumberMap.get(supplierNumber);
            if (subordinateInfoVo != null && subordinateInfoVo.getSupplierId() != null) {
                supplierId = subordinateInfoVo.getSupplierId();
                suppName = subordinateInfoVo.getName();
            } else {
                // ??????????????????
                SubordinateInfoVo infoVo = FeignResponseUtil.getFeignData(feignSupplierClient.findByNumber(supplierNumber));
                if (infoVo == null) {
                    throw new NormalOptionException(Constants.API_CODE_FAIL, "???" + boxVo.getSerialNumber() + "???:????????????????????????");
                }
                suppName = infoVo.getName();
                supplierId = infoVo.getSupplierId();
                supplierNumberMap.put(supplierNumber, infoVo);
            }

            // ????????????????????????????????????????????????,???????????????????????????
            Integer deptId = supplierNumberMap.get(supplierNumber).getMarketAreaId(); // ???????????????
            if (iceBox == null) {
                iceBox = new IceBox();
                iceBox.setPutStatus(PutStatus.NO_PUT.getStatus()); // ?????????
                iceBoxExtend = new IceBoxExtend();
            }
            iceBox.setChestName(chestName)
                    .setAssetId(assetId)
                    .setModelId(modelId)
                    .setModelName(modelStr)
                    .setBrandName(brandName)
                    .setChestNorm(chestNorm)
                    .setChestMoney(new BigDecimal(chestMoney))
                    .setDepositMoney(new BigDecimal(depositMoney))
                    .setSupplierId(supplierId)
                    .setDeptId(deptId);

            iceBoxExtend.setExternalId(externalId)
                    .setAssetId(assetId)
                    .setBluetoothId(bluetoothId)
                    .setBluetoothMac(bluetoothMac)
                    .setQrCode(qrCode)
                    .setGpsMac(gpsMac)
                    .setReleaseTime(releaseTime)
                    .setRepairBeginTime(repairBeginTime);

            /**
             * @Date: 2020/5/20 11:07 xiao
             *  ??????:
             *  ???????????????ID  ????????????ID     ??????????????????     ????????????????????? ????????????,??????????????????,?????????????????????
             *  external_id   bluetooth_id  bluetooth_mac    qr_code
             */
            if (iceBox.getId() == null) {
                try {
                    iceBoxDao.insert(iceBox);
                    iceBoxExtend.setId(iceBox.getId());
                    iceBoxExtendDao.insert(iceBoxExtend);
                } catch (Exception e) {
                    log.info("????????????????????????", e);
                    iceBoxDao.deleteById(iceBox.getId());
                    iceBoxExtendDao.deleteById(iceBox.getId());
                    throw new NormalOptionException(Constants.API_CODE_FAIL, "???" + boxVo.getSerialNumber() + "???:???????????????ID???????????????ID??????????????????????????????????????????????????????");
                }
            } else {
                try {
                    iceBoxDao.updateById(iceBox);
                    iceBoxExtendDao.updateById(iceBoxExtend);
                } catch (Exception e) {
                    log.info("????????????????????????", e);
                    throw new NormalOptionException(Constants.API_CODE_FAIL, "???" + boxVo.getSerialNumber() + "???:???????????????ID???????????????ID??????????????????????????????????????????????????????");
                }
            }

            JSONObject jsonObject = setAssetReportJson(iceBox, "importByEasyExcel");
            lists.add(jsonObject);
        }
        log.info("importExcel ??????????????????-->{}", importSize);
        return lists;
    }

    @Override
    public List<IceBox> getIceBoxList(String pxtNumber) {
        List<IceBox> iceBoxes = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getPutStoreNumber, pxtNumber));
        return iceBoxes;
    }

    @Override
    public Map<String, List<IceBoxVo>> findPutingIceBoxList(IceBoxRequestVo requestVo) {
        List<IceBoxVo> iceBoxVos = new ArrayList<>();
        //?????????
        if (XcxType.IS_PUTING.getStatus().equals(requestVo.getType())) {
            LambdaQueryWrapper<IcePutApply> applyWrapper = Wrappers.<IcePutApply>lambdaQuery();
            applyWrapper.eq(IcePutApply::getPutStoreNumber, requestVo.getStoreNumber());
            applyWrapper.eq(IcePutApply::getStoreSignStatus, StoreSignStatus.DEFAULT_SIGN.getStatus());
            applyWrapper.ne(IcePutApply::getExamineStatus, ExamineStatusEnum.UN_PASS.getStatus());
            List<IcePutApply> icePutApplies = icePutApplyDao.selectList(applyWrapper);
            if (CollectionUtil.isNotEmpty(icePutApplies)) {
                List<IceBoxVo> putIceBoxVos = this.getIceBoxVosByPutApplys(icePutApplies);
                if (CollectionUtil.isNotEmpty(putIceBoxVos)) {
                    iceBoxVos.addAll(putIceBoxVos);
                }
            }
            LambdaQueryWrapper<IceBackApply> backWrapper = Wrappers.<IceBackApply>lambdaQuery();
            backWrapper.eq(IceBackApply::getBackStoreNumber, requestVo.getStoreNumber());
            backWrapper.ne(IceBackApply::getExamineStatus, ExamineStatusEnum.NO_DEFAULT.getStatus());
            backWrapper.and(x -> x.eq(IceBackApply::getExamineStatus, ExamineStatusEnum.NO_DEFAULT.getStatus()).or().eq(IceBackApply::getExamineStatus, ExamineStatusEnum.IS_DEFAULT.getStatus()));

            List<IceBackApply> iceBackApplies = iceBackApplyDao.selectList(backWrapper);
            if (CollectionUtil.isNotEmpty(iceBackApplies)) {
                List<IceBoxVo> backIceBoxVos = this.getIceBoxVosByBackApplys(iceBackApplies);
                if (CollectionUtil.isNotEmpty(backIceBoxVos)) {
                    iceBoxVos.addAll(backIceBoxVos);
                }
            }
        }
        Map<String, List<IceBoxVo>> map = Streams.toStream(iceBoxVos).collect(Collectors.groupingBy(IceBoxVo::getApplyNumber));
        return map;
    }

    @Override
    public List<IceBoxVo> findPutIceBoxList(String pxtNumber) {
        List<IceBoxVo> iceBoxVos = new ArrayList<>();
        List<IceBox> iceBoxes = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getPutStoreNumber, pxtNumber).eq(IceBox::getPutStatus, PutStatus.FINISH_PUT.getStatus()));
        if (CollectionUtil.isEmpty(iceBoxes)) {
            return iceBoxVos;
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (IceBox iceBox : iceBoxes) {
            IceBoxVo boxVo = buildIceBoxVo(dateFormat, iceBox);
            IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(iceBox.getId());
            if (iceBoxExtend != null) {
                boxVo.setQrCode(iceBoxExtend.getQrCode());
            }
            DateTime now = new DateTime();
            Date todayStart = now.withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0).toDate();
            Date todayEnd = now.withHourOfDay(23).withMinuteOfHour(59).withSecondOfMinute(59).toDate();
            IceEventRecord iceEventRecord = iceEventRecordDao.selectOne(Wrappers.<IceEventRecord>lambdaQuery()
                    .eq(IceEventRecord::getAssetId, iceBoxExtend.getAssetId())
                    .orderByDesc(IceEventRecord::getCreateTime)
                    .between(IceEventRecord::getOccurrenceTime,todayStart,todayEnd)
                    .last("limit 1"));
            if (iceEventRecord != null) {
                boxVo.setDetailAddress(iceEventRecord.getDetailAddress());
            }

//            IcePutApply icePutApply = icePutApplyDao.selectOne(Wrappers.<IcePutApply>lambdaQuery().eq(IcePutApply::getApplyNumber, iceBoxExtend.getLastApplyNumber()));
            IcePutApplyRelateBox relateBox = icePutApplyRelateBoxDao.selectOne(Wrappers.<IcePutApplyRelateBox>lambdaQuery().eq(IcePutApplyRelateBox::getApplyNumber, iceBoxExtend.getLastApplyNumber()).eq(IcePutApplyRelateBox::getBoxId, iceBox.getId()));
            if (relateBox != null) {
                boxVo.setFreeType(relateBox.getFreeType());
                IceBackApply iceBackApply = iceBackApplyDao.selectOne(Wrappers.<IceBackApply>lambdaQuery().eq(IceBackApply::getOldPutId, relateBox.getId())
                        .ne(IceBackApply::getExamineStatus, 3));
                boxVo.setBackStatus(iceBackApply == null ? -1 : iceBackApply.getExamineStatus());
            }
            Integer unfinishOrderCount = iceRepairOrderService.getUnfinishOrderCount(iceBox.getId());
            if(unfinishOrderCount>0){
                boxVo.setRepairing(Boolean.TRUE);
            }else{
                boxVo.setRepairing(Boolean.FALSE);
            }
            iceBoxVos.add(boxVo);
        }
        return iceBoxVos;
    }

    @Override
    @Transactional(value = "transactionManager", rollbackFor = Exception.class)
    public Map<String, Object> submitApplyNew(List<IceBoxRequestVo> requestNewVos) throws InterruptedException {
        Map<String, Object> map = new HashMap<>();
        IceBoxRequestVo iceBoxRequestVo = requestNewVos.get(0);
        String applyNumber = "PUT" + IdUtil.simpleUUID().substring(0, 29);
        IcePutApply icePutApply = IcePutApply.builder()
                .applyNumber(applyNumber)
                .putStoreNumber(iceBoxRequestVo.getStoreNumber())
                .userId(iceBoxRequestVo.getUserId())
                .createdBy(iceBoxRequestVo.getUserId())
                .applyPit(iceBoxRequestVo.getApplyPit())
                .build();
        icePutApplyDao.insert(icePutApply);
        iceBoxRequestVo.setVisitTypeName(VisitCycleEnum.getDescByCode(FeignResponseUtil.getFeignData(feignIceboxQueryClient.selectVisitTypeForReport(iceBoxRequestVo.getStoreNumber()))));
        List<IceBoxPutModel.IceBoxModel> iceBoxModels = new ArrayList<>();
        BigDecimal totalMoney = new BigDecimal(0);
        Date now = new Date();

        //????????????????????????
        MatchRuleVo matchRuleVo = new MatchRuleVo();
        matchRuleVo.setOpreateType(3);
        matchRuleVo.setDeptId(iceBoxRequestVo.getMarketAreaId());
        matchRuleVo.setType(2);
        SysRuleIceDetailVo ruleIceDetailVo = FeignResponseUtil.getFeignData(feignDeptRuleClient.matchIceRule(matchRuleVo));
        Integer freeType = null;
        if (ruleIceDetailVo != null) {
            freeType = FreePayTypeEnum.UN_FREE.getType();
            if (ruleIceDetailVo.getIsNoDeposit()) {
                freeType = FreePayTypeEnum.IS_FREE.getType();
            }
        }
        for (IceBoxRequestVo requestVo : requestNewVos) {
            for (int i = 0; i < requestVo.getApplyCount(); i++) {

                RedisLockUtil lock = new RedisLockUtil(redisTemplate, RedisConstant.ICE_BOX_LOCK + requestVo.getSupplierId() + "-" + requestVo.getModelId() + "-" + i, 5000, 10000);
                try {
                    if (lock.lock()) {
//                        List<IceBox> iceBoxes = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getModelId, requestVo.getModelId()).eq(IceBox::getSupplierId, iceBoxRequestVo.getSupplierId()).eq(IceBox::getPutStatus, PutStatus.NO_PUT.getStatus()));
                        List<IceBox> iceBoxes = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getModelId, requestVo.getModelId())
                                .eq(IceBox::getSupplierId, requestVo.getSupplierId()));
                        List<PutStoreRelateModel> putStoreRelateModels = putStoreRelateModelDao.selectList(Wrappers.<PutStoreRelateModel>lambdaQuery().eq(PutStoreRelateModel::getSupplierId, requestVo.getSupplierId())
                                .eq(PutStoreRelateModel::getModelId, requestVo.getModelId())
                                .eq(PutStoreRelateModel::getStatus, CommonStatus.VALID.getStatus())
                                .ne(PutStoreRelateModel::getPutStatus, PutStatus.NO_PUT.getStatus()));
                        int allCount = 0;
                        int putCount = 0;

                        if (CollectionUtil.isNotEmpty(iceBoxes)) {
                            allCount = iceBoxes.size();
                        }
                        if (CollectionUtil.isNotEmpty(putStoreRelateModels)) {
                            putCount = putStoreRelateModels.size();
                        }
                        if (allCount - putCount <= 0) {
                            throw new ImproperOptionException("??????????????????");
                        }
                        IceBox iceBox = iceBoxes.get(0);
                        totalMoney = totalMoney.add(iceBox.getDepositMoney());
                        PutStoreRelateModel relateModel = PutStoreRelateModel.builder()
                                .putStoreNumber(requestVo.getStoreNumber())
                                .modelId(requestVo.getModelId())
                                .supplierId(requestVo.getSupplierId())
                                .createBy(requestVo.getUserId())
                                .createTime(now)
                                .putStatus(PutStatus.LOCK_PUT.getStatus())
                                .build();
                        putStoreRelateModelDao.insert(relateModel);


                        if (freeType == null) {
                            freeType = requestVo.getFreeType();
                        }
                        ApplyRelatePutStoreModel applyRelatePutStoreModel = ApplyRelatePutStoreModel.builder()
                                .applyNumber(applyNumber)
                                .storeRelateModelId(relateModel.getId())
                                .freeType(freeType)
                                .build();
                        applyRelatePutStoreModelDao.insert(applyRelatePutStoreModel);
                        //??????mq??????,???????????????????????????
                        Integer isFree = freeType;
                        CompletableFuture.runAsync(() -> {
                            requestVo.setFreeType(isFree);
                            buildReportAndSendMq(requestVo, applyNumber, now,relateModel.getId());
                        }, ExecutorServiceFactory.getInstance());

                        relateModel.setIsSync(IsSyncEnum.IS_SEND.getStatus());
                        putStoreRelateModelDao.updateById(relateModel);
                    }
                } catch (Exception e) {
                    throw e;
                } finally {
                    lock.unlock();
                }
            }

            SubordinateInfoVo supplier = FeignResponseUtil.getFeignData(feignSupplierClient.findSupplierBySupplierId(requestVo.getSupplierId()));
            if (supplier == null) {
                log.info("???????????????id--??????{}??????????????????????????????", iceBoxRequestVo.getSupplierId());
                throw new ImproperOptionException("???????????????????????????");
            }
            IceBoxPutModel.IceBoxModel iceBoxModel = new IceBoxPutModel.IceBoxModel(requestVo.getChestModel(), requestVo.getChestName(), requestVo.getDepositMoney(), requestVo.getApplyCount(),
                    requestVo.getFreeType(), requestVo.getSupplierName(), supplier.getAddress(), supplier.getLinkman(), supplier.getLinkmanMobile());
            iceBoxModels.add(iceBoxModel);
        }
        boolean regionLeaderCheck = false;
        List<Integer> freeTypes = requestNewVos.stream().map(x -> x.getFreeType()).collect(Collectors.toList());
        if (freeTypes.contains(FreePayTypeEnum.IS_FREE.getType())) {
            regionLeaderCheck = true;
        }
        map.put("isCheck", 0);
        map = createIceBoxPutExamine(iceBoxRequestVo, applyNumber, iceBoxModels, regionLeaderCheck, ruleIceDetailVo);
        List<SessionExamineVo.VisitExamineNodeVo> iceBoxPutExamine = (List<SessionExamineVo.VisitExamineNodeVo>) map.get("iceBoxPutExamine");
        if (CollectionUtil.isNotEmpty(iceBoxPutExamine)) {
            SessionExamineVo.VisitExamineNodeVo visitExamineNodeVo = iceBoxPutExamine.get(0);
            icePutApply.setExamineId(visitExamineNodeVo.getExamineId());
            icePutApplyDao.updateById(icePutApply);
        }
        return map;
    }

    private void buildReportAndSendMq(IceBoxRequestVo iceBoxRequestVo, String applyNumber, Date now,Integer relateModelId) {
        IceBoxPutReportMsg report = new IceBoxPutReportMsg();
        Map<Integer, SessionDeptInfoVo> deptInfoVoMap = FeignResponseUtil.getFeignData(feignCacheClient.getFiveLevelDept(iceBoxRequestVo.getMarketAreaId()));
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

        report.setApplyNumber(applyNumber);
        report.setDepositMoney(iceBoxRequestVo.getDepositMoney());
        report.setFreeType(iceBoxRequestVo.getFreeType());
        report.setIceBoxModelId(iceBoxRequestVo.getModelId());
        report.setIceBoxModelName(iceBoxRequestVo.getChestModel());
        SubordinateInfoVo supplier = FeignResponseUtil.getFeignData(feignSupplierClient.findSupplierBySupplierId(iceBoxRequestVo.getSupplierId()));
        report.setSupplierId(iceBoxRequestVo.getSupplierId());
        if (supplier != null) {
            report.setSupplierNumber(supplier.getNumber());
            report.setSupplierName(supplier.getName());
        }
        report.setPutCustomerNumber(iceBoxRequestVo.getStoreNumber());
        report.setPutCustomerName(iceBoxRequestVo.getStoreName());

        report.setPutCustomerType(SupplierTypeEnum.IS_STORE.getType());
        if (StringUtils.isNotEmpty(iceBoxRequestVo.getStoreNumber()) && !iceBoxRequestVo.getStoreNumber().startsWith("C0")) {
            SubordinateInfoVo putSupplier = FeignResponseUtil.getFeignData(feignSupplierClient.findByNumber(iceBoxRequestVo.getStoreNumber()));
            if (putSupplier != null) {
                report.setPutCustomerType(putSupplier.getSupplierType());
            }
        }

        report.setPutStatus(PutStatus.DO_PUT.getStatus());
        SimpleUserInfoVo userInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findUserById(iceBoxRequestVo.getUserId()));
        report.setSubmitterId(iceBoxRequestVo.getUserId());
        if (userInfoVo != null) {
            report.setSubmitterName(userInfoVo.getRealname());
        }
        report.setSubmitTime(now);
        report.setOperateType(OperateTypeEnum.INSERT.getType());
        if(relateModelId != null && relateModelId > 0){
            report.setPutStoreModelId(relateModelId);
        }
        rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.iceboxReportKey, report);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, value = "transactionManager")
    public void checkIceBoxNew(IceBoxRequest iceBoxRequest) {
        //?????????
        if (iceBoxRequest.getStatus() == null) {
            IcePutApply icePutApply = icePutApplyDao.selectOne(Wrappers.<IcePutApply>lambdaQuery().eq(IcePutApply::getApplyNumber, iceBoxRequest.getApplyNumber()));
            if (icePutApply != null) {
                icePutApply.setExamineStatus(iceBoxRequest.getExamineStatus());
                icePutApply.setUpdatedBy(iceBoxRequest.getUpdateBy());
                icePutApply.setUpdateTime(new Date());
                icePutApplyDao.updateById(icePutApply);
            }

            List<ApplyRelatePutStoreModel> applyRelatePutStoreModels = applyRelatePutStoreModelDao.selectList(Wrappers.<ApplyRelatePutStoreModel>lambdaQuery().eq(ApplyRelatePutStoreModel::getApplyNumber, iceBoxRequest.getApplyNumber()));
            Set<Integer> storeRelateModelIds = Streams.toStream(applyRelatePutStoreModels).map(x -> x.getStoreRelateModelId()).collect(Collectors.toSet());
            if (CollectionUtil.isNotEmpty(storeRelateModelIds)) {
                for (Integer storeRelateModelId : storeRelateModelIds) {
                    PutStoreRelateModel putStoreRelateModel = PutStoreRelateModel.builder()
                            .id(storeRelateModelId)
                            .examineStatus(ExamineStatus.DOING_EXAMINE.getStatus())
                            .updateTime(new Date())
                            .build();
                    putStoreRelateModelDao.update(putStoreRelateModel,Wrappers.<PutStoreRelateModel>lambdaUpdate()
                            .eq(PutStoreRelateModel::getId,storeRelateModelId)
                            .set(PutStoreRelateModel::getExamineRemark,iceBoxRequest.getExamineRemark()));
                }
            }

            //??????mq??????,???????????????????????????
//            CompletableFuture.runAsync(() -> {
//                IceBoxPutReportMsg report = new IceBoxPutReportMsg();
//                report.setApplyNumber(iceBoxRequest.getApplyNumber());
//                report.setExamineTime(new Date());
//                report.setExamineUserId(iceBoxRequest.getUpdateBy());
//                SimpleUserInfoVo userInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findUserById(iceBoxRequest.getUpdateBy()));
//                if (userInfoVo != null) {
//                    report.setExamineUserName(userInfoVo.getRealname());
//                }
//                report.setPutStatus(PutStatus.DO_PUT.getStatus());
//                report.setOperateType(OperateTypeEnum.UPDATE.getType());
//                rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.iceboxReportKey, report);
//            }, ExecutorServiceFactory.getInstance());


            IceBoxPutReportMsg reportMsg = new IceBoxPutReportMsg();
            reportMsg.setApplyNumber(iceBoxRequest.getApplyNumber());
            reportMsg.setExamineTime(new Date());
            reportMsg.setExamineUserId(iceBoxRequest.getUpdateBy());
            String examinePosion = "";
            SimpleUserInfoVo userInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findUserById(iceBoxRequest.getUpdateBy()));
            if (userInfoVo != null) {
                reportMsg.setExamineUserName(userInfoVo.getRealname());
                examinePosion = userInfoVo.getPosion();
            }
            reportMsg.setPutStatus(PutStatus.DO_PUT.getStatus());

            List<IceBoxPutReport> reportList = iceBoxPutReportDao.selectList(Wrappers.<IceBoxPutReport>lambdaQuery().eq(IceBoxPutReport::getApplyNumber, reportMsg.getApplyNumber()));
            if (CollectionUtil.isNotEmpty(reportList)) {
                for (IceBoxPutReport putReport : reportList) {
                    IceBoxPutReport report = new IceBoxPutReport();
                    BeanUtils.copyProperties(reportMsg, report);
                    report.setId(putReport.getId());
                    iceBoxPutReportDao.update(report,Wrappers.<IceBoxPutReport>lambdaUpdate()
                            .eq(IceBoxPutReport::getId,putReport.getId())
                            .set(IceBoxPutReport::getExamineRemark,iceBoxRequest.getExamineRemark())
                            .set(IceBoxPutReport::getExamineUserPosion,examinePosion));
                }
            }
        }
        //??????
        if (IceBoxStatus.NO_PUT.getStatus().equals(iceBoxRequest.getStatus())) {
            IcePutApply icePutApply = icePutApplyDao.selectOne(Wrappers.<IcePutApply>lambdaQuery().eq(IcePutApply::getApplyNumber, iceBoxRequest.getApplyNumber()));
            if (icePutApply != null) {
                icePutApply.setExamineStatus(ExamineStatusEnum.UN_PASS.getStatus());
                icePutApply.setUpdatedBy(iceBoxRequest.getUpdateBy());
                icePutApply.setUpdateTime(new Date());
                icePutApplyDao.updateById(icePutApply);
            }

            List<ApplyRelatePutStoreModel> applyRelatePutStoreModels = applyRelatePutStoreModelDao.selectList(Wrappers.<ApplyRelatePutStoreModel>lambdaQuery().eq(ApplyRelatePutStoreModel::getApplyNumber, iceBoxRequest.getApplyNumber()));
            Set<Integer> storeRelateModelIds = Streams.toStream(applyRelatePutStoreModels).map(x -> x.getStoreRelateModelId()).collect(Collectors.toSet());
            if (CollectionUtil.isNotEmpty(storeRelateModelIds)) {
                for (Integer storeRelateModelId : storeRelateModelIds) {
                    PutStoreRelateModel putStoreRelateModel = PutStoreRelateModel.builder()
                            .id(storeRelateModelId)
                            .putStatus(IceBoxStatus.NO_PUT.getStatus())
                            .examineStatus(ExamineStatus.REJECT_EXAMINE.getStatus())
                            .updateTime(new Date())
                            .build();
                    putStoreRelateModelDao.update(putStoreRelateModel,Wrappers.<PutStoreRelateModel>lambdaUpdate()
                            .eq(PutStoreRelateModel::getId,storeRelateModelId)
                            .set(PutStoreRelateModel::getExamineRemark,iceBoxRequest.getExamineRemark()));
                }
            }

            IceBoxPutReportMsg reportMsg = new IceBoxPutReportMsg();
            reportMsg.setApplyNumber(iceBoxRequest.getApplyNumber());
            reportMsg.setExamineTime(new Date());
            reportMsg.setExamineUserId(iceBoxRequest.getUpdateBy());
            String examinePosion = "";
            SimpleUserInfoVo userInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findUserById(iceBoxRequest.getUpdateBy()));
            if (userInfoVo != null) {
                reportMsg.setExamineUserName(userInfoVo.getRealname());
                examinePosion = userInfoVo.getPosion();
            }
            reportMsg.setPutStatus(PutStatus.NO_PASS.getStatus());
            List<IceBoxPutReport> reportList = iceBoxPutReportDao.selectList(Wrappers.<IceBoxPutReport>lambdaQuery().eq(IceBoxPutReport::getApplyNumber, reportMsg.getApplyNumber()));
            if (CollectionUtil.isNotEmpty(reportList)) {
                for (IceBoxPutReport putReport : reportList) {
                    IceBoxPutReport report = new IceBoxPutReport();
                    BeanUtils.copyProperties(reportMsg, report);
                    report.setId(putReport.getId());
                    iceBoxPutReportDao.update(report,Wrappers.<IceBoxPutReport>lambdaUpdate()
                            .eq(IceBoxPutReport::getId,putReport.getId())
                            .set(IceBoxPutReport::getExamineRemark,iceBoxRequest.getExamineRemark())
                            .set(IceBoxPutReport::getExamineUserPosion,examinePosion));
                }
            }
            //??????mq??????,???????????????????????????
//            CompletableFuture.runAsync(() -> {
//                IceBoxPutReportMsg report = new IceBoxPutReportMsg();
//                report.setApplyNumber(iceBoxRequest.getApplyNumber());
//                report.setExamineTime(new Date());
//                report.setExamineUserId(iceBoxRequest.getUpdateBy());
//                SimpleUserInfoVo userInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findUserById(iceBoxRequest.getUpdateBy()));
//                if (userInfoVo != null) {
//                    report.setExamineUserName(userInfoVo.getRealname());
//                }
//                report.setPutStatus(PutStatus.NO_PASS.getStatus());
//                report.setOperateType(OperateTypeEnum.UPDATE.getType());
//                rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.iceboxReportKey, report);
//            }, ExecutorServiceFactory.getInstance());
        }
        //?????????????????????????????????????????????????????????????????????????????????
        if (IceBoxStatus.IS_PUTING.getStatus().equals(iceBoxRequest.getStatus())) {
            dealCheckPassIceBox(iceBoxRequest);
        }
    }

    private void dealCheckPassIceBox(IceBoxRequest iceBoxRequest) {
        log.info("????????????????????????????????????---??????{}???", JSON.toJSONString(iceBoxRequest));
        IcePutApply icePutApply = icePutApplyDao.selectOne(Wrappers.<IcePutApply>lambdaQuery().eq(IcePutApply::getApplyNumber, iceBoxRequest.getApplyNumber()));
        if (icePutApply != null) {
            icePutApply.setExamineStatus(ExamineStatusEnum.IS_PASS.getStatus());
            icePutApply.setUpdatedBy(0);
            icePutApply.setUpdateTime(new Date());
            icePutApplyDao.updateById(icePutApply);
        }
        SysRuleIceDetailVo ruleIceDetailVo = null;
        List<ApplyRelatePutStoreModel> applyRelatePutStoreModels = applyRelatePutStoreModelDao.selectList(Wrappers.<ApplyRelatePutStoreModel>lambdaQuery().eq(ApplyRelatePutStoreModel::getApplyNumber, iceBoxRequest.getApplyNumber()));
        Set<Integer> storeRelateModelIds = Streams.toStream(applyRelatePutStoreModels).map(x -> x.getStoreRelateModelId()).collect(Collectors.toSet());
        if (CollectionUtil.isNotEmpty(storeRelateModelIds)) {
            for (Integer storeRelateModelId : storeRelateModelIds) {
                Map params = new HashMap();
                PutStoreRelateModel putStoreRelateModel = putStoreRelateModelDao.selectById(storeRelateModelId);
                if (putStoreRelateModel == null) {
                    continue;
                }
                putStoreRelateModel.setPutStatus(IceBoxStatus.IS_PUTING.getStatus());
                putStoreRelateModel.setExamineStatus(ExamineStatus.PASS_EXAMINE.getStatus());
                putStoreRelateModel.setUpdateTime(new Date());
                putStoreRelateModelDao.update(putStoreRelateModel,Wrappers.<PutStoreRelateModel>lambdaUpdate()
                        .eq(PutStoreRelateModel::getId,storeRelateModelId)
                        .set(PutStoreRelateModel::getExamineRemark,iceBoxRequest.getExamineRemark()));
                if(putStoreRelateModel.getSupplierId() != null && putStoreRelateModel.getSupplierId() > 0){
                    SupplierInfo supplierInfo = FeignResponseUtil.getFeignData(feignSupplierClient.findInfoById(putStoreRelateModel.getSupplierId()));
                    params.put("pxtNumber",supplierInfo.getNumber());
                }

                /**
                 * ????????????????????????
                 */
                IceBoxRelateDms iceBoxRelateDms = new IceBoxRelateDms();
                iceBoxRelateDms.setType(1);
                iceBoxRelateDms.setRelateNumber(iceBoxRequest.getApplyNumber());
                iceBoxRelateDms.setPutStoreRelateModelId(storeRelateModelId);
                iceBoxRelateDms.setPutstatus(PutStatus.DO_PUT.getStatus());
                iceBoxRelateDms.setExamineId(icePutApply.getExamineId());
                iceBoxRelateDms.setExamineRemark(iceBoxRequest.getExamineRemark());
                iceBoxRelateDms.setSupplierId(putStoreRelateModel.getSupplierId());
                iceBoxRelateDms.setModelId(putStoreRelateModel.getModelId());
                iceBoxRelateDms.setPutStoreNumber(putStoreRelateModel.getPutStoreNumber());

                //???????????????????????????
                IceBox iceBox = iceBoxDao.selectOne(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getModelId, putStoreRelateModel.getModelId())
                        .eq(IceBox::getSupplierId, putStoreRelateModel.getSupplierId())
                        .eq(IceBox::getPutStatus, PutStatus.NO_PUT.getStatus())
                        .last("limit 1"));
                if (iceBox != null && IceBoxEnums.TypeEnum.OLD_ICE_BOX.getType().equals(iceBox.getIceBoxType())) {

                    /**
                     * ????????????  ????????? ???????????????
                     */
                    /*iceBox.setPutStatus(PutStatus.DO_PUT.getStatus());
                    iceBox.setUpdatedTime(new Date());
                    iceBoxDao.updateById(iceBox);*/


                    OldIceBoxSignNotice oldIceBoxSignNotice = new OldIceBoxSignNotice();
                    oldIceBoxSignNotice.setApplyNumber(iceBoxRequest.getApplyNumber());
                    oldIceBoxSignNotice.setIceBoxId(iceBox.getId());
                    oldIceBoxSignNotice.setAssetId(iceBox.getAssetId());
                    oldIceBoxSignNotice.setPutStoreNumber(putStoreRelateModel.getPutStoreNumber());
                    oldIceBoxSignNotice.setCreateTime(new Date());
                    oldIceBoxSignNoticeDao.insert(oldIceBoxSignNotice);
                }
                //????????????????????????
                MatchRuleVo matchRuleVo = new MatchRuleVo();
                matchRuleVo.setOpreateType(3);
                Integer marketAreaId = iceBoxRequest.getMarketAreaId();
                if (marketAreaId == null) {
                    marketAreaId = iceBox.getDeptId();
                }
                matchRuleVo.setDeptId(marketAreaId);
                matchRuleVo.setType(2);
                ruleIceDetailVo = FeignResponseUtil.getFeignData(feignDeptRuleClient.matchIceRule(matchRuleVo));
                log.info("????????????????????????????????????,??????---??????{}???", JSON.toJSONString(ruleIceDetailVo));
                if (ruleIceDetailVo != null) {
                    if (!ruleIceDetailVo.getIsSign()) {

                       /* icePutApply.setStoreSignStatus(StoreSignStatus.ALREADY_SIGN.getStatus());
                        icePutApply.setUpdateTime(new Date());
                        icePutApplyDao.updateById(icePutApply);
                        //????????????????????????????????????????????????
                        IcePutApplyRelateBox isExist = icePutApplyRelateBoxDao.selectOne(Wrappers.<IcePutApplyRelateBox>lambdaQuery().eq(IcePutApplyRelateBox::getApplyNumber, icePutApply.getApplyNumber()).eq(IcePutApplyRelateBox::getBoxId, iceBox.getId()));
                        log.info("????????????????????????????????????,isExist---??????{}???", JSON.toJSONString(isExist));
                        if (isExist == null) {
                            IcePutApplyRelateBox applyRelateBox = new IcePutApplyRelateBox();
                            applyRelateBox.setApplyNumber(icePutApply.getApplyNumber());
                            applyRelateBox.setBoxId(iceBox.getId());
                            applyRelateBox.setModelId(iceBox.getModelId());
                            applyRelateBox.setFreeType(FreePayTypeEnum.IS_FREE.getType());
                            icePutApplyRelateBoxDao.insert(applyRelateBox);

                            //???????????????????????????
                            iceBox.setPutStoreNumber(putStoreRelateModel.getPutStoreNumber());
                            iceBox.setPutStatus(PutStatus.DO_PUT.getStatus());
                            iceBox.setUpdatedTime(new Date());
                            iceBoxDao.updateById(iceBox);
                            IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(iceBox.getId());
                            iceBoxExtend.setLastApplyNumber(icePutApply.getApplyNumber());
                            iceBoxExtend.setLastPutTime(icePutApply.getCreatedTime());
                            iceBoxExtend.setLastPutId(icePutApply.getId());
                            iceBoxExtendDao.updateById(iceBoxExtend);
                        }*/
                        IceBoxPutReport report = iceBoxPutReportDao.selectOne(Wrappers.<IceBoxPutReport>lambdaQuery().eq(IceBoxPutReport::getApplyNumber, icePutApply.getApplyNumber())
                                .eq(IceBoxPutReport::getIceBoxModelId, iceBox.getModelId())
                                .eq(IceBoxPutReport::getSupplierId, iceBox.getSupplierId())
                                .eq(IceBoxPutReport::getPutStatus, PutStatus.DO_PUT.getStatus()).last("limit 1"));
                        if (report != null) {
                            report.setIceBoxId(iceBox.getId());
                            report.setIceBoxAssetId(iceBox.getAssetId());
                            report.setExamineTime(new Date());
                            report.setExamineUserId(iceBoxRequest.getUpdateBy());
                            SimpleUserInfoVo userInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findUserById(iceBoxRequest.getUpdateBy()));
                            if (userInfoVo != null) {
                                report.setExamineUserName(userInfoVo.getRealname());
                                report.setExamineUserPosion(userInfoVo.getPosion());
                            }
                            /*iceBoxPutReportDao.update(report,Wrappers.<IceBoxPutReport>lambdaUpdate()
                                    .eq(IceBoxPutReport::getId,report.getId())
                                    .set(IceBoxPutReport::getExamineRemark,iceBoxRequest.getExamineRemark())
                                    .set(IceBoxPutReport::getExamineUserPosion,report.getExamineUserPosion()));*/
                            iceBoxRelateDms.setFreeType(report.getFreeType());
                            iceBoxRelateDms.setDepositMoney(report.getDepositMoney());
                        }

                        /**
                         * ??????????????????
                         */
                        iceBoxRelateDms.setIceBoxId(iceBox.getId());
                        iceBoxRelateDms.setIceBoxType(iceBox.getIceBoxType());
                        if(iceBox.getIceBoxType() == 1){
                            iceBoxRelateDms.setIceBoxAssetId(iceBox.getAssetId());
                        }

                    }
                }
                /**
                 * iceboxrelatedms ??????????????????
                 */
                iceBoxRelateDmsDao.insert(iceBoxRelateDms);
                /**
                 * ??????dms??????
                 */

                params.put("type",SendDmsIceboxTypeEnum.PUT_CONFIRM.getCode()+"");
                params.put("relateCode",iceBoxRelateDms.getId()+"");
                CompletableFuture.runAsync(()->SendRequestUtils.sendPostRequest(dmsUrlConfig.getToDmsUrl()+"/drpOpen/pxtAndIceBox/pxtToDmsIceBoxMsg",params), ExecutorServiceFactory.getInstance());

            }
        }

        if (ruleIceDetailVo == null || ruleIceDetailVo.getIsSign()) {
            String examineUserPosion = "";
            IceBoxPutReportMsg reportMsg = new IceBoxPutReportMsg();
            reportMsg.setApplyNumber(iceBoxRequest.getApplyNumber());
            reportMsg.setExamineTime(new Date());
            reportMsg.setExamineUserId(iceBoxRequest.getUpdateBy());
            SimpleUserInfoVo userInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findUserById(iceBoxRequest.getUpdateBy()));
            if (userInfoVo != null) {
                reportMsg.setExamineUserName(userInfoVo.getRealname());
                examineUserPosion = userInfoVo.getPosion();
            }
            reportMsg.setPutStatus(PutStatus.DO_PUT.getStatus());

            List<IceBoxPutReport> reportList = iceBoxPutReportDao.selectList(Wrappers.<IceBoxPutReport>lambdaQuery().eq(IceBoxPutReport::getApplyNumber, reportMsg.getApplyNumber()));
            if (CollectionUtil.isNotEmpty(reportList)) {
                for (IceBoxPutReport putReport : reportList) {
                    IceBoxPutReport report = new IceBoxPutReport();
                    BeanUtils.copyProperties(reportMsg, report);
                    report.setId(putReport.getId());
                    report.setExamineRemark(iceBoxRequest.getExamineRemark());
                    iceBoxPutReportDao.update(report,Wrappers.<IceBoxPutReport>lambdaUpdate()
                            .eq(IceBoxPutReport::getId,putReport.getId())
                            .set(IceBoxPutReport::getExamineRemark,iceBoxRequest.getExamineRemark())
                            .set(IceBoxPutReport::getExamineUserPosion,examineUserPosion));
                }
            }
            //??????mq??????,???????????????????????????
//            CompletableFuture.runAsync(() -> {
//                IceBoxPutReportMsg report = new IceBoxPutReportMsg();
//                report.setApplyNumber(iceBoxRequest.getApplyNumber());
//                report.setExamineTime(new Date());
//                report.setExamineUserId(iceBoxRequest.getUpdateBy());
//                SimpleUserInfoVo userInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findUserById(iceBoxRequest.getUpdateBy()));
//                if (userInfoVo != null) {
//                    report.setExamineUserName(userInfoVo.getRealname());
//                }
//                report.setPutStatus(PutStatus.DO_PUT.getStatus());
//                report.setOperateType(OperateTypeEnum.UPDATE.getType());
//                rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.iceboxReportKey, report);
//            }, ExecutorServiceFactory.getInstance());
        }
    }

    @Override
    public List<IceBoxVo> findIceBoxListNew(IceBoxRequestVo requestVo) {
        List<IceBoxVo> iceBoxVos = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //?????????
        if (XcxType.IS_PUTED.getStatus().equals(requestVo.getType())) {
            List<IceBox> iceBoxes = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getPutStoreNumber, requestVo.getStoreNumber()).eq(IceBox::getPutStatus, PutStatus.FINISH_PUT.getStatus()));
            if (CollectionUtil.isEmpty(iceBoxes)) {
                return iceBoxVos;
            }
            for (IceBox iceBox : iceBoxes) {
                IceBoxVo boxVo = buildIceBoxVo(dateFormat, iceBox);
                IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(iceBox.getId());
                LambdaQueryWrapper<IceExamine> wrapper = Wrappers.<IceExamine>lambdaQuery();
                wrapper.eq(IceExamine::getIceBoxId, iceBox.getId()).orderByDesc(IceExamine::getId).last("limit 1");
//                wrapper.and(x -> x.eq(IceExamine::getExaminStatus,ExamineStatus.DEFAULT_EXAMINE.getStatus()).or().eq(IceExamine::getExaminStatus,ExamineStatus.DOING_EXAMINE.getStatus()));
                IceExamine iceExamine = iceExamineDao.selectOne(wrapper);
                if (iceExamine != null) {
                    boxVo.setExamineStatus(iceExamine.getExaminStatus());
                    boxVo.setExamineNumber(iceExamine.getExamineNumber());
                    boxVo.setIceStatus(iceExamine.getIceStatus());
                    if (ExamineStatus.REJECT_EXAMINE.getStatus().equals(iceExamine.getExaminStatus())) {
                        boxVo.setIceStatus(iceBox.getStatus());
                    }
                } else {
                    boxVo.setIceStatus(iceBox.getStatus());
                }
                IcePutApplyRelateBox relateBox = icePutApplyRelateBoxDao.selectOne(Wrappers.<IcePutApplyRelateBox>lambdaQuery().eq(IcePutApplyRelateBox::getApplyNumber, iceBoxExtend.getLastApplyNumber()).eq(IcePutApplyRelateBox::getBoxId, iceBox.getId()));
                if (relateBox != null) {
                    IceBackApply iceBackApply = iceBackApplyDao.selectOne(Wrappers.<IceBackApply>lambdaQuery().eq(IceBackApply::getOldPutId, relateBox.getId())
                            .ne(IceBackApply::getExamineStatus, 3));
                    boxVo.setBackStatus(iceBackApply == null ? -1 : iceBackApply.getExamineStatus());
                }
                Integer unfinishOrderCount = iceRepairOrderService.getUnfinishOrderCount(iceBox.getId());
                if(unfinishOrderCount>0){
                    boxVo.setRepairing(Boolean.TRUE);
                }else{
                    boxVo.setRepairing(Boolean.FALSE);
                }
                //??????????????????
                boxVo.setVisitTypeName(VisitCycleEnum.getDescByCode(FeignResponseUtil.getFeignData(feignIceboxQueryClient.selectVisitTypeForReport(requestVo.getStoreNumber()))));
                iceBoxVos.add(boxVo);
            }
        }
        //?????????
        if (XcxType.NO_PUT.getStatus().equals(requestVo.getType())) {
            if (requestVo.getMarketAreaId() == null) {
                throw new ImproperOptionException("?????????????????????????????????");
            }
            Integer serviceId = FeignResponseUtil.getFeignData(feignDeptClient.getServiceId(requestVo.getMarketAreaId()));
            List<SimpleSupplierInfoVo> supplierInfoVos = FeignResponseUtil.getFeignData(feignSupplierClient.findByDeptId(serviceId));
            if (CollectionUtil.isEmpty(supplierInfoVos)) {
                return iceBoxVos;
            }
            Set<Integer> supplierIds = supplierInfoVos.stream().map(x -> x.getId()).collect(Collectors.toSet());
            Map<Integer, SimpleSupplierInfoVo> supplierInfoVoMap = supplierInfoVos.stream().collect(Collectors.toMap(SimpleSupplierInfoVo::getId, x -> x));
            LambdaQueryWrapper<IceBox> wrapper = Wrappers.<IceBox>lambdaQuery();
            /**
             * ??????e???????????????  ????????????  ???????????????????????????
             */
            Set<Integer> gxSet = new HashSet<>();
            //???????????? 10925
            gxSet.add(10926);
            gxSet.add(10931);
            gxSet.add(10934);
            gxSet.add(10938);
            gxSet.add(10942);
            gxSet.add(13160);
            gxSet.add(13162);
            gxSet.add(13178);
            //???????????? 10946
            gxSet.add(10947);
            gxSet.add(10952);
            gxSet.add(10955);
            gxSet.add(10959);
            gxSet.add(10963);
            gxSet.add(10966);
            gxSet.add(12368);
            //???????????? 10904
            gxSet.add(10905);
            gxSet.add(10909);
            gxSet.add(10911);
            gxSet.add(10913);
            gxSet.add(10916);
            gxSet.add(10920);
            gxSet.add(10923);
            gxSet.add(12363);

            Set<Integer> hnSet = new HashSet<>();
            //???????????? 13250
            hnSet.add(13252);
            hnSet.add(13268);
            hnSet.add(13274);
            hnSet.add(13278);
            hnSet.add(13281);
            //???????????? 13290
            hnSet.add(13291);
            hnSet.add(13299);
            hnSet.add(13306);
            hnSet.add(13309);
            hnSet.add(13315);
            hnSet.add(13326);
            hnSet.add(13332);

            Set<Integer> zjSet = new HashSet<>();
            //???????????? 8314
            zjSet.add(8318);
            zjSet.add(8322);
            zjSet.add(8325);
            zjSet.add(8329);
            zjSet.add(10332);
            zjSet.add(10334);
            zjSet.add(10335);
            zjSet.add(11178);
            zjSet.add(12221);
            zjSet.add(13099);
            //???????????? 8145
            zjSet.add(8146);
            zjSet.add(8157);
            zjSet.add(8164);
            zjSet.add(8172);
            zjSet.add(8178);
            zjSet.add(8182);
            zjSet.add(8193);
            zjSet.add(8200);
            zjSet.add(8201);
            zjSet.add(10308);
            zjSet.add(10309);
            zjSet.add(11161);
            zjSet.add(13114);
            zjSet.add(13120);
            //???????????? 13015
            zjSet.add(13016);
            zjSet.add(13021);
            zjSet.add(13026);
            zjSet.add(13034);
            zjSet.add(13039);
            zjSet.add(13044);
            zjSet.add(13054);

            if(hnSet.contains(serviceId)){
                //134496	2298
                supplierIds.add(134496);
                SupplierInfo supplierInfo = FeignResponseUtil.getFeignData(feignSupplierClient.findInfoById(134496));
                SimpleSupplierInfoVo vo = new SimpleSupplierInfoVo();
                BeanUtils.copyProperties(supplierInfo,vo);
                supplierInfoVoMap.put(134496,vo);
            }
            if(gxSet.contains(serviceId)){
                //id134494	number2297
                supplierIds.add(134494);
                SupplierInfo supplierInfo = FeignResponseUtil.getFeignData(feignSupplierClient.findInfoById(134494));
                SimpleSupplierInfoVo vo = new SimpleSupplierInfoVo();
                BeanUtils.copyProperties(supplierInfo,vo);
                supplierInfoVoMap.put(134494,vo);
            }
            if(zjSet.contains(serviceId)){
                // 134508	2310
                supplierIds.add(134508);
                SupplierInfo supplierInfo = FeignResponseUtil.getFeignData(feignSupplierClient.findInfoById(134508));
                SimpleSupplierInfoVo vo = new SimpleSupplierInfoVo();
                BeanUtils.copyProperties(supplierInfo,vo);
                supplierInfoVoMap.put(134508,vo);
            }

            wrapper.in(IceBox::getSupplierId, supplierIds)
//                    .eq(IceBox::getPutStatus, PutStatus.NO_PUT.getStatus());
                    .eq(IceBox::getStatus, CommonStatus.VALID.getStatus());
            if (StringUtils.isNotEmpty(requestVo.getSearchContent())) {
                List<IceModel> iceModels = iceModelDao.selectList(Wrappers.<IceModel>lambdaQuery().like(IceModel::getChestModel, requestVo.getSearchContent()));
                if (CollectionUtil.isNotEmpty(iceModels)) {
                    Set<Integer> iceModelIds = iceModels.stream().map(x -> x.getId()).collect(Collectors.toSet());
                    wrapper.and(x -> x.like(IceBox::getChestName, requestVo.getSearchContent()).or().in(IceBox::getModelId, iceModelIds));
                } else {
                    wrapper.like(IceBox::getChestName, requestVo.getSearchContent());
                }

            }
            List<IceBox> iceBoxes = iceBoxDao.selectList(wrapper);
            if (CollectionUtil.isEmpty(iceBoxes)) {
                return iceBoxVos;
            }
            Map<Integer, List<IceBox>> iceGroupMap = iceBoxes.stream().collect(Collectors.groupingBy(IceBox::getSupplierId));
            for (Integer supplierId : iceGroupMap.keySet()) {
                List<IceBoxVo> iceBoxVoList = new ArrayList<>();
                List<IceBox> iceBoxList = iceGroupMap.get(supplierId);
                Map<Integer, Integer> iceBoxCountMap = new HashMap<>();
                for (IceBox iceBox : iceBoxList) {
                    Integer count = iceBoxCountMap.get(iceBox.getModelId());
                    if (count != null) {
                        count = count + 1;
                        iceBoxCountMap.put(iceBox.getModelId(), count);
                        continue;
                    }
                    IceBoxVo boxVo = buildIceBoxVo(dateFormat, iceBox);
                    SimpleSupplierInfoVo simpleSupplierInfoVo = supplierInfoVoMap.get(iceBox.getSupplierId());
                    if (simpleSupplierInfoVo != null) {
                        boxVo.setSupplierName(simpleSupplierInfoVo.getName());
                        boxVo.setSupplierAddress(simpleSupplierInfoVo.getAddress());
                        boxVo.setLinkman(simpleSupplierInfoVo.getLinkMan());
                        boxVo.setLinkmanMobile(simpleSupplierInfoVo.getLinkManMobile());
                    }
                    iceBoxCountMap.put(iceBox.getModelId(), 1);
                    //??????????????????
                    boxVo.setVisitTypeName(VisitCycleEnum.getDescByCode(FeignResponseUtil.getFeignData(feignIceboxQueryClient.selectVisitTypeForReport(requestVo.getStoreNumber()))));
                    iceBoxVoList.add(boxVo);
                }
                if (CollectionUtil.isNotEmpty(iceBoxVoList)) {
                    for (IceBoxVo iceBoxVo : iceBoxVoList) {
                        Integer count = iceBoxCountMap.get(iceBoxVo.getModelId());
                        LambdaQueryWrapper<PutStoreRelateModel> wrappers = Wrappers.lambdaQuery();
                        wrappers.eq(PutStoreRelateModel::getSupplierId, supplierId);
                        wrappers.eq(PutStoreRelateModel::getModelId, iceBoxVo.getModelId());
//                        wrappers.and(x -> x.eq(PutStoreRelateModel::getPutStatus, PutStatus.LOCK_PUT.getStatus()).or().eq(PutStoreRelateModel::getPutStatus, PutStatus.DO_PUT.getStatus()));
                        wrappers.ne(PutStoreRelateModel::getPutStatus, PutStatus.NO_PUT.getStatus());
                        wrappers.eq(PutStoreRelateModel::getStatus, CommonStatus.VALID.getStatus());
                        List<PutStoreRelateModel> putStoreRelateModels = putStoreRelateModelDao.selectList(wrappers);
                        if (CollectionUtil.isNotEmpty(putStoreRelateModels)) {
                            count = count - putStoreRelateModels.size();
                        }
                        iceBoxVo.setIceBoxCount(count);
                    }
                    iceBoxVos.addAll(iceBoxVoList);
                }
            }
            if (CollectionUtil.isNotEmpty(iceBoxVos)) {
                iceBoxVos = iceBoxVos.stream().filter(x -> x.getIceBoxCount() != null && x.getIceBoxCount() > 0).collect(Collectors.toList());
            }
        }
        return iceBoxVos;
    }

    @Override
    public List<PutStoreRelateModel> getIceBoxListNew(String pxtNumber) {
        LambdaQueryWrapper<PutStoreRelateModel> wrappers = Wrappers.lambdaQuery();
        wrappers.eq(PutStoreRelateModel::getPutStoreNumber, pxtNumber);
        wrappers.ne(PutStoreRelateModel::getPutStatus, PutStatus.NO_PUT.getStatus());
        wrappers.eq(PutStoreRelateModel::getStatus, CommonStatus.VALID.getStatus());
        List<PutStoreRelateModel> putStoreRelateModels = putStoreRelateModelDao.selectList(wrappers);
        return putStoreRelateModels;
    }

    @Override
    public IceBoxStatusVo checkIceBoxByQrcodeNew(String qrcode, String pxtNumber) {
        IceBoxStatusVo iceBoxStatusVo = new IceBoxStatusVo();
        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectOne(Wrappers.<IceBoxExtend>lambdaQuery().eq(IceBoxExtend::getQrCode, qrcode));
        log.info("??????????????????--??????{}???,pxtNumber--??????{}???", qrcode, pxtNumber);
        return getIceBoxStatusVo(pxtNumber, iceBoxStatusVo, iceBoxExtend);
    }

    private IceBoxStatusVo getIceBoxStatusVo(String pxtNumber, IceBoxStatusVo iceBoxStatusVo, IceBoxExtend iceBoxExtend) {
        if (Objects.isNull(iceBoxExtend)) {
            // ???????????????(??????????????????)
            iceBoxStatusVo.setSignFlag(false);
            iceBoxStatusVo.setStatus(5);
            iceBoxStatusVo.setMessage("???????????????(??????????????????)");
            return iceBoxStatusVo;
        }

        IceBox iceBox = iceBoxDao.selectById(iceBoxExtend.getId());
        if (iceBox.getPutStatus().equals(PutStatus.FINISH_PUT.getStatus())) {
            if (iceBox.getPutStoreNumber().equals(pxtNumber)) {
                // ????????????????????????
                iceBoxStatusVo.setIceBoxId(iceBox.getId());
                iceBoxStatusVo.setSignFlag(false);
                iceBoxStatusVo.setStatus(6);
                iceBoxStatusVo.setMessage("??????????????????????????????");

                //???????????????????????????
                if (IceBoxEnums.TypeEnum.OLD_ICE_BOX.getType().equals(iceBox.getIceBoxType())) {
                    OldIceBoxSignNotice oldIceBoxSignNotice = oldIceBoxSignNoticeDao.selectOne(Wrappers.<OldIceBoxSignNotice>lambdaQuery().eq(OldIceBoxSignNotice::getIceBoxId, iceBox.getId())
                            .eq(OldIceBoxSignNotice::getPutStoreNumber, iceBox.getPutStoreNumber())
                            .eq(OldIceBoxSignNotice::getApplyNumber, iceBoxExtend.getLastApplyNumber()));
                    if (oldIceBoxSignNotice != null) {
                        oldIceBoxSignNotice.setStatus(OldIceBoxSignNoticeStatusEnums.IS_SIGNED.getStatus());
                        oldIceBoxSignNotice.setUpdateTime(new Date());
                        oldIceBoxSignNoticeDao.updateById(oldIceBoxSignNotice);
                        log.info("?????????????????????---??????{}??????????????????---??????{}????????????---??????{}???", JSON.toJSONString(iceBox), JSON.toJSONString(iceBoxExtend), JSON.toJSONString(oldIceBoxSignNotice));
                    }
                    if (!IceBoxConstant.virtual_asset_id.equals(iceBox.getAssetId())) {
                        iceBox.setOldAssetId(iceBox.getAssetId());
                        iceBox.setAssetId(IceBoxConstant.virtual_asset_id);
                        iceBox.setUpdatedTime(new Date());
                        iceBoxDao.updateById(iceBox);

                        iceBoxExtend.setAssetId(IceBoxConstant.virtual_asset_id);
                        iceBoxExtendDao.updateById(iceBoxExtend);
                    }

                    IcePutApply icePutApply = icePutApplyDao.selectOne(Wrappers.<IcePutApply>lambdaQuery().eq(IcePutApply::getApplyNumber, iceBoxExtend.getLastApplyNumber())
                            .eq(IcePutApply::getStoreSignStatus, StoreSignStatus.DEFAULT_SIGN.getStatus()).last("limit 1"));
                    if (icePutApply != null) {
                        icePutApply.setStoreSignStatus(StoreSignStatus.ALREADY_SIGN.getStatus());
                        icePutApply.setUpdateTime(new Date());
                        icePutApplyDao.updateById(icePutApply);
                    }

                    LambdaQueryWrapper<PutStoreRelateModel> wrapper = Wrappers.<PutStoreRelateModel>lambdaQuery();
                    wrapper.eq(PutStoreRelateModel::getPutStoreNumber, pxtNumber);
                    wrapper.eq(PutStoreRelateModel::getModelId, iceBox.getModelId());
                    wrapper.eq(PutStoreRelateModel::getSupplierId, iceBox.getSupplierId());
                    wrapper.eq(PutStoreRelateModel::getPutStatus, PutStatus.DO_PUT.getStatus());
                    wrapper.eq(PutStoreRelateModel::getStatus, CommonStatus.VALID.getStatus()).last("limit 1");
                    PutStoreRelateModel relateModel = putStoreRelateModelDao.selectOne(wrapper);
                    if (relateModel != null) {
                        relateModel.setPutStatus(PutStatus.FINISH_PUT.getStatus());
                        relateModel.setUpdateTime(new Date());
                        putStoreRelateModelDao.updateById(relateModel);
                    }
                }

                if (IceBoxEnums.TypeEnum.OLD_ICE_BOX.getType().equals(iceBox.getIceBoxType())) {
                    IceBoxPutReport report = iceBoxPutReportDao.selectOne(Wrappers.<IceBoxPutReport>lambdaQuery().eq(IceBoxPutReport::getApplyNumber, iceBoxExtend.getLastApplyNumber())
                            .eq(IceBoxPutReport::getIceBoxModelId, iceBox.getModelId())
                            .eq(IceBoxPutReport::getSupplierId, iceBox.getSupplierId())
                            .eq(IceBoxPutReport::getPutStatus, PutStatus.DO_PUT.getStatus()).last("limit 1"));
                    if (report != null) {
                        report.setIceBoxId(iceBox.getId());
                        report.setIceBoxAssetId(iceBox.getAssetId());
                        iceBoxPutReportDao.updateById(report);
                    }
                }
                IceBoxPutReportMsg reportMsg = new IceBoxPutReportMsg();
                reportMsg.setIceBoxId(iceBox.getId());
                reportMsg.setIceBoxAssetId(iceBox.getAssetId());
                reportMsg.setApplyNumber(iceBoxExtend.getLastApplyNumber());
                reportMsg.setPutStatus(PutStatus.FINISH_PUT.getStatus());
                IceBoxPutReport putReport = iceBoxPutReportDao.selectOne(Wrappers.<IceBoxPutReport>lambdaQuery().eq(IceBoxPutReport::getIceBoxId, reportMsg.getIceBoxId())
                        .eq(IceBoxPutReport::getApplyNumber, reportMsg.getApplyNumber())
                        .eq(IceBoxPutReport::getPutStatus, PutStatus.DO_PUT.getStatus()).last("limit 1"));
                if (putReport != null) {
                    putReport.setPutStatus(reportMsg.getPutStatus());
                    iceBoxPutReportDao.updateById(putReport);
                }
                //??????mq??????,???????????????????????????
//                CompletableFuture.runAsync(() -> {
//                    if (IceBoxEnums.TypeEnum.OLD_ICE_BOX.getType().equals(iceBox.getIceBoxType())) {
//                        IceBoxPutReport report = iceBoxPutReportDao.selectOne(Wrappers.<IceBoxPutReport>lambdaQuery().eq(IceBoxPutReport::getApplyNumber, iceBoxExtend.getLastApplyNumber())
//                                .eq(IceBoxPutReport::getIceBoxModelId, iceBox.getModelId())
//                                .eq(IceBoxPutReport::getSupplierId, iceBox.getSupplierId())
//                                .eq(IceBoxPutReport::getPutStatus, PutStatus.DO_PUT.getStatus()).last("limit 1"));
//                        if (report != null) {
//                            report.setIceBoxAssetId(iceBox.getAssetId());
//                            iceBoxPutReportDao.updateById(report);
//                        }
//                    }
//                    IceBoxPutReportMsg report = new IceBoxPutReportMsg();
//                    report.setIceBoxId(iceBox.getId());
//                    report.setIceBoxAssetId(iceBox.getAssetId());
//                    report.setApplyNumber(iceBoxExtend.getLastApplyNumber());
//                    report.setPutStatus(PutStatus.FINISH_PUT.getStatus());
//                    report.setOperateType(OperateTypeEnum.UPDATE.getType());
//                    log.info("???????????????????????????-----??????{}???", JSON.toJSONString(report));
//                    rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.iceboxReportKey, report);
//                }, ExecutorServiceFactory.getInstance());
                return iceBoxStatusVo;
            }
            // ????????????, ????????????
            iceBoxStatusVo.setSignFlag(false);
            iceBoxStatusVo.setStatus(2);
            iceBoxStatusVo.setMessage("???????????????????????????");
            return iceBoxStatusVo;
        }
        LambdaQueryWrapper<PutStoreRelateModel> wrappers = Wrappers.lambdaQuery();
        wrappers.eq(PutStoreRelateModel::getSupplierId, iceBox.getSupplierId());
        wrappers.eq(PutStoreRelateModel::getModelId, iceBox.getModelId());
        wrappers.eq(PutStoreRelateModel::getPutStoreNumber, pxtNumber);
        wrappers.and(x -> x.eq(PutStoreRelateModel::getPutStatus, PutStatus.LOCK_PUT.getStatus()).or().eq(PutStoreRelateModel::getPutStatus, PutStatus.DO_PUT.getStatus()));
        wrappers.eq(PutStoreRelateModel::getStatus, CommonStatus.VALID.getStatus());
        List<PutStoreRelateModel> putStoreRelateModels = putStoreRelateModelDao.selectList(wrappers);
        if (CollectionUtil.isEmpty(putStoreRelateModels)) {
            // ???????????????
            iceBoxStatusVo.setIceBoxId(iceBox.getId());
            iceBoxStatusVo.setSignFlag(false);
            iceBoxStatusVo.setStatus(3);
            iceBoxStatusVo.setMessage("??????????????????????????????");
            return iceBoxStatusVo;
        }

        List<Integer> putStatus = putStoreRelateModels.stream().map(x -> x.getPutStatus()).collect(Collectors.toList());
        if (!putStatus.contains(PutStatus.DO_PUT.getStatus())) {
            // ??????????????????, ??????????????????
            iceBoxStatusVo.setIceBoxId(iceBox.getId());
            iceBoxStatusVo.setSignFlag(false);
            iceBoxStatusVo.setStatus(4);
            iceBoxStatusVo.setMessage("?????????????????????");
            return iceBoxStatusVo;
        }

        //?????????????????????????????????????????????
        List<PutStoreRelateModel> doPutList = putStoreRelateModels.stream().filter(x -> PutStatus.DO_PUT.getStatus().equals(x.getPutStatus())).collect(Collectors.toList());
        List<Integer> putStoreRelateModelIds = doPutList.stream().map(x -> x.getId()).collect(Collectors.toList());
        List<ApplyRelatePutStoreModel> applyRelatePutStoreModels = applyRelatePutStoreModelDao.selectList(Wrappers.<ApplyRelatePutStoreModel>lambdaQuery().in(ApplyRelatePutStoreModel::getStoreRelateModelId, putStoreRelateModelIds));
        if (CollectionUtil.isEmpty(applyRelatePutStoreModels)) {
            // ???????????????
            iceBoxStatusVo.setIceBoxId(iceBox.getId());
            iceBoxStatusVo.setSignFlag(false);
            iceBoxStatusVo.setStatus(3);
            iceBoxStatusVo.setMessage("??????????????????????????????");
            return iceBoxStatusVo;
        }

        List<String> applyNumbers = applyRelatePutStoreModels.stream().map(x -> x.getApplyNumber()).collect(Collectors.toList());
        List<IcePutApply> icePutApplies = icePutApplyDao.selectList(Wrappers.<IcePutApply>lambdaQuery().in(IcePutApply::getApplyNumber, applyNumbers));
        if (CollectionUtil.isEmpty(icePutApplies)) {
            // ???????????????
            iceBoxStatusVo.setIceBoxId(iceBox.getId());
            iceBoxStatusVo.setSignFlag(false);
            iceBoxStatusVo.setStatus(3);
            iceBoxStatusVo.setMessage("??????????????????????????????");
            return iceBoxStatusVo;
        }

        // ?????????????????????, ?????????????????????
        IcePutApply icePutApply = icePutApplies.get(0);
        iceBoxStatusVo = checkPutApplyByApplyNumber(icePutApply.getApplyNumber(), pxtNumber);
        return iceBoxStatusVo;
    }

    @Override
    public Map<String, List<IceBoxVo>> findPutingIceBoxListNew(IceBoxRequestVo requestVo) {
        List<IceBoxVo> iceBoxVos = new ArrayList<>();
        LambdaQueryWrapper<PutStoreRelateModel> wrapper = Wrappers.<PutStoreRelateModel>lambdaQuery();
        wrapper.eq(PutStoreRelateModel::getPutStoreNumber, requestVo.getStoreNumber());
        wrapper.ne(PutStoreRelateModel::getPutStatus, PutStatus.FINISH_PUT.getStatus());
        wrapper.ne(PutStoreRelateModel::getPutStatus, PutStatus.NO_PUT.getStatus());
        wrapper.ne(PutStoreRelateModel::getExamineStatus, ExamineStatusEnum.UN_PASS.getStatus());
        wrapper.eq(PutStoreRelateModel::getStatus, CommonStatus.VALID.getStatus());
        List<PutStoreRelateModel> relateModelList = putStoreRelateModelDao.selectList(wrapper);
        if (CollectionUtil.isNotEmpty(relateModelList)) {
            List<IceBoxVo> putIceBoxVos = this.getIceBoxVosByPutApplysNew(relateModelList);
            if (CollectionUtil.isNotEmpty(putIceBoxVos)) {
                iceBoxVos.addAll(putIceBoxVos);
            }
        }
        LambdaQueryWrapper<IceBackApply> backWrapper = Wrappers.<IceBackApply>lambdaQuery();
        backWrapper.eq(IceBackApply::getBackStoreNumber, requestVo.getStoreNumber());
        backWrapper.eq(IceBackApply::getExamineStatus, ExamineStatusEnum.IS_DEFAULT.getStatus());
//        backWrapper.and(x -> x.eq(IceBackApply::getExamineStatus, ExamineStatusEnum.NO_DEFAULT.getStatus()).or().eq(IceBackApply::getExamineStatus, ExamineStatusEnum.IS_DEFAULT.getStatus()));

        List<IceBackApply> iceBackApplies = iceBackApplyDao.selectList(backWrapper);
        if (CollectionUtil.isNotEmpty(iceBackApplies)) {
            List<IceBoxVo> backIceBoxVos = this.getIceBoxVosByBackApplys(iceBackApplies);
            if (CollectionUtil.isNotEmpty(backIceBoxVos)) {
                iceBoxVos.addAll(backIceBoxVos);
            }
        }
        Map<String, List<IceBoxVo>> map = Streams.toStream(iceBoxVos).collect(Collectors.groupingBy(IceBoxVo::getApplyNumber));
        return map;
    }

    @Override
    public IceBoxVo getIceBoxByQrcodeNew(String qrcode, String pxtNumber) {
        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectOne(Wrappers.<IceBoxExtend>lambdaQuery().eq(IceBoxExtend::getQrCode, qrcode));
        IceBox iceBox = iceBoxDao.selectById(Objects.requireNonNull(iceBoxExtend).getId());
        //return iceBoxService.getIceBoxVo(pxtNumber, iceBoxExtend, iceBox);
        return getIceBoxVoNew(pxtNumber, iceBoxExtend, iceBox);
    }

    /**
     * ?????????????????????????????????????????????????????????
     * @return
     */
    public IceBoxVo getIceBoxVoNew(String pxtNumber, IceBoxExtend iceBoxExtend, IceBox iceBox){
        IceModel iceModel = iceModelDao.selectById(Objects.requireNonNull(iceBox).getModelId());
        LambdaQueryWrapper<PutStoreRelateModel> wrapper = Wrappers.<PutStoreRelateModel>lambdaQuery();
        wrapper.eq(PutStoreRelateModel::getPutStoreNumber, pxtNumber);
        wrapper.eq(PutStoreRelateModel::getModelId, iceBox.getModelId());
        wrapper.eq(PutStoreRelateModel::getSupplierId, iceBox.getSupplierId());
        wrapper.eq(PutStoreRelateModel::getPutStatus, PutStatus.DO_PUT.getStatus());
        wrapper.eq(PutStoreRelateModel::getStatus, CommonStatus.VALID.getStatus());
        List<PutStoreRelateModel> relateModelList = putStoreRelateModelDao.selectList(wrapper);
        if (CollectionUtil.isEmpty(relateModelList)) {
            return IceBoxConverter.convertToVo(Objects.requireNonNull(iceBox),
                    Objects.requireNonNull(iceBoxExtend),
                    Objects.requireNonNull(iceModel),
                    FreePayTypeEnum.UN_FREE);
        }
        PutStoreRelateModel relateModel = relateModelList.get(0);
        ApplyRelatePutStoreModel applyRelatePutStoreModel = applyRelatePutStoreModelDao.selectOne(Wrappers.<ApplyRelatePutStoreModel>lambdaQuery().eq(ApplyRelatePutStoreModel::getStoreRelateModelId, relateModel.getId()));
        return  IceBoxConverter.convertToVo(Objects.requireNonNull(iceBox),
                Objects.requireNonNull(iceBoxExtend),
                Objects.requireNonNull(iceModel),
                Objects.isNull(applyRelatePutStoreModel) ? FreePayTypeEnum.UN_FREE : FreePayTypeEnum.convertVo(applyRelatePutStoreModel.getFreeType()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class, value = "transactionManager")
    public IceBoxVo getIceBoxVo(String pxtNumber, IceBoxExtend iceBoxExtend, IceBox iceBox) {
        IceModel iceModel = iceModelDao.selectById(Objects.requireNonNull(iceBox).getModelId());
        LambdaQueryWrapper<PutStoreRelateModel> wrapper = Wrappers.<PutStoreRelateModel>lambdaQuery();
        wrapper.eq(PutStoreRelateModel::getPutStoreNumber, pxtNumber);
        wrapper.eq(PutStoreRelateModel::getModelId, iceBox.getModelId());
        wrapper.eq(PutStoreRelateModel::getSupplierId, iceBox.getSupplierId());
        wrapper.eq(PutStoreRelateModel::getPutStatus, PutStatus.DO_PUT.getStatus());
        wrapper.eq(PutStoreRelateModel::getStatus, CommonStatus.VALID.getStatus());
        List<PutStoreRelateModel> relateModelList = putStoreRelateModelDao.selectList(wrapper);
        if (CollectionUtil.isEmpty(relateModelList)) {
            return IceBoxConverter.convertToVo(Objects.requireNonNull(iceBox),
                    Objects.requireNonNull(iceBoxExtend),
                    Objects.requireNonNull(iceModel),
                    FreePayTypeEnum.UN_FREE);
        }
        PutStoreRelateModel relateModel = relateModelList.get(0);
        ApplyRelatePutStoreModel applyRelatePutStoreModel = applyRelatePutStoreModelDao.selectOne(Wrappers.<ApplyRelatePutStoreModel>lambdaQuery().eq(ApplyRelatePutStoreModel::getStoreRelateModelId, relateModel.getId()));
        IcePutApply icePutApply = icePutApplyDao.selectOne(Wrappers.<IcePutApply>lambdaQuery().eq(IcePutApply::getApplyNumber, applyRelatePutStoreModel.getApplyNumber()));

        //????????????????????????????????????????????????
        IcePutApplyRelateBox isExist = icePutApplyRelateBoxDao.selectOne(Wrappers.<IcePutApplyRelateBox>lambdaQuery().eq(IcePutApplyRelateBox::getApplyNumber, icePutApply.getApplyNumber()).eq(IcePutApplyRelateBox::getBoxId, iceBox.getId()));
        if (isExist == null) {
            IcePutApplyRelateBox applyRelateBox = new IcePutApplyRelateBox();
            applyRelateBox.setApplyNumber(icePutApply.getApplyNumber());
            applyRelateBox.setBoxId(iceBox.getId());
            applyRelateBox.setModelId(iceBox.getModelId());
            applyRelateBox.setFreeType(applyRelatePutStoreModel.getFreeType());
            icePutApplyRelateBoxDao.insert(applyRelateBox);

            //???????????????????????????
            iceBox.setPutStoreNumber(pxtNumber);
            iceBox.setPutStatus(PutStatus.DO_PUT.getStatus());
            iceBox.setUpdatedTime(new Date());
            iceBoxDao.updateById(iceBox);

            iceBoxExtend.setLastApplyNumber(icePutApply.getApplyNumber());
            iceBoxExtend.setLastPutTime(icePutApply.getCreatedTime());
            iceBoxExtend.setLastPutId(icePutApply.getId());
//            iceBoxExtend.setLastExamineId(icePutApply.getExamineId());
//            iceBoxExtend.setLastExamineTime(icePutApply.getUpdateTime());
            iceBoxExtendDao.updateById(iceBoxExtend);
        }

        IceBoxPutReportMsg reportMsg = new IceBoxPutReportMsg();
        reportMsg.setIceBoxId(iceBox.getId());
        reportMsg.setIceBoxAssetId(iceBox.getAssetId());
        reportMsg.setIceBoxModelId(iceBox.getModelId());
        reportMsg.setSupplierId(iceBox.getSupplierId());
        reportMsg.setApplyNumber(icePutApply.getApplyNumber());
        reportMsg.setPutStatus(PutStatus.DO_PUT.getStatus());

        IceBoxPutReport report = iceBoxPutReportDao.selectOne(Wrappers.<IceBoxPutReport>lambdaQuery().eq(IceBoxPutReport::getApplyNumber, reportMsg.getApplyNumber())
                .eq(IceBoxPutReport::getIceBoxModelId, reportMsg.getIceBoxModelId())
                .eq(IceBoxPutReport::getSupplierId, reportMsg.getSupplierId())
                .eq(IceBoxPutReport::getPutStatus, PutStatus.DO_PUT.getStatus()).last("limit 1"));
        if (report != null) {
            report.setIceBoxId(reportMsg.getIceBoxId());
            report.setIceBoxAssetId(reportMsg.getIceBoxAssetId());
            iceBoxPutReportDao.updateById(report);
        }
        //??????mq??????,???????????????????????????
//        CompletableFuture.runAsync(() -> {
//            IceBoxPutReportMsg report = new IceBoxPutReportMsg();
//            report.setIceBoxId(iceBox.getId());
//            report.setIceBoxAssetId(iceBox.getAssetId());
//            report.setIceBoxModelId(iceBox.getModelId());
//            report.setSupplierId(iceBox.getSupplierId());
//            report.setApplyNumber(icePutApply.getApplyNumber());
//            report.setPutStatus(PutStatus.DO_PUT.getStatus());
//            report.setOperateType(OperateTypeEnum.UPDATE.getType());
//            rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.iceboxReportKey, report);
//        }, ExecutorServiceFactory.getInstance());

        return IceBoxConverter.convertToVo(Objects.requireNonNull(iceBox),
                Objects.requireNonNull(iceBoxExtend),
                Objects.requireNonNull(iceModel),
                Objects.isNull(applyRelatePutStoreModel) ? FreePayTypeEnum.UN_FREE : FreePayTypeEnum.convertVo(applyRelatePutStoreModel.getFreeType()));
    }

    @Override
    public void exportExcel(IceBoxPage iceBoxPage) throws Exception {

        if (dealIceBoxPage(iceBoxPage)) {
            return;
        }
        Map<String, Object> param = new HashMap<>();
        param.put("deptIds", iceBoxPage.getDeptIds());
        param.put("supplierIdList", iceBoxPage.getSupplierIdList());
        param.put("putStoreNumberList", iceBoxPage.getPutStoreNumberList());
        param.put("assetId", iceBoxPage.getAssetId());
        param.put("status", iceBoxPage.getStatus());
        param.put("putStatus", iceBoxPage.getPutStatus());
        param.put("belongObj", iceBoxPage.getBelongObj());

        Integer count = iceBoxDao.exportExcelCount(param);
        if (count == null || count == 0) {
            return;
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // ??????????????????,????????????????????????
        List<IceModel> iceModels = iceModelDao.selectList(null);
        Map<Integer, IceModel> modelMap = iceModels.stream().collect(Collectors.toMap(IceModel::getId, i -> i));

        // ??????1 ?????????????????????sheet
        String xlsxPath = CreatePathUtil.creatDocPath();
        // ?????? ????????????????????????class??????
        ExcelWriter excelWriter = EasyExcel.write(xlsxPath, IceBoxExcelVo.class).build();
        // ???????????? ???????????????sheet??????????????????
        WriteSheet writeSheet = EasyExcel.writerSheet("??????????????????").build();
        Integer dangQianTiao = 0;
        /**
         *  ??????????????????
         */
        int pageNum = 96; // ????????????
        int totalPage = (count - 1) / pageNum + 1; // ?????????
        for (int j = 0; j < totalPage; j++) {
            Integer pageCode = j * pageNum;
            param.put("pageCode", pageCode);
            param.put("pageNum", pageNum);
            List<IceBox> iceBoxes = iceBoxDao.exportExcel(param);
            if (CollectionUtils.isEmpty(iceBoxes)) {
                continue;
            }
            List<Integer> deptIds = iceBoxes.stream().map(IceBox::getDeptId).collect(Collectors.toSet()).stream().collect(Collectors.toList());
            // ???????????????????????????  ?????????->??????->?????????
            Map<Integer, String> deptMap = null;
            if (CollectionUtils.isNotEmpty(deptIds)) {
                deptMap = FeignResponseUtil.getFeignData(feignCacheClient.getForMarketAreaName(deptIds));
            }
            // ????????? ??????
            List<Integer> suppIds = iceBoxes.stream().map(IceBox::getSupplierId).collect(Collectors.toList());
            Map<Integer, Map<String, String>> suppMaps = null;
            if (CollectionUtils.isNotEmpty(suppIds)) {
                suppMaps = FeignResponseUtil.getFeignData(feignSupplierClient.getSimpledataByIds(suppIds));
            }
            // ??????/?????????/????????? ??????      ?????????????????????,???????????? ??????/?????????/????????????
            Map<String, Map<String, String>> storeMaps = null;
            List<String> storeNumbers = Lists.newArrayList();
            for (IceBox iceBoxe : iceBoxes) {
                if (StringUtils.isBlank(iceBoxe.getPutStoreNumber())) {
                    continue;
                }
                storeNumbers.add(iceBoxe.getPutStoreNumber());
            }
//            List<String> storeNumbers = iceBoxes.stream().map(IceBox::getPutStoreNumber).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(storeNumbers)) {
                log.info("?????????storeNumbers-->{}", storeNumbers);
                storeMaps = FeignResponseUtil.getFeignData(feignStoreClient.getSimpledataByNumber(storeNumbers));
            }
            storeMaps = getSuppMap(storeMaps, storeNumbers);

            List<Integer> idsList = iceBoxes.stream().map(IceBox::getId).collect(Collectors.toList());
            List<IceBoxExtend> boxExtendList = iceBoxExtendDao.selectBatchIds(idsList);
            Map<Integer, IceBoxExtend> boxExtendMap = boxExtendList.stream().collect(Collectors.toMap(IceBoxExtend::getId, i -> i));
            // ??????????????????excel???
            List<IceBoxExcelVo> iceBoxExcelVoList = new ArrayList<>(iceBoxes.size());
            // ????????????
            for (IceBox iceBox : iceBoxes) {
                dangQianTiao = dangQianTiao + 1;
                log.info("??????????????????:?????????:{},????????????:{},iceboxId:{},exportRecordId:{}", count, dangQianTiao, iceBox.getId(),iceBoxPage.getExportRecordId());
                Integer iceBoxId = iceBox.getId();
//                IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(iceBoxId);
                IceBoxExtend iceBoxExtend = boxExtendMap.get(iceBoxId);
                IceBoxExcelVo iceBoxExcelVo = new IceBoxExcelVo();
                if(iceBox.getResponseMan() != null){
                    iceBoxExcelVo.setResponseMan(iceBox.getResponseMan());
                }
                if(iceBox.getCreatedTime() != null){
                    iceBoxExcelVo.setCreateTime(iceBox.getCreatedTime());
                }
                if (deptMap != null) {
                    String deptStr = deptMap.get(iceBox.getDeptId());
                    if (StringUtils.isNotBlank(deptStr)) {
                        String[] split = deptStr.split("/");
                        if (split.length >= 1) {
                            iceBoxExcelVo.setSybStr(split[0]); // ?????????
                        }
                        if (split.length >= 2) {
                            iceBoxExcelVo.setDqStr(split[1]); // ??????
                        }
                        if (split.length >= 3) {
                            iceBoxExcelVo.setFwcStr(split[2]); // ?????????
                        }
                    }
//                    iceBoxExcelVo.setDeptStr(deptStr); // ????????????
                }
                // ????????????????????? ????????? ??????
                if (suppMaps != null && suppMaps.get(iceBox.getSupplierId()) != null) {
                    Map<String, String> suppMap = suppMaps.get(iceBox.getSupplierId());
                    iceBoxExcelVo.setSuppNumber(suppMap.get("suppNumber")); // ?????????????????????
                    iceBoxExcelVo.setSuppName(suppMap.get("suppName")); // ?????????????????????
                    iceBoxExcelVo.setSuppName(suppMap.get("suppName")); // ?????????????????????
                    iceBoxExcelVo.setRealName(suppMap.get("realname")); // ?????????????????????
                }

                // ????????????????????? ?????? ??????
                if (storeMaps != null && storeMaps.get(iceBox.getPutStoreNumber()) != null) {
                    Map<String, String> storeMap = storeMaps.get(iceBox.getPutStoreNumber());
                    iceBoxExcelVo.setStoreTypeName(storeMap.get("storeTypeName")); // ?????????????????????
                    iceBoxExcelVo.setStoreLevel(storeMap.get("storeLevel")); // ?????????????????????
                    iceBoxExcelVo.setStoreNumber(storeMap.get("storeNumber")); // ?????????????????????
                    iceBoxExcelVo.setStoreName(storeMap.get("storeName")); // ?????????????????????
                    iceBoxExcelVo.setMobile(storeMap.get("mobile")); // ????????????????????????
                    iceBoxExcelVo.setAddress(storeMap.get("address")); // ?????????????????????
                    iceBoxExcelVo.setStatusStr(storeMap.get("statusStr")); // ????????????
                    iceBoxExcelVo.setRealName(storeMap.get("realName")); // ?????????????????????


                    if(iceBox.getPutStoreNumber().contains("C0")){
                        iceBoxExcelVo.setSybStr(storeMap.get("businessDeptName")); // ?????????
                        iceBoxExcelVo.setDqStr(storeMap.get("regionDeptName")); // ??????
                        iceBoxExcelVo.setFwcStr(storeMap.get("serviceDeptName")); // ?????????
                        iceBoxExcelVo.setGroupStr(storeMap.get("groupDeptName")); // ???
                        //??????????????????
                        List<CusLabelDetailVo> labelDetailVos = new ArrayList<>();
                        labelDetailVos = FeignResponseUtil.getFeignData(feignCusLabelClient.queryLabelsByCustomerNumber(iceBox.getPutStoreNumber()));
                        String label = "";
                        if(labelDetailVos != null){
                            //?????????????????????
                            List<CusLabelDetailVo> autoDetails = labelDetailVos.stream().filter(x->x.getLabelFlag()==1).collect(Collectors.toList());
                            if(autoDetails != null){
                                for(CusLabelDetailVo detailVo : autoDetails){
                                    if(StringUtils.isNotBlank(label)){
                                        label += "," + detailVo.getLabelName();
                                    }else {
                                        label = detailVo.getLabelName();
                                    }
                                }
                            }
                        }
                        iceBoxExcelVo.setLabel(label);
                    }
                    StoreInfoDtoVo storeInfoDtoVo = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(iceBox.getPutStoreNumber()));
                    if(storeInfoDtoVo != null  && storeInfoDtoVo.getMerchantNumber() != null){
                        iceBoxExcelVo.setMerchantNumber(storeInfoDtoVo.getMerchantNumber());
                    }
                }

                iceBoxExcelVo.setAssetId(iceBox.getAssetId());
                Integer iceBoxType = iceBox.getIceBoxType(); // ????????????????????????
                if (IceBoxEnums.TypeEnum.OLD_ICE_BOX.getType().equals(iceBoxType)) {
                    iceBoxExcelVo.setXiuGaiAssetId(iceBox.getAssetId());  // ????????????(??????)
                    iceBoxExcelVo.setAssetId(iceBox.getOldAssetId()); // ????????????
                }
                IceModel iceModel = modelMap.get(iceBox.getModelId());
                iceBoxExcelVo.setChestModel(iceModel == null ? null : iceModel.getChestModel()); // ????????????
                iceBoxExcelVo.setDepositMoney(iceBox.getDepositMoney().toString()); // ??????????????????
                iceBoxExcelVo.setIceStatusStr(IceBoxEnums.StatusEnum.getDesc(iceBox.getStatus())); // ????????????
                iceBoxExcelVo.setPutStatusStr(PutStatus.convertEnum(iceBox.getPutStatus()).getDesc()); // ??????????????????
                iceBoxExcelVo.setLastPutTimeStr(iceBoxExtend.getLastPutTime() == null ? null : new DateTime(iceBoxExtend.getLastPutTime()).toString("yyyy-MM-dd HH:mm:ss")); // ????????????
                iceBoxExcelVo.setLastExamineTimeStr(iceBoxExtend.getLastExamineTime() == null ? null : new DateTime(iceBoxExtend.getLastExamineTime()).toString("yyyy-MM-dd HH:mm:ss")); // ????????????????????????
                iceBoxExcelVo.setRemark(iceBox.getRemark()); // ????????????
                if(iceBoxExtend.getReleaseTime() != null){
                    iceBoxExcelVo.setReleaseTimeStr(dateFormat.format(iceBoxExtend.getReleaseTime()));

                    SimpleDateFormat yearDateFormat = new SimpleDateFormat("yyyy");
                    iceBoxExcelVo.setIceboxYear(yearDateFormat.format(iceBoxExtend.getReleaseTime()));
                }

                if(iceBoxExtend.getRepairBeginTime() != null){
                    iceBoxExcelVo.setRepairBeginTimeStr(dateFormat.format(iceBoxExtend.getRepairBeginTime()));
                }
                /**
                 * 5.18?????????????????????
                 */
                /*Map<String, Object> equipMap = readEquipNews(iceBox.getId());
                iceBoxExcelVo.setTotalSum((Integer) equipMap.get("totalSum"));
                iceBoxExcelVo.setMonthSum((Integer) equipMap.get("monthSum"));
                iceBoxExcelVo.setTotalSum((Integer) equipMap.get("todaySum"));
                iceBoxExcelVo.setTemperature((String) equipMap.get("temperature"));
                iceBoxExcelVo.setGpsAddress((String) equipMap.get("address"));
                iceBoxExcelVo.setOccurrenceTime((Date)equipMap.get("occurrenceTime"));
                String iceboxLatStr = (String) equipMap.get("lat");
                String iceboxLngStr = (String) equipMap.get("lat");
                double iceboxLat = 0.0;
                double iceboxLng = 0.0;
                if(StringUtils.isNotBlank(iceboxLatStr) && StringUtils.isNotBlank(iceboxLngStr)){
                    iceboxLat = Double.parseDouble(iceboxLatStr);
                    iceboxLng = Double.parseDouble(iceboxLngStr);
                }
                double cusLat = 0.0;
                double cusLng = 0.0;
                double distance = 0.0;
                if(StringUtils.isNotBlank(iceBox.getPutStoreNumber())){
                    //???????????????
                    StoreInfoDtoVo storeInfoDtoVo = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(iceBox.getPutStoreNumber()));
                    if(storeInfoDtoVo != null){
                        cusLat = Double.parseDouble(storeInfoDtoVo.getLatitude());
                        cusLng = Double.parseDouble(storeInfoDtoVo.getLongitude());
                    }
                }else{
                    if(iceBox.getSupplierId() != null && iceBox.getSupplierId() > 0){
                        SupplierInfo info = FeignResponseUtil.getFeignData(feignSupplierClient.findInfoById(iceBox.getSupplierId()));
                        if(info != null){
                            cusLat = Double.parseDouble(info.getLatitude());
                            cusLng = Double.parseDouble(info.getLongitude());
                        }
                    }
                }
                if(!Double.isNaN(cusLat) && !Double.isNaN(cusLng)){
                    distance = getDistance(iceboxLat,iceboxLng,cusLat,cusLng);
                }
                iceBoxExcelVo.setDistance(distance);*/
                //??????????????????
                List<IceAlarm> iceAlarms = iceAlarmMapper.selectList(Wrappers.<IceAlarm>lambdaQuery().eq(IceAlarm::getIceBoxAssetid, iceBoxExcelVo.getAssetId()).eq(IceAlarm::getStatus, IceAlarmStatusEnum.NEWALARM.getType()));
                final String[] alarmDec = {""};
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                Optional.ofNullable(iceAlarms).ifPresent(alarms->{
                    alarms.stream().forEach(alarm->{
                        if(alarm.getAlarmType() != null){
                            String desc = IceAlarmTypeEnum.getDesc(alarm.getAlarmType());
                            if(StringUtils.isNotBlank(alarmDec[0])){
                                alarmDec[0] += "," + desc+simpleDateFormat.format(alarm.getCreateTime());
                            }else {
                                alarmDec[0] = desc+simpleDateFormat.format(alarm.getCreateTime());
                            }
                        }
                    });
                });
                iceBoxExcelVo.setAlarmDec(alarmDec[0]);
                iceBoxExcelVoList.add(iceBoxExcelVo);
            }
            // ??????excel
            excelWriter.write(iceBoxExcelVoList, writeSheet);
            iceBoxExcelVoList = null;
            deptMap = null;
            suppMaps = null;
            storeMaps = null;
        }
        // ???????????????finish ??????????????????
        excelWriter.finish();

        File xlsxFile = new File(xlsxPath);
        @Cleanup InputStream in = new FileInputStream(xlsxFile);
        try {
            String frontName = new DateTime().toString("yyyy-MM-dd-HH-mm-ss");
            String imgUrl = imageUploadUtil.wechatUpload(in, IceBoxConstant.ICE_BOX, "BGDC" + frontName, "xlsx");
            // ??????????????????????????????
            feignExportRecordsClient.updateExportRecord(imgUrl, 1, iceBoxPage.getExportRecordId());
        } catch (Exception e) {
            log.info("??????????????????excel??????", e);
        } finally {
            // ??????????????????
            if (StringUtils.isNotBlank(xlsxPath)) {
                FileUtils.deleteQuietly(xlsxFile);
            }
        }
    }


    private static double EARTH_RADIUS = 6378.137;

    private static double rad(double d) {
        return d * Math.PI / 180.0;
    }

    /**
     * ???????????????????????????(????????????)
     * @param lat1
     * @param lng1
     * @param lat2
     * @param lng2
     * @return
     */
    public static double getDistance(double lat1, double lng1, double lat2,double lng2) {
        double radLat1 = rad(lat1);
        double radLat2 = rad(lat2);
        double a = radLat1 - radLat2;
        double b = rad(lng1) - rad(lng2);
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2)
                + Math.cos(radLat1) * Math.cos(radLat2)
                * Math.pow(Math.sin(b / 2), 2)));
        s = s * EARTH_RADIUS;
        s = Math.round(s * 10000d) / 10000d;
        s = s*1000;
        return s;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, value = "transactionManager")
    public void cancelApplyByNumber(IceBoxVo iceBoxVo) {
        List<ApplyRelatePutStoreModel> applyRelatePutStoreModels = applyRelatePutStoreModelDao.selectList(Wrappers.<ApplyRelatePutStoreModel>lambdaQuery().eq(ApplyRelatePutStoreModel::getApplyNumber, iceBoxVo.getApplyNumber()));
        if (CollectionUtil.isEmpty(applyRelatePutStoreModels)) {
            throw new ImproperOptionException("??????????????????????????????");
        }
        for (ApplyRelatePutStoreModel applyRelatePutStoreModel : applyRelatePutStoreModels) {
            PutStoreRelateModel relateModel = putStoreRelateModelDao.selectById(applyRelatePutStoreModel.getStoreRelateModelId());
            if (relateModel == null) {
                throw new ImproperOptionException("??????????????????????????????");
            }
            if (PutStatus.FINISH_PUT.getStatus().equals(relateModel.getPutStatus())) {
                throw new ImproperOptionException("???????????????????????????????????????");
            }
            relateModel.setCancelMsg(iceBoxVo.getCancelMsg());
            relateModel.setPutStatus(PutStatus.NO_PUT.getStatus());
            relateModel.setStatus(CommonStatus.INVALID.getStatus());
            relateModel.setUpdateBy(iceBoxVo.getUserId());
            relateModel.setUpdateByName(iceBoxVo.getUserName());
            relateModel.setUpdateTime(new Date());
            putStoreRelateModelDao.updateById(relateModel);

            List<IceBoxPutReport> iceBoxPutReports = iceBoxPutReportDao.selectList(Wrappers.<IceBoxPutReport>lambdaQuery().eq(IceBoxPutReport::getApplyNumber, applyRelatePutStoreModel.getApplyNumber()));
            if (CollectionUtil.isNotEmpty(iceBoxPutReports)) {
                for (IceBoxPutReport putReport : iceBoxPutReports) {
                    putReport.setPutStatus(PutStatus.IS_CANCEL.getStatus());
                    putReport.setExamineUserId(iceBoxVo.getUserId());
                    putReport.setExamineUserName(iceBoxVo.getUserName());
                    putReport.setExamineTime(new Date());
                    SimpleUserInfoVo exaine = FeignResponseUtil.getFeignData(feignUserClient.findUserById(relateModel.getUpdateBy()));
                    if (Objects.nonNull(exaine)){
                        putReport.setExamineUserPosion(exaine.getPosion());
                    }
                    iceBoxPutReportDao.update(putReport,
                            Wrappers.<IceBoxPutReport>lambdaUpdate().eq(IceBoxPutReport::getId, putReport.getId())
                                    .set(IceBoxPutReport::getIceBoxAssetId, null)
                                    .set(IceBoxPutReport::getIceBoxId, null));
                }
            }
        }
        this.deleteBacklogByCode(iceBoxVo);

        /**
         * ???????????????????????????,?????????????????????????????????,?????????????????????icebox??????
         */
        List<IceBoxExtend> extendList = iceBoxExtendDao.selectList(Wrappers.<IceBoxExtend>lambdaQuery().eq(IceBoxExtend::getLastApplyNumber, iceBoxVo.getApplyNumber()));
        if(extendList.size()>0){
            Set<Integer> iceboxIds = extendList.stream().map(x-> x.getId()).collect(Collectors.toSet());
            if(iceboxIds.size() > 0){
                List<IceBox> iceBoxes = iceBoxDao.selectBatchIds(iceboxIds);
                List<IceBox> doPutingBoxs = iceBoxes.stream().filter(iceBox -> PutStatus.DO_PUT.getStatus().equals(iceBox.getPutStatus())).collect(Collectors.toList());
                List<IceBox> lockPutBoxs = iceBoxes.stream().filter(iceBox -> PutStatus.LOCK_PUT.getStatus().equals(iceBox.getPutStatus())).collect(Collectors.toList());
                if(doPutingBoxs.size()>0){
                    for (IceBox iceBox : doPutingBoxs){
                        iceBox.setPutStatus(PutStatus.NO_PUT.getStatus());
                        iceBox.setPutStoreNumber(0+"");
                        iceBox.setUpdatedTime(new Date());
                        iceBoxDao.updateById(iceBox);
                    }
                }
                if(lockPutBoxs.size()>0){
                    for(IceBox iceBox : lockPutBoxs){
                        iceBox.setPutStatus(PutStatus.NO_PUT.getStatus());
                        iceBox.setPutStoreNumber(0+"");
                        iceBox.setUpdatedTime(new Date());
                        iceBoxDao.updateById(iceBox);
                    }
                }
            }
        }

        List<ExamineNodeVo> examineNodeVoList = iceBoxVo.getExamineNodeVoList();
        for (ExamineNodeVo nodeVo : examineNodeVoList) {
            if (ExamineNodeStatusEnum.IS_PASS.getStatus().equals(nodeVo.getExamineStatus())) {
                SessionVisitExamineBacklog backlog = new SessionVisitExamineBacklog();
                backlog.setBacklogName(iceBoxVo.getUserName() + "??????????????????????????????");
                backlog.setCode(iceBoxVo.getApplyNumber());
                backlog.setExamineId(nodeVo.getExamineId());
                backlog.setExamineStatus(nodeVo.getExamineStatus());
                backlog.setExamineType(ExamineTypeEnum.ICEBOX_PUT.getType());
                backlog.setSendType(1);
                backlog.setSendUserId(nodeVo.getUserId());
                backlog.setCreateBy(iceBoxVo.getUserId());
                feignBacklogClient.createBacklog(backlog);
            }
        }

    }

    @Override
    @Transactional(rollbackFor = Exception.class, value = "transactionManager")
    public void cancelBoxBack(String applyNumber,String cancelRemark) {
        //???????????????????????????????????????????????????t_ice_back_apply_relate_box??????????????????????????????????????????????????????
        IceBackApplyRelateBox iceBackApplyRelateBox = iceBackApplyRelateBoxDao.selectOne(Wrappers.<IceBackApplyRelateBox>lambdaQuery().eq(IceBackApplyRelateBox::getApplyNumber,applyNumber));
        if(Objects.isNull(iceBackApplyRelateBox)){
            throw new ImproperOptionException("??????????????????????????????");
        }
        IceBackApply iceBackApply = iceBackApplyDao.selectOne(Wrappers.<IceBackApply>lambdaQuery().eq(IceBackApply::getApplyNumber,iceBackApplyRelateBox.getApplyNumber()));
        if(Objects.nonNull(iceBackApply)){
            //??????????????????????????????????????????3??????????????????????????????????????????  ????????? ???????????????3  ??????????????????  ?????????3 ?????????????????????
            iceBackApply.setExamineStatus(3);
            iceBackApply.setCancelRemark(cancelRemark);
            iceBackApplyDao.updateById(iceBackApply);
            //?????????????????????
            iceBackApplyReportService.remove(Wrappers.<IceBackApplyReport>lambdaQuery().eq(IceBackApplyReport::getApplyNumber, iceBackApply.getApplyNumber()));
        }
    }

    private void deleteBacklogByCode(IceBoxVo iceBoxVo) {
        SessionVisitExamineBacklog log = new SessionVisitExamineBacklog();
        log.setCode(iceBoxVo.getApplyNumber());
        feignBacklogClient.deleteBacklogByCode(log);
    }

    @Override
    public PutStoreRelateModel getApplyInfoByNumber(String applyNumber) {
        ApplyRelatePutStoreModel storeModel = applyRelatePutStoreModelDao.selectOne(Wrappers.<ApplyRelatePutStoreModel>lambdaQuery().eq(ApplyRelatePutStoreModel::getApplyNumber, applyNumber).last("limit 1"));
        if (storeModel == null) {
            return null;
        }
        PutStoreRelateModel relateModel = putStoreRelateModelDao.selectById(storeModel.getStoreRelateModelId());
        return relateModel;
    }

    @Override
    public List<IceBoxVo> findIceBoxsBySupplierId(Integer supplierId) {
        List<IceBox> iceBoxList = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getSupplierId, supplierId));
        if (CollectionUtil.isEmpty(iceBoxList)) {
            return null;
        }
        List<IceBoxVo> iceBoxVoList = new ArrayList<>();
        for (IceBox iceBox : iceBoxList) {
            IceBoxVo iceBoxVo = new IceBoxVo();
            BeanUtils.copyProperties(iceBox, iceBoxVo);
            iceBoxVo.setChestModel(iceBox.getModelName());
            LambdaQueryWrapper<IceBoxTransferHistory> wrapper = Wrappers.<IceBoxTransferHistory>lambdaQuery();
            wrapper.eq(IceBoxTransferHistory::getIceBoxId, iceBox.getId());
            wrapper.and(x -> x.eq(IceBoxTransferHistory::getExamineStatus, ExamineStatus.DEFAULT_EXAMINE.getStatus()).or().eq(IceBoxTransferHistory::getExamineStatus, ExamineStatus.DOING_EXAMINE.getStatus()));
            IceBoxTransferHistory history = iceBoxTransferHistoryDao.selectOne(wrapper);
            if (history != null) {
                iceBoxVo.setExamineStatus(history.getExamineStatus());
            }
            iceBoxVoList.add(iceBoxVo);
        }
        return iceBoxVoList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, value = "transactionManager")
    public Map<String, Object> transferIceBoxs(IceBoxTransferHistoryVo historyVo) {
        List<Integer> iceBoxIds = historyVo.getIceBoxIds();
        if (CollectionUtil.isEmpty(iceBoxIds)) {
            throw new NormalOptionException(Constants.API_CODE_FAIL, "??????????????????????????????");
        }
        List<IceBox> iceBoxList = iceBoxDao.selectBatchIds(iceBoxIds);

        List<IceBox> allIceBoxList = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getSupplierId, historyVo.getOldSupplierId()).eq(IceBox::getStatus, IceBoxEnums.StatusEnum.NORMAL.getType()));

        List<PutStoreRelateModel> relateModelList = putStoreRelateModelDao.selectList(Wrappers.<PutStoreRelateModel>lambdaQuery().eq(PutStoreRelateModel::getSupplierId, historyVo.getOldSupplierId()).eq(PutStoreRelateModel::getStatus, IceBoxEnums.StatusEnum.NORMAL.getType()));
        int used = 0;
        if(CollectionUtil.isNotEmpty(relateModelList)){
            used = relateModelList.size();
        }
        if(used > allIceBoxList.size() - iceBoxList.size()){
            throw new NormalOptionException(Constants.API_CODE_FAIL, "??????????????????????????????????????????");
        }
        Map<String, Object> map = createIceBoxTransferCheckProcess(historyVo);
        for (IceBox iceBox : iceBoxList) {
            LambdaQueryWrapper<IceBoxTransferHistory> wrapper = Wrappers.<IceBoxTransferHistory>lambdaQuery();
            wrapper.eq(IceBoxTransferHistory::getIceBoxId, iceBox.getId());
            wrapper.and(x -> x.eq(IceBoxTransferHistory::getExamineStatus, ExamineStatus.DEFAULT_EXAMINE.getStatus()).or().eq(IceBoxTransferHistory::getExamineStatus, ExamineStatus.DOING_EXAMINE.getStatus()));
            IceBoxTransferHistory isExist = iceBoxTransferHistoryDao.selectOne(wrapper);
            if (isExist != null) {
                continue;
            }
            IceBoxTransferHistory history = new IceBoxTransferHistory();
            BeanUtils.copyProperties(historyVo, history);
            Map<Integer, SessionDeptInfoVo> deptInfoVoMap = FeignResponseUtil.getFeignData(feignCacheClient.getFiveLevelDept(historyVo.getOldMarketAreaId()));
            SessionDeptInfoVo group = deptInfoVoMap.get(1);
            if (group != null) {
                history.setGroupDeptId(group.getId());
                history.setGroupDeptName(group.getName());
            }
            SessionDeptInfoVo service = deptInfoVoMap.get(2);
            if (service != null) {
                history.setServiceDeptId(service.getId());
                history.setServiceDeptName(service.getName());
            }
            SessionDeptInfoVo region = deptInfoVoMap.get(3);
            if (region != null) {
                history.setRegionDeptId(region.getId());
                history.setRegionDeptName(region.getName());
            }

            SessionDeptInfoVo business = deptInfoVoMap.get(4);
            if (business != null) {
                history.setBusinessDeptId(business.getId());
                history.setBusinessDeptName(business.getName());
            }

            SessionDeptInfoVo headquarters = deptInfoVoMap.get(5);
            if (headquarters != null) {
                history.setHeadquartersDeptId(headquarters.getId());
                history.setHeadquartersDeptName(headquarters.getName());
            }
            history.setIceBoxId(iceBox.getId());
            history.setAssetId(iceBox.getAssetId());
            history.setTransferNumber(map.get("transferNumber").toString());
            history.setIsCheck(0);
            history.setExamineStatus(ExamineStatus.PASS_EXAMINE.getStatus());
            Object isCheck = map.get("isCheck");
            if (isCheck == null) {
                history.setIsCheck(1);
                history.setExamineStatus(ExamineStatus.DEFAULT_EXAMINE.getStatus());
            }
            history.setCreateTime(new Date());
            iceBoxTransferHistoryDao.insert(history);
        }
        return map;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, value = "transactionManager")
    public void dealTransferCheck(IceBoxTransferHistoryVo historyVo) {
        List<IceBoxTransferHistory> iceBoxTransferHistoryList = iceBoxTransferHistoryDao.selectList(Wrappers.<IceBoxTransferHistory>lambdaQuery().eq(IceBoxTransferHistory::getTransferNumber, historyVo.getTransferNumber()));
        if (CollectionUtil.isEmpty(iceBoxTransferHistoryList)) {
            throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????????????????????????????");
        }
        for (IceBoxTransferHistory history : iceBoxTransferHistoryList) {
            history.setExamineStatus(historyVo.getExamineStatus());
            history.setUpdateTime(new Date());
            history.setReviewerId(historyVo.getReviewerId());
            SimpleUserInfoVo userInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findSimpleUserById(historyVo.getReviewerId()));
            if (userInfoVo != null) {
                history.setReviewerName(userInfoVo.getRealname());
                history.setReviewerOfficeName(userInfoVo.getPosion());
            }
            history.setReviewerTime(new Date());
            iceBoxTransferHistoryDao.updateById(history);
        }

        if (ExamineStatus.PASS_EXAMINE.getStatus().equals(historyVo.getExamineStatus())) {
            List<Integer> iceBoxIds = iceBoxTransferHistoryList.stream().map(x -> x.getIceBoxId()).collect(Collectors.toList());
            List<IceBox> iceBoxList = iceBoxDao.selectBatchIds(iceBoxIds);
            if (CollectionUtil.isEmpty(iceBoxList)) {
                throw new NormalOptionException(Constants.API_CODE_FAIL, "??????????????????????????????");
            }
            Map<Integer, IceBox> iceBoxMap = iceBoxList.stream().collect(Collectors.toMap(IceBox::getId, x -> x));
            for (IceBoxTransferHistory history : iceBoxTransferHistoryList) {
                IceBox iceBox = iceBoxMap.get(history.getIceBoxId());
                if (iceBox == null) {
                    continue;
                }
                iceBox.setSupplierId(history.getNewSupplierId());
                iceBox.setDeptId(history.getNewMarketAreaId());
                iceBox.setUpdatedBy(history.getCreateBy());
                iceBox.setUpdatedTime(new Date());
                iceBoxDao.updateById(iceBox);
            }
        }
    }

    //?????????????????????????????????
    private Map<String, Object> createIceBoxTransferCheckProcess(IceBoxTransferHistoryVo historyVo) throws
            ImproperOptionException, NormalOptionException {
        Map<String, Object> map = new HashMap<>();
        String transferNumber = UUID.randomUUID().toString().replace("-", "");
        map.put("transferNumber", transferNumber);
        Date now = new Date();
        log.info("???????????????marketAreaId--??????{}???????????????marketAreaId--??????{}???", historyVo.getOldMarketAreaId(), historyVo.getNewMarketAreaId());
        SessionDeptInfoVo sameDept = FeignResponseUtil.getFeignData(feignDeptClient.getSameDeptInfoById(historyVo.getOldMarketAreaId(), historyVo.getNewMarketAreaId()));
        SimpleUserInfoVo simpleUserInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findSimpleUserById(historyVo.getCreateBy()));
        Map<Integer, SessionUserInfoVo> sessionUserInfoMap = FeignResponseUtil.getFeignData(feignDeptClient.findLevelLeaderByDeptIdNew(historyVo.getOldMarketAreaId()));
        List<Integer> ids = new ArrayList<Integer>();
        //????????????????????????
        SessionUserInfoVo serviceUser = new SessionUserInfoVo();
        SessionUserInfoVo regionUser = new SessionUserInfoVo();
        SessionUserInfoVo businessUser = new SessionUserInfoVo();
        SessionUserInfoVo yxbbUser = new SessionUserInfoVo();
        Set<Integer> keySet = sessionUserInfoMap.keySet();
        for (Integer key : keySet) {
            SessionUserInfoVo userInfoVo = sessionUserInfoMap.get(key);
            if (userInfoVo == null) {
                continue;
            }
            if (DeptTypeEnum.SERVICE.getType().equals(userInfoVo.getDeptType())) {
                serviceUser = userInfoVo;
                if (userInfoVo.getId() == null) {
                    serviceUser = null;
                }
                continue;
            }
            if (DeptTypeEnum.LARGE_AREA.getType().equals(userInfoVo.getDeptType())) {
                regionUser = userInfoVo;
                if (userInfoVo.getId() == null) {
                    regionUser = null;
                }
                continue;
            }
            if (DeptTypeEnum.BUSINESS_UNIT.getType().equals(userInfoVo.getDeptType())) {
                businessUser = userInfoVo;
                if (userInfoVo.getId() == null) {
                    businessUser = null;
                }
                continue;
            }
            if (DeptTypeEnum.THIS_PART.getType().equals(userInfoVo.getDeptType())) {
                yxbbUser = userInfoVo;
                if (userInfoVo.getId() == null) {
                    yxbbUser = null;
                }
                continue;
            }
        }

        //????????????????????????????????????????????????????????????
//        if (sameDept.getName().endsWith(FWC)) {
        if (DeptTypeEnum.SERVICE.getType().equals(sameDept.getDeptType())) {

            if (serviceUser == null) {
                throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,???????????????????????????");
            }
            //??????????????????????????????????????????????????????
            if ((serviceUser.getId() != null && serviceUser.getId().equals(simpleUserInfoVo.getId()))
                    || DeptTypeEnum.LARGE_AREA.getType().equals(simpleUserInfoVo.getDeptType())
                    || DeptTypeEnum.BUSINESS_UNIT.getType().equals(simpleUserInfoVo.getDeptType())
                    || DeptTypeEnum.THIS_PART.getType().equals(simpleUserInfoVo.getDeptType())) {
                return updateIceBoxTransferIsCheck(historyVo, map);
            }

            //?????????????????????
            if (serviceUser.getId() != null && !serviceUser.getId().equals(historyVo.getCreateBy()) && !ids.contains(serviceUser.getId())
                    && (DeptTypeEnum.GROUP.getType().equals(simpleUserInfoVo.getDeptType()) || DeptTypeEnum.SERVICE.getType().equals(simpleUserInfoVo.getDeptType()))) {
                ids.add(serviceUser.getId());
            }
            //???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
            if (CollectionUtil.isEmpty(ids)) {
                List<SessionDeptInfoVo> deptInfoVos = getDeptInfoByUserId(sessionUserInfoMap, keySet);
                if (CollectionUtil.isNotEmpty(deptInfoVos)) {
                    for (SessionDeptInfoVo deptInfoVo : deptInfoVos) {
                        if (DeptTypeEnum.LARGE_AREA.getType().equals(deptInfoVo.getDeptType())
                                || DeptTypeEnum.BUSINESS_UNIT.getType().equals(deptInfoVo.getDeptType()) || DeptTypeEnum.THIS_PART.getType().equals(deptInfoVo.getDeptType())) {
                            return updateIceBoxTransferIsCheck(historyVo, map);
                        }
                    }
                }
            }
        }
        //???????????????????????????
//        if (sameDept.getName().endsWith(DQ)) {
        if (DeptTypeEnum.LARGE_AREA.getType().equals(sameDept.getDeptType())) {

            if (regionUser == null) {
                throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,????????????????????????");
            }
            //???????????????????????????????????????????????????
            if ((regionUser.getId() != null && regionUser.getId().equals(simpleUserInfoVo.getId()))
                    || (DeptTypeEnum.BUSINESS_UNIT.getType().equals(simpleUserInfoVo.getDeptType())
                    || DeptTypeEnum.THIS_PART.getType().equals(simpleUserInfoVo.getDeptType()))) {
                return updateIceBoxTransferIsCheck(historyVo, map);
            }

            if (serviceUser == null) {
                throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,???????????????????????????");
            }
            //?????????????????????
            if (serviceUser.getId() != null && !serviceUser.getId().equals(historyVo.getCreateBy()) && !ids.contains(serviceUser.getId())
                    && (DeptTypeEnum.GROUP.getType().equals(simpleUserInfoVo.getDeptType()) || DeptTypeEnum.SERVICE.getType().equals(simpleUserInfoVo.getDeptType()))) {
                ids.add(serviceUser.getId());
            }

            //??????????????????
            if (regionUser.getId() != null && !regionUser.getId().equals(historyVo.getCreateBy()) && !ids.contains(regionUser.getId())
                    && !DeptTypeEnum.BUSINESS_UNIT.getType().equals(simpleUserInfoVo.getDeptType()) && !DeptTypeEnum.THIS_PART.getType().equals(simpleUserInfoVo.getDeptType())) {
                ids.add(regionUser.getId());
            }
            //????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
            if (CollectionUtil.isEmpty(ids)) {
                List<SessionDeptInfoVo> deptInfoVos = getDeptInfoByUserId(sessionUserInfoMap, keySet);
                if (CollectionUtil.isNotEmpty(deptInfoVos)) {
                    for (SessionDeptInfoVo deptInfoVo : deptInfoVos) {
                        if (DeptTypeEnum.BUSINESS_UNIT.getType().equals(deptInfoVo.getDeptType()) || DeptTypeEnum.THIS_PART.getType().equals(deptInfoVo.getDeptType())) {
                            return updateIceBoxTransferIsCheck(historyVo, map);
                        }
                    }
                }
            }
        }

        //??????????????????????????????
        if (DeptTypeEnum.BUSINESS_UNIT.getType().equals(sameDept.getDeptType())) {
            if (businessUser == null) {
                throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,??????????????????????????????");
            }
            //?????????????????????????????????????????????????????????
            if ((businessUser.getId() != null && businessUser.getId().equals(simpleUserInfoVo.getId()))
                    || DeptTypeEnum.THIS_PART.getType().equals(simpleUserInfoVo.getDeptType())) {
                return updateIceBoxTransferIsCheck(historyVo, map);
            }

            if (serviceUser == null) {
                throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,???????????????????????????");
            }
            //?????????????????????
            if (serviceUser.getId() != null && !serviceUser.getId().equals(historyVo.getCreateBy()) && !ids.contains(serviceUser.getId())
                    && (DeptTypeEnum.GROUP.getType().equals(simpleUserInfoVo.getDeptType()) || DeptTypeEnum.SERVICE.getType().equals(simpleUserInfoVo.getDeptType()))) {
                ids.add(serviceUser.getId());
            }
            if (regionUser == null) {
                throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,????????????????????????");
            }
            //??????????????????
            if (regionUser.getId() != null && !regionUser.getId().equals(historyVo.getCreateBy()) && !ids.contains(regionUser.getId())
                    && !DeptTypeEnum.BUSINESS_UNIT.getType().equals(simpleUserInfoVo.getDeptType()) && !DeptTypeEnum.THIS_PART.getType().equals(simpleUserInfoVo.getDeptType())) {
                ids.add(regionUser.getId());
            }

            //?????????????????????
            if (businessUser.getId() != null && !businessUser.getId().equals(historyVo.getCreateBy()) && !ids.contains(businessUser.getId())
                    && !DeptTypeEnum.THIS_PART.getType().equals(simpleUserInfoVo.getDeptType())) {
                ids.add(businessUser.getId());
            }
        }
        //?????????????????????????????????
        if (DeptTypeEnum.THIS_PART.getType().equals(sameDept.getDeptType())) {
            if (yxbbUser == null) {
                throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????????????????????????????????????????????????????????????????");
            }

            //?????????????????????
            if (serviceUser == null) {
                throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,???????????????????????????");
            }
            if (serviceUser.getId() != null && !serviceUser.getId().equals(historyVo.getCreateBy()) && !ids.contains(serviceUser.getId())
                    && (DeptTypeEnum.GROUP.getType().equals(simpleUserInfoVo.getDeptType()) || DeptTypeEnum.SERVICE.getType().equals(simpleUserInfoVo.getDeptType()))) {
                ids.add(serviceUser.getId());
            }

            //??????????????????
            if (regionUser == null) {
                throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,????????????????????????");
            }
            if (regionUser.getId() != null && !regionUser.getId().equals(historyVo.getCreateBy()) && !ids.contains(regionUser.getId())
                    && !DeptTypeEnum.BUSINESS_UNIT.getType().equals(simpleUserInfoVo.getDeptType()) && !DeptTypeEnum.THIS_PART.getType().equals(simpleUserInfoVo.getDeptType())) {
                ids.add(regionUser.getId());
            }

            //????????????????????????
            if (businessUser == null) {
                throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,??????????????????????????????");
            }
            if (businessUser.getId() != null && !businessUser.getId().equals(historyVo.getCreateBy()) && !ids.contains(businessUser.getId())
                    && !DeptTypeEnum.THIS_PART.getType().equals(simpleUserInfoVo.getDeptType())) {
                ids.add(businessUser.getId());
            }

            //?????????????????????????????????
            if (!ids.contains(yxbbUser.getId())) {
                ids.add(yxbbUser.getId());
            }

        }

        //????????????????????????????????????
        if (sameDept != null && sameDept.getId().equals(1)) {
            if (yxbbUser == null) {
                throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????????????????????????????????????????????????????????????????");
            }

            //?????????????????????
            if (serviceUser == null) {
                throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,???????????????????????????");
            }
            if (serviceUser.getId() != null && !serviceUser.getId().equals(historyVo.getCreateBy()) && !ids.contains(serviceUser.getId())
                    && (DeptTypeEnum.GROUP.getType().equals(simpleUserInfoVo.getDeptType()) || DeptTypeEnum.SERVICE.getType().equals(simpleUserInfoVo.getDeptType()))) {
                ids.add(serviceUser.getId());
            }

            //??????????????????
            if (regionUser == null) {
                throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,????????????????????????");
            }
            if (regionUser.getId() != null && !regionUser.getId().equals(historyVo.getCreateBy()) && !ids.contains(regionUser.getId())
                    && !DeptTypeEnum.BUSINESS_UNIT.getType().equals(simpleUserInfoVo.getDeptType()) && !DeptTypeEnum.THIS_PART.getType().equals(simpleUserInfoVo.getDeptType())) {
                ids.add(regionUser.getId());
            }

            //????????????????????????
            if (businessUser == null) {
                throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????,??????????????????????????????");
            }
            if (businessUser.getId() != null && !businessUser.getId().equals(historyVo.getCreateBy()) && !ids.contains(businessUser.getId())
                    && !DeptTypeEnum.THIS_PART.getType().equals(simpleUserInfoVo.getDeptType())) {
                ids.add(businessUser.getId());
            }

            //?????????????????????????????????
//            Integer ownerBusinessId = FeignResponseUtil.getFeignData(feignDeptClient.getBusinessLeaderByDeptId(billInfo.getOwnerMarketAreaId()));
            if (!ids.contains(yxbbUser.getId())) {
                ids.add(yxbbUser.getId());
            }

            //?????????????????????????????????
            Integer supplierBusinessId = FeignResponseUtil.getFeignData(feignDeptClient.getBusinessLeaderByDeptId(historyVo.getNewMarketAreaId()));
            if (supplierBusinessId == null) {
                throw new NormalOptionException(Constants.API_CODE_FAIL, "???????????????????????????????????????????????????????????????");
            }
            if (!ids.contains(supplierBusinessId)) {
                ids.add(supplierBusinessId);
            }

        }
        if (CollectionUtil.isEmpty(ids)) {
            throw new NormalOptionException(Constants.API_CODE_FAIL, "??????????????????????????????????????????");
        }

        IceBoxTransferModel transferModel = new IceBoxTransferModel();


        transferModel.setTransferNumber(transferNumber);

        transferModel.setOldSupplierName(historyVo.getOldSupplierName());
        transferModel.setNewSupplierName(historyVo.getNewSupplierName());
        transferModel.setTransferCount(historyVo.getIceBoxIds().size());
        transferModel.setCreateByName(historyVo.getCreateByName());
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        transferModel.setCreateTimeStr(dateFormat.format(now));
        List<Integer> iceBoxIds = historyVo.getIceBoxIds();
        List<IceBox> iceBoxList = iceBoxDao.selectBatchIds(iceBoxIds);

        if (CollectionUtil.isNotEmpty(iceBoxList)) {
            for (IceBox iceBox : iceBoxList) {
                transferModel.addIceModelList(iceBox.getId(), iceBox.getAssetId(), iceBox.getModelName(), iceBox.getChestName(), iceBox.getDepositMoney());
            }
        }
        SessionExamineVo sessionExamineVo = new SessionExamineVo();
        SessionExamineCreateVo sessionExamineCreateVo = SessionExamineCreateVo.builder()
                .code(transferNumber)
                .relateCode(transferNumber)
                .createBy(historyVo.getCreateBy())
                .userIds(ids)
                .build();
        sessionExamineVo.setSessionExamineCreateVo(sessionExamineCreateVo);
        sessionExamineVo.setIceBoxTransferModel(transferModel);
        SessionExamineVo examineVo = FeignResponseUtil.getFeignData(feignExamineClient.createIceBoxTransfer(sessionExamineVo));
        List<SessionExamineVo.VisitExamineNodeVo> visitExamineNodes = examineVo.getVisitExamineNodes();
        map.put("iceBoxTransferNodes", visitExamineNodes);
        return map;
    }

    private List<SessionDeptInfoVo> getDeptInfoByUserId
            (Map<Integer, SessionUserInfoVo> sessionUserInfoMap, Set<Integer> keySet) throws
            ImproperOptionException, NormalOptionException {
        List<SessionDeptInfoVo> deptInfoVos;
        List<Integer> userIds = new ArrayList<>();
        for (Integer key : keySet) {
            SessionUserInfoVo userInfoVo = sessionUserInfoMap.get(key);
            if (userInfoVo != null) {
                userIds.add(userInfoVo.getId());
                break;
            }
        }
        CommonIdsVo commonIdsVo = new CommonIdsVo();
        commonIdsVo.setIds(userIds);
        Map<Integer, List<SessionDeptInfoVo>> userMap = FeignResponseUtil.getFeignData(feignUserClient.findDeptVoByUserids(commonIdsVo));

        if (CollectionUtil.isNotEmpty(userIds)) {
            deptInfoVos = userMap.get(userIds.get(0));
            return deptInfoVos;
        }
        return null;
    }

    private Map<String, Object> updateIceBoxTransferIsCheck(IceBoxTransferHistoryVo
                                                                    historyVo, Map<String, Object> map) {
        List<IceBox> iceBoxList = iceBoxDao.selectBatchIds(historyVo.getIceBoxIds());
        if (CollectionUtil.isEmpty(iceBoxList)) {
            throw new NormalOptionException(Constants.API_CODE_FAIL, "??????????????????????????????");
        }
        for (IceBox iceBox : iceBoxList) {
            iceBox.setSupplierId(historyVo.getNewSupplierId());
            iceBox.setDeptId(historyVo.getNewMarketAreaId());
            iceBox.setUpdatedBy(historyVo.getCreateBy());
            iceBox.setUpdatedTime(new Date());
            iceBoxDao.updateById(iceBox);
        }
        map.put("isCheck", CommonIsCheckEnum.IS_CHECK.getStatus());
        return map;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, value = "transactionManager")
    public void changeIcebox(IceBoxManagerVo iceBoxManagerVo) {
        UserManageVo userManageVo = FeignResponseUtil.getFeignData(feignUserClient.getSessionUserInfo());
        judgeChange(iceBoxManagerVo);

        IceBox iceBox = iceBoxManagerVo.convertToIceBox();
        Integer iceBoxId = iceBoxManagerVo.getIceBoxId();
        Integer modifyCustomerType = iceBoxManagerVo.getModifyCustomerType();
        String assetId = iceBox.getAssetId();
        IceBox oldIceBox = iceBoxDao.selectById(iceBoxId);
        Integer oldPutStatus = oldIceBox.getPutStatus();
        String oldPutStoreNumber = oldIceBox.getPutStoreNumber();
        if (PutStatus.LOCK_PUT.getStatus().equals(oldPutStatus) || PutStatus.DO_PUT.getStatus().equals(oldPutStatus) || PutStatus.FINISH_PUT.getStatus().equals(equals(oldPutStatus))) {
            throw new NormalOptionException(ResultEnum.CANNOT_CHANGE_ICEBOX.getCode(), "??????????????????????????????????????????????????????");
        }
        Integer count = iceBoxExamineExceptionReportDao.selectCount(Wrappers.<IceBoxExamineExceptionReport>lambdaQuery()
                .eq(IceBoxExamineExceptionReport::getIceBoxAssetId, oldIceBox.getAssetId())
                .ne(IceBoxExamineExceptionReport::getStatus, ExamineExceptionStatusEnums.is_unpass.getStatus())
                .ne(IceBoxExamineExceptionReport::getToOaType, 1));

        if (count > 0) {
            throw new NormalOptionException(ResultEnum.CANNOT_CHANGE_ICEBOX.getCode(), "????????????????????????????????????");
        }


        Integer oldIceBoxModelId = oldIceBox.getModelId();
        Integer oldSupplierId = oldIceBox.getSupplierId();
        Integer modelId = iceBox.getModelId();
        Integer supplierId = iceBox.getSupplierId();

        if ((!PutStatus.FINISH_PUT.getStatus().equals(oldPutStatus)) && ((!oldIceBoxModelId.equals(modelId)) || (!supplierId.equals(oldSupplierId)))) {
            // ???????????????  ?????? ????????????????????????????????????
            Integer usedCount = putStoreRelateModelDao.selectCount(Wrappers.<PutStoreRelateModel>lambdaQuery()
                    .eq(PutStoreRelateModel::getModelId, oldIceBoxModelId)
                    .eq(PutStoreRelateModel::getSupplierId, oldSupplierId)
                    .between(PutStoreRelateModel::getPutStatus, PutStatus.LOCK_PUT.getStatus(), PutStatus.DO_PUT.getStatus())
                    .eq(PutStoreRelateModel::getStatus, CommonStatus.VALID.getStatus()));

            // ??????????????????????????????????????????
            Integer allCount = iceBoxDao.selectCount(Wrappers.<IceBox>lambdaQuery()
                    .eq(IceBox::getSupplierId, oldSupplierId)
                    .eq(IceBox::getModelId, oldIceBoxModelId)
                    .eq(IceBox::getStatus, IceBoxEnums.StatusEnum.NORMAL.getType())
                    .eq(IceBox::getPutStatus, PutStatus.NO_PUT.getStatus()));
            if (usedCount > 0 && usedCount >= allCount) {
                throw new NormalOptionException(ResultEnum.CANNOT_CHANGE_ICEBOX.getCode(), "????????????????????????????????????????????????????????????" + usedCount);
            }
        }

        IceBoxChangeHistory iceBoxChangeHistory = new IceBoxChangeHistory();

        // ??????????????????
        IceBox currentIceBox = iceBoxDao.selectById(iceBoxId);
        if (!currentIceBox.getAssetId().contains(assetId + "-")) {
            IceBox selectIceBox = iceBoxDao.selectOne(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getAssetId, assetId).ne(IceBox::getId, iceBoxId));
            if (null != selectIceBox) {
                List<IceBox> iceBoxes = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().likeRight(IceBox::getAssetId, assetId).ne(IceBox::getId, iceBoxId));
                // ?????????
                int integer = iceBoxes.stream().map(iceBox1 -> {
                    String assetId1 = iceBox1.getAssetId();
                    if (!assetId1.contains("-")) {
                        return 0;
                    }
                    return Integer.parseInt(assetId1.substring(assetId1.indexOf("-") + 1));
                }).reduce(Integer::max).orElse(0) + 1;
                iceBox.setAssetId(assetId + "-" + integer);
            }
        }
        LambdaUpdateWrapper<IceBox> updateWrapper = Wrappers.<IceBox>lambdaUpdate().eq(IceBox::getId, iceBoxId);

        Integer newStatus = iceBox.getStatus();
        //???????????? ?????? ????????????????????????????????????
        if(PutStatus.FINISH_PUT.getStatus().equals(iceBox.getPutStatus())&&IceBoxEnums.StatusEnum.LOSE.equals(newStatus)||IceBoxEnums.StatusEnum.SCRAP.equals(newStatus)){
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    IceInspectionReportMsg reportMsg = new IceInspectionReportMsg();
                    reportMsg.setOperateType(5);
                    reportMsg.setBoxId(iceBoxId);
                    rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.iceInspectionReportKey,reportMsg);
                }
            });

        }
        Integer oldStatus = oldIceBox.getStatus();

        boolean modifyCustomer = iceBoxManagerVo.isModifyCustomer();

        if (IceBoxEnums.StatusEnum.NORMAL.getType().equals(oldStatus) && !IceBoxEnums.StatusEnum.NORMAL.getType().equals(newStatus) && modifyCustomer) {
            // ??????????????????????????????????????? ????????????????????????
            throw new NormalOptionException(ResultEnum.CANNOT_CHANGE_CUSTOMER.getCode(), ResultEnum.CANNOT_CHANGE_CUSTOMER.getMessage());
        }
        /**
         * ????????????????????????
         */
        //if (modifyCustomer && null != modifyCustomerType) {
        if(false){
            // ???????????????1-????????????2-????????????3-?????????4-?????????  5-??????
            String customerNumber = iceBoxManagerVo.getCustomerNumber();
            if (modifyCustomerType == 1) {
                SubordinateInfoVo subordinateInfoVo = FeignResponseUtil.getFeignData(feignSupplierClient.findSupplierBySupplierId(iceBoxManagerVo.getSupplierId()));
                if (null != subordinateInfoVo && StringUtils.isNotBlank(subordinateInfoVo.getNumber())) {
                    String supplierNumber = subordinateInfoVo.getNumber();
                    if (supplierNumber.equals(customerNumber)) {
                        // ??????
                        updateWrapper.set(IceBox::getPutStoreNumber, null).set(IceBox::getPutStatus, PutStatus.NO_PUT.getStatus());

                        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(iceBoxId);
                        String lastApplyNumber = iceBoxExtend.getLastApplyNumber();
                        if (null != lastApplyNumber) { // ????????????????????????
                            // ????????????????????????
                            IcePutApplyRelateBox icePutApplyRelateBox = icePutApplyRelateBoxDao.selectOne(Wrappers.<IcePutApplyRelateBox>lambdaQuery().eq(IcePutApplyRelateBox::getApplyNumber, lastApplyNumber));
                            if (null != icePutApplyRelateBox) {
                                List<ApplyRelatePutStoreModel> applyRelatePutStoreModelList = applyRelatePutStoreModelDao.selectList(Wrappers.<ApplyRelatePutStoreModel>lambdaQuery()
                                        .eq(ApplyRelatePutStoreModel::getApplyNumber, iceBoxExtend.getLastApplyNumber())
                                        .eq(ApplyRelatePutStoreModel::getFreeType, icePutApplyRelateBox.getFreeType()));
                                if (CollectionUtil.isNotEmpty(applyRelatePutStoreModelList)) {
                                    for (ApplyRelatePutStoreModel applyRelatePutStoreModel : applyRelatePutStoreModelList) {
                                        Integer storeRelateModelId = applyRelatePutStoreModel.getStoreRelateModelId();
                                        PutStoreRelateModel putStoreRelateModel = putStoreRelateModelDao.selectOne(Wrappers.<PutStoreRelateModel>lambdaQuery()
                                                .eq(PutStoreRelateModel::getId, storeRelateModelId)
                                                .eq(PutStoreRelateModel::getModelId, oldIceBoxModelId)
                                                .eq(PutStoreRelateModel::getPutStatus, com.szeastroc.icebox.newprocess.enums.PutStatus.FINISH_PUT.getStatus()));
                                        if (null != putStoreRelateModel) {
                                            putStoreRelateModelDao.update(putStoreRelateModel, Wrappers.<PutStoreRelateModel>lambdaUpdate()
                                                    .set(PutStoreRelateModel::getPutStatus, com.szeastroc.icebox.newprocess.enums.PutStatus.NO_PUT.getStatus())
                                                    .set(PutStoreRelateModel::getUpdateTime, new Date())
                                                    .eq(PutStoreRelateModel::getId, storeRelateModelId));
                                            break;
                                        }
                                    }
                                }
                                iceBoxExtendDao.update(null, Wrappers.<IceBoxExtend>lambdaUpdate()
                                        .eq(IceBoxExtend::getId, iceBoxId)
                                        .set(IceBoxExtend::getLastPutId, 0)
                                        .set(IceBoxExtend::getLastApplyNumber, null));
                            }
                        }
                    } else {
                        throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????????????????????????????????????????????????????????????????");
                    }
                }
            } else {
                // ??????????????????????????????????????????????????????
                // ????????? ?????? ?????? ??????????????????
                /*Integer selectCount = iceBoxDao.selectCount(Wrappers.<IceBox>lambdaQuery()
                        .eq(IceBox::getPutStoreNumber, customerNumber)
                        .ne(IceBox::getId, iceBoxId)
                        .ne(IceBox::getPutStatus, PutStatus.NO_PUT.getStatus()));
                if (selectCount > 2) {
                    // ????????????????????????????????????
                    throw new NormalOptionException(ResultEnum.CANNOT_CHANGE_CUSTOMER.getCode(), "?????????????????????????????????????????????");
                }*/
                iceBoxChangeHistory.setNewPutStoreNumber(customerNumber);
                iceBox.setPutStoreNumber(customerNumber);
                iceBox.setPutStatus(PutStatus.FINISH_PUT.getStatus());

                if ((null == oldPutStoreNumber || "0".equals(oldPutStoreNumber)) || (!oldPutStoreNumber.equals(customerNumber))) {
                    // ????????? ????????????
                    iceBox.setId(iceBoxId);
                    iceBoxService.changeCustomer(iceBox, oldIceBox);
                }
                //??????????????????????????????????????????
                if(iceBox.getPutStoreNumber().startsWith("C0")){
                    StoreInfoDtoVo store = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(iceBox.getPutStoreNumber()));
                    if(store != null){
                        iceBox.setDeptId(store.getMarketArea());
                    }
                }else {
                    SubordinateInfoVo supplier = FeignResponseUtil.getFeignData(feignSupplierClient.findByNumber(iceBox.getPutStoreNumber()));
                    if(supplier != null){
                        iceBox.setDeptId(supplier.getMarketAreaId());
                    }
                }
                //todo ???????????????????????????
            }
        } else {
            iceBox.setPutStoreNumber(oldPutStoreNumber);
        }
        iceBoxDao.update(iceBox, updateWrapper);
        iceBoxExtendDao.update(null, Wrappers.<IceBoxExtend>lambdaUpdate().eq(IceBoxExtend::getId, iceBoxId).set(IceBoxExtend::getAssetId, iceBox.getAssetId()));
        convertToIceBoxChangeHistory(oldIceBox, iceBox, iceBoxChangeHistory, userManageVo);

            JSONObject jsonObject = setAssetReportJson(iceBox,"changeIcebox");
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.ICEBOX_ASSETS_REPORT_ROUTING_KEY, jsonObject.toString());
                    //??????????????????????????????
                    IceInspectionReportMsg reportMsg = new IceInspectionReportMsg();
                    reportMsg.setOperateType(1);
                    reportMsg.setBoxId(iceBox.getId());
                    rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.iceInspectionReportKey,reportMsg);
                }
            });
    }

    @Override
    public void test() {

        IceBox iceBox = iceBoxDao.selectById(479);

        log.info("icebox-->[{}]", JSON.toJSONString(iceBox, true));

        iceBoxDao.update(iceBox, Wrappers.<IceBox>lambdaUpdate().set(IceBox::getPutStoreNumber, null).eq(IceBox::getId, iceBox.getId()));

    }

    @Override
    public void changeAssetId(Integer iceBoxId, String assetId, boolean reconfirm) {
        if (null == iceBoxId) {
            throw new ImproperOptionException(Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }
        if (StringUtils.isBlank(assetId)) {
            throw new NormalOptionException(Constants.API_CODE_FAIL, "????????????????????????");
        }
        IceBox currentIceBox = iceBoxDao.selectById(iceBoxId);
        if (currentIceBox.getAssetId().contains(assetId + "-")) {
            return;
        }
        IceBox iceBox = iceBoxDao.selectOne(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getAssetId, assetId).ne(IceBox::getId, iceBoxId));
        String newAssetId = "";
        if (null != iceBox) {
            if (reconfirm) {
                List<IceBox> iceBoxes = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().likeRight(IceBox::getAssetId, assetId).ne(IceBox::getId, iceBoxId));
                // ?????????
                /*if (iceBoxes.size() == 1) {
                    newAssetId = assetId + "-1";
                } else {
                    List<IceBox> otherList = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().ne(IceBox::getId, iceBoxId).likeRight(IceBox::getAssetId, assetId + "-"));
                    if (CollectionUtil.isEmpty(otherList)) {
                        newAssetId = assetId + "-1";
                    } else {
                        int integer = otherList.stream().map(iceBox1 -> {
                            String assetId1 = iceBox1.getAssetId();
                            return Integer.parseInt(assetId1.substring(assetId1.indexOf("-") + 1));
                        }).reduce(Integer::max).orElse(0) + 1;
                        newAssetId = assetId + "-" + integer;
                    }
                }*/

                // ?????????
                int integer = iceBoxes.stream().map(iceBox1 -> {
                    String assetId1 = iceBox1.getAssetId();
                    if (!assetId1.contains("-")) {
                        return 0;
                    }
                    return Integer.parseInt(assetId1.substring(assetId1.indexOf("-") + 1));
                }).reduce(Integer::max).orElse(0) + 1;
                newAssetId = assetId + "-" + integer;
            } else {
                String message = "";
                String deptName = "";
                String supplierName = "";
                Integer supplierId = iceBox.getSupplierId();
                Integer deptId = iceBox.getDeptId();
                SessionDeptInfoVo sessionDeptInfoVo = FeignResponseUtil.getFeignData(feignCacheClient.getForDeptInfoVo(deptId));
                if (null != sessionDeptInfoVo) {
                    deptName = sessionDeptInfoVo.getName();
                }
                SubordinateInfoVo subordinateInfoVo = FeignResponseUtil.getFeignData(feignSupplierClient.findSupplierBySupplierId(supplierId));
                if (null != subordinateInfoVo) {
                    supplierName = subordinateInfoVo.getName();
                }
                message = "??????????????????" + deptName + supplierName + "??????????????????????????????,?????????????????????";
                throw new NormalOptionException(4101, message);
            }
        } else {
            newAssetId = assetId;
        }
        iceBoxDao.update(null, Wrappers.<IceBox>lambdaUpdate().eq(IceBox::getId, iceBoxId).set(IceBox::getAssetId, newAssetId));
        iceBoxExtendDao.update(null, Wrappers.<IceBoxExtend>lambdaUpdate().eq(IceBoxExtend::getId, iceBoxId).set(IceBoxExtend::getAssetId, newAssetId));
    }

    private Map<String, Map<String, String>> getSuppMap
            (Map<String, Map<String, String>> storeMaps, List<String> storeNumbers) {
        // ?????????????????????,???????????????  t_cus_supplier_info  ???
        List<SubordinateInfoVo> supplierInfoList = FeignResponseUtil.getFeignData(feignSupplierClient.readByNumbers(storeNumbers));
        if (CollectionUtils.isNotEmpty(supplierInfoList)) {
//                map.put("realName", realName);
//                map.put("statusStr", statusStr);
//                map.put("address", address);
//                map.put("mobile", mobile);
//                map.put("storeName", storeName);
//                map.put("storeNumber", storeNumber);
//                map.put("storeLevel", storeLevel);
//                map.put("storeTypeName", storeTypeName);
//                maps.put(storeNumber, map);
            if (storeMaps == null) {
                storeMaps = Maps.newHashMap();
            }
            for (SubordinateInfoVo infoVo : supplierInfoList) {
                Map<String, String> mm = Maps.newHashMap();
                mm.put("realName", infoVo.getRealName());
                mm.put("address", infoVo.getAddress());
                mm.put("mobile", infoVo.getLinkmanMobile());
                mm.put("storeName", infoVo.getName());
                mm.put("storeNumber", infoVo.getNumber());
                mm.put("storeLevel", infoVo.getLevel());
                mm.put("storeTypeName", infoVo.getSupplierTypeName());
                storeMaps.put(infoVo.getNumber(), mm);
            }
        }
        return storeMaps;
    }

    @Override
    public void autoAddLabel() {
        // ??????????????????????????????

        List<IceBox> iceBoxList = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery()
                .eq(IceBox::getPutStatus, PutStatus.FINISH_PUT.getStatus()).ne(IceBox::getSupplierId, 0));

        if (CollectionUtil.isNotEmpty(iceBoxList)) {
            iceBoxList.forEach(iceBox -> {
                String putStoreNumber = iceBox.getPutStoreNumber();

                if (StringUtils.isNotBlank(putStoreNumber)) {
                    Integer iceBoxId = iceBox.getId();

                    IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(iceBoxId);

                    String lastApplyNumber = iceBoxExtend.getLastApplyNumber();

                    IcePutPactRecord icePutPactRecord = icePutPactRecordDao.selectOne(Wrappers.<IcePutPactRecord>lambdaQuery()
                            .eq(IcePutPactRecord::getBoxId, iceBoxId)
                            .eq(IcePutPactRecord::getApplyNumber, lastApplyNumber));
                    if (icePutPactRecord != null) {
                        Date putTime = icePutPactRecord.getPutTime();
                        Date putExpireTime = icePutPactRecord.getPutExpireTime();
                        String assetId = iceBoxExtend.getAssetId();
                        //????????????
                        addLabel(assetId, putStoreNumber, putTime, putExpireTime);
                    }
                }
            });
        }
    }


    private void addLabel(String assetId, String putStoreNumber, Date putTime, Date putExpireTime) {
        CustomerLabelDetailDto customerLabelDetailDto = new CustomerLabelDetailDto();
        customerLabelDetailDto.setLabelId(9999);
        customerLabelDetailDto.setCreateTime(putTime);
        customerLabelDetailDto.setCustomerNumber(putStoreNumber);
        customerLabelDetailDto.setCreateBy(0);
        customerLabelDetailDto.setCreateByName("??????");
        customerLabelDetailDto.setPutProject("??????");
        customerLabelDetailDto.setCancelTime(putExpireTime);
        customerLabelDetailDto.setRemarks(assetId);
        SubordinateInfoVo subordinateInfoVo = FeignResponseUtil.getFeignData(feignSupplierClient.findByNumber(putStoreNumber));
        if (subordinateInfoVo != null && StringUtils.isNotBlank(subordinateInfoVo.getNumber())) {
            customerLabelDetailDto.setCustomerType(1);
        } else {
            customerLabelDetailDto.setCustomerType(0);
        }
        feignCusLabelClient.createCustomerLabelDetail(customerLabelDetailDto);
    }

    @Override
    public void dealOldIceBoxNotice() {
        List<IceBox> iceBoxList = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getPutStatus, PutStatus.FINISH_PUT.getStatus())
                .eq(IceBox::getIceBoxType, IceBoxEnums.TypeEnum.OLD_ICE_BOX.getType()));
        if (CollectionUtil.isEmpty(iceBoxList)) {
            return;
        }
        List<IceBox> noNoticeIceBoxList = new ArrayList<>();
        for (IceBox iceBox : iceBoxList) {
            OldIceBoxSignNotice oldIceBoxSignNotice = oldIceBoxSignNoticeDao.selectOne(Wrappers.<OldIceBoxSignNotice>lambdaQuery().eq(OldIceBoxSignNotice::getIceBoxId, iceBox.getId()).eq(OldIceBoxSignNotice::getPutStoreNumber, iceBox.getPutStoreNumber()));
            if (oldIceBoxSignNotice == null) {
                noNoticeIceBoxList.add(iceBox);
            }
        }
        if (CollectionUtil.isEmpty(noNoticeIceBoxList)) {
            return;
        }
        for (IceBox iceBox : noNoticeIceBoxList) {
            //??????????????????
            String putStoreNumber = iceBox.getPutStoreNumber();
            if (StringUtils.isEmpty(putStoreNumber)) {
                continue;
            }
            String applyNumber = "PUT" + IdUtil.simpleUUID().substring(0, 29);
            Integer mainUserId = null;
            if (putStoreNumber.startsWith("C0")) {
                mainUserId = FeignResponseUtil.getFeignData(feignStoreClient.getMainSaleManId(putStoreNumber));
            } else {
                mainUserId = FeignResponseUtil.getFeignData(feignSupplierClient.getMainSaleManId(putStoreNumber));
            }

            IcePutApply icePutApply = IcePutApply.builder()
                    .applyNumber(applyNumber)
                    .putStoreNumber(iceBox.getPutStoreNumber())
                    .examineStatus(ExamineStatus.PASS_EXAMINE.getStatus())
                    .userId(iceBox.getUpdatedBy())
                    .createdBy(mainUserId)
                    .build();
            icePutApplyDao.insert(icePutApply);

            Date now = new Date();
            PutStoreRelateModel relateModel = PutStoreRelateModel.builder()
                    .putStoreNumber(iceBox.getPutStoreNumber())
                    .modelId(iceBox.getModelId())
                    .supplierId(iceBox.getSupplierId())
                    .createBy(mainUserId)
                    .createTime(now)
                    .putStatus(PutStatus.DO_PUT.getStatus())
                    .examineStatus(ExamineStatus.PASS_EXAMINE.getStatus())
                    .remark("?????????????????????????????????")
                    .build();
            putStoreRelateModelDao.insert(relateModel);

            ApplyRelatePutStoreModel applyRelatePutStoreModel = ApplyRelatePutStoreModel.builder()
                    .applyNumber(applyNumber)
                    .storeRelateModelId(relateModel.getId())
                    .freeType(FreePayTypeEnum.IS_FREE.getType())
                    .build();
            applyRelatePutStoreModelDao.insert(applyRelatePutStoreModel);

            IceBoxExtend iceBoxExtend = new IceBoxExtend();
            iceBoxExtend.setId(iceBox.getId());
            iceBoxExtend.setLastApplyNumber(applyNumber);
            iceBoxExtendDao.updateById(iceBoxExtend);

            IcePutApplyRelateBox icePutApplyRelateBox = icePutApplyRelateBoxDao.selectOne(Wrappers.<IcePutApplyRelateBox>lambdaQuery()
                    .eq(IcePutApplyRelateBox::getApplyNumber, iceBoxExtend.getLastApplyNumber())
                    .eq(IcePutApplyRelateBox::getBoxId, iceBox.getId()));
            if (icePutApplyRelateBox == null) {
                IcePutApplyRelateBox relateBox = new IcePutApplyRelateBox();
                relateBox.setApplyNumber(iceBoxExtend.getLastApplyNumber());
                relateBox.setFreeType(FreePayTypeEnum.IS_FREE.getType());
                relateBox.setBoxId(iceBox.getId());
                relateBox.setModelId(iceBox.getModelId());
                icePutApplyRelateBoxDao.insert(relateBox);
            }
            //??????mq??????,???????????????????????????
            Integer userId = mainUserId;
            CompletableFuture.runAsync(() -> {
                IceBoxRequestVo requestVo = new IceBoxRequestVo();
                requestVo.setMarketAreaId(iceBox.getDeptId());
                requestVo.setModelId(iceBox.getModelId());
                requestVo.setChestModel(iceBox.getModelName());
                requestVo.setDepositMoney(iceBox.getDepositMoney());
                requestVo.setFreeType(FreePayTypeEnum.IS_FREE.getType());
                requestVo.setStoreNumber(iceBox.getPutStoreNumber());
                requestVo.setStoreType(SupplierTypeEnum.IS_STORE.getType());
                requestVo.setUserId(userId);
                if (iceBox.getPutStoreNumber().contains("C7") || iceBox.getPutStoreNumber().contains("C8")) {
                    SubordinateInfoVo supplier = FeignResponseUtil.getFeignData(feignSupplierClient.findByNumber(iceBox.getPutStoreNumber()));
                    if (supplier != null) {
                        requestVo.setStoreName(supplier.getName());
                        requestVo.setStoreType(supplier.getSupplierType());
                    }
                } else {
                    StoreInfoDtoVo store = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(iceBox.getPutStoreNumber()));
                    if (store != null) {
                        requestVo.setStoreName(store.getStoreName());
                    }
                }
                requestVo.setSupplierId(iceBox.getSupplierId());
                buildReportAndSendMq(requestVo, applyNumber, now,0);
            }, ExecutorServiceFactory.getInstance());

            OldIceBoxSignNotice oldIceBoxSignNotice = new OldIceBoxSignNotice();
            oldIceBoxSignNotice.setApplyNumber(applyNumber);
            oldIceBoxSignNotice.setAssetId(iceBox.getAssetId());
            oldIceBoxSignNotice.setIceBoxId(iceBox.getId());
            oldIceBoxSignNotice.setPutStoreNumber(iceBox.getPutStoreNumber());
            oldIceBoxSignNotice.setCreateTime(new Date());
            oldIceBoxSignNoticeDao.insert(oldIceBoxSignNotice);
        }
    }

    @Override
    public IceBoxStatusVo checkIceBoxById(Integer id, String pxtNumber) {
        IceBoxStatusVo iceBoxStatusVo = new IceBoxStatusVo();
        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(id);
        log.info("??????????????????id--??????{}???,pxtNumber--??????{}???,????????????---???", id, pxtNumber, JSON.toJSONString(iceBoxExtend));
        return getIceBoxStatusVo(pxtNumber, iceBoxStatusVo, iceBoxExtend);
    }

    @Override
    public IceBoxVo getIceBoxById(Integer id, String pxtNumber) {
        IceBox iceBox = iceBoxDao.selectById(id);
        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(id);
        //IceBoxVo iceBoxVo = iceBoxService.getIceBoxVo(pxtNumber, iceBoxExtend, iceBox);
        /**
         * ????????????  ????????????????????????  ?????????????????????
         */
        IceBoxVo iceBoxVo = getIceBoxVoNew(pxtNumber, iceBoxExtend, iceBox);
        return iceBoxVo;
    }

    private void judgeChange(IceBoxManagerVo iceBoxManagerVo) {
        boolean modifyDept = iceBoxManagerVo.isModifyDept();
        boolean modifySupplier = iceBoxManagerVo.isModifySupplier();
        boolean modifyCustomer = iceBoxManagerVo.isModifyCustomer();
        if (modifyDept && !modifySupplier) {
            throw new NormalOptionException(Constants.API_CODE_FAIL, "?????????????????????????????????");
        }
        boolean result = iceBoxManagerVo.validateMain();
        if (!result) {
            throw new NormalOptionException(Constants.API_CODE_FAIL, "???????????????");
        }
    }

    private void convertToIceBoxChangeHistory(IceBox oldIceBox, IceBox newIcebox, IceBoxChangeHistory
            iceBoxChangeHistory, UserManageVo userManageVo) {
        iceBoxChangeHistory.setOldAssetId(oldIceBox.getAssetId());
        iceBoxChangeHistory.setOldBrandName(oldIceBox.getBrandName());
        iceBoxChangeHistory.setOldChestDepositMoney(oldIceBox.getDepositMoney());
        iceBoxChangeHistory.setOldChestMoney(oldIceBox.getChestMoney());
        iceBoxChangeHistory.setOldChestDepositMoney(oldIceBox.getDepositMoney());
        iceBoxChangeHistory.setOldMarketAreaId(oldIceBox.getDeptId());
        iceBoxChangeHistory.setOldModelId(oldIceBox.getModelId());
        iceBoxChangeHistory.setOldModelName(oldIceBox.getModelName());
        Integer oldSupplierId = oldIceBox.getSupplierId();
        iceBoxChangeHistory.setOldSupplierId(oldSupplierId);
        iceBoxChangeHistory.setOldChestNorm(oldIceBox.getChestNorm());
        iceBoxChangeHistory.setOldPutStoreNumber(oldIceBox.getPutStoreNumber());
        iceBoxChangeHistory.setOldChestName(oldIceBox.getChestName());
        iceBoxChangeHistory.setOldStatus(oldIceBox.getStatus());
        iceBoxChangeHistory.setOldRemake(oldIceBox.getRemark());


        iceBoxChangeHistory.setNewAssetId(newIcebox.getAssetId());
        iceBoxChangeHistory.setNewBrandName(newIcebox.getBrandName());
        iceBoxChangeHistory.setNewChestDepositMoney(newIcebox.getDepositMoney());
        iceBoxChangeHistory.setNewChestMoney(newIcebox.getChestMoney());
        iceBoxChangeHistory.setNewChestDepositMoney(newIcebox.getDepositMoney());
        iceBoxChangeHistory.setNewMarketAreaId(newIcebox.getDeptId());
        iceBoxChangeHistory.setNewModelId(newIcebox.getModelId());
        iceBoxChangeHistory.setNewModelName(newIcebox.getModelName());
        Integer newSupplierId = newIcebox.getSupplierId();
        iceBoxChangeHistory.setNewSupplierId(newSupplierId);
        iceBoxChangeHistory.setNewChestNorm(newIcebox.getChestNorm());
        iceBoxChangeHistory.setNewPutStoreNumber(newIcebox.getPutStoreNumber());
        iceBoxChangeHistory.setNewChestName(newIcebox.getChestName());
        iceBoxChangeHistory.setNewStatus(newIcebox.getStatus());
        iceBoxChangeHistory.setNewRemake(newIcebox.getRemark());

        iceBoxChangeHistory.setCreateBy(userManageVo.getSessionUserInfoVo().getId());
        iceBoxChangeHistory.setCreateByName(userManageVo.getSessionUserInfoVo().getRealname());


        iceBoxChangeHistory.setIceBoxId(oldIceBox.getId());

        List<Integer> list = new ArrayList<>();
        list.add(oldSupplierId);
        list.add(newSupplierId);

        Map<Integer, SubordinateInfoVo> map = FeignResponseUtil.getFeignData(feignSupplierClient.findByIds(list));


        iceBoxChangeHistory.setOldSupplierName(null == map.get(oldSupplierId) ? "" : map.get(oldSupplierId).getName());
        iceBoxChangeHistory.setNewSupplierName(null == map.get(newSupplierId) ? "" : map.get(newSupplierId).getName());
        iceBoxChangeHistory.setOldSupplierNumber(null == map.get(oldSupplierId) ? "" : map.get(oldSupplierId).getNumber());
        iceBoxChangeHistory.setNewSupplierNumber(null == map.get(newSupplierId) ? "" : map.get(newSupplierId).getNumber());
        iceBoxChangeHistory.setCreateTime(new Date());

        iceBoxChangeHistoryDao.insert(iceBoxChangeHistory);
    }

    @Override
    public List<Map<String, String>> findIceBoxsModelBySupplierId(Integer supplierId) {
        List<Map<String, String>> mapList = new ArrayList<>();
        List<IceBox> iceBoxList = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getSupplierId, supplierId));
        if (CollectionUtil.isEmpty(iceBoxList)) {
            return mapList;
        }
        Map<Integer, List<IceBox>> groupMap = iceBoxList.stream().collect(Collectors.groupingBy(IceBox::getModelId));
        for (Integer modelId : groupMap.keySet()) {
            IceBox iceBox = groupMap.get(modelId).get(0);
            Map<String, String> map = new HashMap<>();
            map.put("modelId", modelId + "");
            map.put("modelName", iceBox.getModelName());
            mapList.add(map);
        }
        return mapList;
    }

    @Override
    public List<IceBoxVo> findIceBoxsBySupplierIdAndModelId(Integer supplierId, Integer modelId) {
        List<IceBox> iceBoxList = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getSupplierId, supplierId).eq(IceBox::getModelId, modelId));
        if (CollectionUtil.isEmpty(iceBoxList)) {
            return null;
        }
        List<IceBoxVo> iceBoxVoList = new ArrayList<>();
        for (IceBox iceBox : iceBoxList) {
            IceBoxVo iceBoxVo = new IceBoxVo();
            BeanUtils.copyProperties(iceBox, iceBoxVo);
            iceBoxVo.setChestModel(iceBox.getModelName());
            LambdaQueryWrapper<IceBoxTransferHistory> wrapper = Wrappers.<IceBoxTransferHistory>lambdaQuery();
            wrapper.eq(IceBoxTransferHistory::getIceBoxId, iceBox.getId());
            wrapper.and(x -> x.eq(IceBoxTransferHistory::getExamineStatus, ExamineStatus.DEFAULT_EXAMINE.getStatus()).or().eq(IceBoxTransferHistory::getExamineStatus, ExamineStatus.DOING_EXAMINE.getStatus()));
            IceBoxTransferHistory history = iceBoxTransferHistoryDao.selectOne(wrapper);
            if (history != null) {
                iceBoxVo.setExamineStatus(history.getExamineStatus());
            }
            iceBoxVoList.add(iceBoxVo);
        }
        return iceBoxVoList;
    }

    @Override
    public JSONObject setAssetReportJson(IceBox iceBox, String resourceStr) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("suppId", iceBox.getSupplierId());
        jsonObject.put("modelId", iceBox.getModelId());
        jsonObject.put("deptId", iceBox.getDeptId());
        jsonObject.put("resourceStr", resourceStr); // ????????????
        jsonObject.put(IceBoxConstant.methodName, MethodNameOfMQ.CREATE_ICE_BOX_ASSETS_REPORT);
        return jsonObject;
    }


    @Override
    public int getLostScrapCount(List<Integer> putBoxIds) {
        if(CollectionUtils.isEmpty(putBoxIds)){
            return 0;
        }
        LambdaQueryWrapper<IceBox> iceBoxWrapper = Wrappers.<IceBox>lambdaQuery();
        iceBoxWrapper.in(IceBox::getStatus,IceBoxEnums.StatusEnum.SCRAP.getType(),IceBoxEnums.StatusEnum.LOSE.getType())
                .in(IceBox::getId, putBoxIds);
        return iceBoxDao.selectCount(iceBoxWrapper);
    }

    @Override
    public int getLostScrapCount(Integer userId) {
        List<Integer> putBoxIds = getPutBoxIds(userId);
        return getLostScrapCount(putBoxIds);
    }

    @Override
    public List<Integer> getPutBoxIds(Integer userId) {
        List<String> numbers = FeignResponseUtil.getFeignData(feignSupplierRelateUserClient.getMainCustomerNumber(userId));
        if(CollectionUtils.isEmpty(numbers)){
            return Lists.newArrayList();
        }
        LambdaQueryWrapper<IceBox> iceBoxWrapper = Wrappers.<IceBox>lambdaQuery();
        iceBoxWrapper.select(IceBox::getId).eq(IceBox::getPutStatus,3).in(IceBox::getPutStoreNumber,numbers);
        return iceBoxDao.selectList(iceBoxWrapper).stream().map(IceBox::getId).collect(Collectors.toList());
    }

    @Override
    public List<Integer> getNormalPutBoxIds(Integer userId) {
        List<String> numbers = FeignResponseUtil.getFeignData(feignSupplierRelateUserClient.getMainCustomerNumber(userId));
        if(CollectionUtils.isEmpty(numbers)){
            return Lists.newArrayList();
        }
        LambdaQueryWrapper<IceBox> iceBoxWrapper = Wrappers.<IceBox>lambdaQuery();
        iceBoxWrapper.select(IceBox::getId)
                .eq(IceBox::getPutStatus,3)
                .in(IceBox::getPutStoreNumber,numbers)
                .notIn(IceBox::getStatus,IceBoxEnums.StatusEnum.SCRAP.getType(),IceBoxEnums.StatusEnum.LOSE.getType());
        return iceBoxDao.selectList(iceBoxWrapper).stream().map(IceBox::getId).collect(Collectors.toList());
    }
    /**
     * ?????????????????????????????????
     *
     * @param
     */
    @Override
    public void changeCustomer(IceBox newIceBox, IceBox oldIceBox) {
        UserManageVo userManageVo = FeignResponseUtil.getFeignData(feignUserClient.getSessionUserInfo());
        //????????????????????????
        Integer oldPutStatus = oldIceBox.getPutStatus();
        String newPutStoreNumber = newIceBox.getPutStoreNumber(); //??????????????????
        String oldApplyNumber = "";
        String newApplyNumber = "";
        Boolean isPush = false; //??????????????????????????????

        if (!newPutStoreNumber.equals(oldIceBox.getPutStoreNumber())) {
            if (PutStatus.NO_PUT.getStatus().equals(oldPutStatus)) {

                // ???????????????  ?????????????????????????????????????????????????????? ????????????
                // ????????????????????????
                // ??????????????????????????????
                // ??????????????????
                newApplyNumber = iceBoxService.createIcePutData(newIceBox, newPutStoreNumber);

                // ???????????????????????????
                iceBoxService.saveIceBoxPutReport(newIceBox, newApplyNumber, newPutStoreNumber);
                isPush = true;
            } else if (PutStatus.FINISH_PUT.getStatus().equals(oldPutStatus)) {
                // ?????????????????????????????????????????????????????????, ?????????????????????????????? TODO
                // ????????????????????????  ?????????????????????????????????????????? ??????????????????
                // ????????????????????? ??????????????????????????? (???????????????)
                IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectOne(Wrappers.<IceBoxExtend>lambdaQuery().eq(IceBoxExtend::getId, newIceBox.getId()));
                String lastApplyNumber = iceBoxExtend.getLastApplyNumber();
                oldApplyNumber = lastApplyNumber;
                if (StringUtils.isNotBlank(lastApplyNumber)) {
                    //?????????????????????????????? ??????????????????
                    IcePutApplyRelateBox icePutApplyRelate = icePutApplyRelateBoxDao.selectOne(Wrappers.<IcePutApplyRelateBox>lambdaQuery()
                            .eq(IcePutApplyRelateBox::getApplyNumber, lastApplyNumber)
                            .eq(IcePutApplyRelateBox::getBoxId, oldIceBox.getId())
                    );

                    // ????????????????????????????????????ID
                    List<Integer> storeRelateModelIds = new ArrayList<>();
                    List<ApplyRelatePutStoreModel> applyRelatePutStoreModels = applyRelatePutStoreModelDao.selectList(Wrappers.<ApplyRelatePutStoreModel>lambdaQuery()
                                    .eq(ApplyRelatePutStoreModel::getApplyNumber, lastApplyNumber)
                                    .eq(ApplyRelatePutStoreModel::getFreeType, icePutApplyRelate.getFreeType()));

                    if (applyRelatePutStoreModels.size() > 0){
                        applyRelatePutStoreModels.forEach(applyRelatePutStoreModel -> {
                            storeRelateModelIds.add(applyRelatePutStoreModel.getStoreRelateModelId());
                        });
                    }

                    //?????? ????????????????????????????????????????????????
                    List<PutStoreRelateModel> putStoreRelateModels = putStoreRelateModelDao.selectList(Wrappers.<PutStoreRelateModel>lambdaQuery()
                            .in(PutStoreRelateModel::getId, storeRelateModelIds));
                    for (PutStoreRelateModel putStoreRelateModel : putStoreRelateModels) {
                        if (putStoreRelateModel.getPutStoreNumber().equals(oldIceBox.getPutStoreNumber())
                                && putStoreRelateModel.getModelId().equals(oldIceBox.getModelId())
                                && putStoreRelateModel.getSupplierId().equals(oldIceBox.getSupplierId())
                                && putStoreRelateModel.getPutStatus().equals(PutStatus.FINISH_PUT.getStatus())
                                && putStoreRelateModel.getStatus().equals(IsValidEnum.IS_VALID.getStatus())
                        ){
                            putStoreRelateModelDao.update(null, Wrappers.<PutStoreRelateModel>lambdaUpdate()
                                    .eq(PutStoreRelateModel::getId, putStoreRelateModel.getId())
                                    .set(PutStoreRelateModel::getStatus, IsValidEnum.NO_VALID.getStatus())
                                    .set(PutStoreRelateModel::getRemark, "??????????????????????????????")
                                    .set(PutStoreRelateModel::getUpdateTime, new Date())
                                    .set(PutStoreRelateModel::getUpdateBy, userManageVo.getSessionUserInfoVo().getId()));
                            break;
                        }
                    }

                    // ????????????????????????
                    newApplyNumber = createIcePutData(newIceBox, newPutStoreNumber);

                    // ????????????????????????
                    IceBoxCustomerVo iceBoxCustomerVo = this.getIceBoxCustomerVo(newPutStoreNumber);
                    iceBoxPutReportDao.update(null, Wrappers.<IceBoxPutReport>lambdaUpdate()
                            .eq(IceBoxPutReport::getApplyNumber, lastApplyNumber)
                            .eq(IceBoxPutReport::getIceBoxId, oldIceBox.getId())
                            .set(IceBoxPutReport::getFreeType, FreePayTypeEnum.IS_FREE.getType())
                            .set(IceBoxPutReport::getPutCustomerNumber, newPutStoreNumber)
                            .set(IceBoxPutReport::getPutCustomerName, iceBoxCustomerVo.getCustomerName())
                            .set(IceBoxPutReport::getPutCustomerType, iceBoxCustomerVo.getSupplierType())
                            .set(IceBoxPutReport::getSubmitterId, iceBoxCustomerVo.getMainSalesmanId())
                            .set(IceBoxPutReport::getSubmitterName, iceBoxCustomerVo.getMainSalesmanName())
                            .set(IceBoxPutReport::getApplyNumber, newApplyNumber)
                    );
                }
                isPush = true;
            }
            if (isPush) {
                if(IceBoxEnums.TypeEnum.OLD_ICE_BOX.getType().equals(oldIceBox.getIceBoxType())) {
                    // ???????????????????????????????????????????????????
                    oldIceBoxSignNoticeDao.update(null, Wrappers.<OldIceBoxSignNotice>lambdaUpdate()
                            .eq(OldIceBoxSignNotice::getApplyNumber, oldApplyNumber)
                            .set(OldIceBoxSignNotice::getStatus, OldIceBoxSignNoticeStatusEnums.IS_SIGNED.getStatus()));

                    // ??????????????????
                    OldIceBoxSignNotice oldIceBoxSignNotice = new OldIceBoxSignNotice();
                    oldIceBoxSignNotice.setApplyNumber(newApplyNumber);
                    oldIceBoxSignNotice.setIceBoxId(newIceBox.getId());
                    oldIceBoxSignNotice.setAssetId(newIceBox.getAssetId());
                    oldIceBoxSignNotice.setPutStoreNumber(newPutStoreNumber);
                    oldIceBoxSignNotice.setCreateTime(new Date());
                    oldIceBoxSignNoticeDao.insert(oldIceBoxSignNotice);
                }
            }
        }
    }

    /**
     * ????????????????????????
     * @param iceBox
     */
    @Override
    public String createIcePutData(IceBox iceBox, String newPutStoreNumber){
        UserManageVo userManageVo = FeignResponseUtil.getFeignData(feignUserClient.getSessionUserInfo());
        // ???????????????  ?????????????????????????????????????????????????????? ????????????
        // ????????????????????????
        // ??????????????????????????????
        // ??????????????????
        // ???????????????????????????
        Integer optUserId = userManageVo.getSessionUserInfoVo().getId();
        String applyNumber = "PUT" + IdUtil.simpleUUID().substring(0, 29);
        IcePutApply icePutApply = IcePutApply.builder()
                .applyNumber(applyNumber)
                .putStoreNumber(newPutStoreNumber)
                .examineStatus(ExamineStatus.PASS_EXAMINE.getStatus())
                .userId(optUserId)
                .createdBy(optUserId)
                .build();
        icePutApplyDao.insert(icePutApply);

        Date now = new Date();
        PutStoreRelateModel relateModel = PutStoreRelateModel.builder()
                .putStoreNumber(newPutStoreNumber)
                .modelId(iceBox.getModelId())
                .supplierId(iceBox.getSupplierId())
                .createBy(optUserId)
                .createTime(now)
                .putStatus(PutStatus.FINISH_PUT.getStatus())
                .examineStatus(ExamineStatus.PASS_EXAMINE.getStatus())
                .remark("??????????????????????????????")
                .build();
        putStoreRelateModelDao.insert(relateModel);

        ApplyRelatePutStoreModel applyRelatePutStoreModel = ApplyRelatePutStoreModel.builder()
                .applyNumber(applyNumber)
                .storeRelateModelId(relateModel.getId())
                .freeType(FreePayTypeEnum.IS_FREE.getType())
                .build();
        applyRelatePutStoreModelDao.insert(applyRelatePutStoreModel);

        IceBoxExtend iceBoxExtend = new IceBoxExtend();
        iceBoxExtend.setId(iceBox.getId());
        iceBoxExtend.setLastApplyNumber(applyNumber);
        iceBoxExtendDao.updateById(iceBoxExtend);

        IcePutApplyRelateBox icePutApplyRelateBox = icePutApplyRelateBoxDao.selectOne(Wrappers.<IcePutApplyRelateBox>lambdaQuery()
                .eq(IcePutApplyRelateBox::getApplyNumber, iceBoxExtend.getLastApplyNumber())
                .eq(IcePutApplyRelateBox::getBoxId, iceBox.getId()));
        if (icePutApplyRelateBox == null) {
            IcePutApplyRelateBox relateBox = new IcePutApplyRelateBox();
            relateBox.setApplyNumber(iceBoxExtend.getLastApplyNumber());
            relateBox.setFreeType(FreePayTypeEnum.IS_FREE.getType());
            relateBox.setBoxId(iceBox.getId());
            relateBox.setModelId(iceBox.getModelId());
            icePutApplyRelateBoxDao.insert(relateBox);
        }

        return applyNumber;
    }

    /**
     * ????????????????????????
     * @param iceBox ????????????
     * @param applyNumber ????????????ID
     * @param putStoreNumber ??????????????????
     */
    @Override
    public void saveIceBoxPutReport(IceBox iceBox, String applyNumber, String putStoreNumber){
        IceBoxPutReportMsg report = new IceBoxPutReportMsg();
        //????????????????????????
        IceBoxCustomerVo iceBoxCustomerVo = this.getIceBoxCustomerVo(putStoreNumber);
        report.setPutCustomerType(iceBoxCustomerVo.getSupplierType());
        report.setSubmitterId(iceBoxCustomerVo.getMainSalesmanId());
        report.setSubmitterName(iceBoxCustomerVo.getMainSalesmanName());
        report.setPutCustomerNumber(putStoreNumber);
        report.setPutCustomerName(iceBoxCustomerVo.getCustomerName());

        //??????????????????
        Map<Integer, SessionDeptInfoVo> deptInfoVoMap = FeignResponseUtil.getFeignData(feignCacheClient.getFiveLevelDept(iceBoxCustomerVo.getMarketArea()));
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

        report.setApplyNumber(applyNumber);
        report.setFreeType(FreePayTypeEnum.IS_FREE.getType());
        report.setIceBoxModelId(iceBox.getModelId());
        report.setIceBoxModelName(iceBox.getModelName());
        report.setIceBoxId(iceBox.getId());
        report.setIceBoxAssetId(iceBox.getAssetId());
        SubordinateInfoVo supplier = FeignResponseUtil.getFeignData(feignSupplierClient.findSupplierBySupplierId(iceBox.getSupplierId()));
        report.setSupplierId(iceBox.getSupplierId());
        if (supplier != null) {
            report.setSupplierNumber(supplier.getNumber());
            report.setSupplierName(supplier.getName());
        }

        report.setSubmitTime(new Date());
        report.setPutStatus(PutStatus.FINISH_PUT.getStatus());
        report.setOperateType(OperateTypeEnum.INSERT.getType());
        rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.iceboxReportKey, report);
    }

    /**
     * ??????????????????
     * @param putStoreNumber
     * @return
     */
    @Override
    public IceBoxCustomerVo getIceBoxCustomerVo(String putStoreNumber){
        IceBoxCustomerVo iceBoxCustomerVo = new IceBoxCustomerVo();
        Integer marketArea = null;
        String customerName = "";
        Integer supplierType = null;
        Integer mainSaleManId = null;
        String mainSaleManName = "";

        //??????????????????
        StoreInfoDtoVo storeInfoDtoVo = feignStoreClient.getByStoreNumber(putStoreNumber).getData();
        if (!Objects.isNull(storeInfoDtoVo)){
            // ??????
            marketArea = storeInfoDtoVo.getMarketArea();
            customerName = storeInfoDtoVo.getStoreName();
            supplierType = SupplierTypeEnum.IS_STORE.getType();
            mainSaleManId = storeInfoDtoVo.getMainSaleManId();
            mainSaleManName = storeInfoDtoVo.getMainSaleManName();
        } else {
            // ?????????
            SubordinateInfoVo subordinateInfoVo = feignSupplierClient.findByNumber(putStoreNumber).getData();
            marketArea = subordinateInfoVo.getMarketAreaId();
            customerName = subordinateInfoVo.getName();
            supplierType = subordinateInfoVo.getSupplierType();

            mainSaleManId = feignSupplierClient.getMainSaleManId(putStoreNumber).getData();
            SimpleUserInfoVo userInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findUserById(mainSaleManId));
            if (userInfoVo != null) {
                mainSaleManName = userInfoVo.getRealname();
            }
        }
        iceBoxCustomerVo.setCustomerName(customerName);
        iceBoxCustomerVo.setMarketArea(marketArea);
        iceBoxCustomerVo.setMainSalesmanId(mainSaleManId);
        iceBoxCustomerVo.setMainSalesmanName(mainSaleManName);
        iceBoxCustomerVo.setSupplierType(supplierType);
        return iceBoxCustomerVo;
    }

    @Override
    public void createOldIceBoxSignNotice(IceBox iceBox, String applyNumber, String storeNumber) {
        // ??????????????????
        OldIceBoxSignNotice oldIceBoxSignNotice = new OldIceBoxSignNotice();
        oldIceBoxSignNotice.setApplyNumber(applyNumber);
        oldIceBoxSignNotice.setIceBoxId(iceBox.getId());
        oldIceBoxSignNotice.setAssetId(iceBox.getAssetId());
        oldIceBoxSignNotice.setPutStoreNumber(storeNumber);
        oldIceBoxSignNotice.setCreateTime(new Date());
        oldIceBoxSignNoticeDao.insert(oldIceBoxSignNotice);
    }

    @Override
    public Map<String, Object> checkApplyStatus(List<IceBoxRequestVo> iceBoxRequestVos) {
        Map responseMap = new HashMap();
        responseMap.put("flag",true);
        responseMap.put("message","");
        if(iceBoxRequestVos.size() > 0){
            for(IceBoxRequestVo iceBoxRequestVo : iceBoxRequestVos){
                if(iceBoxRequestVo != null){
                    List<PutStoreRelateModel> putStoreRelateModels = putStoreRelateModelDao.selectList(Wrappers.<PutStoreRelateModel>lambdaQuery().eq(PutStoreRelateModel::getSupplierId, iceBoxRequestVo.getSupplierId()).eq(PutStoreRelateModel::getModelId, iceBoxRequestVo.getModelId()).eq(PutStoreRelateModel::getPutStoreNumber, iceBoxRequestVo.getStoreNumber()).eq(PutStoreRelateModel::getStatus,1).and(x -> x.eq(PutStoreRelateModel::getPutStatus,PutStatus.DO_PUT.getStatus()).or().eq(PutStoreRelateModel::getPutStatus,PutStatus.LOCK_PUT.getStatus())));
                    if(putStoreRelateModels.size() > 0){
                        IceModel iceModel = iceModelDao.selectById(putStoreRelateModels.get(0).getModelId());
                        CommonResponse<UserInfoVo> commonResponse = feignUserClient.findById(putStoreRelateModels.get(0).getCreateBy());
                        responseMap.put("flag",false);
                        responseMap.put("message","???????????????"+iceModel.getChestName()+"????????????????????????????????????:"+commonResponse.getData().getRealname()+",?????????????????????????????????????????????");
                    }
                }
            }
        }
        return responseMap;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, value = "transactionManager")
    public void helpSignIcebox(String assestId, String applyNumber) {
        if(StringUtils.isEmpty(assestId) || StringUtils.isEmpty(assestId)){
            throw new NormalOptionException(Constants.API_CODE_FAIL,"??????????????????");
        }
        IceBox iceBox = iceBoxDao.selectOne(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getAssetId, assestId));
        if(iceBox == null){
            throw new NormalOptionException(Constants.API_CODE_FAIL,"???????????????");
        }
        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectOne(Wrappers.<IceBoxExtend>lambdaQuery().eq(IceBoxExtend::getAssetId, assestId));
        if(iceBoxExtend == null){
            throw new NormalOptionException(Constants.API_CODE_FAIL,"???????????????????????????");
        }
        IcePutApplyRelateBox relateBox = icePutApplyRelateBoxDao.selectOne(Wrappers.<IcePutApplyRelateBox>lambdaQuery().eq(IcePutApplyRelateBox::getBoxId,iceBox.getId()).eq(IcePutApplyRelateBox::getApplyNumber,applyNumber));
        IcePutApply icePutApply = icePutApplyDao.selectOne(Wrappers.<IcePutApply>lambdaQuery().eq(IcePutApply::getApplyNumber, applyNumber));
        if(icePutApply == null){
            throw new NormalOptionException(Constants.API_CODE_FAIL,"?????????????????????");
        }
        List<ApplyRelatePutStoreModel> applyRelatePutStoreModels = applyRelatePutStoreModelDao.selectList(Wrappers.<ApplyRelatePutStoreModel>lambdaQuery().eq(ApplyRelatePutStoreModel::getApplyNumber, applyNumber));
        if(applyRelatePutStoreModels.size()==0){
            throw new NormalOptionException(Constants.API_CODE_FAIL,"?????????????????????????????????");
        }
        List<Integer> storeRelateModelIds = applyRelatePutStoreModels.stream().map(o -> o.getStoreRelateModelId()).collect(Collectors.toList());
        List<PutStoreRelateModel> putStoreRelateModels = putStoreRelateModelDao.selectList(Wrappers.<PutStoreRelateModel>lambdaQuery().eq(PutStoreRelateModel::getModelId, iceBox.getModelId()).eq(PutStoreRelateModel::getSupplierId, iceBox.getSupplierId()).in(PutStoreRelateModel::getId, storeRelateModelIds).ne(PutStoreRelateModel::getPutStatus,PutStatus.FINISH_PUT.getStatus()).orderByDesc(PutStoreRelateModel::getId));
        if (putStoreRelateModels.size()==0){
            throw new NormalOptionException(Constants.API_CODE_FAIL,"??????????????????????????????");
        }
        PutStoreRelateModel putStoreRelateModel = putStoreRelateModels.get(0);
        IceBoxPutReport report = iceBoxPutReportDao.selectOne(Wrappers.<IceBoxPutReport>lambdaQuery().eq(IceBoxPutReport::getPutStoreModelId,putStoreRelateModel.getId()));

        /**????????????
         *
         */
        iceBox.setPutStatus(PutStatus.FINISH_PUT.getStatus()).setPutStoreNumber(putStoreRelateModel.getPutStoreNumber());
        iceBoxDao.updateById(iceBox);
        log.info("icebox update id???{}???",iceBox.getId());

        iceBoxExtend.setLastPutId(icePutApply.getId()).setLastApplyNumber(applyNumber);
        iceBoxExtendDao.updateById(iceBoxExtend);

        icePutApply.setStoreSignStatus(1);
        icePutApplyDao.updateById(icePutApply);

        putStoreRelateModel.setPutStatus(PutStatus.FINISH_PUT.getStatus());
        putStoreRelateModel.setExamineStatus(2);
        putStoreRelateModel.setUpdateTime(new Date());
        putStoreRelateModel.setSignTime(new Date());
        if(putStoreRelateModel.getStatus().equals(0)){
            log.info("putStoreRelateModel????????????,id???->{}",putStoreRelateModel.getId());
            putStoreRelateModel.setStatus(1);
        }
        putStoreRelateModelDao.updateById(putStoreRelateModel);

        if(report != null){
            report.setIceBoxId(iceBox.getId());
            report.setIceBoxAssetId(iceBox.getAssetId());
            report.setPutStatus(PutStatus.FINISH_PUT.getStatus());
            report.setSignTime(new Date());
            iceBoxPutReportDao.updateById(report);
        }

        if(relateBox == null){
            IcePutApplyRelateBox newRelateBox = IcePutApplyRelateBox.builder()
                    .applyNumber(applyNumber)
                    .boxId(iceBox.getId())
                    .modelId(iceBox.getModelId())
                    .freeType(applyRelatePutStoreModels.get(0).getFreeType())
                    .build();
            icePutApplyRelateBoxDao.insert(newRelateBox);
            log.info("icePutApplyRelateBox insert id:{}",newRelateBox.getId());
        }
    }

    @Override
    public List<IceBox> getByResponsmanId(Integer userId) {
        List<IceBox> boxList = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getResponseManId, userId).eq(IceBox::getPutStatus, 3).eq(IceBox::getStatus, 1));
        //????????????????????????
        Calendar cale = Calendar.getInstance();
        cale.add(Calendar.MONTH, -1);
        cale.set(Calendar.DAY_OF_MONTH,1);
        cale.set(Calendar.HOUR_OF_DAY,0);
        cale.set(Calendar.MINUTE,0);
        cale.set(Calendar.SECOND,0);
        Date monthStart = cale.getTime();
        //???????????????????????????
        Calendar cale2 = Calendar.getInstance();
        cale2.set(Calendar.DAY_OF_MONTH,0);
        cale2.set(Calendar.HOUR_OF_DAY,23);
        cale2.set(Calendar.MINUTE,59);
        cale2.set(Calendar.SECOND,59);
        Date monthEnd = cale2.getTime();

        if(CollectionUtil.isNotEmpty(boxList)){
            List<IceBox> boxListFor = new ArrayList<>(boxList);

            List<ExamineError> examineErrors = examineErrorMapper.selectList(Wrappers.<ExamineError>lambdaQuery().eq(ExamineError::getCreateUserId, userId).ge(ExamineError::getCreateTime, monthStart).le(ExamineError::getCreateTime, monthEnd).eq(ExamineError::getPassStatus,1));
            if(CollectionUtil.isNotEmpty(examineErrors)){
                List<String> boxs = examineErrors.stream().map(ExamineError::getBoxAssetid).collect(Collectors.toList());
                for (String box : boxs){
                    for(IceBox i : boxListFor){
                        if(i.getAssetId().equals(box)){
                            boxList.remove(i);
                        }
                    }
                }
            }
        }

        return boxList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, transactionManager = "transactionManager")
    public void handelIceBoxDate(MultipartFile file) throws IOException {
        //???????????? 0:?????????1:?????????2:?????????3:?????????4:??????
        List<IceBoxExceptionDateVo> data = null;
//        try {
            data = EasyExcel.read(file.getInputStream()).head(IceBoxExceptionDateVo.class).sheet().headRowNumber(2).doReadSync();
            log.info("?????????????????????{}", JSON.toJSONString(data));
            if (org.apache.commons.collections4.CollectionUtils.isEmpty(data)){
                throw new ImproperOptionException("??????????????????");
            }
        /*} catch (Exception e) {
            log.error("??????????????????????????????");
            throw new ImproperOptionException("??????????????????????????????");
        }*/
        for (IceBoxExceptionDateVo iceBoxExceptionDate : data) {
            if (StringUtils.isBlank(iceBoxExceptionDate.getImportType()) || StringUtils.isBlank(iceBoxExceptionDate.getAssetId())) {
                continue;
            }
            //????????????????????? ?????????????????????????????????
            IceBox iceBox = iceBoxDao.selectOne(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getAssetId, iceBoxExceptionDate.getAssetId()));
            IceBoxExtend iceBoxExtend = null;
            if (Objects.nonNull(iceBox)) {
                iceBoxExtend = iceBoxExtendDao.selectOne(Wrappers.<IceBoxExtend>lambdaQuery().eq(IceBoxExtend::getId, iceBox.getId()));
            }
            IceModel iceModel = iceModelDao.selectOne(Wrappers.<IceModel>lambdaQuery().eq(IceModel::getChestModel, iceBoxExceptionDate.getIceModel()));
            SupplierInfoSessionVo supplierInfo = FeignResponseUtil.getFeignData(feignSupplierClient.getSuppliserInfoByNumber(iceBoxExceptionDate.getNumber()));
            Integer type = IceExceptionDataEnum.getEnumType(iceBoxExceptionDate.getImportType());
            IceBox box = new IceBox();
            IceBoxExtend boxExtend = new IceBoxExtend();
            switch (type) {
                case 1:
                    //1????????????????????????????????????last_put_id???last_apply_number???????????????????????????  ????????????
                    if (iceBoxExtend.getLastPutId() != 0) {
                        iceBoxExtendDao.update(iceBoxExtend, Wrappers.<IceBoxExtend>lambdaUpdate().eq(IceBoxExtend::getId, iceBox.getId()).set(IceBoxExtend::getLastPutId, 0).set(IceBoxExtend::getLastApplyNumber, null));
                    }
                    //2??????????????????????????????????????????0 ???????????????put_store_number????????????null;
                    iceBox.setPutStatus(IceBoxStatus.NO_PUT.getStatus());
                    iceBox.setPutStoreNumber(null);
                    iceBoxDao.updateById(iceBox);
                    break;
                case 2:
                    //?????????????????????????????????2
                    iceBox.setStatus(IceBoxEnums.StatusEnum.SCRAP.getType());
                    iceBoxDao.updateById(iceBox);
                    break;
                case 3:
                    //?????????????????????????????????3
                    iceBox.setStatus(IceBoxEnums.StatusEnum.LOSE.getType());
                    iceBoxDao.updateById(iceBox);
                    break;
                case 4:
                    iceBox.setPutStatus(IceBoxStatus.IS_PUTED.getStatus());
                    iceBox.setPutStoreNumber(iceBoxExceptionDate.getStoreNumber());
                    iceBoxDao.updateById(iceBox);
                    break;
                case 5:
                    //1????????? ?????????????????????????????????last_put_id???last_apply_number???????????????????????????  ????????????
                    if (iceBoxExtend.getLastPutId() != 0) {
                        iceBoxExtendDao.update(iceBoxExtend, Wrappers.<IceBoxExtend>lambdaUpdate().eq(IceBoxExtend::getId, iceBox.getId()).set(IceBoxExtend::getLastPutId, 0).set(IceBoxExtend::getLastApplyNumber, null));
                    }
                    iceBox.setPutStatus(IceBoxStatus.NO_PUT.getStatus());
                    iceBox.setPutStoreNumber(null);
                    iceBox.setStatus(IceBoxEnums.StatusEnum.SCRAP.getType());
                    iceBoxDao.updateById(iceBox);
                    break;
                case 6:
                    //(chest_name,model_id,brand_name,chest_norm,deposit_money,supplier_id,dept_id,put_status,status,create_time,updated_time,asset_id,model_name,ice_box_type)
                    box.setChestName(iceBoxExceptionDate.getChestName())
                            .setModelId(iceModel.getId())
                            .setBrandName(iceBoxExceptionDate.getBrandName())
                            .setChestNorm(iceBoxExceptionDate.getChestNorm())
                            .setDepositMoney(iceBoxExceptionDate.getChestMoney())
                            .setSupplierId(supplierInfo.getId())
                            .setDeptId(supplierInfo.getMarketAreaId())
                            .setPutStatus(IceBoxStatus.NO_PUT.getStatus())
                            .setStatus(IceBoxEnums.StatusEnum.NORMAL.getType())
                            .setCreatedTime(now())
                            .setUpdatedTime(now())
                            .setAssetId(iceBoxExceptionDate.getAssetId())
                            .setIceBoxType(0);
                    iceBoxDao.insert(box);
                    boxExtend.setId(box.getId()).setAssetId(iceBoxExceptionDate.getAssetId());
                    iceBoxExtendDao.insert(boxExtend);
                    break;
                case 7:
                    box.setChestName(iceBoxExceptionDate.getChestName())
                            .setModelId(iceModel.getId())
                            .setBrandName(iceBoxExceptionDate.getBrandName())
                            .setChestNorm(iceBoxExceptionDate.getChestNorm())
                            .setDepositMoney(iceBoxExceptionDate.getChestMoney())
                            .setSupplierId(supplierInfo.getId())
                            .setDeptId(supplierInfo.getMarketAreaId())
                            .setPutStatus(IceBoxStatus.NO_PUT.getStatus())
                            .setStatus(IceBoxEnums.StatusEnum.SCRAP.getType())
                            .setCreatedTime(now())
                            .setUpdatedTime(now())
                            .setAssetId(iceBoxExceptionDate.getAssetId())
                            .setIceBoxType(0);
                    iceBoxDao.insert(box);
                    boxExtend.setId(box.getId()).setAssetId(iceBoxExceptionDate.getAssetId());
                    iceBoxExtendDao.insert(boxExtend);
                    break;
                case 8:
                    box.setChestName(iceBoxExceptionDate.getChestName())
                            .setModelId(iceModel.getId())
                            .setBrandName(iceBoxExceptionDate.getBrandName())
                            .setChestNorm(iceBoxExceptionDate.getChestNorm())
                            .setDepositMoney(iceBoxExceptionDate.getChestMoney())
                            .setSupplierId(supplierInfo.getId())
                            .setDeptId(supplierInfo.getMarketAreaId())
                            .setPutStatus(IceBoxStatus.NO_PUT.getStatus())
                            .setStatus(IceBoxEnums.StatusEnum.LOSE.getType())
                            .setCreatedTime(now())
                            .setUpdatedTime(now())
                            .setAssetId(iceBoxExceptionDate.getAssetId())
                            .setIceBoxType(0);
                    iceBoxDao.insert(box);
                    boxExtend.setId(box.getId()).setAssetId(iceBoxExceptionDate.getAssetId());
                    iceBoxExtendDao.insert(boxExtend);
                    break;
                case 9:
                    iceBox.setStatus(IceBoxEnums.StatusEnum.NORMAL.getType());
                    iceBoxDao.updateById(iceBox);
                    IceExamine iceExamine = new IceExamine();
                    iceExamine.setIceStatus(IceBoxEnums.StatusEnum.NORMAL.getType());
                    iceExamineDao.update(iceExamine,Wrappers.<IceExamine>lambdaUpdate().eq(IceExamine::getIceBoxId,iceBox.getId()));
                    break;
                default:
                    break;
            }
        }
            /*if (IceExceptionDataEnum.ICE_RETURN.getDesc().equals(iceBoxExceptionDate.getImportType())){  //??????
                //1????????? ?????????????????????????????????last_put_id???last_apply_number???????????????????????????  ????????????
                if(iceBoxExtend.getLastPutId() != 0){
                    iceBoxExtendDao.update(iceBoxExtend,Wrappers.<IceBoxExtend>lambdaUpdate().eq(IceBoxExtend::getId,iceBox.getId()).set(IceBoxExtend::getLastPutId,0).set(IceBoxExtend::getLastApplyNumber,null));
                }
                //2??????????????????????????????????????????0 ???????????????put_store_number????????????null;
                iceBox.setPutStatus(IceBoxStatus.NO_PUT.getStatus());
                iceBox.setPutStoreNumber(null);
            }else if (IceExceptionDataEnum.ICE_LOSE.getDesc().equals(iceBoxExceptionDate.getImportType())){ //??????
                //?????? ?????????????????????????????????3
                iceBox.setStatus(IceBoxEnums.StatusEnum.LOSE.getType());
            }else if (IceExceptionDataEnum.ICE_SCRAP.getDesc().equals(iceBoxExceptionDate.getImportType())){ //??????
                //?????? ?????????????????????????????????2
                iceBox.setStatus(IceBoxEnums.StatusEnum.SCRAP.getType());
            }else if (IceExceptionDataEnum.ICE_ADD.getDesc().equals(iceBoxExceptionDate.getImportType())){ //??????
                iceBox.setPutStatus(IceBoxStatus.IS_PUTED.getStatus());
                iceBox.setPutStoreNumber(iceBoxExceptionDate.getStoreNumber());
            }else if (IceExceptionDataEnum.ICE_RETURN_SCRAP.getDesc().equals(iceBoxExceptionDate.getImportType())){ //???????????????
                //1????????? ?????????????????????????????????last_put_id???last_apply_number???????????????????????????  ????????????
                if(iceBoxExtend.getLastPutId() != 0){
                    iceBoxExtendDao.update(iceBoxExtend,Wrappers.<IceBoxExtend>lambdaUpdate().eq(IceBoxExtend::getId,iceBox.getId()).set(IceBoxExtend::getLastPutId,0).set(IceBoxExtend::getLastApplyNumber,null));
                }
                iceBox.setPutStatus(IceBoxStatus.NO_PUT.getStatus());
                iceBox.setPutStoreNumber(null);
                iceBox.setStatus(IceBoxEnums.StatusEnum.SCRAP.getType());
            }else{ //????????????????????? ????????????
                continue;
            }
            iceBoxDao.updateById(iceBox);*/

    }

    @Override
    public List<IceBox> getByResponsmanIdAndTime(Integer userId, String endTime) throws ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
        List<IceBox> boxList = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getResponseManId, userId).eq(IceBox::getPutStatus, 3).eq(IceBox::getStatus, 1));
        //????????????????????????
        Calendar cale = Calendar.getInstance();
        cale.add(Calendar.MONTH, -1);
        cale.set(Calendar.DAY_OF_MONTH,1);
        cale.set(Calendar.HOUR_OF_DAY,0);
        cale.set(Calendar.MINUTE,0);
        cale.set(Calendar.SECOND,0);
        Date monthStart = cale.getTime();
        //???????????????????????????
        Calendar cale2 = Calendar.getInstance();
        cale2.set(Calendar.DAY_OF_MONTH,0);
        cale2.set(Calendar.HOUR_OF_DAY,23);
        cale2.set(Calendar.MINUTE,59);
        cale2.set(Calendar.SECOND,59);
        Date monthEnd = cale2.getTime();
        if(CollectionUtil.isNotEmpty(boxList)){
            //????????????
            List<IceBox> boxListFor = new ArrayList<>(boxList);
            List<ExamineError> examineErrors = examineErrorMapper.selectList(Wrappers.<ExamineError>lambdaQuery().eq(ExamineError::getCreateUserId, userId).ge(ExamineError::getCreateTime, monthStart).le(ExamineError::getCreateTime, monthEnd).eq(ExamineError::getPassStatus,1));
            if(CollectionUtil.isNotEmpty(examineErrors)){
                List<String> boxs = examineErrors.stream().map(ExamineError::getBoxAssetid).collect(Collectors.toList());
                for (String box : boxs){
                    for(IceBox i : boxListFor){
                        if(i.getAssetId().equals(box)){
                            boxList.remove(i);
                        }
                    }
                }
            }

            List<String> boxIds = boxListFor.stream().map(IceBox::getAssetId).collect(Collectors.toList());
            List<IceBoxPutReport> iceBoxPutReports = iceBoxPutReportDao.selectList(Wrappers.<IceBoxPutReport>lambdaQuery().in(IceBoxPutReport::getIceBoxAssetId, boxIds).eq(IceBoxPutReport::getSubmitterId, userId).ge(IceBoxPutReport::getSignTime, simpleDateFormat.parse(endTime)));
            if(CollectionUtil.isNotEmpty(iceBoxPutReports)){
                for (IceBoxPutReport report : iceBoxPutReports){
                    for(IceBox i : boxListFor){
                        if(i.getAssetId().equals(report.getIceBoxAssetId())){
                            boxList.remove(i);
                        }
                    }
                }
            }
        }
        return boxList;
    }

}