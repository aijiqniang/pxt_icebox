package com.szeastroc.icebox.oldprocess.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.entity.customer.vo.StoreInfoDtoVo;
import com.szeastroc.common.entity.customer.vo.SupplierInfoSessionVo;
import com.szeastroc.common.entity.user.session.MatchRuleVo;
import com.szeastroc.common.entity.user.vo.SysRuleIcealarmDetailVo;
import com.szeastroc.common.entity.user.vo.UserInfoVo;
import com.szeastroc.common.entity.visit.NoticeBacklogRequestVo;
import com.szeastroc.common.entity.visit.enums.NoticeTypeEnum;
import com.szeastroc.common.exception.DongPengException;
import com.szeastroc.common.exception.NormalOptionException;
import com.szeastroc.common.feign.customer.FeignStoreClient;
import com.szeastroc.common.feign.customer.FeignSupplierClient;
import com.szeastroc.common.feign.user.FeignUserClient;
import com.szeastroc.common.feign.visit.FeignOutBacklogClient;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.commondb.config.redis.JedisClient;
import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.icebox.newprocess.dao.*;
import com.szeastroc.icebox.newprocess.entity.*;
import com.szeastroc.icebox.newprocess.enums.IceAlarmOpencountEnum;
import com.szeastroc.icebox.newprocess.enums.IceAlarmStatusEnum;
import com.szeastroc.icebox.newprocess.enums.IceAlarmTypeEnum;
import com.szeastroc.icebox.newprocess.service.IceRepairOrderService;
import com.szeastroc.icebox.newprocess.vo.IceEventVo;
import com.szeastroc.icebox.oldprocess.dao.IceChestInfoDao;
import com.szeastroc.icebox.oldprocess.dao.IceEventRecordDao;
import com.szeastroc.icebox.oldprocess.entity.IceEventRecord;
import com.szeastroc.icebox.oldprocess.service.IceEventRecordService;
import com.szeastroc.icebox.oldprocess.vo.HisenseDTO;
import com.szeastroc.icebox.util.MD5;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.gavaghan.geodesy.Ellipsoid;
import org.gavaghan.geodesy.GeodeticCalculator;
import org.gavaghan.geodesy.GlobalCoordinates;
import org.joda.time.DateTime;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author yuqi9
 * @since 2019/5/24
 */
@Service
@Slf4j
public class IceEventRecordServiceImpl extends ServiceImpl<IceEventRecordDao, IceEventRecord> implements IceEventRecordService {

    @Resource
    private IceChestInfoDao iceChestInfoDao;

    @Resource
    private IceEventRecordDao iceEventRecordDao;

    @Resource
    private JedisClient jedisClient;

    private static final Integer EVENT_PUSH_TIME = 30;
    @Resource
    private IceBoxExtendDao iceBoxExtendDao;

    @Value("${hisense.secretKey}")
    private String secretKey;

    @Resource
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private IceBoxDao iceBoxDao;

    @Resource
    private FeignStoreClient feignStoreClient;
    @Resource
    private FeignSupplierClient feignSupplierClient;
    @Resource
    private IceAlarmMapper iceAlarmMapper;
    @Resource
    private FeignOutBacklogClient feignOutBacklogClient;
    @Resource
    private FeignUserClient feignUserClient;
    @Resource
    private IceAlarmOpencountDao iceAlarmOpencountDao;
    @Resource
    private IceRepairOrderService iceRepairOrderService;
    @Resource
    private IceBackApplyDao iceBackApplyDao;
    @Resource
    private IcePutApplyRelateBoxDao icePutApplyRelateBoxDao;
    /**
     * ??????????????????????????????
     *
     * @param hisenseDTO :
     * @return void
     * @author island
     * @since 2019/5/24
     */
    @Override
    @Transactional(value = "transactionManager")
    public void EventPush(HisenseDTO hisenseDTO) {
        // ???????????????+?????????????????????+?????????????????????
        if (StringUtils.isBlank(jedisClient.get(hisenseDTO.getOccurrenceTime().getTime() + hisenseDTO.getControlId() + hisenseDTO.getType()))) {
            jedisClient.set(hisenseDTO.getOccurrenceTime().getTime() + hisenseDTO.getControlId() + hisenseDTO.getType(), hisenseDTO.getControlId(), EVENT_PUSH_TIME, TimeUnit.SECONDS);
            //?????????????????????????????????
            IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectOne(Wrappers.<IceBoxExtend>lambdaQuery().eq(IceBoxExtend::getExternalId, hisenseDTO.getControlId()));

            if (null == iceBoxExtend) {
                throw new DongPengException("??????????????????");
            }

            Map<String, Object> map = new HashMap<>();
            map.put("occurrence_time", hisenseDTO.getOccurrenceTime());
            map.put("asset_id", iceBoxExtend.getAssetId());
            map.put("type", hisenseDTO.getType());
            List<IceEventRecord> list = iceEventRecordDao.selectByMap(map);
            if (null != list && list.size() > 0) {
                return;
            }
            // ????????????
            IceEventRecord iceEventRecord = new IceEventRecord();
            iceEventRecord.setAssetId(iceBoxExtend.getAssetId());
            iceEventRecord.setCreateTime(new Date());
            BeanUtils.copyProperties(hisenseDTO, iceEventRecord);
            iceEventRecordDao.insert(iceEventRecord);
            //??????????????????
            IceBoxExtend updateIceBoxExtend = new IceBoxExtend();
            updateIceBoxExtend.setId(iceBoxExtend.getId());
            Integer openTotal = iceBoxExtend.getOpenTotal();
            if (null == openTotal) {
                openTotal = 0;
            }
            updateIceBoxExtend.setOpenTotal(openTotal + hisenseDTO.getOpenCloseCount());
            iceBoxExtendDao.updateById(updateIceBoxExtend);
        }
    }

    @Override
    public void newEventPush(List<HisenseDTO> hisenseDTOList) {
        if (CollectionUtil.isEmpty(hisenseDTOList)) {
            return;
        }
        hisenseDTOList.forEach(hisenseDTO -> {
            rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.ICEBOX_EVENT_PUSH_QUEUE, hisenseDTO);
        });
    }

    @Override
    public void eventPushConsumer(HisenseDTO hisenseDTO) {
        if (hisenseDTO.validate()) {
            log.info("????????????-->[{}]", JSON.toJSONString(hisenseDTO));
        }else {
            String sign = createSign(buildMap(hisenseDTO));
            String submitSign = hisenseDTO.getSign();
            log.info("??????????????????:" + sign + ",?????????????????????:" + submitSign);
            //saveHisensePushEvent(hisenseDTO);
            if (!sign.equals(submitSign)) {
                log.info("??????????????????,?????????-->[{}]", JSON.toJSONString(hisenseDTO));
            } else {
                saveHisensePushEvent(hisenseDTO);
            }
        }
    }

    @Override
    public void createTable(String startTime, String endTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        try{
            Date dateOne = sdf.parse(startTime);
            Date dateTwo = sdf.parse(endTime);

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(dateOne);

            try{
                iceEventRecordDao.createTableMySelf("t_ice_event_record_"+startTime);
            } catch(Exception e){
                e.printStackTrace();
            }
            while(calendar.getTime().before(dateTwo)){
                //????????????,??????after???before????????????????????????
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                try{
                    iceEventRecordDao.createTableMySelf("t_ice_event_record_"+sdf.format(calendar.getTime()));
                } catch(Exception e){
                    e.printStackTrace();
                }
            }

        } catch(Exception e){
            e.printStackTrace();
        }

    }

    @Override
    public List<IceEventVo.IceboxList> xfaList(Integer userId, String assetId, String relateCode) {
        List<IceEventVo.IceboxList> iceboxLists = iceEventRecordDao.getIntelIceboxs(userId,assetId);
        Optional.ofNullable(iceboxLists).ifPresent(list->{
            list.forEach(oneBox->{
                if(StringUtils.isNotEmpty(relateCode)){
                    List<IceAlarm> iceAlarms = iceAlarmMapper.selectList(Wrappers.<IceAlarm>lambdaQuery().eq(IceAlarm::getIceBoxAssetid, oneBox.getAssetId()).eq(IceAlarm::getSendUserId, userId).eq(IceAlarm::getRelateCode,relateCode).orderByDesc(IceAlarm::getId).last("limit 1"));
                    if (iceAlarms.size() > 0){
                        oneBox.setAlarmList(iceAlarms);
                    }
                }else{
                    List<IceAlarm> iceAlarms = iceAlarmMapper.selectList(Wrappers.<IceAlarm>lambdaQuery().eq(IceAlarm::getIceBoxAssetid, oneBox.getAssetId()).eq(IceAlarm::getSendUserId, userId).and(wrapper -> wrapper.eq(IceAlarm::getStatus, IceAlarmStatusEnum.NEWALARM.getType()).or().eq(IceAlarm::getStatus, IceAlarmStatusEnum.FEEDBACKED.getType())));
                    if (iceAlarms.size() > 0){
                        oneBox.setAlarmList(iceAlarms);
                    }
                }

            });
        });
        return iceboxLists;
    }

    @Override
    public IceEventVo.IceboxDetail boxDetail(String assetId) {
        IceEventVo.IceboxDetail detail = new IceEventVo.IceboxDetail();
        IceBox iceBox = iceBoxDao.selectOne(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getAssetId,assetId).last("limit 1"));
        String lat = "";
        String lng = "";
        if(iceBox != null && iceBox.getPutStatus()==3 && StringUtils.isNotEmpty(iceBox.getPutStoreNumber())){
            if(iceBox.getPutStoreNumber().startsWith("C0")){
                StoreInfoDtoVo storeInfoDtoVo = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(iceBox.getPutStoreNumber()));
                if(storeInfoDtoVo != null){
                    lat = storeInfoDtoVo.getLatitude();
                    lng = storeInfoDtoVo.getLongitude();
                    detail.setStoreName(storeInfoDtoVo.getStoreName());
                    detail.setAddress(storeInfoDtoVo.getAddress());
                }
            }else {
                SupplierInfoSessionVo supplierInfoSessionVo = FeignResponseUtil.getFeignData(feignSupplierClient.getSuppliserInfoByNumber(iceBox.getPutStoreNumber()));
                if(supplierInfoSessionVo != null){
                    lat = supplierInfoSessionVo.getLatitude();
                    lng = supplierInfoSessionVo.getLongitude();
                    detail.setStoreName(supplierInfoSessionVo.getName());
                    detail.setAddress(supplierInfoSessionVo.getAddress());
                }
            }
        }
        Integer unfinishOrderCount = iceRepairOrderService.getUnfinishOrderCount(iceBox.getId());
        if(unfinishOrderCount>0){
            detail.setRepairing(Boolean.TRUE);
        }else{
            detail.setRepairing(Boolean.FALSE);
        }
        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectById(iceBox.getId());
        IcePutApplyRelateBox relateBox = icePutApplyRelateBoxDao.selectOne(Wrappers.<IcePutApplyRelateBox>lambdaQuery().eq(IcePutApplyRelateBox::getApplyNumber, iceBoxExtend.getLastApplyNumber()).eq(IcePutApplyRelateBox::getBoxId, iceBox.getId()));
        IceBackApply iceBackApply = iceBackApplyDao.selectOne(Wrappers.<IceBackApply>lambdaQuery().eq(IceBackApply::getOldPutId, relateBox.getId())
                .ne(IceBackApply::getExamineStatus, 3));
        detail.setBackStatus(iceBackApply == null ? -1 : iceBackApply.getExamineStatus());

        SimpleDateFormat dayFormat = new SimpleDateFormat("yyyyMMdd");
        String nyr = dayFormat.format(new Date());
        SimpleDateFormat forMatter = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
        Date taskDate = new Date();
        try{
            taskDate = forMatter.parse(nyr + " 00:00:00");
        }catch (Exception e){
            e.printStackTrace();
        }
        IceEventRecord iceEventRecord = iceEventRecordDao.selectOne(Wrappers.<IceEventRecord>lambdaQuery().eq(IceEventRecord::getAssetId, iceBox.getAssetId()).ge(IceEventRecord::getOccurrenceTime,taskDate).le(IceEventRecord::getOccurrenceTime, new Date()).orderByDesc(IceEventRecord::getId).last("limit 1"));
        if(iceEventRecord != null && StringUtils.isNotEmpty(iceEventRecord.getLat()) && StringUtils.isNotEmpty(iceEventRecord.getLng()) && StringUtils.isNotEmpty(lat) && StringUtils.isNotEmpty(lng)){
            double distance = getDistance(Double.parseDouble(iceEventRecord.getLat()), Double.parseDouble(iceEventRecord.getLng()), Double.parseDouble(lat), Double.parseDouble(lng));
            detail.setDistance(distance);
        }else{
            detail.setDistance(0.0);
        }
        return detail;
    }

    @Override
    public void sychAlarm(Integer alarmId) {


        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMdd HH:mm:ss");//?????????????????????
        Date dNow = new Date(); //????????????
        Date dBefore = new Date();
        Calendar calendar = Calendar.getInstance(); //????????????
        /*calendar.setTime(dNow);//???????????????????????????
        calendar.add(Calendar.DAY_OF_MONTH, -1); //??????????????????*/
        dBefore = calendar.getTime(); //????????????????????????
        String defaultStartDate = dateFmt.format(dBefore); //??????????????????
        defaultStartDate = defaultStartDate.substring(0,9)+" 00:00:00";
        String defaultEndDate = defaultStartDate.substring(0,9)+" 23:59:59";
        Date startDate = new Date();
        Date endDate = new Date();
        try {
            startDate = dateFmt.parse(defaultStartDate);
            endDate = dateFmt.parse(defaultEndDate);
        }catch (Exception e){
            e.printStackTrace();
        }

        LambdaQueryWrapper<IceAlarm> wrapper =  Wrappers.<IceAlarm>lambdaQuery();
        wrapper.eq(IceAlarm::getStatus,IceAlarmStatusEnum.PRE_ALARM.getType());
        if(alarmId != null && alarmId > 0){
            wrapper.eq(IceAlarm::getId,alarmId);
        }
        List<IceAlarm> preIceAlarms = iceAlarmMapper.selectList(wrapper);

        for(IceAlarm alarm : preIceAlarms){
            try {
                solvePreAlarm(alarm);
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        LambdaQueryWrapper<IceAlarm> syncWrapper =  Wrappers.<IceAlarm>lambdaQuery();
        syncWrapper.and(wp -> wp.eq(IceAlarm::getStatus,IceAlarmStatusEnum.NEWALARM.getType()).or().eq(IceAlarm::getStatus,IceAlarmStatusEnum.FEEDBACKED.getType()));
        if(alarmId != null && alarmId > 0){
            syncWrapper.eq(IceAlarm::getId,alarmId);
        }
        List<IceAlarm> syncIceAlarms = iceAlarmMapper.selectList(syncWrapper);
        for(IceAlarm alarm : syncIceAlarms){
            try{
                solveSyncAlarm(alarm,startDate,endDate);
            }catch (Exception e){
                e.printStackTrace();
            }
        }

    }

    private void solveSyncAlarm(IceAlarm alarm, Date startDate, Date endDate) {
        IceEventRecord iceEventRecord = iceEventRecordDao.selectOne(Wrappers.<IceEventRecord>lambdaQuery().ge(IceEventRecord::getOccurrenceTime, startDate).le(IceEventRecord::getOccurrenceTime, endDate).eq(IceEventRecord::getAssetId, alarm.getIceBoxAssetid()).orderByDesc(IceEventRecord::getId).last("limit 1"));
        if(iceEventRecord != null){
            if(IceAlarmTypeEnum.OUTLINE.getType() == alarm.getAlarmType()){
                //??????
                if(StringUtils.isNotEmpty(iceEventRecord.getLng()) && StringUtils.isNotEmpty(iceEventRecord.getLat()) && StringUtils.isNotEmpty(iceEventRecord.getDetailAddress())){
                    alarm.setStatus(IceAlarmStatusEnum.AUTO.getType());
                    alarm.setAlarmRemoveTime(new Date());
                    iceAlarmMapper.updateById(alarm);
                    feignOutBacklogClient.updateIceAlarmStatus(alarm.getRelateCode());
                }
            }else if(IceAlarmTypeEnum.DISTANCE.getType() == alarm.getAlarmType()){
                if(StringUtils.isNotEmpty(iceEventRecord.getLng()) && StringUtils.isNotEmpty(iceEventRecord.getLat()) && StringUtils.isNotEmpty(iceEventRecord.getDetailAddress())){
                    IceBox iceBox = iceBoxDao.selectOne(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getAssetId,alarm.getIceBoxAssetid()).last("limit 1"));
                    String lat = "";
                    String lng = "";
                    if(iceBox != null && iceBox.getPutStatus()==3 && StringUtils.isNotEmpty(iceBox.getPutStoreNumber())){
                        if(iceBox.getPutStoreNumber().startsWith("C0")){
                            StoreInfoDtoVo storeInfoDtoVo = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(iceBox.getPutStoreNumber()));
                            if(storeInfoDtoVo != null){
                                lat = storeInfoDtoVo.getLatitude();
                                lng = storeInfoDtoVo.getLongitude();
                            }
                        }else {
                            SupplierInfoSessionVo supplierInfoSessionVo = FeignResponseUtil.getFeignData(feignSupplierClient.getSuppliserInfoByNumber(iceBox.getPutStoreNumber()));
                            if(supplierInfoSessionVo != null){
                                lat = supplierInfoSessionVo.getLatitude();
                                lng = supplierInfoSessionVo.getLongitude();
                            }
                        }
                        double distance = getDistance(Double.parseDouble(iceEventRecord.getLat()), Double.parseDouble(iceEventRecord.getLng()), Double.parseDouble(lat), Double.parseDouble(lng));
                        BigDecimal limit = BigDecimal.valueOf(200);
                        if(limit.compareTo(BigDecimal.valueOf(distance)) > -1){
                            alarm.setStatus(IceAlarmStatusEnum.AUTO.getType());
                            alarm.setAlarmRemoveTime(new Date());
                            iceAlarmMapper.updateById(alarm);
                            feignOutBacklogClient.updateIceAlarmStatus(alarm.getRelateCode());
                        }
                    }
                }
            }else if(IceAlarmTypeEnum.OVER_TEMPERTURE.getType() == alarm.getAlarmType()){
                if(iceEventRecord.getTemperature() != null && BigDecimal.valueOf(iceEventRecord.getTemperature()).compareTo(BigDecimal.valueOf(alarm.getOverTepWd())) == -1){
                    alarm.setStatus(IceAlarmStatusEnum.AUTO.getType());
                    alarm.setAlarmRemoveTime(new Date());
                    iceAlarmMapper.updateById(alarm);
                    feignOutBacklogClient.updateIceAlarmStatus(alarm.getRelateCode());
                }
            }
        }
    }

    private void solvePreAlarm(IceAlarm alarm) {
        if(alarm.getAlarmType().equals(IceAlarmTypeEnum.OUTLINE.getType())){
            //??????????????????
            if(alarm.getOutlineCount() >= alarm.getOutlineLimit()){
                //????????????
                NoticeBacklogRequestVo noticeBacklogRequestVo = NoticeBacklogRequestVo.builder()
                        .backlogName(alarm.getIceBoxAssetid()+"_????????????:"+IceAlarmTypeEnum.OUTLINE.getDesc())
                        .noticeTypeEnum(NoticeTypeEnum.ICEBOX_ALARM)
                        .relateCode(alarm.getRelateCode())
                        .sendUserId(alarm.getSendUserId())
                        .build();
                // ????????????
                feignOutBacklogClient.createNoticeBacklog(noticeBacklogRequestVo);
                alarm.setStatus(IceAlarmStatusEnum.NEWALARM.getType());
                iceAlarmMapper.updateById(alarm);
            }

        }else if(alarm.getAlarmType().equals(IceAlarmTypeEnum.OVER_TEMPERTURE.getType())){
            //????????????
            if(alarm.getOverTepCount() >= alarm.getOverTepLimit()){
                //????????????
                NoticeBacklogRequestVo noticeBacklogRequestVo = NoticeBacklogRequestVo.builder()
                        .backlogName(alarm.getIceBoxAssetid()+"_????????????:"+IceAlarmTypeEnum.OVER_TEMPERTURE.getDesc())
                        .noticeTypeEnum(NoticeTypeEnum.ICEBOX_ALARM)
                        .relateCode(alarm.getRelateCode())
                        .sendUserId(alarm.getSendUserId())
                        .build();
                // ????????????
                feignOutBacklogClient.createNoticeBacklog(noticeBacklogRequestVo);
                alarm.setStatus(IceAlarmStatusEnum.NEWALARM.getType());
                iceAlarmMapper.updateById(alarm);
            }
        }
    }

    @Override
    public void createTableMonth() {
        String firstDayOfNextMonth = getFirstDayOfNextMonth();
        String lastDayOfNextMonth = getLastDayOfNextMonth();
        createTable(firstDayOfNextMonth,lastDayOfNextMonth);
    }

    @Override
    public void sychAlarmPerson(Integer alarmId) {
        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMdd HH:mm:ss");//?????????????????????
        Date dNow = new Date(); //????????????
        Date dBefore = new Date();
        Calendar calendar = Calendar.getInstance(); //????????????
        calendar.setTime(dNow);//???????????????????????????
        calendar.add(Calendar.DAY_OF_MONTH, -1); //??????????????????
        dBefore = calendar.getTime(); //????????????????????????
        String defaultStartDate = dateFmt.format(dBefore); //??????????????????
        defaultStartDate = defaultStartDate.substring(0,9)+" 00:00:00";
        String defaultEndDate = defaultStartDate.substring(0,9)+" 23:59:59";
        Date startDate = new Date();
        Date endDate = new Date();
        try {
            startDate = dateFmt.parse(defaultStartDate);
            endDate = dateFmt.parse(defaultEndDate);
        }catch (Exception e){
            e.printStackTrace();
        }

        //?????????
        List<IceAlarmOpencount> iceAlarmOpencounts = iceAlarmOpencountDao.selectList(Wrappers.<IceAlarmOpencount>lambdaQuery().eq(IceAlarmOpencount::getStatus, IceAlarmOpencountEnum.WAIT_RUN.getType()));
        Date finalStartDate = startDate;
        Date finalEndDate = endDate;
        Optional.ofNullable(iceAlarmOpencounts).ifPresent(alarms->{
            alarms.forEach(personAlarm->{
                try {
                    solvePersonPre(personAlarm,finalStartDate,finalEndDate);
                }catch (Exception e){
                    e.printStackTrace();
                }
            });
        });

        //??????
        List<IceAlarm> iceAlarms = iceAlarmMapper.selectList(Wrappers.<IceAlarm>lambdaQuery().and(wrapper->wrapper.eq(IceAlarm::getStatus, IceAlarmStatusEnum.NEWALARM.getType()).or().eq(IceAlarm::getStatus, IceAlarmStatusEnum.FEEDBACKED.getType())).eq(IceAlarm::getAlarmType, IceAlarmTypeEnum.PERSON.getType()));
        Optional.ofNullable(iceAlarms).ifPresent(alarms->{
            alarms.forEach(alarm->{
                try {
                    solvePersonAlarm(alarm,finalStartDate,finalEndDate);
                }catch (Exception e){
                    e.printStackTrace();
                }
            });
        });
    }

    private void solvePersonAlarm(IceAlarm alarm, Date finalStartDate, Date finalEndDate) {
        IceAlarmOpencount iceAlarmOpencount = iceAlarmOpencountDao.selectById(alarm.getOpenCountId());
        if(iceAlarmOpencount == null){
            throw new NormalOptionException(Constants.API_CODE_FAIL,"???????????????:opencountid???????????????" + alarm.getOpenCountId());
        }
        List<IceEventRecord> iceEventRecords = iceEventRecordDao.selectList(Wrappers.<IceEventRecord>lambdaQuery().ge(IceEventRecord::getOccurrenceTime, finalStartDate).le(IceEventRecord::getOccurrenceTime, finalEndDate).eq(IceEventRecord::getAssetId, alarm.getIceBoxAssetid()));
        if(iceEventRecords.size() > 0){
            int sum = iceEventRecords.stream().mapToInt(IceEventRecord::getOpenCloseCount).sum();
            if(sum > iceAlarmOpencount.getLimitCount()){
                //?????????????????????  ?????????????????????
                alarm.setStatus(IceAlarmStatusEnum.AUTO.getType());
                alarm.setUpdateTime(new Date());
                iceAlarmMapper.updateById(alarm);
                feignOutBacklogClient.updateIceAlarmStatus(alarm.getRelateCode());
            }
        }
    }

    private void solvePersonPre(IceAlarmOpencount personAlarm, Date finalStartDate, Date finalEndDate) {
        List<IceEventRecord> iceEventRecords = iceEventRecordDao.selectList(Wrappers.<IceEventRecord>lambdaQuery().ge(IceEventRecord::getOccurrenceTime, finalStartDate).le(IceEventRecord::getOccurrenceTime, finalEndDate).eq(IceEventRecord::getAssetId, personAlarm.getBoxAssetid()));
        if(iceEventRecords.size() > 0){
            int yesterdayCount = iceEventRecords.stream().mapToInt(IceEventRecord::getOpenCloseCount).sum();
            if(yesterdayCount > personAlarm.getLimitCount()){
                //???????????????????????????
                personAlarm.setStatus(IceAlarmOpencountEnum.UN_VALID.getType());
                personAlarm.setUpdateTime(new Date());
                iceAlarmOpencountDao.updateById(personAlarm);
            }else if(yesterdayCount <= personAlarm.getLimitCount()){
                //???????????????
                Integer todayCount = personAlarm.getTodayCount();
                int nowCount = todayCount + 1;
                if(nowCount >= personAlarm.getKeepTime()){
                    //???????????????????????? ??????
                    DateTime date = new DateTime();
                    String prefix = date.toString("yyyyMMddHHmmss");
                    IceBox iceBox = iceBoxDao.selectOne(Wrappers.<IceBox>lambdaQuery().eq(IceBox::getAssetId, personAlarm.getBoxAssetid()).last("limit 1"));
                    if(iceBox == null || StringUtils.isEmpty(iceBox.getPutStoreNumber())){
                        throw new NormalOptionException(Constants.API_CODE_FAIL,"??????????????????????????????????????????????????????");
                    }
                    if(iceBox.getResponseManId() == null || iceBox.getResponseManId() == 0){
                        throw new NormalOptionException(Constants.API_CODE_FAIL,"???????????????????????????????????????");
                    }
                    String relateCode = iceBox.getResponseManId()+"_"+iceBox.getAssetId()+"_"+prefix+"_"+IceAlarmTypeEnum.PERSON.getType();
                    IceAlarm iceAlarm = new IceAlarm();
                    String storeName = "";
                    if(StringUtils.isNotEmpty(iceBox.getPutStoreNumber())){
                        StoreInfoDtoVo storeInfoDtoVo = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(iceBox.getPutStoreNumber()));
                        if(storeInfoDtoVo != null){
                            storeName = storeInfoDtoVo.getStoreName();
                        }else {
                            SupplierInfoSessionVo supplierInfoSessionVo = FeignResponseUtil.getFeignData(feignSupplierClient.getSuppliserInfoByNumber(iceBox.getPutStoreNumber()));
                            storeName = supplierInfoSessionVo.getName();
                        }
                    }
                    iceAlarm.setPersonCount(nowCount).setPersonLimit(personAlarm.getLimitCount()).setPersonTime(personAlarm.getKeepTime()).setOpenCountId(personAlarm.getId()).setPutStoreName(storeName).setPutStoreNumber(iceBox.getPutStoreNumber()).setRelateCode(relateCode).setIceBoxId(iceBox.getId()).setIceBoxAssetid(iceBox.getAssetId()).setAlarmType(IceAlarmTypeEnum.PERSON.getType()).setSendUserId(iceBox.getResponseManId()).setStatus(IceAlarmStatusEnum.NEWALARM.getType()).setCreateTime(new Date()).setUpdateTime(new Date());
                    iceAlarmMapper.insert(iceAlarm);
                    //????????????
                    NoticeBacklogRequestVo noticeBacklogRequestVo = NoticeBacklogRequestVo.builder()
                            .backlogName(personAlarm.getBoxAssetid()+"_????????????:"+IceAlarmTypeEnum.PERSON.getDesc())
                            .noticeTypeEnum(NoticeTypeEnum.ICEBOX_ALARM)
                            .relateCode(relateCode)
                            .sendUserId(iceBox.getResponseManId())
                            .build();
                    // ????????????
                    feignOutBacklogClient.createNoticeBacklog(noticeBacklogRequestVo);
                    personAlarm.setStatus(IceAlarmOpencountEnum.SUC_ALARM.getType());
                    personAlarm.setUpdateTime(new Date());
                    personAlarm.setTodayCount(nowCount);
                    iceAlarmOpencountDao.updateById(personAlarm);
                }else {
                    personAlarm.setTodayCount(nowCount);
                    personAlarm.setUpdateTime(new Date());
                    iceAlarmOpencountDao.updateById(personAlarm);
                }
            }
        }
    }


    public static String getFirstDayOfNextMonth() {
        SimpleDateFormat dft = new SimpleDateFormat("yyyyMMdd");
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, 1);
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH));
        return dft.format(calendar.getTime());
    }

    public static String getLastDayOfNextMonth() {
        SimpleDateFormat dft = new SimpleDateFormat("yyyyMMdd");
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.MONTH, 1);
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        return dft.format(calendar.getTime());
    }

    private SortedMap<String, String> buildMap(HisenseDTO hisenseDTO) {
        SortedMap<String, String> map = new TreeMap<>();

        map.put("controlId", hisenseDTO.getControlId());
        map.put("occurrenceTime", String.valueOf(hisenseDTO.getOccurrenceTime().getTime()));
        map.put("openCloseCount", String.valueOf(hisenseDTO.getOpenCloseCount()));
        map.put("temperature", String.valueOf(hisenseDTO.getTemperature()));
        map.put("type", String.valueOf(hisenseDTO.getType()));

        String lat = hisenseDTO.getLat();
        String lng = hisenseDTO.getLng();
        String detailAddress = hisenseDTO.getDetailAddress();
        if (org.apache.commons.lang3.StringUtils.isNotBlank(lat)) {
            map.put("lat", lat);
        }

        if (org.apache.commons.lang3.StringUtils.isNotBlank(lng)) {
            map.put("lng", lng);
        }
        if (org.apache.commons.lang3.StringUtils.isNotBlank(detailAddress)) {
            map.put("detailAddress", detailAddress);
        }
        return map;
    }


    private String createSign(SortedMap<String, String> parameters) {
        StringBuilder param = new StringBuilder();
        //?????????????????????????????????accsii??????????????????
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String k = entry.getKey();
            String v = entry.getValue();
            if (StringUtils.isBlank(v)) {// ????????????
                continue;
            }
            param.append(k).append("=").append(v).append("&");
        }
        if (!parameters.isEmpty()) {
            param.deleteCharAt(param.length() - 1);
        }
        param.append("&key=").append(secretKey);
        //log.info("????????????:" + param.toString());
        //MD5??????,???????????????????????????
        return MD5.md5(param.toString()).toUpperCase();
    }


    private void saveHisensePushEvent(HisenseDTO hisenseDTO) {
        //?????????????????????????????????
        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectOne(Wrappers.<IceBoxExtend>lambdaQuery().eq(IceBoxExtend::getExternalId, hisenseDTO.getControlId()).last("limit 1"));

        if (null == iceBoxExtend) {
            log.info("??????????????????,?????????-->[{}]", JSON.toJSONString(hisenseDTO));
        } else {
            Map<String, Object> map = new HashMap<>();
            map.put("occurrence_time", hisenseDTO.getOccurrenceTime());
            map.put("asset_id", iceBoxExtend.getAssetId());
            map.put("type", hisenseDTO.getType());
            List<IceEventRecord> list = iceEventRecordDao.selectByMap(map);
            if (null != list && list.size() > 0) {
                return;
            }
            // ????????????
            IceEventRecord iceEventRecord = new IceEventRecord();
            iceEventRecord.setAssetId(iceBoxExtend.getAssetId());
            iceEventRecord.setCreateTime(new Date());
            BeanUtils.copyProperties(hisenseDTO, iceEventRecord);
            log.info("??????????????????->assetId[{}]+occTme[{}]+[{}]",iceEventRecord.getAssetId(),iceEventRecord.getOccurrenceTime(),JSON.toJSONString(hisenseDTO));
            iceEventRecordDao.insert(iceEventRecord);
            //??????????????????
            IceBoxExtend updateIceBoxExtend = new IceBoxExtend();
            updateIceBoxExtend.setId(iceBoxExtend.getId());
            Integer openTotal = iceBoxExtend.getOpenTotal();
            if (null == openTotal) {
                openTotal = 0;
            }
            updateIceBoxExtend.setOpenTotal(openTotal + hisenseDTO.getOpenCloseCount());
            iceBoxExtendDao.updateById(updateIceBoxExtend);

            //????????????
            IceBox iceBox = iceBoxDao.selectById(iceBoxExtend.getId());
            if(iceBox != null && iceBox.getResponseManId() == null){
                if(iceBox.getPutStatus() == 3 && StringUtils.isNotEmpty(iceBox.getPutStoreNumber())){
                    Integer userid = FeignResponseUtil.getFeignData(feignStoreClient.getMainSaleManId(iceBox.getPutStoreNumber()));
                    iceBox.setResponseManId(userid);
                    if(userid != null && userid > 0){
                        UserInfoVo userInfoVo = FeignResponseUtil.getFeignData(feignUserClient.findById(userid));
                        if(userInfoVo != null){
                            iceBox.setResponseMan(userInfoVo.getRealname());
                        }
                    }
                    iceBoxDao.updateById(iceBox);
                }
            }
            String lat = "";
            String lng = "";
            String putStoreNumber = "";
            String putStoreName = "";

            //????????????
            MatchRuleVo ruleVo = new MatchRuleVo();
            ruleVo.setDeptId(iceBox.getDeptId());
            ruleVo.setOpreateType(17);
            SysRuleIcealarmDetailVo icealarmDetailVo = FeignResponseUtil.getFeignData(feignUserClient.matchIceAlarmRule(ruleVo));

            if(icealarmDetailVo != null){
                if(iceBox != null && iceBox.getPutStatus() == 3 && StringUtils.isNotEmpty(iceBox.getPutStoreNumber())) {
                    if (iceBox.getPutStoreNumber().startsWith("C0")) {
                        //??????
                        StoreInfoDtoVo storeInfoDtoVo = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(iceBox.getPutStoreNumber()));
                        if (storeInfoDtoVo != null && StringUtils.isNotEmpty(storeInfoDtoVo.getLatitude()) && StringUtils.isNotEmpty(storeInfoDtoVo.getLongitude())) {
                            lat = storeInfoDtoVo.getLatitude();
                            lng = storeInfoDtoVo.getLongitude();
                            putStoreName = storeInfoDtoVo.getStoreName();
                            putStoreNumber = storeInfoDtoVo.getStoreNumber();
                        }
                    } else {
                        //????????????
                        SupplierInfoSessionVo supplierInfoSessionVo = FeignResponseUtil.getFeignData(feignSupplierClient.getSuppliserInfoByNumber(iceBox.getPutStoreNumber()));
                        if (supplierInfoSessionVo != null && StringUtils.isNotEmpty(supplierInfoSessionVo.getLatitude()) && StringUtils.isNotEmpty(supplierInfoSessionVo.getLongitude())) {
                            lat = supplierInfoSessionVo.getLatitude();
                            lng = supplierInfoSessionVo.getLongitude();
                            putStoreName = supplierInfoSessionVo.getName();
                            putStoreNumber = supplierInfoSessionVo.getNumber();
                        }
                    }
                }
                if(icealarmDetailVo.getDistance() != null && icealarmDetailVo.getDistance() > 0){
                    //????????????
                    log.info("??????{}??????????????????{},??????{}-???????????????{},??????{}",iceBox.getAssetId(),lat,lng,hisenseDTO.getLat(),hisenseDTO.getLng());
                    IceAlarm alarm = iceAlarmMapper.selectOne(Wrappers.<IceAlarm>lambdaQuery().eq(IceAlarm::getPutStoreNumber, putStoreNumber).eq(IceAlarm::getIceBoxAssetid, iceBox.getAssetId()).eq(IceAlarm::getSendUserId, iceBox.getResponseManId()).eq(IceAlarm::getAlarmType,IceAlarmTypeEnum.DISTANCE.getType()).and(wrapper->wrapper.eq(IceAlarm::getStatus,IceAlarmStatusEnum.NEWALARM.getType()).or().eq(IceAlarm::getStatus,IceAlarmStatusEnum.FEEDBACKED.getType())).orderByDesc(IceAlarm::getId).last("limit 1"));
                    if(alarm == null && StringUtils.isNotEmpty(hisenseDTO.getLat()) && StringUtils.isNotEmpty(hisenseDTO.getLng())){
                        double distance = getDistance(Double.parseDouble(hisenseDTO.getLat()), Double.parseDouble(hisenseDTO.getLng()), Double.parseDouble(lat), Double.parseDouble(lng));
                        log.info("??????{}??????????????????{}:????????????{}",iceBox.getAssetId(),icealarmDetailVo.getDistance(),distance);
                        BigDecimal dis = BigDecimal.valueOf(distance);
                        BigDecimal limit = BigDecimal.valueOf(icealarmDetailVo.getDistance());
                        if(dis.compareTo(limit) > -1){
                            DateTime date = new DateTime();
                            String prefix = date.toString("yyyyMMddHHmmss");
                            String relateCode = iceBox.getResponseManId()+"_"+iceBox.getAssetId()+"_"+prefix+"_"+IceAlarmTypeEnum.DISTANCE.getType();
                            IceAlarm iceAlarm = new IceAlarm();
                            iceAlarm.setDistanceNow(distance).setDistanceLimit(icealarmDetailVo.getDistance()).setPutStoreName(putStoreName).setPutStoreNumber(putStoreNumber).setRelateCode(relateCode).setIceBoxId(iceBox.getId()).setIceBoxAssetid(iceBox.getAssetId()).setAlarmType(IceAlarmTypeEnum.DISTANCE.getType()).setSendUserId(iceBox.getResponseManId()).setStatus(IceAlarmStatusEnum.NEWALARM.getType()).setCreateTime(new Date()).setUpdateTime(new Date());
                            iceAlarmMapper.insert(iceAlarm);
                            //????????????
                            NoticeBacklogRequestVo noticeBacklogRequestVo = NoticeBacklogRequestVo.builder()
                                    .backlogName(iceBox.getAssetId()+"_????????????:"+IceAlarmTypeEnum.DISTANCE.getDesc())
                                    .noticeTypeEnum(NoticeTypeEnum.ICEBOX_ALARM)
                                    .relateCode(relateCode)
                                    .sendUserId(iceBox.getResponseManId())
                                    .build();
                            // ????????????
                            feignOutBacklogClient.createNoticeBacklog(noticeBacklogRequestVo);
                        }
                    }else if(alarm != null && alarm.getRepairTime()!=null && StringUtils.isNotEmpty(hisenseDTO.getLat()) && StringUtils.isNotEmpty(hisenseDTO.getLng())){
                        //??????????????????????????????????????????????????????
                        if(alarm.getRepairTime().before(new Date())){
                            double distance = getDistance(Double.parseDouble(hisenseDTO.getLat()), Double.parseDouble(hisenseDTO.getLng()), Double.parseDouble(lat), Double.parseDouble(lng));
                            log.info("??????{}??????????????????{}:????????????{}",iceBox.getAssetId(),icealarmDetailVo.getDistance(),distance);
                            BigDecimal dis = BigDecimal.valueOf(distance);
                            BigDecimal limit = BigDecimal.valueOf(icealarmDetailVo.getDistance());
                            if(dis.compareTo(limit) > -1){
                                DateTime date = new DateTime();
                                String prefix = date.toString("yyyyMMddHHmmss");
                                String relateCode = iceBox.getResponseManId()+"_"+iceBox.getAssetId()+"_"+prefix+"_"+IceAlarmTypeEnum.DISTANCE.getType();
                                IceAlarm iceAlarm = new IceAlarm();
                                iceAlarm.setDistanceNow(distance).setDistanceLimit(icealarmDetailVo.getDistance()).setPutStoreName(putStoreName).setPutStoreNumber(putStoreNumber).setRelateCode(relateCode).setIceBoxId(iceBox.getId()).setIceBoxAssetid(iceBox.getAssetId()).setAlarmType(IceAlarmTypeEnum.DISTANCE.getType()).setSendUserId(iceBox.getResponseManId()).setStatus(IceAlarmStatusEnum.NEWALARM.getType()).setCreateTime(new Date()).setUpdateTime(new Date());
                                iceAlarmMapper.insert(iceAlarm);
                                //????????????
                                NoticeBacklogRequestVo noticeBacklogRequestVo = NoticeBacklogRequestVo.builder()
                                        .backlogName(iceBox.getAssetId()+"_????????????:"+IceAlarmTypeEnum.DISTANCE.getDesc())
                                        .noticeTypeEnum(NoticeTypeEnum.ICEBOX_ALARM)
                                        .relateCode(relateCode)
                                        .sendUserId(iceBox.getResponseManId())
                                        .build();
                                // ????????????
                                feignOutBacklogClient.createNoticeBacklog(noticeBacklogRequestVo);
                            }
                        }
                    }

                }
                if(icealarmDetailVo.getOutlineTime() != null && icealarmDetailVo.getOutlineTime() > 0){
                    if(hisenseDTO.getLat().equals("") && hisenseDTO.getLng().equals("") && hisenseDTO.getDetailAddress().equals("")){
                        //????????????
                        IceAlarm alarm = iceAlarmMapper.selectOne(Wrappers.<IceAlarm>lambdaQuery().eq(IceAlarm::getPutStoreNumber, putStoreNumber).eq(IceAlarm::getIceBoxAssetid, iceBox.getAssetId()).eq(IceAlarm::getSendUserId, iceBox.getResponseManId()).eq(IceAlarm::getAlarmType,IceAlarmTypeEnum.OUTLINE.getType()).and(wrapper->wrapper.eq(IceAlarm::getStatus,IceAlarmStatusEnum.NEWALARM.getType()).or().eq(IceAlarm::getStatus,IceAlarmStatusEnum.FEEDBACKED.getType())).orderByDesc(IceAlarm::getId).last("limit 1"));
                        if(alarm == null){
                            IceAlarm preAlarm = iceAlarmMapper.selectOne(Wrappers.<IceAlarm>lambdaQuery().eq(IceAlarm::getPutStoreNumber, putStoreNumber).eq(IceAlarm::getIceBoxAssetid, iceBox.getAssetId()).eq(IceAlarm::getSendUserId, iceBox.getResponseManId()).eq(IceAlarm::getAlarmType,IceAlarmTypeEnum.OUTLINE.getType()).eq(IceAlarm::getStatus,IceAlarmStatusEnum.PRE_ALARM.getType()).orderByDesc(IceAlarm::getId).last("limit 1"));
                            if(preAlarm == null){
                                DateTime date = new DateTime();
                                String prefix = date.toString("yyyyMMddHHmmss");
                                String relateCode = iceBox.getResponseManId()+"_"+iceBox.getAssetId()+"_"+prefix+"_"+IceAlarmTypeEnum.OUTLINE.getType();
                                IceAlarm iceAlarm = new IceAlarm();
                                iceAlarm.setOutlineLimit(icealarmDetailVo.getOutlineTime()).setOutlineCount(1).setPutStoreName(putStoreName).setPutStoreNumber(putStoreNumber).setRelateCode(relateCode).setIceBoxId(iceBox.getId()).setIceBoxAssetid(iceBox.getAssetId()).setAlarmType(IceAlarmTypeEnum.OUTLINE.getType()).setSendUserId(iceBox.getResponseManId()).setStatus(IceAlarmStatusEnum.PRE_ALARM.getType()).setCreateTime(new Date()).setUpdateTime(new Date());
                                iceAlarmMapper.insert(iceAlarm);
                            }else {
                                preAlarm.setOutlineCount(preAlarm.getOutlineCount()+1);
                                preAlarm.setUpdateTime(new Date());
                                iceAlarmMapper.updateById(preAlarm);
                            }
                        }else if(alarm != null && alarm.getRepairTime() != null){
                            if(alarm.getRepairTime().before(new Date())){
                                IceAlarm preAlarm = iceAlarmMapper.selectOne(Wrappers.<IceAlarm>lambdaQuery().eq(IceAlarm::getPutStoreNumber, putStoreNumber).eq(IceAlarm::getIceBoxAssetid, iceBox.getAssetId()).eq(IceAlarm::getSendUserId, iceBox.getResponseManId()).eq(IceAlarm::getAlarmType,IceAlarmTypeEnum.OUTLINE.getType()).eq(IceAlarm::getStatus,IceAlarmStatusEnum.PRE_ALARM.getType()).orderByDesc(IceAlarm::getId).last("limit 1"));
                                if(preAlarm == null){
                                    DateTime date = new DateTime();
                                    String prefix = date.toString("yyyyMMddHHmmss");
                                    String relateCode = iceBox.getResponseManId()+"_"+iceBox.getAssetId()+"_"+prefix+"_"+IceAlarmTypeEnum.OUTLINE.getType();
                                    IceAlarm iceAlarm = new IceAlarm();
                                    iceAlarm.setOutlineLimit(icealarmDetailVo.getOutlineTime()).setOutlineCount(1).setPutStoreName(putStoreName).setPutStoreNumber(putStoreNumber).setRelateCode(relateCode).setIceBoxId(iceBox.getId()).setIceBoxAssetid(iceBox.getAssetId()).setAlarmType(IceAlarmTypeEnum.OUTLINE.getType()).setSendUserId(iceBox.getResponseManId()).setStatus(IceAlarmStatusEnum.PRE_ALARM.getType()).setCreateTime(new Date()).setUpdateTime(new Date());
                                    iceAlarmMapper.insert(iceAlarm);
                                }else {
                                    preAlarm.setOutlineCount(preAlarm.getOutlineCount()+1);
                                    preAlarm.setUpdateTime(new Date());
                                    iceAlarmMapper.updateById(preAlarm);
                                }
                            }
                        }
                    }
                }
                if(icealarmDetailVo.getOvertempWd() != null && icealarmDetailVo.getOvertempWd() > 0 && icealarmDetailVo.getOvertempSj() != null && icealarmDetailVo.getOvertempSj() > 0){
                      //????????????
                      if(BigDecimal.valueOf(hisenseDTO.getTemperature()).compareTo(BigDecimal.valueOf(icealarmDetailVo.getOvertempWd())) > -1){
                          IceAlarm alarm = iceAlarmMapper.selectOne(Wrappers.<IceAlarm>lambdaQuery().eq(IceAlarm::getPutStoreNumber, putStoreNumber).eq(IceAlarm::getIceBoxAssetid, iceBox.getAssetId()).eq(IceAlarm::getSendUserId, iceBox.getResponseManId()).eq(IceAlarm::getAlarmType,IceAlarmTypeEnum.OVER_TEMPERTURE.getType()).and(wrapper->wrapper.eq(IceAlarm::getStatus,IceAlarmStatusEnum.NEWALARM.getType()).or().eq(IceAlarm::getStatus,IceAlarmStatusEnum.FEEDBACKED.getType())).orderByDesc(IceAlarm::getId).last("limit 1"));
                          if(alarm == null){
                              IceAlarm preAlarm = iceAlarmMapper.selectOne(Wrappers.<IceAlarm>lambdaQuery().eq(IceAlarm::getPutStoreNumber, putStoreNumber).eq(IceAlarm::getIceBoxAssetid, iceBox.getAssetId()).eq(IceAlarm::getSendUserId, iceBox.getResponseManId()).eq(IceAlarm::getAlarmType,IceAlarmTypeEnum.OVER_TEMPERTURE.getType()).eq(IceAlarm::getStatus,IceAlarmStatusEnum.PRE_ALARM.getType()).orderByDesc(IceAlarm::getId).last("limit 1"));
                              if(preAlarm == null){
                                  DateTime date = new DateTime();
                                  String prefix = date.toString("yyyyMMddHHmmss");
                                  String relateCode = iceBox.getResponseManId()+"_"+iceBox.getAssetId()+"_"+prefix+"_"+IceAlarmTypeEnum.OVER_TEMPERTURE.getType();
                                  IceAlarm iceAlarm = new IceAlarm();
                                  iceAlarm.setOverTepWd(icealarmDetailVo.getOvertempWd()).setOverTepLimit(icealarmDetailVo.getOvertempSj()).setOverTepCount(1).setPutStoreName(putStoreName).setPutStoreNumber(putStoreNumber).setRelateCode(relateCode).setIceBoxId(iceBox.getId()).setIceBoxAssetid(iceBox.getAssetId()).setAlarmType(IceAlarmTypeEnum.OVER_TEMPERTURE.getType()).setSendUserId(iceBox.getResponseManId()).setStatus(IceAlarmStatusEnum.PRE_ALARM.getType()).setCreateTime(new Date()).setUpdateTime(new Date());
                                  iceAlarmMapper.insert(iceAlarm);
                              }else {
                                  preAlarm.setOverTepCount(preAlarm.getOverTepCount()+1);
                                  preAlarm.setUpdateTime(new Date());
                                  iceAlarmMapper.updateById(preAlarm);
                              }
                          }else if(alarm != null && alarm.getRepairTime() != null){
                              if(alarm.getRepairTime().before(new Date())){
                                  IceAlarm preAlarm = iceAlarmMapper.selectOne(Wrappers.<IceAlarm>lambdaQuery().eq(IceAlarm::getPutStoreNumber, putStoreNumber).eq(IceAlarm::getIceBoxAssetid, iceBox.getAssetId()).eq(IceAlarm::getSendUserId, iceBox.getResponseManId()).eq(IceAlarm::getAlarmType,IceAlarmTypeEnum.OVER_TEMPERTURE.getType()).eq(IceAlarm::getStatus,IceAlarmStatusEnum.PRE_ALARM.getType()).orderByDesc(IceAlarm::getId).last("limit 1"));
                                  if(preAlarm == null){
                                      DateTime date = new DateTime();
                                      String prefix = date.toString("yyyyMMddHHmmss");
                                      String relateCode = iceBox.getResponseManId()+"_"+iceBox.getAssetId()+"_"+prefix+"_"+IceAlarmTypeEnum.OVER_TEMPERTURE.getType();
                                      IceAlarm iceAlarm = new IceAlarm();
                                      iceAlarm.setOverTepWd(icealarmDetailVo.getOvertempWd()).setOverTepLimit(icealarmDetailVo.getOvertempSj()).setOverTepCount(1).setPutStoreName(putStoreName).setPutStoreNumber(putStoreNumber).setRelateCode(relateCode).setIceBoxId(iceBox.getId()).setIceBoxAssetid(iceBox.getAssetId()).setAlarmType(IceAlarmTypeEnum.OVER_TEMPERTURE.getType()).setSendUserId(iceBox.getResponseManId()).setStatus(IceAlarmStatusEnum.PRE_ALARM.getType()).setCreateTime(new Date()).setUpdateTime(new Date());
                                      iceAlarmMapper.insert(iceAlarm);
                                  }else {
                                      preAlarm.setOverTepCount(preAlarm.getOverTepCount()+1);
                                      preAlarm.setUpdateTime(new Date());
                                      iceAlarmMapper.updateById(preAlarm);
                                  }
                              }
                          }
                      }
                }
                if(icealarmDetailVo.getPersonCount() != null && icealarmDetailVo.getPersonCount() > 0 && icealarmDetailVo.getPersonTime() != null && icealarmDetailVo.getPersonTime() > 0){
                    //???????????????
                    if(hisenseDTO.getOpenCloseCount() != null && hisenseDTO.getOpenCloseCount() > 0){
                        IceAlarm alarm = iceAlarmMapper.selectOne(Wrappers.<IceAlarm>lambdaQuery().eq(IceAlarm::getPutStoreNumber, putStoreNumber).eq(IceAlarm::getIceBoxAssetid, iceBox.getAssetId()).eq(IceAlarm::getSendUserId, iceBox.getResponseManId()).eq(IceAlarm::getAlarmType,IceAlarmTypeEnum.PERSON.getType()).and(wrapper->wrapper.eq(IceAlarm::getStatus,IceAlarmStatusEnum.NEWALARM.getType()).or().eq(IceAlarm::getStatus,IceAlarmStatusEnum.FEEDBACKED.getType())).orderByDesc(IceAlarm::getId).last("limit 1"));
                        if(alarm == null){
                            IceAlarmOpencount a = iceAlarmOpencountDao.selectOne(Wrappers.<IceAlarmOpencount>lambdaQuery().eq(IceAlarmOpencount::getBoxAssetid, iceBox.getAssetId()).eq(IceAlarmOpencount::getPutStoreNumber, iceBox.getPutStoreNumber()).eq(IceAlarmOpencount::getStatus, IceAlarmOpencountEnum.WAIT_RUN.getType()).orderByDesc(IceAlarmOpencount::getId).last("limit 1"));
                            if(a == null){
                                IceAlarmOpencount iceAlarmOpencount = IceAlarmOpencount.builder()
                                        .boxId(iceBox.getId()+"")
                                        .boxAssetid(iceBox.getAssetId())
                                        .iceAlarmRuleDetailId(icealarmDetailVo.getId())
                                        .putStoreNumber(iceBox.getPutStoreNumber())
                                        .limitCount(icealarmDetailVo.getPersonCount())
                                        .keepTime(icealarmDetailVo.getPersonTime())
                                        .todayCount(0)
                                        .status(IceAlarmOpencountEnum.WAIT_RUN.getType())
                                        .createTime(new Date())
                                        .updateTime(new Date())
                                        .build();
                                iceAlarmOpencountDao.insert(iceAlarmOpencount);
                            }
                        }else if(alarm != null && alarm.getRepairTime() != null) {
                            if (alarm.getRepairTime().before(new Date())) {
                                IceAlarmOpencount a = iceAlarmOpencountDao.selectOne(Wrappers.<IceAlarmOpencount>lambdaQuery().eq(IceAlarmOpencount::getBoxAssetid, iceBox.getAssetId()).eq(IceAlarmOpencount::getPutStoreNumber, iceBox.getPutStoreNumber()).eq(IceAlarmOpencount::getStatus, IceAlarmOpencountEnum.WAIT_RUN.getType()).orderByDesc(IceAlarmOpencount::getId).last("limit 1"));
                                if (a == null) {
                                    IceAlarmOpencount iceAlarmOpencount = IceAlarmOpencount.builder()
                                            .boxId(iceBox.getId() + "")
                                            .boxAssetid(iceBox.getAssetId())
                                            .iceAlarmRuleDetailId(icealarmDetailVo.getId())
                                            .putStoreNumber(iceBox.getPutStoreNumber())
                                            .limitCount(icealarmDetailVo.getPersonCount())
                                            .keepTime(icealarmDetailVo.getPersonTime())
                                            .todayCount(0)
                                            .status(IceAlarmOpencountEnum.WAIT_RUN.getType())
                                            .createTime(new Date())
                                            .updateTime(new Date())
                                            .build();
                                    iceAlarmOpencountDao.insert(iceAlarmOpencount);
                                }
                            }
                        }
                    }
                }

            }else {
                log.info("??????????????????assetid:"+iceBoxExtend.getAssetId());
            }

        }
    }


    public static double getDistance(double lat_a, double lng_a, double lat_b, double lng_b) {
        Double PI = Math.PI;

        Double PK = 180 / PI;

        double t1 = Math.cos(lat_a / PK) * Math.cos(lng_a / PK) * Math.cos(lat_b / PK) * Math.cos(lng_b / PK);
        double t2 = Math.cos(lat_a / PK) * Math.sin(lng_a / PK) * Math.cos(lat_b / PK) * Math.sin(lng_b / PK);
        double t3 = Math.sin(lat_a / PK) * Math.sin(lat_b / PK);
        double num = t1 + t2 + t3;
        if(num < -1){
            num=-1;
        }
        if(num > 1){
            num =1;
        }
        double tt = Math.acos(num);
        return 6366000 * tt;
    }

}
