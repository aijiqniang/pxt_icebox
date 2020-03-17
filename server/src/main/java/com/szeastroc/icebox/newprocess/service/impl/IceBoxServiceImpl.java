package com.szeastroc.icebox.newprocess.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.utils.ExecutorServiceFactory;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.customer.client.FeignSupplierClient;
import com.szeastroc.customer.common.vo.SimpleSupplierInfoVo;
import com.szeastroc.icebox.constant.IceBoxConstant;
import com.szeastroc.icebox.newprocess.convert.IceBoxConverter;
import com.szeastroc.icebox.newprocess.dao.*;
import com.szeastroc.icebox.newprocess.entity.*;
import com.szeastroc.icebox.newprocess.enums.ExamineStatus;
import com.szeastroc.icebox.newprocess.enums.PutStatus;
import com.szeastroc.icebox.newprocess.enums.XcxType;
import com.szeastroc.icebox.newprocess.service.IceBoxService;
import com.szeastroc.icebox.newprocess.vo.ExamineNodeVo;
import com.szeastroc.icebox.newprocess.vo.IceBoxStatusVo;
import com.szeastroc.icebox.newprocess.vo.IceBoxStoreVo;
import com.szeastroc.icebox.newprocess.vo.IceBoxVo;
import com.szeastroc.icebox.newprocess.vo.request.IceBoxRequestVo;
import com.szeastroc.icebox.oldprocess.dao.IceEventRecordDao;
import com.szeastroc.icebox.oldprocess.entity.IceEventRecord;
import com.szeastroc.user.client.FeignDeptClient;
import com.szeastroc.visit.client.FeignExamineClient;
import com.szeastroc.visit.common.SessionExamineVo;
import com.szeastroc.visit.common.request.RequestExamineVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StopWatch;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
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

    private final IceEventRecordDao iceEventRecordDao;

    @Override
    public List<IceBoxVo> findIceBoxList(IceBoxRequestVo requestVo) {

        List<IceBoxVo> iceBoxVos = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //已投放
        if(XcxType.IS_PUTED.getStatus().equals(requestVo.getType())){
            List<IceBox> iceBoxes = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getPutStoreNumber, requestVo.getStoreNumber()).eq(IceBox::getPutStatus, PutStatus.IS_PUTED.getStatus()));
            if(CollectionUtil.isEmpty(iceBoxes)){
                return iceBoxVos;
            }
            for(IceBox iceBox:iceBoxes){
                IceBoxVo boxVo = buildIceBoxVo(dateFormat, iceBox);
                iceBoxVos.add(boxVo);
            }
        }
        //可申请
        if(XcxType.NO_PUT.getStatus().equals(requestVo.getType())){
            if(requestVo.getMarketAreaId() == null){
                throw new ImproperOptionException("门店营销区域不能为空！");
            }
            Integer serviceId = FeignResponseUtil.getFeignData(feignDeptClient.getServiceId(requestVo.getMarketAreaId()));
            List<SimpleSupplierInfoVo> supplierInfoVos = FeignResponseUtil.getFeignData(feignSupplierClient.findByDeptId(serviceId));
            if(CollectionUtil.isEmpty(supplierInfoVos)){
                return iceBoxVos;
            }
            Set<Integer> supplierIds = supplierInfoVos.stream().map(x -> x.getId()).collect(Collectors.toSet());
            Map<Integer, SimpleSupplierInfoVo> supplierInfoVoMap = supplierInfoVos.stream().collect(Collectors.toMap(SimpleSupplierInfoVo::getId, x -> x));
            List<IceBox> iceBoxes = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().in(IceBox::getSupplierId, supplierIds).eq(IceBox::getPutStatus, PutStatus.NO_PUT.getStatus()));
            if(CollectionUtil.isEmpty(iceBoxes)){
                return iceBoxVos;
            }
            Map<Integer,Integer> iceBoxCountMap = new HashMap<>();
            for(IceBox iceBox:iceBoxes){
                Integer count = iceBoxCountMap.get(iceBox.getModelId());
                if(count != null){
                    count = count + 1;
                    iceBoxCountMap.put(iceBox.getModelId(),count);
                    continue;
                }
                IceBoxVo boxVo = buildIceBoxVo(dateFormat, iceBox);
                SimpleSupplierInfoVo simpleSupplierInfoVo = supplierInfoVoMap.get(iceBox.getSupplierId());
                if(simpleSupplierInfoVo != null){
                    boxVo.setSupplierName(simpleSupplierInfoVo.getName());
                }
                iceBoxCountMap.put(iceBox.getModelId(),1);
                iceBoxVos.add(boxVo);
            }
            if(CollectionUtil.isNotEmpty(iceBoxVos)){
                for(IceBoxVo iceBoxVo:iceBoxVos){
                    Integer count = iceBoxCountMap.get(iceBoxVo.getModelId());
                    iceBoxVo.setIceBoxCount(count);
                }
            }
        }
        //处理中
        if(XcxType.IS_PUTING.getStatus().equals(requestVo.getType())){
            List<IcePutApply> icePutApplies = icePutApplyDao.selectList(Wrappers.<IcePutApply>lambdaQuery().eq(IcePutApply::getPutStoreNumber, requestVo.getStoreNumber()));
            if(CollectionUtil.isNotEmpty(icePutApplies)){
                List<IceBoxVo> putIceBoxVos = this.getIceBoxVosByPutApplys(icePutApplies);
                if(CollectionUtil.isNotEmpty(putIceBoxVos)){
                    iceBoxVos.addAll(putIceBoxVos);
                }
            }
            List<IceBackApply> iceBackApplies = iceBackApplyDao.selectList(Wrappers.<IceBackApply>lambdaQuery().eq(IceBackApply::getBackStoreNumber, requestVo.getStoreNumber()));
            if(CollectionUtil.isNotEmpty(iceBackApplies)){
                List<IceBoxVo> backIceBoxVos = this.getIceBoxVosByBackApplys(iceBackApplies);
                if(CollectionUtil.isNotEmpty(backIceBoxVos)){
                    iceBoxVos.addAll(backIceBoxVos);
                }
            }
        }
        return iceBoxVos;
    }

    @Override
    public IceBoxVo findBySupplierIdAndModelId(Integer supplierId, Integer modelId) {
//        SubordinateInfoVo subordinateInfoVo = FeignResponseUtil.getFeignData(feignSupplierClient.findSupplierBySupplierId(supplierId));
//        if(subordinateInfoVo == null){
//            throw new ImproperOptionException("无法获取经销商信息");
//        }
//        List<IceBox> iceBoxes = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getModelId, modelId).eq(IceBox::getSupplierId, supplierId).eq(IceBox::getPutStatus, PutStatus.NO_PUT.getStatus()));
//        if(CollectionUtil.isNotEmpty(iceBoxes)){
//            IceBox iceBox = iceBoxes.get(0);
//            IceBoxVo iceBoxVo = new IceBoxVo();
//            BeanUtils.copyProperties(iceBox,iceBoxVo);
//            iceBoxVo.setSupplierName(subordinateInfoVo.getName());
//            iceBoxVo.setSupplierAddress(subordinateInfoVo.getAddress());
//            iceBoxVo.setLinkman(subordinateInfoVo.getLinkman());
//            iceBoxVo.setLinkmanMobile(subordinateInfoVo.getLinkmanMobile());
//            return iceBoxVo;
//        }
        return null;
    }

    @Override
    public Map<String, String> submitApply(IceBoxRequestVo iceBoxRequestVo) {
        // 返回锁的value值，供释放锁时候进行判断

        IcePutApply icePutApply = new IcePutApply();
        String applyNumber = "PUT" + IdUtil.simpleUUID().substring(0, 29);
        icePutApply.setApplyNumber(applyNumber);


        StopWatch watch = new StopWatch();
        watch.start();
        IceBoxApplyThread iceBoxApplyThread = new IceBoxApplyThread(iceBoxDao,iceBoxRequestVo);

        executorService.submit(iceBoxApplyThread);
        try {
            executorService.shutdown();
            watch.stop();
            System.out.println("耗 时:" + watch.getTotalTimeSeconds() + "秒");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private List<IceBoxVo> getIceBoxVosByBackApplys(List<IceBackApply> iceBackApplies) {
        List<IceBoxVo> iceBoxVos = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Map<String, IceBackApply> iceBackApplyMap = iceBackApplies.stream().collect(Collectors.toMap(IceBackApply::getApplyNumber, x -> x));
        List<Integer> examineIds = iceBackApplies.stream().map(x -> x.getExamineId()).collect(Collectors.toList());
        RequestExamineVo examineVo = new RequestExamineVo();
        examineVo.setExamineInfoIds(examineIds);
        List<SessionExamineVo> sessionExamineVos = FeignResponseUtil.getFeignData(feignExamineClient.getExamineNodesByList(examineVo));
        if(CollectionUtil.isEmpty(sessionExamineVos)){
            log.error("退押查询不到审批流信息");
            return iceBoxVos;
        }
        Map<Integer, SessionExamineVo> sessionExamineVoMap = sessionExamineVos.stream().collect(Collectors.toMap(SessionExamineVo::getExamineInfoId, x -> x));
        Set<String> applyNumbers = iceBackApplies.stream().map(x -> x.getApplyNumber()).collect(Collectors.toSet());
        List<IceBackApplyRelateBox> iceBackApplyRelateBoxes = iceBackApplyRelateBoxDao.selectList(Wrappers.<IceBackApplyRelateBox>lambdaQuery().in(IceBackApplyRelateBox::getApplyNumber, applyNumbers));
        if(CollectionUtil.isEmpty(iceBackApplyRelateBoxes)){
            log.error("查询不到申请退押信息和冰柜的关联关系");
            return iceBoxVos;
        }
        Map<Integer, IceBackApplyRelateBox> relateBoxMap = iceBackApplyRelateBoxes.stream().collect(Collectors.toMap(IceBackApplyRelateBox::getBoxId, x -> x));

        Set<Integer> boxIds = iceBackApplyRelateBoxes.stream().map(x -> x.getBoxId()).collect(Collectors.toSet());
        List<IceBox> iceBoxes = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().in(IceBox::getId, boxIds));
        if(CollectionUtil.isEmpty(iceBoxes)){
            log.error("查询不到申请退押信息关联的冰柜详情");
            return iceBoxVos;
        }
        for(IceBox iceBox:iceBoxes){
            IceBoxVo boxVo = buildIceBoxVo(dateFormat, iceBox);
            boxVo.setStatusStr(IceBoxConstant.IS_BACKING);
            IceBackApplyRelateBox iceBackApplyRelateBox = relateBoxMap.get(iceBox.getId());
            if(iceBackApplyRelateBox == null){
                continue;
            }
            IceBackApply backApply = iceBackApplyMap.get(iceBackApplyRelateBox.getApplyNumber());
            if(backApply == null){
                continue;
            }
            SessionExamineVo sessionExamineVo = sessionExamineVoMap.get(backApply.getExamineId());
            if(sessionExamineVo == null){
                continue;
            }
            List<SessionExamineVo.SessionVisitExamineNodeVo> visitExamineNodes = sessionExamineVo.getVisitExamineNodes();
            if(CollectionUtil.isNotEmpty(visitExamineNodes)){
                List<ExamineNodeVo> nodeVos = new ArrayList<>();
                for(SessionExamineVo.SessionVisitExamineNodeVo sessionVisitExamineNodeVo:visitExamineNodes){
                    ExamineNodeVo nodeVo = new ExamineNodeVo();
                    BeanUtils.copyProperties(sessionVisitExamineNodeVo,nodeVo);
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
        if(CollectionUtil.isEmpty(sessionExamineVos)){
            log.error("投放查询不到审批流信息");
            return iceBoxVos;
        }
        Map<Integer, SessionExamineVo> sessionExamineVoMap = sessionExamineVos.stream().collect(Collectors.toMap(SessionExamineVo::getExamineInfoId, x -> x));
        Set<String> applyNumbers = icePutApplies.stream().map(x -> x.getApplyNumber()).collect(Collectors.toSet());
        List<IcePutApplyRelateBox> icePutApplyRelateBoxes = icePutApplyRelateBoxDao.selectList(Wrappers.<IcePutApplyRelateBox>lambdaQuery().in(IcePutApplyRelateBox::getApplyNumber, applyNumbers));
        if(CollectionUtil.isEmpty(icePutApplyRelateBoxes)){
            log.error("查询不到申请投放信息和冰柜的关联关系");
            return iceBoxVos;
        }
        Map<Integer, IcePutApplyRelateBox> relateBoxMap = icePutApplyRelateBoxes.stream().collect(Collectors.toMap(IcePutApplyRelateBox::getBoxId, x -> x));

        Set<Integer> boxIds = icePutApplyRelateBoxes.stream().map(x -> x.getBoxId()).collect(Collectors.toSet());
        List<IceBox> iceBoxes = iceBoxDao.selectList(Wrappers.<IceBox>lambdaQuery().in(IceBox::getId, boxIds));
        if(CollectionUtil.isEmpty(iceBoxes)){
            log.error("查询不到申请投放信息关联的冰柜详情");
            return iceBoxVos;
        }
        for(IceBox iceBox:iceBoxes){
            IceBoxVo boxVo = buildIceBoxVo(dateFormat, iceBox);
            boxVo.setStatusStr(IceBoxConstant.IS_APPLYING);
            IcePutApplyRelateBox icePutApplyRelateBox = relateBoxMap.get(iceBox.getId());
            if(icePutApplyRelateBox == null){
                continue;
            }
            IcePutApply putApply = icePutApplyMap.get(icePutApplyRelateBox.getApplyNumber());
            if(putApply == null){
                continue;
            }
            SessionExamineVo sessionExamineVo = sessionExamineVoMap.get(putApply.getExamineId());
            if(sessionExamineVo == null){
                continue;
            }
            List<SessionExamineVo.SessionVisitExamineNodeVo> visitExamineNodes = sessionExamineVo.getVisitExamineNodes();
            if(CollectionUtil.isNotEmpty(visitExamineNodes)){
                List<ExamineNodeVo> nodeVos = new ArrayList<>();
                for(SessionExamineVo.SessionVisitExamineNodeVo sessionVisitExamineNodeVo:visitExamineNodes){
                    ExamineNodeVo nodeVo = new ExamineNodeVo();
                    BeanUtils.copyProperties(sessionVisitExamineNodeVo,nodeVo);
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

    /**
     * 根据 鹏讯通编号(门店) 找到该门店对应的投放冰柜, 并拼接Vo返回
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
     * @param qrcode
     * @param pxtNumber
     * @return
     */
    @Override
    public IceBoxStatusVo checkBoxByQrcode(String qrcode, String pxtNumber) {

        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectOne(Wrappers.<IceBoxExtend>lambdaQuery().eq(IceBoxExtend::getQrCode, qrcode));
        if(Objects.isNull(iceBoxExtend)){
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
        switch (Objects.requireNonNull(PutStatus.convertEnum(iceBox.getStatus()))){
            case NO_PUT:
                // 冰柜未申请
                iceBoxStatusVo.setSignFlag(false);
                iceBoxStatusVo.setStatus(3);
                iceBoxStatusVo.setMessage("当前门店未申请该冰柜");
                break;
            case LOCK_PUT:
                iceBoxStatusVo = checkPutApplyByApplyNumber(applyNumber, pxtNumber);
                break;
            case DO_PUT:
            case FINISH_PUT:
                // 已有投放, 不能继续
                iceBoxStatusVo.setSignFlag(false);
                iceBoxStatusVo.setStatus(2);
                iceBoxStatusVo.setMessage("冰柜已投放");
                break;
        }
        return iceBoxStatusVo;
    }

    /**
     * 判断当前冰柜的投放申请信息
     * @param applyNumber
     * @param pxtNumber
     * @return
     */
    private IceBoxStatusVo checkPutApplyByApplyNumber(String applyNumber, String pxtNumber) {
        IceBoxStatusVo iceBoxStatusVo = new IceBoxStatusVo();

        IcePutApply icePutApply = icePutApplyDao.selectOne(Wrappers.<IcePutApply>lambdaQuery().eq(IcePutApply::getApplyNumber, applyNumber));
        if(!icePutApply.getPutStoreNumber().equals(pxtNumber)){
            // 冰柜申请门店非当前门店, 返回已投放的提示
            iceBoxStatusVo.setSignFlag(false);
            iceBoxStatusVo.setStatus(2);
            iceBoxStatusVo.setMessage("冰柜已投放");
            return iceBoxStatusVo;
        }
        if(!icePutApply.getExamineStatus().equals(ExamineStatus.PASS_EXAMINE.getStatus())){
            // 冰柜申请的审批流未完成
            iceBoxStatusVo.setSignFlag(false);
            iceBoxStatusVo.setStatus(4);
            iceBoxStatusVo.setMessage("申请审批未完成");
            return iceBoxStatusVo;
        }

        // 该冰柜是当前门店申请的, 并且审批流已完成, 可以进行签收
        iceBoxStatusVo.setSignFlag(true);
        iceBoxStatusVo.setStatus(1);
        return iceBoxStatusVo;
    }
}








