package com.szeastroc.icebox.newprocess.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.exception.NormalOptionException;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.customer.client.FeignStoreClient;
import com.szeastroc.customer.client.FeignSupplierClient;
import com.szeastroc.customer.common.vo.StoreInfoDtoVo;
import com.szeastroc.customer.common.vo.SubordinateInfoVo;
import com.szeastroc.icebox.enums.IceBoxStatus;
import com.szeastroc.icebox.newprocess.dao.IceBoxDao;
import com.szeastroc.icebox.newprocess.dao.IceBoxExtendDao;
import com.szeastroc.icebox.newprocess.dao.IceExamineDao;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.entity.IceBoxExtend;
import com.szeastroc.icebox.newprocess.entity.IceExamine;
import com.szeastroc.icebox.newprocess.enums.*;
import com.szeastroc.icebox.newprocess.service.IceExamineService;
import com.szeastroc.icebox.newprocess.vo.IceExamineVo;
import com.szeastroc.icebox.newprocess.vo.request.IceExamineRequest;
import com.szeastroc.icebox.oldprocess.dao.IceEventRecordDao;
import com.szeastroc.icebox.oldprocess.entity.IceEventRecord;
import com.szeastroc.user.client.FeignDeptClient;
import com.szeastroc.user.client.FeignUserClient;
import com.szeastroc.user.common.vo.SessionDeptInfoVo;
import com.szeastroc.user.common.vo.SessionUserInfoVo;
import com.szeastroc.user.common.vo.SimpleUserInfoVo;
import com.szeastroc.visit.client.FeignBacklogClient;
import com.szeastroc.visit.client.FeignExamineClient;
import com.szeastroc.visit.common.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.*;
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

        Integer iceExamineId = iceExamine.getId();
        IceBoxExtend iceBoxExtend = new IceBoxExtend();
        iceBoxExtend.setLastExamineId(iceExamineId);
        iceBoxExtend.setLastExamineTime(new Date());

        iceBoxExtendDao.update(iceBoxExtend, Wrappers.<IceBoxExtend>lambdaUpdate().eq(IceBoxExtend::getId, iceBoxId));
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
        IceExamine iceExamine = new IceExamine();
        BeanUtils.copyProperties(iceExamineVo,iceExamine);
        Map<String, Object> map = new HashMap<>();
        //冰柜状态是正常，巡检也是正常，不需要审批
        if(IceBoxEnums.StatusEnum.NORMAL.equals(iceExamineVo.getIceStatus()) && IceBoxEnums.StatusEnum.NORMAL.equals(iceExamineVo.getIceExamineStatus())){

            doExamine(iceExamine);
        }
        //冰柜状态是报废，巡检是正常，需要走与报废相同的审批
        if(IceBoxEnums.StatusEnum.SCRAP.equals(iceExamineVo.getIceStatus()) && IceBoxEnums.StatusEnum.NORMAL.equals(iceExamineVo.getIceExamineStatus())){

            map = createExamineCheckProcess(iceExamineVo,map);
        }
        //冰柜状态是遗失，巡检是正常，需要走与遗失相同的审批
        if(IceBoxEnums.StatusEnum.LOSE.equals(iceExamineVo.getIceStatus()) && IceBoxEnums.StatusEnum.NORMAL.equals(iceExamineVo.getIceExamineStatus())){

            map = createExamineCheckProcess(iceExamineVo,map);
        }
        //冰柜状态是报修，巡检是正常，需要走与报修相同的审批
        if(IceBoxEnums.StatusEnum.REPAIR.equals(iceExamineVo.getIceStatus()) && IceBoxEnums.StatusEnum.NORMAL.equals(iceExamineVo.getIceExamineStatus())){

            map = createExamineCheckProcess(iceExamineVo,map);
        }
        //冰柜状态不是报废，巡检是报废，需要走报废审批
        if(!IceBoxEnums.StatusEnum.SCRAP.equals(iceExamineVo.getIceStatus()) && IceBoxEnums.StatusEnum.SCRAP.equals(iceExamineVo.getIceExamineStatus())){

            map = createExamineCheckProcess(iceExamineVo,map);
        }

        //冰柜状态不是遗失，巡检是遗失，需要走遗失审批
        if(!IceBoxEnums.StatusEnum.LOSE.equals(iceExamineVo.getIceStatus()) && IceBoxEnums.StatusEnum.LOSE.equals(iceExamineVo.getIceExamineStatus())){

            map = createExamineCheckProcess(iceExamineVo,map);
        }

        //冰柜状态不是报修，巡检是报修，需要走报修通知上级
        if(!IceBoxEnums.StatusEnum.REPAIR.equals(iceExamineVo.getIceStatus()) && IceBoxEnums.StatusEnum.REPAIR.equals(iceExamineVo.getIceExamineStatus())){

            SimpleUserInfoVo simpleUserInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findSimpleUserById(iceExamineVo.getCreateBy()));
            Map<Integer, SessionUserInfoVo> sessionUserInfoMap = FeignResponseUtil.getFeignData(feignDeptClient.findLevelLeaderByDeptIdNew(iceExamineVo.getMarketAreaId()));
            List<Integer> ids = new ArrayList<Integer>();
            //获取上级部门领导
            SessionUserInfoVo groupUser = new SessionUserInfoVo();
            SessionUserInfoVo serviceUser = new SessionUserInfoVo();
            Set<Integer> keySet = sessionUserInfoMap.keySet();
            for (Integer key : keySet) {
                SessionUserInfoVo userInfoVo = sessionUserInfoMap.get(key);
                if(userInfoVo == null){
                    continue;
                }
                if(DeptTypeEnum.GROUP.getType().equals(userInfoVo.getDeptType())){
                    groupUser = userInfoVo;
                    continue;
                }
                if(DeptTypeEnum.SERVICE.getType().equals(userInfoVo.getDeptType())){
                    serviceUser = userInfoVo;
                    continue;
                }

            }
            if(groupUser.getId() != null){
                ids.add(groupUser.getId());
            }

            if(serviceUser.getId() != null && !ids.contains(serviceUser.getId())){
                ids.add(groupUser.getId());
            }

            if(CollectionUtil.isNotEmpty(ids)){
                for(Integer id:ids){
                    SessionVisitExamineBacklog backlog = new SessionVisitExamineBacklog();
                    backlog.setBacklogName(iceExamineVo.getCreateName()+"冰柜报修通知信息");
                    backlog.setCode(iceExamineVo.getAssetId());
                    backlog.setExamineStatus(ExamineStatus.PASS_EXAMINE.getStatus());
                    backlog.setExamineType(ExamineTypeEnum.ICEBOX_LOSE.getType());
                    backlog.setSendType(1);
                    backlog.setSendUserId(id);
                    backlog.setCreateBy(iceExamineVo.getCreateBy());
                    feignBacklogClient.createBacklog(backlog);
                }
            }
        }
        return map;
    }

    private Map<String, Object> createExamineCheckProcess(IceExamineVo iceExamineVo, Map<String, Object> map) {
        IceBox isExist = iceBoxDao.selectById(iceExamineVo.getIceBoxId());
        if(isExist == null ){
            throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败,找不到冰柜信息！");
        }
        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(iceExamineVo.getIceBoxId());
        if(iceBoxExtend == null ){
            throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败,找不到冰柜信息！");
        }
        SimpleUserInfoVo simpleUserInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findSimpleUserById(iceExamineVo.getCreateBy()));
        Map<Integer, SessionUserInfoVo> sessionUserInfoMap = FeignResponseUtil.getFeignData(feignDeptClient.findLevelLeaderByDeptIdNew(iceExamineVo.getMarketAreaId()));
        List<Integer> ids = new ArrayList<Integer>();
        //获取上级部门领导
        SessionUserInfoVo groupUser = new SessionUserInfoVo();
        SessionUserInfoVo serviceUser = new SessionUserInfoVo();
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

        }
        if(serviceUser == null ){
            throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败,找不到服务处经理！");
        }
        //申请人是服务处经理，直接置为审核状态
        if ((serviceUser.getId() != null && serviceUser.getId().equals(simpleUserInfoVo.getId()))) {
            return checkExamine(iceExamineVo,map);
        }
        if(groupUser == null ){
            throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败,找不到组长！");
        }
        ids.add(groupUser.getId());
        if(!ids.contains(serviceUser.getId())){
            ids.add(serviceUser.getId());
        }

        if (CollectionUtil.isEmpty(ids)) {
            throw new NormalOptionException(Constants.API_CODE_FAIL, "提交失败，找不到上级审批人！");
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        IceBoxExamineModel examineModel = new IceBoxExamineModel();
        examineModel.setExamineNumber(iceExamineVo.getAssetId());
        examineModel.setAssetId(isExist.getAssetId());
        examineModel.setDepositMoney(isExist.getDepositMoney());
        examineModel.setDisplayImage(iceExamineVo.getDisplayImage());
        examineModel.setExaminMsg(iceExamineVo.getExaminMsg());
        examineModel.setExteriorImage(iceExamineVo.getExteriorImage());
        examineModel.setIceBoxModel(isExist.getModelName());
        examineModel.setIceBoxName(isExist.getChestName());
        examineModel.setIceStatus(iceExamineVo.getIceStatus());
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
                .code(iceExamineVo.getAssetId())
                .relateCode(iceExamineVo.getAssetId())
                .createBy(iceExamineVo.getCreateBy())
                .userIds(ids)
                .build();
        sessionExamineVo.setSessionExamineCreateVo(sessionExamineCreateVo);
        sessionExamineVo.setIceBoxExamineModel(examineModel);
        SessionExamineVo examineVo = FeignResponseUtil.getFeignData(feignExamineClient.createIceBoxExamine(sessionExamineVo));
        List<SessionExamineVo.VisitExamineNodeVo> visitExamineNodes = examineVo.getVisitExamineNodes();
        map.put("iceBoxTransferNodes",visitExamineNodes);
        map.put("transferNumber",iceExamineVo.getAssetId());
        return map;
    }

    private Map<String, Object> checkExamine(IceExamineVo iceExamineVo, Map<String, Object> map) {

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
}
