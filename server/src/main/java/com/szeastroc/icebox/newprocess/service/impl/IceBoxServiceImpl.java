package com.szeastroc.icebox.newprocess.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.utils.ExecutorServiceFactory;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.customer.client.FeignStoreClient;
import com.szeastroc.customer.client.FeignSupplierClient;
import com.szeastroc.customer.common.vo.SimpleSupplierInfoVo;
import com.szeastroc.customer.common.vo.StoreInfoDtoVo;
import com.szeastroc.customer.common.vo.SubordinateInfoVo;
import com.szeastroc.icebox.constant.IceBoxConstant;
import com.szeastroc.icebox.constant.RedisConstant;
import com.szeastroc.icebox.enums.FreePayTypeEnum;
import com.szeastroc.icebox.newprocess.convert.IceBoxConverter;
import com.szeastroc.icebox.newprocess.dao.*;
import com.szeastroc.icebox.newprocess.entity.*;
import com.szeastroc.icebox.newprocess.enums.PutStatus;
import com.szeastroc.icebox.newprocess.enums.XcxType;
import com.szeastroc.icebox.newprocess.service.IceBoxService;
import com.szeastroc.icebox.newprocess.vo.*;
import com.szeastroc.icebox.newprocess.vo.request.IceBoxRequestVo;
import com.szeastroc.icebox.oldprocess.dao.IceEventRecordDao;
import com.szeastroc.icebox.oldprocess.entity.IceEventRecord;
import com.szeastroc.icebox.util.redis.RedisLockUtil;
import com.szeastroc.user.client.FeignDeptClient;
import com.szeastroc.user.client.FeignUserClient;
import com.szeastroc.user.common.vo.SessionUserInfoVo;
import com.szeastroc.user.common.vo.SimpleUserInfoVo;
import com.szeastroc.visit.client.FeignExamineClient;
import com.szeastroc.visit.common.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @_(@Autowired))
public class IceBoxServiceImpl extends ServiceImpl<IceBoxDao, IceBox> implements IceBoxService {


    private ExecutorService executorService = ExecutorServiceFactory.getInstance();

    @Resource
    private IceBoxDao iceBoxDao;
    @Resource
    private IceBoxExtendDao iceBoxExtendDao;
    @Resource
    private IceModelDao iceModelDao;
    @Resource
    private FeignDeptClient feignDeptClient;
    @Resource
    private FeignSupplierClient feignSupplierClient;
    @Resource
    private IcePutApplyDao icePutApplyDao;
    @Resource
    private IceBackApplyDao iceBackApplyDao;
    @Resource
    private IcePutApplyRelateBoxDao icePutApplyRelateBoxDao;
    @Resource
    private IceBackApplyRelateBoxDao iceBackApplyRelateBoxDao;
    @Resource
    private FeignExamineClient feignExamineClient;

    @Resource
    private RedisTemplate redisTemplate;
    @Resource
    private IceExamineDao iceExamineDao;
    @Resource
    private FeignUserClient feignUserClient;

    private final IcePutPactRecordDao icePutPactRecordDao;
    private final FeignStoreClient feignStoreClient;
    private final IceEventRecordDao iceEventRecordDao;

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
            List<IceBox> iceBoxes = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().in(IceBox::getSupplierId, supplierIds).eq(IceBox::getPutStatus, PutStatus.NO_PUT.getStatus()));
            if (CollectionUtil.isEmpty(iceBoxes)) {
                return iceBoxVos;
            }
            Map<Integer, Integer> iceBoxCountMap = new HashMap<>();
            for (IceBox iceBox : iceBoxes) {
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
                }
                iceBoxCountMap.put(iceBox.getModelId(), 1);
                iceBoxVos.add(boxVo);
            }
            if (CollectionUtil.isNotEmpty(iceBoxVos)) {
                for (IceBoxVo iceBoxVo : iceBoxVos) {
                    Integer count = iceBoxCountMap.get(iceBoxVo.getModelId());
                    iceBoxVo.setIceBoxCount(count);
                }
            }
        }
        //处理中
        if (XcxType.IS_PUTING.getStatus().equals(requestVo.getType())) {
            List<IcePutApply> icePutApplies = icePutApplyDao.selectList(Wrappers.<IcePutApply>lambdaQuery().eq(IcePutApply::getPutStoreNumber, requestVo.getStoreNumber()));
            if (CollectionUtil.isNotEmpty(icePutApplies)) {
                List<IceBoxVo> putIceBoxVos = this.getIceBoxVosByPutApplys(icePutApplies);
                if (CollectionUtil.isNotEmpty(putIceBoxVos)) {
                    iceBoxVos.addAll(putIceBoxVos);
                }
            }
            List<IceBackApply> iceBackApplies = iceBackApplyDao.selectList(Wrappers.<IceBackApply>lambdaQuery().eq(IceBackApply::getBackStoreNumber, requestVo.getStoreNumber()));
            if (CollectionUtil.isNotEmpty(iceBackApplies)) {
                List<IceBoxVo> backIceBoxVos = this.getIceBoxVosByBackApplys(iceBackApplies);
                if (CollectionUtil.isNotEmpty(backIceBoxVos)) {
                    iceBoxVos.addAll(backIceBoxVos);
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
        for(IceBoxRequestVo requestVo:iceBoxRequestVos){
            for(int i = 0;i<requestVo.getApplyCount();i++){
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

                        IceBoxExtend iceBoxExtend = new IceBoxExtend();
                        iceBoxExtend.setId(iceBox.getId());
                        iceBoxExtend.setLastApplyNumber(applyNumber);
                        iceBoxExtend.setLastPutId(icePutApply.getId());
                        iceBoxExtend.setLastPutTime(new Date());
                        iceBoxExtendDao.updateById(iceBoxExtend);

                        IceBoxPutModel.IceBoxModel iceBoxModel = new IceBoxPutModel.IceBoxModel(requestVo.getChestModel(),iceBox.getChestName(),iceBox.getDepositMoney(),requestVo.getApplyCount(),requestVo.getIsFree());
                        iceBoxModels.add(iceBoxModel);
                    }
                } catch (Exception e) {
                    throw e;
                } finally {
                    lock.unlock();
                }
            }
        }


        List<SessionExamineVo.VisitExamineNodeVo> iceBoxPutExamine = createIceBoxPutExamine(iceBoxRequestVo, applyNumber, iceBoxModels);
        map.put("iceBoxPutExamine",iceBoxPutExamine);
        if(CollectionUtil.isNotEmpty(iceBoxPutExamine)){
            SessionExamineVo.VisitExamineNodeVo visitExamineNodeVo = iceBoxPutExamine.get(0);
            icePutApply.setExamineId(visitExamineNodeVo.getExamineId());
            icePutApplyDao.updateById(icePutApply);
        }
        return map;
    }

    private List<SessionExamineVo.VisitExamineNodeVo> createIceBoxPutExamine(IceBoxRequestVo iceBoxRequestVo, String applyNumber, List<IceBoxPutModel.IceBoxModel> iceBoxModels) {
        // 创建审批流

        SimpleUserInfoVo simpleUserInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findSimpleUserById(iceBoxRequestVo.getUserId()));
        Map<Integer, SessionUserInfoVo> sessionUserInfoMap = FeignResponseUtil.getFeignData(feignDeptClient.findLevelLeaderByDeptId(simpleUserInfoVo.getSimpleDeptInfoVos().get(0).getId()));
//        List<Integer> userIds = new ArrayList<Integer>();
//        获取上级部门领导
//        SessionUserInfoVo userInfoVo1 = sessionUserInfoMap.get(1);
//        SessionUserInfoVo userInfoVo2 = sessionUserInfoMap.get(2);
//        SessionUserInfoVo userInfoVo3 = sessionUserInfoMap.get(2);
//        if (userInfoVo1 == null || userInfoVo2 == null || userInfoVo3 == null) {
//            throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败，找不到上级审批人！");
//        }
//        userIds.add(userInfoVo1.getId());
//        userIds.add(userInfoVo2.getId());
//        userIds.add(userInfoVo3.getId());

        List<Integer> userIds = Arrays.asList(5941, 2103);
        SessionExamineVo sessionExamineVo = new SessionExamineVo();
        IceBoxPutModel iceBoxPutModel = new IceBoxPutModel();

        iceBoxPutModel.setApplyNumber(applyNumber);
        SubordinateInfoVo supplier = FeignResponseUtil.getFeignData(feignSupplierClient.findSupplierBySupplierId(iceBoxRequestVo.getSupplierId()));
        if(supplier == null){
            log.info("根据经销商id--》【{}】查询不到经销商信息",iceBoxRequestVo.getSupplierId());
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
            IceBackApply backApply = iceBackApplyMap.get(iceBackApplyRelateBox.getApplyNumber());
            if (backApply == null) {
                continue;
            }
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
            IcePutApply putApply = icePutApplyMap.get(icePutApplyRelateBox.getApplyNumber());
            if (putApply == null) {
                continue;
            }
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

            IceBoxStoreVo iceBoxStoreVo = IceBoxConverter.convertToStoreVo(iceBox, iceBoxExtend, iceModel, icePutApplyRelateBox, iceEventRecord);
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
}








