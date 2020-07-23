package com.szeastroc.icebox.newprocess.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.exception.NormalOptionException;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.common.utils.Streams;
import com.szeastroc.commondb.config.redis.JedisClient;
import com.szeastroc.customer.client.FeignCusLabelClient;
import com.szeastroc.customer.client.FeignStoreClient;
import com.szeastroc.customer.client.FeignSupplierClient;
import com.szeastroc.customer.common.dto.CustomerLabelDetailDto;
import com.szeastroc.customer.common.vo.*;
import com.szeastroc.icebox.constant.IceBoxConstant;
import com.szeastroc.icebox.constant.RedisConstant;
import com.szeastroc.icebox.enums.RecordStatus;
import com.szeastroc.icebox.enums.ServiceType;
import com.szeastroc.icebox.enums.*;
import com.szeastroc.icebox.newprocess.convert.IceBoxConverter;
import com.szeastroc.icebox.newprocess.dao.*;
import com.szeastroc.icebox.newprocess.entity.*;
import com.szeastroc.icebox.newprocess.enums.PutStatus;
import com.szeastroc.icebox.newprocess.enums.*;
import com.szeastroc.icebox.newprocess.service.IceBoxService;
import com.szeastroc.icebox.newprocess.vo.*;
import com.szeastroc.icebox.newprocess.vo.request.IceBoxPage;
import com.szeastroc.icebox.newprocess.vo.request.IceBoxRequestVo;
import com.szeastroc.icebox.newprocess.vo.request.IceExaminePage;
import com.szeastroc.icebox.newprocess.vo.request.IceTransferRecordPage;
import com.szeastroc.icebox.oldprocess.dao.IceEventRecordDao;
import com.szeastroc.icebox.oldprocess.entity.IceEventRecord;
import com.szeastroc.icebox.util.redis.RedisLockUtil;
import com.szeastroc.icebox.vo.IceBoxRequest;
import com.szeastroc.user.client.FeignCacheClient;
import com.szeastroc.user.client.FeignDeptClient;
import com.szeastroc.user.client.FeignUserClient;
import com.szeastroc.user.client.FeignXcxBaseClient;
import com.szeastroc.user.common.vo.*;
import com.szeastroc.visit.client.FeignExamineClient;
import com.szeastroc.visit.common.IceBoxPutModel;
import com.szeastroc.visit.common.RequestExamineVo;
import com.szeastroc.visit.common.SessionExamineCreateVo;
import com.szeastroc.visit.common.SessionExamineVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @_(@Autowired))
public class IceBoxServiceImpl extends ServiceImpl<IceBoxDao, IceBox> implements IceBoxService {


    private final String FWCJL = "服务处经理";
    private final String FWCFJL = "服务处副经理";
    private final String DQZJ = "大区总监";
    private final String DQFZJ = "大区副总监";

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

    @Override
    public List<IceBoxVo> findIceBoxList(IceBoxRequestVo requestVo) {

        List<IceBoxVo> iceBoxVos = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //已投放
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
        //可申请
        if (XcxType.NO_PUT.getStatus().equals(requestVo.getType())) {
            if (requestVo.getMarketAreaId() == null) {
                throw new ImproperOptionException("门店营销区域不能为空！");
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
            throw new ImproperOptionException("无法获取经销商信息");
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
                .build();
        icePutApplyDao.insert(icePutApply);
        List<IceBoxPutModel.IceBoxModel> iceBoxModels = new ArrayList<>();
        BigDecimal totalMoney = new BigDecimal(0);
        for (IceBoxRequestVo requestVo : iceBoxRequestVos) {
            for (int i = 0; i < requestVo.getApplyCount(); i++) {
                List<IceBox> iceBoxes = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getModelId, requestVo.getModelId()).eq(IceBox::getSupplierId, iceBoxRequestVo.getSupplierId()).eq(IceBox::getPutStatus, PutStatus.NO_PUT.getStatus()));
                IceBox iceBox = null;
                if (CollectionUtil.isNotEmpty(iceBoxes)) {
                    iceBox = iceBoxes.get(0);

                } else {
                    throw new ImproperOptionException("无可申请冰柜");
                }
                RedisLockUtil lock = new RedisLockUtil(redisTemplate, RedisConstant.ICE_BOX_LOCK + iceBox.getId(), 5000, 10000);
                try {
                    if (lock.lock()) {
                        log.info("申请到的冰柜信息-->" + JSON.toJSONString(iceBox));
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

                        //在商户小程序同意《冷藏设备使用陈列协议》时创建
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
                log.info("根据经销商id--》【{}】查询不到经销商信息", iceBoxRequestVo.getSupplierId());
                throw new ImproperOptionException("查询不到经销商信息");
            }
            IceBoxPutModel.IceBoxModel iceBoxModel = new IceBoxPutModel.IceBoxModel(requestVo.getChestModel(), requestVo.getChestName(), requestVo.getDepositMoney(), requestVo.getApplyCount(),
                    requestVo.getFreeType(),requestVo.getSupplierName(),supplier.getAddress(),supplier.getLinkman(),supplier.getLinkmanMobile());
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
        if(freeTypes.contains(FreePayTypeEnum.IS_FREE.getType())){
            regionLeaderCheck = true;
        }
        List<SessionExamineVo.VisitExamineNodeVo> iceBoxPutExamine = createIceBoxPutExamine(iceBoxRequestVo, applyNumber, iceBoxModels, regionLeaderCheck);
        map.put("iceBoxPutExamine", iceBoxPutExamine);
        if (CollectionUtil.isNotEmpty(iceBoxPutExamine)) {
            SessionExamineVo.VisitExamineNodeVo visitExamineNodeVo = iceBoxPutExamine.get(0);
            icePutApply.setExamineId(visitExamineNodeVo.getExamineId());
            icePutApplyDao.updateById(icePutApply);
        }
        return map;
    }

    private List<SessionExamineVo.VisitExamineNodeVo> createIceBoxPutExamine(IceBoxRequestVo iceBoxRequestVo, String applyNumber, List<IceBoxPutModel.IceBoxModel> iceBoxModels, boolean regionLeaderCheck) {
        // 创建审批流

        SimpleUserInfoVo simpleUserInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findSimpleUserById(iceBoxRequestVo.getUserId()));
        Map<Integer, SessionUserInfoVo> sessionUserInfoMap = FeignResponseUtil.getFeignData(feignDeptClient.findLevelLeaderByDeptId(simpleUserInfoVo.getSimpleDeptInfoVos().get(0).getId()));
        List<Integer> userIds = new ArrayList<>();
        //获取上级部门领导
        if (CollectionUtil.isEmpty(sessionUserInfoMap)) {
            throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败，找不到上级审批人！");
        }
        for (Integer key : sessionUserInfoMap.keySet()) {
            SessionUserInfoVo sessionUserInfoVo = sessionUserInfoMap.get(key);
            if (sessionUserInfoVo != null && sessionUserInfoVo.getId().equals(simpleUserInfoVo.getId())) {
                continue;
            }
            if (sessionUserInfoVo != null && (FWCJL.equals(sessionUserInfoVo.getOfficeName()) || FWCFJL.equals(sessionUserInfoVo.getOfficeName()))) {
                if (userIds.contains(sessionUserInfoVo.getId())) {
                    continue;
                }
                userIds.add(sessionUserInfoVo.getId());
                break;
            }
        }
        for (Integer key : sessionUserInfoMap.keySet()) {
            SessionUserInfoVo sessionUserInfoVo = sessionUserInfoMap.get(key);
            if (sessionUserInfoVo != null && sessionUserInfoVo.getId().equals(simpleUserInfoVo.getId())) {
                continue;
            }
            if (regionLeaderCheck && sessionUserInfoVo != null && (DQZJ.equals(sessionUserInfoVo.getOfficeName()) || DQFZJ.equals(sessionUserInfoVo.getOfficeName()))) {
                if (userIds.contains(sessionUserInfoVo.getId())) {
                    continue;
                }
                userIds.add(sessionUserInfoVo.getId());
                break;
            }
        }

        if (CollectionUtil.isEmpty(userIds)) {
            throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败，找不到上级审批人！");
        }

//        List<Integer> userIds = Arrays.asList(5941, 2103,3088);
        SessionExamineVo sessionExamineVo = new SessionExamineVo();
        IceBoxPutModel iceBoxPutModel = new IceBoxPutModel();

        iceBoxPutModel.setApplyNumber(applyNumber);
        SubordinateInfoVo supplier = FeignResponseUtil.getFeignData(feignSupplierClient.findSupplierBySupplierId(iceBoxRequestVo.getSupplierId()));
        if (supplier == null) {
            log.info("根据经销商id--》【{}】查询不到经销商信息", iceBoxRequestVo.getSupplierId());
            throw new ImproperOptionException("查询不到经销商信息");
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        iceBoxPutModel.setAddress(supplier.getAddress());
        iceBoxPutModel.setLinkman(supplier.getLinkman());
        iceBoxPutModel.setLinkmanMobile(supplier.getLinkmanMobile());
        iceBoxPutModel.setSupplierName(supplier.getName());
        iceBoxPutModel.setCreateByName(simpleUserInfoVo.getRealname());
        iceBoxPutModel.setCreateTimeStr(dateFormat.format(new Date()));
        iceBoxPutModel.setIceBoxModelList(iceBoxModels);
        iceBoxPutModel.setApplyStoreName(iceBoxRequestVo.getStoreName());
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

        return visitExamineNodes;

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
            log.error("退押查询不到审批流信息");
            return iceBoxVos;
        }
        Map<Integer, SessionExamineVo> sessionExamineVoMap = sessionExamineVos.stream().collect(Collectors.toMap(SessionExamineVo::getExamineInfoId, x -> x));
        Set<String> applyNumbers = iceBackApplies.stream().map(x -> x.getApplyNumber()).collect(Collectors.toSet());
        List<IceBackApplyRelateBox> iceBackApplyRelateBoxes = iceBackApplyRelateBoxDao.selectList(Wrappers.<IceBackApplyRelateBox>lambdaQuery().in(IceBackApplyRelateBox::getApplyNumber, applyNumbers));
        if (CollectionUtil.isEmpty(iceBackApplyRelateBoxes)) {
            log.error("查询不到申请退押信息和冰柜的关联关系");
            return iceBoxVos;
        }
        Map<Integer, IceBackApplyRelateBox> relateBoxMap = iceBackApplyRelateBoxes.stream().collect(Collectors.toMap(IceBackApplyRelateBox::getBoxId, x -> x));

        Set<Integer> boxIds = iceBackApplyRelateBoxes.stream().map(x -> x.getBoxId()).collect(Collectors.toSet());
        List<IceBox> iceBoxes = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().in(IceBox::getId, boxIds));
        if (CollectionUtil.isEmpty(iceBoxes)) {
            log.error("查询不到申请退押信息关联的冰柜详情");
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
            log.error("投放查询不到审批流信息");
            return iceBoxVos;
        }
        Map<Integer, SessionExamineVo> sessionExamineVoMap = sessionExamineVos.stream().collect(Collectors.toMap(SessionExamineVo::getExamineInfoId, x -> x));
        Set<String> applyNumbers = icePutApplies.stream().map(x -> x.getApplyNumber()).collect(Collectors.toSet());
        List<IcePutApplyRelateBox> icePutApplyRelateBoxes = icePutApplyRelateBoxDao.selectList(Wrappers.<IcePutApplyRelateBox>lambdaQuery().in(IcePutApplyRelateBox::getApplyNumber, applyNumbers));
        if (CollectionUtil.isEmpty(icePutApplyRelateBoxes)) {
            log.error("查询不到申请投放信息和冰柜的关联关系");
            return iceBoxVos;
        }
        Map<Integer, IcePutApplyRelateBox> relateBoxMap = icePutApplyRelateBoxes.stream().collect(Collectors.toMap(IcePutApplyRelateBox::getBoxId, x -> x));

        Set<Integer> boxIds = icePutApplyRelateBoxes.stream().map(x -> x.getBoxId()).collect(Collectors.toSet());
        List<IceBox> iceBoxes = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().in(IceBox::getId, boxIds));
        if (CollectionUtil.isEmpty(iceBoxes)) {
            log.error("查询不到申请投放信息关联的冰柜详情");
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
        Map<Integer, List<PutStoreRelateModel>> relateModelGroup = relateModelList.stream().collect(Collectors.groupingBy(PutStoreRelateModel::getModelId));
        Set<Integer> relateModelIds = relateModelList.stream().map(x -> x.getId()).collect(Collectors.toSet());
        List<ApplyRelatePutStoreModel> putStoreModels = applyRelatePutStoreModelDao.selectList(Wrappers.<ApplyRelatePutStoreModel>lambdaQuery().in(ApplyRelatePutStoreModel::getStoreRelateModelId, relateModelIds));
        if (CollectionUtil.isEmpty(putStoreModels)) {
            log.error("查询不到申请投放信息和冰柜的关联关系");
            return iceBoxVos;
        }
        Map<Integer, ApplyRelatePutStoreModel> putStoreModelMap = putStoreModels.stream().collect(Collectors.toMap(ApplyRelatePutStoreModel::getStoreRelateModelId, x -> x));
        Set<String> applyNumbers = putStoreModels.stream().map(x -> x.getApplyNumber()).collect(Collectors.toSet());
        List<IcePutApply> icePutApplies = icePutApplyDao.selectList(Wrappers.<IcePutApply>lambdaQuery().in(IcePutApply::getApplyNumber, applyNumbers));
        if (CollectionUtil.isEmpty(icePutApplies)) {
            log.error("查询不到申请投放信息和冰柜的关联关系");
            return iceBoxVos;
        }
        Map<String, IcePutApply> icePutApplyMap = icePutApplies.stream().collect(Collectors.toMap(IcePutApply::getApplyNumber, x -> x));
        List<Integer> examineIds = icePutApplies.stream().map(x -> x.getExamineId()).collect(Collectors.toList());
        RequestExamineVo examineVo = new RequestExamineVo();
        examineVo.setExamineInfoIds(examineIds);
        List<SessionExamineVo> sessionExamineVos = FeignResponseUtil.getFeignData(feignExamineClient.getExamineNodesByList(examineVo));
        if (CollectionUtil.isEmpty(sessionExamineVos)) {
            log.error("投放查询不到审批流信息");
            return iceBoxVos;
        }
        for(SessionExamineVo sessionExamineVo:sessionExamineVos){
            String applyInfoStr = jedis.get(sessionExamineVo.getVisitExamineInfoVo().getRedisKey());
            JSONObject applyInfo = JSON.parseObject(applyInfoStr);
            JSONArray iceBoxModelList = applyInfo.getJSONArray("iceBoxModelList");
            for(Object object:iceBoxModelList){
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
//            log.error("查询不到申请投放信息关联的冰柜详情");
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
            boxVo.setAssetId(iceBoxExtend.getAssetId());
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

        if (storeInfoDtoVo == null) {
            throw new ImproperOptionException(Constants.ErrorMsg.CAN_NOT_FIND_RECORD);
        }

        // 门店编号和冰柜的id 以及最后的投放编号确定一个唯一的记录
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
                .assetId(iceBoxExtend.getAssetId())
                .chestModel(iceModel.getChestModel())
                .chestName(iceModel.getChestName())
                .depositMoney(iceBox.getDepositMoney())
                .lastPutTime(iceBoxExtend.getLastPutTime())
                .openTotal(iceBoxExtend.getOpenTotal())
                .putStoreNumber(storeNumber)
                .repairBeginTime(iceBoxExtend.getRepairBeginTime())
                .storeAddress(storeInfoDtoVo.getAddress())
                .releaseTime(iceBoxExtend.getReleaseTime())
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

            IceExamineVo firstExamineVo = firstExamine.convert(firstExamine, map.get(firstExamineCreateBy).getRealname(), storeInfoDtoVo.getStoreName(), storeNumber);
            IceExamineVo lastExamineVo = firstExamine.convert(lastExamine, map.get(lastExamineCreateBy).getRealname(), storeInfoDtoVo.getStoreName(), storeNumber);
            iceBoxDetailVo.setFirstExamine(firstExamineVo);
            iceBoxDetailVo.setLastExamine(lastExamineVo);
        }
        return iceBoxDetailVo;
    }

    @Override
    public List<SimpleSupplierInfoVo> findSupplierByDeptId(Integer deptId) {
        // 通过部门id 查询下面所有的经销商的supplier_id 然后聚合 t_ice_box表

        List<SimpleSupplierInfoVo> simpleSupplierInfoVoList = FeignResponseUtil.getFeignData(feignSupplierClient.findByDeptId(deptId));

        Map<Integer, SimpleSupplierInfoVo> map = simpleSupplierInfoVoList.stream().collect(Collectors.toMap(SimpleSupplierInfoVo::getId, Function.identity()));

        List<Integer> list = simpleSupplierInfoVoList.stream().map(SimpleSupplierInfoVo::getId).collect(Collectors.toList());

        List<IceBox> iceBoxList = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().in(IceBox::getSupplierId, list));

        Set<Integer> collect = iceBoxList.stream().map(IceBox::getSupplierId).collect(Collectors.toSet());


        List<SimpleSupplierInfoVo> supplierInfoVoList = new ArrayList<>();

        collect.forEach(supplierId -> supplierInfoVoList.add(map.get(supplierId)));


        return supplierInfoVoList;


    }


    /**
     * 根据 鹏讯通编号(门店) 找到该门店对应的投放冰柜, 并拼接Vo返回
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
        for (IceBox iceBox : iceBoxes) {

            IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(iceBox.getId());
            IceEventRecord iceEventRecord = iceEventRecordDao.selectOne(Wrappers.<IceEventRecord>lambdaQuery()
                    .eq(IceEventRecord::getAssetId, iceBoxExtend.getAssetId())
                    .orderByDesc(IceEventRecord::getCreateTime)
                    .last("limit 1"));
            IcePutApplyRelateBox icePutApplyRelateBox = icePutApplyRelateBoxDao.selectOne(Wrappers.<IcePutApplyRelateBox>lambdaQuery()
                    .eq(IcePutApplyRelateBox::getApplyNumber, iceBoxExtend.getLastApplyNumber())
                    .eq(IcePutApplyRelateBox::getBoxId, iceBox.getId()));
            IceModel iceModel = iceModelDao.selectById(iceBox.getModelId());

            IcePutApply icePutApply = icePutApplyDao.selectOne(Wrappers.<IcePutApply>lambdaQuery().eq(IcePutApply::getApplyNumber, iceBoxExtend.getLastApplyNumber()));

            IceBackApply iceBackApply = iceBackApplyDao.selectOne(Wrappers.<IceBackApply>lambdaQuery().eq(IceBackApply::getOldPutId, icePutApplyRelateBox.getId()));

            IceBoxStoreVo iceBoxStoreVo = IceBoxConverter.convertToStoreVo(iceBox, iceBoxExtend, iceModel, icePutApplyRelateBox, iceEventRecord, iceBackApply);
            iceBoxStoreVos.add(iceBoxStoreVo);
        }
        return iceBoxStoreVos;
    }

    /**
     * 检查当前冰柜状态
     * 1. 是否已投放
     * 2. 是否申请投放的门店是当前门店
     * 3. 申请流程是否走完审批流
     *
     * @param qrcode
     * @param pxtNumber
     * @return
     */
    @Override
    public IceBoxStatusVo checkBoxByQrcode(String qrcode, String pxtNumber) {

        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectOne(Wrappers.<IceBoxExtend>lambdaQuery().eq(IceBoxExtend::getQrCode, qrcode));
        if (Objects.isNull(iceBoxExtend)) {
            // 冰柜不存在(二维码未找到)
            IceBoxStatusVo iceBoxStatusVo = new IceBoxStatusVo();
            iceBoxStatusVo.setSignFlag(false);
            iceBoxStatusVo.setStatus(5);
            iceBoxStatusVo.setMessage("冰柜不存在(二维码未找到)");
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
                // 冰柜未申请
                iceBoxStatusVo.setSignFlag(false);
                iceBoxStatusVo.setStatus(3);
                iceBoxStatusVo.setMessage("当前门店未申请该冰柜");
                break;
            case LOCK_PUT:
                // 冰柜在锁定中, 未走完审批流
                iceBoxStatusVo.setSignFlag(false);
                iceBoxStatusVo.setStatus(4);
                iceBoxStatusVo.setMessage("冰柜未审批完成");
                break;
            case DO_PUT:
                // 冰柜处于投放中, 可以签收的状态
                iceBoxStatusVo = checkPutApplyByApplyNumber(applyNumber, pxtNumber);
                break;
            case FINISH_PUT:
                if (iceBox.getPutStoreNumber().equals(pxtNumber)) {
                    // 已投放到当前门店
                    iceBoxStatusVo.setSignFlag(false);
                    iceBoxStatusVo.setStatus(6);
                    iceBoxStatusVo.setMessage("冰柜已投放当当前门店");
                    break;
                }
                // 已有投放, 不能继续
                iceBoxStatusVo.setSignFlag(false);
                iceBoxStatusVo.setStatus(2);
                iceBoxStatusVo.setMessage("冰柜投放到其他门店");
                break;
        }
        return iceBoxStatusVo;
    }

    /**
     * 判断当前冰柜的投放申请信息
     *
     * @param applyNumber
     * @param pxtNumber
     * @return
     */
    private IceBoxStatusVo checkPutApplyByApplyNumber(String applyNumber, String pxtNumber) {
        IceBoxStatusVo iceBoxStatusVo = new IceBoxStatusVo();

        IcePutApply icePutApply = icePutApplyDao.selectOne(Wrappers.<IcePutApply>lambdaQuery().eq(IcePutApply::getApplyNumber, applyNumber));
        if (!icePutApply.getPutStoreNumber().equals(pxtNumber)) {
            // 冰柜申请门店非当前门店, 返回已投放的提示
            iceBoxStatusVo.setSignFlag(false);
            iceBoxStatusVo.setStatus(2);
            iceBoxStatusVo.setMessage("冰柜投放到其他门店");
            return iceBoxStatusVo;
        }
        // 该冰柜是当前门店申请的, 并且审批流已完成, 可以进行签收
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
        //驳回
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
        //审批中
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
        //审批通过将冰箱置为投放中状态，商户签收将状态置为已投放
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
                    IceBox iceBox = new IceBox();
                    iceBox.setId(iceBoxId);
                    iceBox.setPutStatus(IceBoxStatus.IS_PUTING.getStatus());
                    iceBox.setUpdatedTime(new Date());
                    iceBox.setCreatedBy(0);
                    iceBoxDao.updateById(iceBox);
                }
            }
        }
    }

    //@RoutingDataSource(value = Datasources.SLAVE_DB)
    @Override
    public IPage findPage(IceBoxPage iceBoxPage) {
        Integer deptId = iceBoxPage.getDeptId(); // 营销区域id
        if (deptId != null) {
            DeptNameRequest request = new DeptNameRequest();
            request.setParentIds(deptId.toString());
            // 查询出当前部门下面的服务处
            List<SessionDeptInfoVo> deptInfoVos = FeignResponseUtil.getFeignData(feignDeptClient.findDeptInfoListByParentId(request));
            Set<Integer> deptIds = new HashSet<>();
            if (CollectionUtils.isNotEmpty(deptInfoVos)) {
                deptInfoVos.forEach(i -> {
                    deptIds.add(i.getId());
                });
            }
            deptIds.add(deptId);
            iceBoxPage.setDeptIds(deptIds);
        }
        // 当所在对象编号或者所在对象名称不为空时,所在对象字段为必填
        if ((StringUtils.isNotBlank(iceBoxPage.getBelongObjNumber()) || StringUtils.isNotBlank(iceBoxPage.getBelongObjName()))
                && iceBoxPage.getBelongObj() == null) {
            throw new NormalOptionException(Constants.API_CODE_FAIL, "请选择所在对象类型");
        }

        Set<Integer> supplierIdList = new HashSet<>(); // 拥有者的经销商
        // 所在对象  (put_status  投放状态 0: 未投放 1:已锁定(被业务员申请) 2:投放中 3:已投放; 当经销商时为 0-未投放;当门店时为非未投放状态;)
        String belongObjNumber = iceBoxPage.getBelongObjNumber();
        String belongObjName = iceBoxPage.getBelongObjName();
        String limit = " limit 30";
        // 所在对象为 经销商
        if (iceBoxPage.getBelongObj() != null && PutStatus.NO_PUT.getStatus().equals(iceBoxPage.getBelongObj())) {
            // supplier_type 客户类型：1-经销商，2-分销商，3-邮差，4-批发商
            // status 状态：0-禁用，1-启用
            if (StringUtils.isNotBlank(belongObjNumber)) { // 用 number 去查
                List<SubordinateInfoVo> infoVoList = FeignResponseUtil.getFeignData(feignSupplierClient.getByNameOrNumber(null, belongObjNumber, null, 1, limit));
                Optional.ofNullable(infoVoList).ifPresent(list -> {
                    list.forEach(i -> {
                        supplierIdList.add(i.getId());
                    });
                });
                if (CollectionUtils.isEmpty(supplierIdList)) {
                    return new Page();
                }
            }
            if (StringUtils.isNotBlank(belongObjName)) { // 用 name 去查
                List<SubordinateInfoVo> infoVoList = FeignResponseUtil.getFeignData(feignSupplierClient.getByNameOrNumber(belongObjName, null, null, 1, limit));
                Optional.ofNullable(infoVoList).ifPresent(list -> {
                    list.forEach(i -> {
                        supplierIdList.add(i.getId());
                    });
                });
                if (CollectionUtils.isEmpty(supplierIdList)) {
                    return new Page();
                }
            }
        }
        Set<String> putStoreNumberList = new HashSet<>(); // 投放的门店number
        // 所在对象为 门店
        if (iceBoxPage.getBelongObj() != null && !PutStatus.NO_PUT.getStatus().equals(iceBoxPage.getBelongObj())) {
            if (StringUtils.isNotBlank(belongObjNumber)) { // 用 number 去查
                List<SimpleStoreVo> storeVoList = FeignResponseUtil.getFeignData(feignStoreClient.getByNameOrNumber(belongObjNumber, null, null, null, limit));
                Optional.ofNullable(storeVoList).ifPresent(list -> {
                    list.forEach(i -> {
                        putStoreNumberList.add(i.getStoreNumber());
                    });
                });
                if (CollectionUtils.isEmpty(putStoreNumberList)) {
                    return new Page();
                }
            }
            if (StringUtils.isNotBlank(belongObjName)) { // 用 name 去查
                List<SimpleStoreVo> storeVoList = FeignResponseUtil.getFeignData(feignStoreClient.getByNameOrNumber(null, belongObjName, null, null, limit));
                Optional.ofNullable(storeVoList).ifPresent(list -> {
                    list.forEach(i -> {
                        putStoreNumberList.add(i.getStoreNumber());
                    });
                });
                if (CollectionUtils.isEmpty(putStoreNumberList)) {
                    return new Page();
                }
            }
        }

        iceBoxPage.setSupplierIdList(supplierIdList);
        iceBoxPage.setPutStoreNumberList(putStoreNumberList);
        List<IceBox> iceBoxList = iceBoxDao.findPage(iceBoxPage);
        if (CollectionUtils.isEmpty(iceBoxList)) {
            return new Page();
        }
        List<Integer> deptIds = iceBoxList.stream().map(IceBox::getDeptId).collect(Collectors.toList());
        // 营销区域对应得部门  服务处->大区->事业部
        Map<Integer, String> deptMap = null;
        if (CollectionUtils.isNotEmpty(deptIds)) {
            deptMap = FeignResponseUtil.getFeignData(feignCacheClient.getForMarketAreaName(deptIds));
        }
        // 设备型号
        List<IceModel> iceModels = iceModelDao.selectList(Wrappers.<IceModel>lambdaQuery()
                .in(IceModel::getId, iceBoxList.stream().map(IceBox::getModelId).collect(Collectors.toSet())));
        Map<Integer, IceModel> modelMap = new HashMap<>();
        Optional.ofNullable(iceModels).ifPresent(list -> {
            list.forEach(i -> {
                modelMap.put(i.getId(), i);
            });
        });
        // 经销商 集合
        List<Integer> suppIds = iceBoxList.stream().filter(i -> i.getPutStatus().equals(PutStatus.NO_PUT.getStatus())).map(IceBox::getSupplierId).collect(Collectors.toList());
        Map<Integer, SubordinateInfoVo> suppMap = null;
        if (CollectionUtils.isNotEmpty(suppIds)) {
            suppMap = FeignResponseUtil.getFeignData(feignSupplierClient.findByIds(suppIds));
        }
        // 门店 集合
        Map<String, SimpleStoreVo> storeMap = null;
        List<String> storeNumbers = iceBoxList.stream().filter(i -> !i.getPutStatus().equals(PutStatus.NO_PUT.getStatus())).map(IceBox::getPutStoreNumber).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(storeNumbers)) {
            storeMap = FeignResponseUtil.getFeignData(feignStoreClient.getSimpleStoreByNumberList(storeNumbers));
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (IceBox iceBox : iceBoxList) {
            // t_ice_box 的id 和 t_ice_box_extend 的id是一一对应的
            IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(iceBox.getId());
            Map<String, Object> map = new HashMap<>(32);
            map.put("statusStr", IceBoxEnums.StatusEnum.getDesc(iceBox.getStatus())); // 设备状态
            String deptStr = null;
            if (deptMap != null) {
                deptStr = deptMap.get(iceBox.getDeptId()); // 营销区域
            }
            map.put("deptStr", deptStr); // 营销区域
            map.put("assetId", iceBoxExtend.getAssetId()); // 设备编号 --东鹏资产id
            map.put("chestName", iceBox.getChestName()); // 设备名称
            map.put("brandName", iceBox.getBrandName()); // 品牌
            IceModel iceModel = modelMap.get(iceBox.getModelId());
            map.put("chestModel", iceModel == null ? null : iceModel.getChestModel()); // 设备型号
            map.put("chestNorm", iceBox.getChestNorm()); // 规格
            map.put("lastPutTime", iceBoxExtend.getLastPutTime()); // 最近投放日期
            map.put("lastExamineTime", iceBoxExtend.getLastExamineTime()); // 最近巡检日期
            String lastApplyNumber = iceBoxExtend.getLastApplyNumber(); // 最近一次申请编号
            IcePutApplyRelateBox icePutApplyRelateBox = null;
            if (StringUtils.isNotBlank(lastApplyNumber)) {
                icePutApplyRelateBox = icePutApplyRelateBoxDao.selectOne(Wrappers.<IcePutApplyRelateBox>lambdaQuery()
                        .eq(IcePutApplyRelateBox::getApplyNumber, lastApplyNumber).last(" limit 1"));
            }
            map.put("freeTypeStr", icePutApplyRelateBox == null ? null : FreePayTypeEnum.getDesc(icePutApplyRelateBox.getFreeType())); // 押金收取
            map.put("belongObjStr", iceBox.getPutStatus().equals(0) ? "经销商" : "门店"); // 所在客户类型
            String name = null;
            String number = null;
            if (PutStatus.NO_PUT.getStatus().equals(iceBox.getPutStatus()) && suppMap != null) { // 经销商
                SubordinateInfoVo infoVo = suppMap.get(iceBox.getSupplierId());
                name = infoVo == null ? null : infoVo.getName();
                number = infoVo == null ? null : infoVo.getNumber();
            }
            if (!PutStatus.NO_PUT.getStatus().equals(iceBox.getPutStatus()) && storeMap != null) { // 门店
                SimpleStoreVo storeVo = storeMap.get(iceBox.getPutStoreNumber());
                name = storeVo == null ? null : storeVo.getStoreName();
                number = storeVo == null ? null : storeVo.getStoreNumber();
            }
            map.put("number", number); // 客户编号
            map.put("name", name); // 客户名称
            map.put("id", iceBox.getId());
//            map.put("putStatus",iceBox.getPutStatus());
//            map.put("deptId",iceBox.getDeptId());
//            map.put("remark","暂无备注"); // 备注

            list.add(map);
        }

        return new Page(iceBoxPage.getCurrent(), iceBoxPage.getSize(), iceBoxPage.getTotal()).setRecords(list);
    }

    @Override
    public Map<String, Object> readBasic(Integer id) {

        IceBox iceBox = iceBoxDao.selectById(id);
        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(id);
        Map<String, Object> map = new HashMap<>(32);
        map.put("assetId", iceBoxExtend.getAssetId()); // 设备编号 --东鹏资产id
        map.put("chestName", iceBox.getChestName()); // 名称
        IceModel iceModel = iceModelDao.selectOne(Wrappers.<IceModel>lambdaQuery().eq(IceModel::getId, iceBox.getModelId()).last(" limit 1"));
        map.put("chestModel", iceModel == null ? null : iceModel.getChestModel()); // 型号
        map.put("chestNorm", iceBox.getChestNorm()); // 规格
        map.put("brandName", iceBox.getBrandName()); // 品牌
        map.put("chestMoney", iceBox.getChestMoney()); // 价值
        map.put("depositMoney", iceBox.getDepositMoney()); // 标准押金
        map.put("statusStr", IceBoxEnums.StatusEnum.getDesc(iceBox.getStatus())); // 状态
        map.put("releaseTime", iceBoxExtend.getReleaseTime()); // 生产日期
        map.put("repairBeginTime", iceBoxExtend.getRepairBeginTime()); // 保修起算日期
        // 营销区域对应得部门  服务处->大区->事业部
        String deptStr = null;
        if (iceBox.getDeptId() != null) {
            deptStr = FeignResponseUtil.getFeignData(feignCacheClient.getForMarketAreaName(iceBox.getDeptId()));
        }
        map.put("deptStr", deptStr); // 责任部门
        map.put("putStatusStr", PutStatus.NO_PUT.getStatus().equals(iceBox.getPutStatus()) ? "经销商" : "门店"); // 客户类型

        String khName = null;
        String khAddress = null;
        String khGrade = null;
        String khStatusStr = null;
        String khContactPerson = null;
        String khContactNumber = null;
        if (PutStatus.NO_PUT.getStatus().equals(iceBox.getPutStatus())) { // 经销商
            SubordinateInfoVo infoVo = FeignResponseUtil.getFeignData(feignSupplierClient.readId(iceBox.getSupplierId()));
            if (infoVo != null) {
                khName = infoVo.getName();
                khAddress = infoVo.getAddress();
                khGrade = infoVo.getLevel();
                // 状态：0-禁用，1-启用
                khStatusStr = (infoVo.getStatus() != null && infoVo.getStatus().equals(1)) ? "启用" : "禁用";
                khContactPerson = infoVo.getLinkman();
                khContactNumber = infoVo.getLinkmanMobile();
            }
        } else { // 门店
            StoreInfoDtoVo dtoVo = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(iceBox.getPutStoreNumber()));
            Map<String, SessionStoreInfoVo> storeInfoVoMap = FeignResponseUtil.getFeignData(feignStoreClient.getSessionStoreInfoVo(Lists.newArrayList(iceBox.getPutStoreNumber())));
            if (dtoVo != null) {
                khName = dtoVo.getStoreName();
                khAddress = dtoVo.getAddress();
                khGrade = dtoVo.getStoreLevel();
                // 状态：0-禁用，1-启用
                khStatusStr = (dtoVo.getStatus() != null && dtoVo.getStatus().equals(1)) ? "启用" : "禁用";
                if (storeInfoVoMap != null && storeInfoVoMap.get(iceBox.getPutStoreNumber()) != null) {
                    SessionStoreInfoVo infoVo = storeInfoVoMap.get(iceBox.getPutStoreNumber());
                    khContactPerson = infoVo.getMemberName();
                    khContactNumber = infoVo.getMemberMobile();
                }
            }
        }
        map.put("khName", khName); // 客户名称
        map.put("khAddress", khAddress); // 客户地址
        map.put("khGrade", khGrade); // 客户等级
        map.put("khStatusStr", khStatusStr); // 客户状态
        map.put("khContactPerson", khContactPerson); // 联系人
        map.put("khContactNumber", khContactNumber); // 联系电话

        return map;
    }

    @Override
    public Map<String, Object> readModule(Integer id) {

        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(id);
        Map<String, Object> map = Maps.newHashMap();
        map.put("bluetoothId", iceBoxExtend.getBluetoothId()); // 设备蓝牙ID
        map.put("bluetoothMac", iceBoxExtend.getBluetoothMac()); // 设备蓝牙地址
        map.put("gpsMac", iceBoxExtend.getGpsMac()); // GPS模块地址
        map.put("qrCode", iceBoxExtend.getQrCode()); // 唯一二维码

        return map;
    }

    @Override
    public Map<String, Object> readEquipNews(Integer id) {

        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(id);
        IceEventRecord iceEventRecord = iceEventRecordDao.selectOne(Wrappers.<IceEventRecord>lambdaQuery()
                .eq(IceEventRecord::getAssetId, iceBoxExtend.getAssetId()).orderByDesc(IceEventRecord::getId)
                .last(" limit 1"));
        Map<String, Object> map = Maps.newHashMap();
        Optional.ofNullable(iceEventRecord).ifPresent(info -> {
            map.put("temperature", info.getTemperature()); // 温度
            String assetId = iceBoxExtend.getAssetId();
            DateTime now = new DateTime();
            // 开关门次数 -- 累计总数
            Integer totalSum = iceEventRecordDao.sumTotalOpenCloseCount(assetId);

            // 开关门次数 -- 月累计
            Date monStart = now.dayOfMonth().withMinimumValue().withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0).toDate();
            Date monEnd = now.dayOfMonth().withMaximumValue().withHourOfDay(23).withMinuteOfHour(59).withSecondOfMinute(59).toDate();
            Integer monthSum = iceEventRecordDao.sumOpenCloseCount(assetId, monStart, monEnd);

            // 开关门次数 -- 今日累计
            Date todayStart = now.withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0).toDate();
            Date todayEnd = now.withHourOfDay(23).withMinuteOfHour(59).withSecondOfMinute(59).toDate();
            Integer todaySum = iceEventRecordDao.sumOpenCloseCount(assetId, todayStart, todayEnd);

            map.put("totalSum", totalSum);
            map.put("monthSum", monthSum);
            map.put("todaySum", todaySum);
            String address = info.getDetailAddress();
            if (StringUtils.isBlank(address)) {
                if (StringUtils.isNotBlank(info.getLng()) && StringUtils.isNotBlank(info.getLat())) {
                    AddressVo addressVo = FeignResponseUtil.getFeignData(feignXcxBaseClient.getAddressBylatAndLng(info.getLng(), info.getLat()));
                    if (addressVo != null) {
                        address = addressVo.getAddress();
                    }
                }
            }
            map.put("address", address); // 定位位置
            map.put("occurrenceTime", info.getOccurrenceTime()); // 最近采集时间
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

        // 参与的门店  map
        Map<String, SimpleStoreVo> storeVoMap = null;
        Set<String> storeNumbers = iceTransferRecordList.stream().map(IceTransferRecord::getStoreNumber).collect(Collectors.toSet());
        if (CollectionUtils.isNotEmpty(storeNumbers)) {
            storeVoMap = FeignResponseUtil.getFeignData(feignStoreClient.getSimpleStoreByNumberList(Lists.newArrayList(storeNumbers)));
        }
        // 参与的经销商 map
        Map<Integer, SubordinateInfoVo> supplierMap = null;
        Set<Integer> supplierIds = iceTransferRecordList.stream().map(IceTransferRecord::getSupplierId).collect(Collectors.toSet());
        if (CollectionUtils.isNotEmpty(supplierIds)) {
            supplierMap = FeignResponseUtil.getFeignData(feignSupplierClient.findByIds(Lists.newArrayList(supplierIds)));
        }
        // 经办人  map
        Map<Integer, SessionUserInfoVo> userInfoVoMap = null;
        Set<Integer> userIds = iceTransferRecordList.stream().map(IceTransferRecord::getApplyUserId).collect(Collectors.toSet());
        if (CollectionUtils.isNotEmpty(userIds)) {
            userInfoVoMap = FeignResponseUtil.getFeignData(feignUserClient.getSessionUserInfoVoByIds(Lists.newArrayList(userIds)));
        }

        List<Map<String, Object>> transferRecordList = Lists.newArrayList(); // 收集数据
        for (IceTransferRecord transferRecord : iceTransferRecordList) {
            Map<String, Object> map = Maps.newHashMap();
            Integer serviceType = transferRecord.getServiceType(); //  业务类型 0:入库 1:投放 2:退还
            map.put("serviceTypeStr", com.szeastroc.icebox.newprocess.enums.ServiceType.getDesc(serviceType)); // 业务类型
            Integer supplierId = transferRecord.getSupplierId(); // 参与的经销商ID
            String storeNumber = transferRecord.getStoreNumber(); // 参与的门店Number
            String sendName = null;
            String receiveName = null;
            if (com.szeastroc.icebox.newprocess.enums.ServiceType.ENTER_WAREHOUSE.getType().equals(serviceType)) { // 入库  (厂家-->经销商)
                if (supplierMap != null && supplierId != null) {
                    SubordinateInfoVo infoVo = supplierMap.get(supplierId);
                    receiveName = infoVo == null ? null : infoVo.getName();
                }
            } else if (com.szeastroc.icebox.newprocess.enums.ServiceType.IS_PUT.getType().equals(serviceType)) { // 投放  (经销商-->门店)
                if (supplierMap != null && supplierId != null) {
                    SubordinateInfoVo infoVo = supplierMap.get(supplierId);
                    sendName = infoVo == null ? null : infoVo.getName();
                }
                if (storeVoMap != null && StringUtils.isNotBlank(storeNumber)) {
                    SimpleStoreVo storeVo = storeVoMap.get(storeNumber);
                    receiveName = storeVo == null ? null : storeVo.getStoreName();
                }
            } else if (com.szeastroc.icebox.newprocess.enums.ServiceType.IS_RETURN.getType().equals(serviceType)) { // 退还  (门店-->经销商)
                if (storeVoMap != null && StringUtils.isNotBlank(storeNumber)) {
                    SimpleStoreVo storeVo = storeVoMap.get(storeNumber);
                    sendName = storeVo == null ? null : storeVo.getStoreName();
                }
                if (supplierMap != null && supplierId != null) {
                    SubordinateInfoVo infoVo = supplierMap.get(supplierId);
                    receiveName = infoVo == null ? null : infoVo.getName();
                }
            }

            map.put("sendName", sendName); // 发出方
            map.put("sendTime", transferRecord.getSendTime()); // 发出日期
            map.put("receiveName", receiveName); // 接收方
            map.put("receiveTime", transferRecord.getReceiveTime()); // 接收日期
            SessionUserInfoVo userInfoVo = null;
            if (userInfoVoMap != null && transferRecord.getApplyUserId() != null) {
                userInfoVo = userInfoVoMap.get(transferRecord.getApplyUserId());
            }
            map.put("applyUser", userInfoVo == null ? null : userInfoVo.getRealname()); // 经办人
            map.put("applyTime", transferRecord.getApplyTime()); // 经办日期
            map.put("transferMoney", transferRecord.getTransferMoney()); // 交易金额
            map.put("applyNumber", transferRecord.getApplyNumber()); // 业务单号
            map.put("recordStatusStr", com.szeastroc.icebox.newprocess.enums.RecordStatus.getDesc(transferRecord.getRecordStatus())); // 状态
//            map.put("supplierId", transferRecord.getSupplierId()); // 备注

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
        Map<String, SimpleStoreVo> storeVoMap = null; // 客户集合
        if (CollectionUtils.isNotEmpty(storeNumbers)) {
            storeVoMap = FeignResponseUtil.getFeignData(feignStoreClient.getSimpleStoreByNumberList(Lists.newArrayList(storeNumbers)));
        }
        Map<Integer, SessionUserInfoVo> userInfoVoMap = null; // 巡检人员集合
        if (CollectionUtils.isNotEmpty(userIds)) {
            userInfoVoMap = FeignResponseUtil.getFeignData(feignUserClient.getSessionUserInfoVoByIds(Lists.newArrayList(userIds)));
        }

        List<Map<String, Object>> examineList = Lists.newArrayList(); // 收集数据
        for (IceExamine i : iceExamineList) {
            Map<String, Object> map = Maps.newHashMap();
            map.put("storeNumber", i.getStoreNumber()); // 门店编号
            SimpleStoreVo storeVo = null;
            if (storeVoMap != null) {
                storeVo = storeVoMap.get(i.getStoreNumber());
            }
            map.put("storeName", storeVo == null ? null : storeVo.getStoreName()); // 客户名称
            SessionUserInfoVo userInfoVo = null;
            if (userInfoVoMap != null) {
                userInfoVo = userInfoVoMap.get(i.getCreateBy());
            }
            map.put("realname", userInfoVo == null ? null : userInfoVo.getRealname()); // 巡检人员
            map.put("createTime", i.getCreateTime()); // 巡检日期
            map.put("displayImage", i.getDisplayImage()); // 现场图片
            map.put("exteriorImage", i.getExteriorImage()); // 外观照片的URL
//            map.put("storeNumber", i.getStoreNumber()); // 备注
            examineList.add(map);
        }

        return page.setRecords(examineList);
    }

    @Transactional(rollbackFor = Exception.class, value = "transactionManager")
    @Override
    public void importByEasyExcel(MultipartFile mfile) throws Exception {

        /**
         * @Date: 2020/5/20 9:19 xiao
         *  同步的返回，不推荐使用，如果数据量大会把数据放到内存里面
         */
        // 这里 需要指定读用哪个class去读，然后读取第一个sheet 同步读取会自动finish
        List<ImportIceBoxVo> importDataList = EasyExcel.read(mfile.getInputStream()).head(ImportIceBoxVo.class).sheet().doReadSync();
        if (CollectionUtils.isEmpty(importDataList)) {
            throw new NormalOptionException(Constants.API_CODE_FAIL, "数据读取为空");
        }

        // 获取设备型号集合
        List<IceModel> iceModels = iceModelDao.selectList(Wrappers.<IceModel>lambdaQuery());
        Map<String, Integer> iceModelMap = Maps.newHashMap();
        Optional.ofNullable(iceModels).ifPresent(list -> {
            list.forEach(i -> {
                iceModelMap.put(i.getChestModel(), i.getId());
            });
        });
        Map<String, SubordinateInfoVo> supplierNumberMap = Maps.newHashMap(); // 存储经销商编号和id

        int importSize = importDataList.size();
        //List<String> message = Lists.newArrayList();
        for (ImportIceBoxVo boxVo : importDataList) {

            String externalId = boxVo.getExternalId();  // 冰箱控制器ID
            String assetId = boxVo.getAssetId();// 设备编号
            // 根据 设备编号--东鹏资产id 校验此冰柜是否插入数据库
            if (StringUtils.isBlank(assetId)) {
//                message.add(boxVo.getSerialNumber() + "行:设备编号为空");
//                continue;
                throw new NormalOptionException(Constants.API_CODE_FAIL, "第" + boxVo.getSerialNumber() + "行:设备编号为空");
            }

            IceBox iceBox = iceBoxDao.selectOne(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getAssetId, assetId).last(" limit 1"));
            IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectOne(Wrappers.<IceBoxExtend>lambdaQuery().eq(IceBoxExtend::getAssetId, assetId).last(" limit 1"));
            if ((iceBox == null && iceBoxExtend != null) || (iceBox != null && iceBoxExtend == null)) { // 两者要么同时存在,要不同时不存在
//                message.add(boxVo.getSerialNumber() + "行:数据库存在脏数据");
//                continue;
                throw new NormalOptionException(Constants.API_CODE_FAIL, "第" + boxVo.getSerialNumber() + "行:数据库存在脏数据");
            }

            String bluetoothId = boxVo.getBluetoothId();// 蓝牙设备ID
            String bluetoothMac = boxVo.getBluetoothMac();// 蓝牙设备地址
            String qrCode = boxVo.getQrCode();// 冰箱二维码
            String gpsMac = boxVo.getGpsMac();// gps模块MAC
            String chestName = boxVo.getChestName();// 设备名称
            String brandName = boxVo.getBrandName();// 生产厂家
            String modelStr = boxVo.getModelStr();// 设备型号
            if (iceModelMap == null || iceModelMap.get(modelStr) == null) {
//                message.add(boxVo.getSerialNumber() + "行:设备型号不存在于数据库");
//                continue;
                throw new NormalOptionException(Constants.API_CODE_FAIL, "第" + boxVo.getSerialNumber() + "行:设备型号不存在于数据库");
            }
            Integer modelId = iceModelMap.get(modelStr); // 设备型号
            String chestNorm = boxVo.getChestNorm();// 设备规格
            Long chestMoney = boxVo.getChestMoney();// 冰柜价值
            Long depositMoney = boxVo.getDepositMoney();// 冰柜押金

            // 经销商id
            Integer supplierId = null;
            String supplierNumber = boxVo.getSupplierNumber();
            if (StringUtils.isBlank(supplierNumber)) {
//                message.add(boxVo.getSerialNumber() + "行:经销商编号为空");
//                continue;
                throw new NormalOptionException(Constants.API_CODE_FAIL, "第" + boxVo.getSerialNumber() + "行:经销商编号为空");
            }

            SubordinateInfoVo subordinateInfoVo = supplierNumberMap.get(supplierNumber);
            if (subordinateInfoVo != null && subordinateInfoVo.getSupplierId() != null) {
                supplierId = subordinateInfoVo.getSupplierId();
            } else {
                // 去数据库查询
                SubordinateInfoVo infoVo = FeignResponseUtil.getFeignData(feignSupplierClient.findByNumber(supplierNumber));
                if (infoVo == null) {
//                    message.add(boxVo.getSerialNumber() + "行:经销商编号不存在");
//                    continue;
                    throw new NormalOptionException(Constants.API_CODE_FAIL, "第" + boxVo.getSerialNumber() + "行:经销商编号不存在");
                }
                supplierId = infoVo.getSupplierId();
                supplierNumberMap.put(supplierNumber, infoVo);
            }
            // 鉴于服务处就是对应经销商的服务处,所以直接用经销商的
            Integer deptId = supplierNumberMap.get(supplierNumber).getMarketAreaId(); // 所属服务处
            Date releaseTime = boxVo.getReleaseTime();// 生产日期
            Date repairBeginTime = boxVo.getRepairBeginTime();// 保修起算日期

            if (iceBox == null) {
                iceBox = new IceBox();
                iceBox.setPutStatus(PutStatus.NO_PUT.getStatus()); // 未投放
                iceBoxExtend = new IceBoxExtend();
            }
            iceBox.setChestName(chestName);
            iceBox.setAssetId(assetId);
            iceBox.setModelId(modelId);
            iceBox.setModelName(modelStr);
            iceBox.setBrandName(brandName);
            iceBox.setChestNorm(chestNorm);
            iceBox.setChestMoney(new BigDecimal(chestMoney));
            iceBox.setDepositMoney(new BigDecimal(depositMoney));
            iceBox.setSupplierId(supplierId);
            iceBox.setDeptId(deptId);

            iceBoxExtend.setExternalId(externalId);
            iceBoxExtend.setAssetId(assetId);
            iceBoxExtend.setBluetoothId(bluetoothId);
            iceBoxExtend.setBluetoothMac(bluetoothMac);
            iceBoxExtend.setQrCode(qrCode);
            iceBoxExtend.setGpsMac(gpsMac);
            iceBoxExtend.setReleaseTime(releaseTime);
            iceBoxExtend.setRepairBeginTime(repairBeginTime);

            /**
             * @Date: 2020/5/20 11:07 xiao
             *  需求:
             *  冰柜控制器ID  蓝牙设备ID     蓝牙设备地址     冰箱二维码链接 如果有值,必须是唯一的,不允许重复导入
             *  external_id   bluetooth_id  bluetooth_mac    qr_code
             */
//            iceBoxExtendDao.selectCount(Wrappers.<IceBoxExtend>lambdaQuery().eq(IceBoxExtend::getExternalId,externalId));

            if (iceBox.getId() == null) {
                try {
                    iceBoxDao.insert(iceBox);
                    iceBoxExtend.setId(iceBox.getId());
                    iceBoxExtendDao.insert(iceBoxExtend);
                } catch (Exception e) {
                    log.error("插入冰柜数据错误", e);
                    iceBoxDao.deleteById(iceBox.getId());
                    iceBoxExtendDao.deleteById(iceBox.getId());
                    throw new NormalOptionException(Constants.API_CODE_FAIL, "第" + boxVo.getSerialNumber() + "行:冰柜控制器ID、蓝牙设备ID、蓝牙设备地址、冰箱二维码链接不唯一");
                }
            } else {
                try {
                    iceBoxDao.updateById(iceBox);
                    iceBoxExtendDao.updateById(iceBoxExtend);
                } catch (Exception e) {
                    log.error("更新冰柜数据错误", e);
                    throw new NormalOptionException(Constants.API_CODE_FAIL, "第" + boxVo.getSerialNumber() + "行:冰柜控制器ID、蓝牙设备ID、蓝牙设备地址、冰箱二维码链接不唯一");
                }
            }
        }
        log.info("importExcel 处理数据结束-->{}", importSize);
    }

    @Override
    public List<IceBox> getIceBoxList(String pxtNumber) {
        List<IceBox> iceBoxes = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getPutStoreNumber, pxtNumber));
        return iceBoxes;
    }

    @Override
    public Map<String, List<IceBoxVo>> findPutingIceBoxList(IceBoxRequestVo requestVo) {
        List<IceBoxVo> iceBoxVos = new ArrayList<>();
        //处理中
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

            IceEventRecord iceEventRecord = iceEventRecordDao.selectOne(Wrappers.<IceEventRecord>lambdaQuery()
                    .eq(IceEventRecord::getAssetId, iceBoxExtend.getAssetId())
                    .orderByDesc(IceEventRecord::getCreateTime)
                    .last("limit 1"));
            if (iceEventRecord != null) {
                boxVo.setDetailAddress(iceEventRecord.getDetailAddress());
            }

//            IcePutApply icePutApply = icePutApplyDao.selectOne(Wrappers.<IcePutApply>lambdaQuery().eq(IcePutApply::getApplyNumber, iceBoxExtend.getLastApplyNumber()));
            IcePutApplyRelateBox relateBox = icePutApplyRelateBoxDao.selectOne(Wrappers.<IcePutApplyRelateBox>lambdaQuery().eq(IcePutApplyRelateBox::getApplyNumber, iceBoxExtend.getLastApplyNumber()).eq(IcePutApplyRelateBox::getBoxId, iceBox.getId()));
            if (relateBox != null) {
                boxVo.setFreeType(relateBox.getFreeType());
                IceBackApply iceBackApply = iceBackApplyDao.selectOne(Wrappers.<IceBackApply>lambdaQuery().eq(IceBackApply::getOldPutId, relateBox.getId()));
                boxVo.setBackStatus(iceBackApply == null ? -1 : iceBackApply.getExamineStatus());
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
                .build();
        icePutApplyDao.insert(icePutApply);
        List<IceBoxPutModel.IceBoxModel> iceBoxModels = new ArrayList<>();
        BigDecimal totalMoney = new BigDecimal(0);
        for (IceBoxRequestVo requestVo : requestNewVos) {
            for (int i = 0; i < requestVo.getApplyCount(); i++) {

                RedisLockUtil lock = new RedisLockUtil(redisTemplate, RedisConstant.ICE_BOX_LOCK + requestVo.getSupplierId()+"-"+requestVo.getModelId()+"-"+i, 5000, 10000);
                try {
                    if (lock.lock()) {
//                        List<IceBox> iceBoxes = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getModelId, requestVo.getModelId()).eq(IceBox::getSupplierId, iceBoxRequestVo.getSupplierId()).eq(IceBox::getPutStatus, PutStatus.NO_PUT.getStatus()));
                        List<IceBox> iceBoxes = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getModelId, requestVo.getModelId()).eq(IceBox::getSupplierId, iceBoxRequestVo.getSupplierId()));
                        List<PutStoreRelateModel> putStoreRelateModels = putStoreRelateModelDao.selectList(Wrappers.<PutStoreRelateModel>lambdaQuery().eq(PutStoreRelateModel::getSupplierId, requestVo.getSupplierId()).eq(PutStoreRelateModel::getModelId, requestVo.getModelId()));
                        int allCount = 0;
                        int putCount = 0;

                        if (CollectionUtil.isNotEmpty(iceBoxes)) {
                            allCount = iceBoxes.size();
                        }
                        if (CollectionUtil.isNotEmpty(putStoreRelateModels)) {
                            putCount = putStoreRelateModels.size();
                        }
                        if(allCount - putCount <= 0){
                            throw new ImproperOptionException("无可申请冰柜");
                        }
                        IceBox iceBox = iceBoxes.get(0);
                        totalMoney = totalMoney.add(iceBox.getDepositMoney());
                        PutStoreRelateModel relateModel = PutStoreRelateModel.builder()
                                .putStoreNumber(requestVo.getStoreNumber())
                                .modelId(requestVo.getModelId())
                                .supplierId(requestVo.getSupplierId())
                                .createBy(requestVo.getUserId())
                                .createTime(new Date())
                                .putStatus(PutStatus.LOCK_PUT.getStatus())
                                .build();
                        putStoreRelateModelDao.insert(relateModel);

                        ApplyRelatePutStoreModel applyRelatePutStoreModel = ApplyRelatePutStoreModel.builder()
                                .applyNumber(applyNumber)
                                .storeRelateModelId(relateModel.getId())
                                .freeType(requestVo.getFreeType())
                                .build();
                        applyRelatePutStoreModelDao.insert(applyRelatePutStoreModel);
                    }
                } catch (Exception e) {
                    throw e;
                } finally {
                    lock.unlock();
                }
            }

            SubordinateInfoVo supplier = FeignResponseUtil.getFeignData(feignSupplierClient.findSupplierBySupplierId(requestVo.getSupplierId()));
            if (supplier == null) {
                log.info("根据经销商id--》【{}】查询不到经销商信息", iceBoxRequestVo.getSupplierId());
                throw new ImproperOptionException("查询不到经销商信息");
            }
            IceBoxPutModel.IceBoxModel iceBoxModel = new IceBoxPutModel.IceBoxModel(requestVo.getChestModel(), requestVo.getChestName(), requestVo.getDepositMoney(), requestVo.getApplyCount(),
                    requestVo.getFreeType(),requestVo.getSupplierName(),supplier.getAddress(),supplier.getLinkman(),supplier.getLinkmanMobile());
            iceBoxModels.add(iceBoxModel);
        }
        boolean regionLeaderCheck = false;
        List<Integer> freeTypes = requestNewVos.stream().map(x -> x.getFreeType()).collect(Collectors.toList());
        if(freeTypes.contains(FreePayTypeEnum.IS_FREE.getType())){
            regionLeaderCheck = true;
        }
        List<SessionExamineVo.VisitExamineNodeVo> iceBoxPutExamine = createIceBoxPutExamine(iceBoxRequestVo, applyNumber, iceBoxModels,regionLeaderCheck);
        map.put("iceBoxPutExamine", iceBoxPutExamine);
        if (CollectionUtil.isNotEmpty(iceBoxPutExamine)) {
            SessionExamineVo.VisitExamineNodeVo visitExamineNodeVo = iceBoxPutExamine.get(0);
            icePutApply.setExamineId(visitExamineNodeVo.getExamineId());
            icePutApplyDao.updateById(icePutApply);
        }
        return map;
    }

    @Override
    public void checkIceBoxNew(IceBoxRequest iceBoxRequest) {
        //审批中
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
                    putStoreRelateModelDao.updateById(putStoreRelateModel);
                }
            }

        }
        //驳回
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
                    putStoreRelateModelDao.updateById(putStoreRelateModel);
                }
            }

        }
        //审批通过将冰箱置为投放中状态，商户签收将状态置为已投放
        if (IceBoxStatus.IS_PUTING.getStatus().equals(iceBoxRequest.getStatus())) {
            IcePutApply icePutApply = icePutApplyDao.selectOne(Wrappers.<IcePutApply>lambdaQuery().eq(IcePutApply::getApplyNumber, iceBoxRequest.getApplyNumber()));
            if (icePutApply != null) {
                icePutApply.setExamineStatus(ExamineStatusEnum.IS_PASS.getStatus());
                icePutApply.setUpdatedBy(0);
                icePutApply.setUpdateTime(new Date());
                icePutApplyDao.updateById(icePutApply);
            }

            List<ApplyRelatePutStoreModel> applyRelatePutStoreModels = applyRelatePutStoreModelDao.selectList(Wrappers.<ApplyRelatePutStoreModel>lambdaQuery().eq(ApplyRelatePutStoreModel::getApplyNumber, iceBoxRequest.getApplyNumber()));
            Set<Integer> storeRelateModelIds = Streams.toStream(applyRelatePutStoreModels).map(x -> x.getStoreRelateModelId()).collect(Collectors.toSet());
            if (CollectionUtil.isNotEmpty(storeRelateModelIds)) {
                for (Integer storeRelateModelId : storeRelateModelIds) {
                    PutStoreRelateModel putStoreRelateModel = PutStoreRelateModel.builder()
                            .id(storeRelateModelId)
                            .putStatus(IceBoxStatus.IS_PUTING.getStatus())
                            .examineStatus(ExamineStatus.PASS_EXAMINE.getStatus())
                            .updateTime(new Date())
                            .build();
                    putStoreRelateModelDao.updateById(putStoreRelateModel);
                }
            }
        }
    }

    @Override
    public List<IceBoxVo> findIceBoxListNew(IceBoxRequestVo requestVo) {
        List<IceBoxVo> iceBoxVos = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //已投放
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
        //可申请
        if (XcxType.NO_PUT.getStatus().equals(requestVo.getType())) {
            if (requestVo.getMarketAreaId() == null) {
                throw new ImproperOptionException("门店营销区域不能为空！");
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
            for(Integer supplierId:iceGroupMap.keySet()){
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
                        LambdaQueryWrapper<PutStoreRelateModel> wrappers = Wrappers.lambdaQuery();
                        wrappers.eq(PutStoreRelateModel::getSupplierId,supplierId);
                        wrappers.eq(PutStoreRelateModel::getModelId,iceBoxVo.getModelId());
                        wrappers.and(x -> x.eq(PutStoreRelateModel::getPutStatus,PutStatus.LOCK_PUT.getStatus()).or().eq(PutStoreRelateModel::getPutStatus,PutStatus.DO_PUT.getStatus()));
                        List<PutStoreRelateModel> putStoreRelateModels = putStoreRelateModelDao.selectList(wrappers);
                        if(CollectionUtil.isNotEmpty(putStoreRelateModels)){
                            count = count - putStoreRelateModels.size();
                        }
                        iceBoxVo.setIceBoxCount(count);
                    }
                    iceBoxVos.addAll(iceBoxVoList);
                }
            }
            if(CollectionUtil.isNotEmpty(iceBoxVos)){
                iceBoxVos = iceBoxVos.stream().filter(x -> x.getIceBoxCount() != null && x.getIceBoxCount() >0).collect(Collectors.toList());
            }
        }
        return iceBoxVos;
    }

    @Override
    public List<PutStoreRelateModel> getIceBoxListNew(String pxtNumber) {
        LambdaQueryWrapper<PutStoreRelateModel> wrappers = Wrappers.lambdaQuery();
        wrappers.eq(PutStoreRelateModel::getPutStoreNumber,pxtNumber);
        wrappers.ne(PutStoreRelateModel::getPutStatus,PutStatus.NO_PUT.getStatus());
        List<PutStoreRelateModel> putStoreRelateModels = putStoreRelateModelDao.selectList(wrappers);
        return putStoreRelateModels;
    }

    @Override
    public IceBoxStatusVo checkIceBoxByQrcodeNew(String qrcode, String pxtNumber) {
        IceBoxStatusVo iceBoxStatusVo = new IceBoxStatusVo();
        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectOne(Wrappers.<IceBoxExtend>lambdaQuery().eq(IceBoxExtend::getQrCode, qrcode));
        if (Objects.isNull(iceBoxExtend)) {
            // 冰柜不存在(二维码未找到)
            iceBoxStatusVo.setSignFlag(false);
            iceBoxStatusVo.setStatus(5);
            iceBoxStatusVo.setMessage("冰柜不存在(二维码未找到)");
            return iceBoxStatusVo;
        }

        IceBox iceBox = iceBoxDao.selectById(iceBoxExtend.getId());
        if(iceBox.getPutStatus().equals(PutStatus.FINISH_PUT.getStatus())){
            if (iceBox.getPutStoreNumber().equals(pxtNumber)) {
                // 已投放到当前门店
                iceBoxStatusVo.setIceBoxId(iceBox.getId());
                iceBoxStatusVo.setSignFlag(false);
                iceBoxStatusVo.setStatus(6);
                iceBoxStatusVo.setMessage("冰柜已投放到当前门店");
                return iceBoxStatusVo;
            }
            // 已有投放, 不能继续
            iceBoxStatusVo.setSignFlag(false);
            iceBoxStatusVo.setStatus(2);
            iceBoxStatusVo.setMessage("冰柜投放到其他门店");
            return iceBoxStatusVo;
        }
        LambdaQueryWrapper<PutStoreRelateModel> wrappers = Wrappers.lambdaQuery();
        wrappers.eq(PutStoreRelateModel::getSupplierId,iceBox.getSupplierId());
        wrappers.eq(PutStoreRelateModel::getModelId,iceBox.getModelId());
        wrappers.eq(PutStoreRelateModel::getPutStoreNumber,pxtNumber);
        wrappers.and(x -> x.eq(PutStoreRelateModel::getPutStatus,PutStatus.LOCK_PUT.getStatus()).or().eq(PutStoreRelateModel::getPutStatus,PutStatus.DO_PUT.getStatus()));
        List<PutStoreRelateModel> putStoreRelateModels = putStoreRelateModelDao.selectList(wrappers);
        if(CollectionUtil.isEmpty(putStoreRelateModels)){
            // 冰柜未申请
            iceBoxStatusVo.setIceBoxId(iceBox.getId());
            iceBoxStatusVo.setSignFlag(false);
            iceBoxStatusVo.setStatus(3);
            iceBoxStatusVo.setMessage("当前门店未申请该冰柜");
            return iceBoxStatusVo;
        }

        List<Integer> putStatus = putStoreRelateModels.stream().map(x -> x.getPutStatus()).collect(Collectors.toList());
        if(!putStatus.contains(PutStatus.DO_PUT.getStatus())){
            // 冰柜在锁定中, 未走完审批流
            iceBoxStatusVo.setIceBoxId(iceBox.getId());
            iceBoxStatusVo.setSignFlag(false);
            iceBoxStatusVo.setStatus(4);
            iceBoxStatusVo.setMessage("冰柜未审批完成");
            return iceBoxStatusVo;
        }

        //获取投放中（已审批完成）的数据
        List<PutStoreRelateModel> doPutList = putStoreRelateModels.stream().filter(x -> PutStatus.DO_PUT.getStatus().equals(x.getPutStatus())).collect(Collectors.toList());
        List<Integer> putStoreRelateModelIds = doPutList.stream().map(x -> x.getId()).collect(Collectors.toList());
        List<ApplyRelatePutStoreModel> applyRelatePutStoreModels = applyRelatePutStoreModelDao.selectList(Wrappers.<ApplyRelatePutStoreModel>lambdaQuery().in(ApplyRelatePutStoreModel::getStoreRelateModelId, putStoreRelateModelIds));
        if(CollectionUtil.isEmpty(applyRelatePutStoreModels)){
            // 冰柜未申请
            iceBoxStatusVo.setIceBoxId(iceBox.getId());
            iceBoxStatusVo.setSignFlag(false);
            iceBoxStatusVo.setStatus(3);
            iceBoxStatusVo.setMessage("当前门店未申请该冰柜");
            return iceBoxStatusVo;
        }

        List<String> applyNumbers = applyRelatePutStoreModels.stream().map(x -> x.getApplyNumber()).collect(Collectors.toList());
        List<IcePutApply> icePutApplies = icePutApplyDao.selectList(Wrappers.<IcePutApply>lambdaQuery().in(IcePutApply::getApplyNumber, applyNumbers));
        if(CollectionUtil.isEmpty(icePutApplies)){
            // 冰柜未申请
            iceBoxStatusVo.setIceBoxId(iceBox.getId());
            iceBoxStatusVo.setSignFlag(false);
            iceBoxStatusVo.setStatus(3);
            iceBoxStatusVo.setMessage("当前门店未申请该冰柜");
            return iceBoxStatusVo;
        }

        // 冰柜处于投放中, 可以签收的状态
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
        IceModel iceModel = iceModelDao.selectById(Objects.requireNonNull(iceBox).getModelId());
        LambdaQueryWrapper<PutStoreRelateModel> wrapper = Wrappers.<PutStoreRelateModel>lambdaQuery();
        wrapper.eq(PutStoreRelateModel::getPutStoreNumber,pxtNumber);
        wrapper.eq(PutStoreRelateModel::getModelId,iceBox.getModelId());
        wrapper.eq(PutStoreRelateModel::getSupplierId,iceBox.getSupplierId());
        wrapper.eq(PutStoreRelateModel::getPutStatus, PutStatus.DO_PUT.getStatus());
        List<PutStoreRelateModel> relateModelList = putStoreRelateModelDao.selectList(wrapper);
        if(CollectionUtil.isEmpty(relateModelList)){
            return IceBoxConverter.convertToVo(Objects.requireNonNull(iceBox),
                    Objects.requireNonNull(iceBoxExtend),
                    Objects.requireNonNull(iceModel),
                    FreePayTypeEnum.UN_FREE);
        }
        PutStoreRelateModel relateModel = relateModelList.get(0);
        ApplyRelatePutStoreModel applyRelatePutStoreModel = applyRelatePutStoreModelDao.selectOne(Wrappers.<ApplyRelatePutStoreModel>lambdaQuery().eq(ApplyRelatePutStoreModel::getStoreRelateModelId, relateModel.getId()));
        IcePutApply icePutApply = icePutApplyDao.selectOne(Wrappers.<IcePutApply>lambdaQuery().eq(IcePutApply::getApplyNumber, applyRelatePutStoreModel.getApplyNumber()));

        //创建冰柜和投放申请编号的关联关系
        IcePutApplyRelateBox isExist = icePutApplyRelateBoxDao.selectOne(Wrappers.<IcePutApplyRelateBox>lambdaQuery().eq(IcePutApplyRelateBox::getApplyNumber, icePutApply.getApplyNumber()).eq(IcePutApplyRelateBox::getBoxId, iceBox.getId()));
        if(isExist == null){
            IcePutApplyRelateBox applyRelateBox = new IcePutApplyRelateBox();
            applyRelateBox.setApplyNumber(icePutApply.getApplyNumber());
            applyRelateBox.setBoxId(iceBox.getId());
            applyRelateBox.setModelId(iceBox.getModelId());
            applyRelateBox.setFreeType(applyRelatePutStoreModel.getFreeType());
            icePutApplyRelateBoxDao.insert(applyRelateBox);

            //更新和冰柜关联关系
            iceBox.setPutStoreNumber(pxtNumber);
            iceBox.setPutStatus(PutStatus.DO_PUT.getStatus());
            iceBox.setUpdatedTime(new Date());
            iceBoxDao.updateById(iceBox);

            iceBoxExtend.setLastApplyNumber(icePutApply.getApplyNumber());
            iceBoxExtend.setLastPutTime(icePutApply.getCreatedTime());
            iceBoxExtend.setLastPutId(icePutApply.getId());
            iceBoxExtend.setLastExamineId(icePutApply.getExamineId());
            iceBoxExtend.setLastExamineTime(icePutApply.getUpdateTime());
            iceBoxExtendDao.updateById(iceBoxExtend);
        }

        return IceBoxConverter.convertToVo(Objects.requireNonNull(iceBox),
                Objects.requireNonNull(iceBoxExtend),
                Objects.requireNonNull(iceModel),
                Objects.isNull(applyRelatePutStoreModel) ? FreePayTypeEnum.UN_FREE : FreePayTypeEnum.convertVo(applyRelatePutStoreModel.getFreeType()));
    }

    @Override
    public void autoAddLabel() {
        // 查找所有已投放的冰柜

        List<IceBox> iceBoxList = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery()
                .eq(IceBox::getPutStatus, PutStatus.FINISH_PUT.getStatus()).ne(IceBox::getSupplierId,0));

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
                    if(icePutPactRecord != null) {
                        Date putTime = icePutPactRecord.getPutTime();
                        Date putExpireTime = icePutPactRecord.getPutExpireTime();
                        String assetId = iceBoxExtend.getAssetId();
                        //存在门店
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
        customerLabelDetailDto.setCreateByName("系统");
        customerLabelDetailDto.setPutProject("冰柜");
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
}