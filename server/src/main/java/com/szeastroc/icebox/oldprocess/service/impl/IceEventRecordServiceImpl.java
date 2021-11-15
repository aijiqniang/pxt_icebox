package com.szeastroc.icebox.oldprocess.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.entity.customer.vo.StoreInfoDtoVo;
import com.szeastroc.common.entity.customer.vo.SupplierInfoSessionVo;
import com.szeastroc.common.entity.user.session.MatchRuleVo;
import com.szeastroc.common.entity.user.vo.SysRuleIcealarmDetailVo;
import com.szeastroc.common.entity.visit.NoticeBacklogRequestVo;
import com.szeastroc.common.entity.visit.enums.NoticeTypeEnum;
import com.szeastroc.common.exception.DongPengException;
import com.szeastroc.common.feign.customer.FeignStoreClient;
import com.szeastroc.common.feign.customer.FeignSupplierClient;
import com.szeastroc.common.feign.user.FeignUserClient;
import com.szeastroc.common.feign.visit.FeignOutBacklogClient;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.commondb.config.redis.JedisClient;
import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.icebox.newprocess.dao.IceAlarmMapper;
import com.szeastroc.icebox.newprocess.dao.IceBoxDao;
import com.szeastroc.icebox.newprocess.dao.IceBoxExtendDao;
import com.szeastroc.icebox.newprocess.entity.IceAlarm;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.entity.IceBoxExtend;
import com.szeastroc.icebox.newprocess.enums.IceAlarmStatusEnum;
import com.szeastroc.icebox.newprocess.enums.IceAlarmTypeEnum;
import com.szeastroc.icebox.newprocess.vo.IceEventVo;
import com.szeastroc.icebox.oldprocess.dao.IceChestInfoDao;
import com.szeastroc.icebox.oldprocess.dao.IceEventRecordDao;
import com.szeastroc.icebox.oldprocess.entity.IceEventRecord;
import com.szeastroc.icebox.oldprocess.service.IceEventRecordService;
import com.szeastroc.icebox.oldprocess.vo.HisenseDTO;
import com.szeastroc.icebox.util.MD5;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
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
    /**
     * 冰箱数据推送业务处理
     *
     * @param hisenseDTO :
     * @return void
     * @author island
     * @since 2019/5/24
     */
    @Override
    @Transactional(value = "transactionManager")
    public void EventPush(HisenseDTO hisenseDTO) {
        // 同一台设备+同一个事件时间+同一个事件类型
        if (StringUtils.isBlank(jedisClient.get(hisenseDTO.getOccurrenceTime().getTime() + hisenseDTO.getControlId() + hisenseDTO.getType()))) {
            jedisClient.set(hisenseDTO.getOccurrenceTime().getTime() + hisenseDTO.getControlId() + hisenseDTO.getType(), hisenseDTO.getControlId(), EVENT_PUSH_TIME, TimeUnit.SECONDS);
            //查询是否有对应冰箱数据
            IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectOne(Wrappers.<IceBoxExtend>lambdaQuery().eq(IceBoxExtend::getExternalId, hisenseDTO.getControlId()));

            if (null == iceBoxExtend) {
                throw new DongPengException("无效设备信息");
            }

            Map<String, Object> map = new HashMap<>();
            map.put("occurrence_time", hisenseDTO.getOccurrenceTime());
            map.put("asset_id", iceBoxExtend.getAssetId());
            map.put("type", hisenseDTO.getType());
            List<IceEventRecord> list = iceEventRecordDao.selectByMap(map);
            if (null != list && list.size() > 0) {
                return;
            }
            // 新增记录
            IceEventRecord iceEventRecord = new IceEventRecord();
            iceEventRecord.setAssetId(iceBoxExtend.getAssetId());
            iceEventRecord.setCreateTime(new Date());
            BeanUtils.copyProperties(hisenseDTO, iceEventRecord);
            iceEventRecordDao.insert(iceEventRecord);
            //增加冰箱次数
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
            //log.info("参数错误-->[{}]", JSON.toJSONString(hisenseDTO));
        }else {
            String sign = createSign(buildMap(hisenseDTO));
            String submitSign = hisenseDTO.getSign();
            //log.info("实际签名数据:" + sign + ",提交的签名数据:" + submitSign);
            if (!sign.equals(submitSign)) {
                //log.info("签名数据错误,参数为-->[{}]", JSON.toJSONString(hisenseDTO));
            } else {
                saveHisensePushEvent(hisenseDTO);
                //log.info("推送数据入库");
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
                //倒序时间,顺序after改before其他相应的改动。
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
    public List<IceEventVo.IceboxList> xfaList(Integer userId, String assetId) {
        List<IceEventVo.IceboxList> iceboxLists = iceEventRecordDao.getIntelIceboxs(userId,assetId);
        Optional.ofNullable(iceboxLists).ifPresent(list->{
            list.forEach(oneBox->{
                List<IceAlarm> iceAlarms = iceAlarmMapper.selectList(Wrappers.<IceAlarm>lambdaQuery().eq(IceAlarm::getIceBoxAssetid, oneBox.getAssetId()).eq(IceAlarm::getSendUserId, userId).eq(IceAlarm::getStatus, IceAlarmStatusEnum.NEWALARM.getType()));
                if (iceAlarms.size() > 0){
                    oneBox.setAlarmList(iceAlarms);
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
        LambdaQueryWrapper<IceAlarm> wrapper =  Wrappers.<IceAlarm>lambdaQuery();
        wrapper.eq(IceAlarm::getStatus,IceAlarmStatusEnum.NEWALARM.getType());
        if(alarmId != null && alarmId > 0){
            wrapper.eq(IceAlarm::getId,alarmId);
        }
        List<IceAlarm> iceAlarms = iceAlarmMapper.selectList(wrapper);

        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMdd HH:mm:ss");//格式化一下时间
        Date dNow = new Date(); //当前时间
        Date dBefore = new Date();
        Calendar calendar = Calendar.getInstance(); //得到日历
        calendar.setTime(dNow);//把当前时间赋给日历
        calendar.add(Calendar.DAY_OF_MONTH, -1); //设置为前一天
        dBefore = calendar.getTime(); //得到前一天的时间
        String defaultStartDate = dateFmt.format(dBefore); //格式化前一天
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

        for(IceAlarm alarm : iceAlarms){
            try {
                IceEventRecord iceEventRecord = iceEventRecordDao.selectOne(Wrappers.<IceEventRecord>lambdaQuery().ge(IceEventRecord::getOccurrenceTime, startDate).le(IceEventRecord::getOccurrenceTime, endDate).eq(IceEventRecord::getAssetId, alarm.getIceBoxAssetid()).orderByDesc(IceEventRecord::getId).last("limit 1"));
                if(iceEventRecord != null){
                    if(IceAlarmTypeEnum.OUTLINE.getType() == alarm.getAlarmType()){
                        //上线
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
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    @Override
    public void createTableMonth() {
        String firstDayOfNextMonth = getFirstDayOfNextMonth();
        String lastDayOfNextMonth = getLastDayOfNextMonth();
        createTable(firstDayOfNextMonth,lastDayOfNextMonth);
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
        //所有参与传参的参数按照accsii排序（升序）
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String k = entry.getKey();
            String v = entry.getValue();
            if (StringUtils.isBlank(v)) {// 过滤空值
                continue;
            }
            param.append(k).append("=").append(v).append("&");
        }
        if (!parameters.isEmpty()) {
            param.deleteCharAt(param.length() - 1);
        }
        param.append("&key=").append(secretKey);
        log.info("签名参数:" + param.toString());
        //MD5加密,结果转换为大写字符
        return MD5.md5(param.toString()).toUpperCase();
    }


    private void saveHisensePushEvent(HisenseDTO hisenseDTO) {
        //查询是否有对应冰箱数据
        IceBoxExtend iceBoxExtend = iceBoxExtendDao.selectOne(Wrappers.<IceBoxExtend>lambdaQuery().eq(IceBoxExtend::getExternalId, hisenseDTO.getControlId()).last("limit 1"));

        if (null == iceBoxExtend) {
            log.info("无效设备信息,参数为-->[{}]", JSON.toJSONString(hisenseDTO));
        } else {
            Map<String, Object> map = new HashMap<>();
            map.put("occurrence_time", hisenseDTO.getOccurrenceTime());
            map.put("asset_id", iceBoxExtend.getAssetId());
            map.put("type", hisenseDTO.getType());
            List<IceEventRecord> list = iceEventRecordDao.selectByMap(map);
            if (null != list && list.size() > 0) {
                return;
            }
            // 新增记录
            IceEventRecord iceEventRecord = new IceEventRecord();
            iceEventRecord.setAssetId(iceBoxExtend.getAssetId());
            iceEventRecord.setCreateTime(new Date());
            BeanUtils.copyProperties(hisenseDTO, iceEventRecord);
            iceEventRecordDao.insert(iceEventRecord);
            //增加冰箱次数
            IceBoxExtend updateIceBoxExtend = new IceBoxExtend();
            updateIceBoxExtend.setId(iceBoxExtend.getId());
            Integer openTotal = iceBoxExtend.getOpenTotal();
            if (null == openTotal) {
                openTotal = 0;
            }
            updateIceBoxExtend.setOpenTotal(openTotal + hisenseDTO.getOpenCloseCount());
            iceBoxExtendDao.updateById(updateIceBoxExtend);

            //增加报警
            IceBox iceBox = iceBoxDao.selectById(iceBoxExtend.getId());
            String lat = "";
            String lng = "";
            String putStoreNumber = "";
            String putStoreName = "";

            //匹配规则
            MatchRuleVo ruleVo = new MatchRuleVo();
            ruleVo.setDeptId(iceBox.getDeptId());
            SysRuleIcealarmDetailVo icealarmDetailVo = FeignResponseUtil.getFeignData(feignUserClient.matchIceAlarmRule(ruleVo));

            if(icealarmDetailVo != null){
                if(iceBox != null && iceBox.getPutStatus() == 3 && StringUtils.isNotEmpty(iceBox.getPutStoreNumber())) {
                    if (iceBox.getPutStoreNumber().startsWith("C0")) {
                        //门店
                        StoreInfoDtoVo storeInfoDtoVo = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(iceBox.getPutStoreNumber()));
                        if (storeInfoDtoVo != null && StringUtils.isNotEmpty(storeInfoDtoVo.getLatitude()) && StringUtils.isNotEmpty(storeInfoDtoVo.getLongitude())) {
                            lat = storeInfoDtoVo.getLatitude();
                            lng = storeInfoDtoVo.getLongitude();
                            putStoreName = storeInfoDtoVo.getStoreName();
                            putStoreNumber = storeInfoDtoVo.getStoreNumber();
                        }
                    } else {
                        //批发邮差
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
                    IceAlarm alarm = iceAlarmMapper.selectOne(Wrappers.<IceAlarm>lambdaQuery().eq(IceAlarm::getPutStoreNumber, putStoreNumber).eq(IceAlarm::getIceBoxAssetid, iceBox.getAssetId()).eq(IceAlarm::getSendUserId, iceBox.getResponseManId()).eq(IceAlarm::getAlarmType,IceAlarmTypeEnum.DISTANCE.getType()).last("limit 1"));
                    if(alarm == null){
                        double distance = getDistance(Double.parseDouble(hisenseDTO.getLat()), Double.parseDouble(hisenseDTO.getLng()), Double.parseDouble(lat), Double.parseDouble(lng));
                        BigDecimal dis = BigDecimal.valueOf(distance);
                        BigDecimal limit = BigDecimal.valueOf(icealarmDetailVo.getDistance());
                        if(dis.compareTo(limit) > -1){
                            DateTime date = new DateTime();
                            String prefix = date.toString("yyyyMMddHHmmss");
                            String relateCode = iceBox.getResponseManId()+"_"+iceBox.getAssetId()+"_"+prefix;
                            IceAlarm iceAlarm = new IceAlarm();
                            iceAlarm.setPutStoreName(putStoreName).setPutStoreNumber(putStoreNumber).setRelateCode(relateCode).setIceBoxId(iceBox.getId()).setIceBoxAssetid(iceBox.getAssetId()).setAlarmType(IceAlarmTypeEnum.DISTANCE.getType()).setSendUserId(iceBox.getResponseManId()).setStatus(IceAlarmStatusEnum.NEWALARM.getType()).setCreateTime(new Date()).setUpdateTime(new Date());
                            iceAlarmMapper.insert(iceAlarm);
                            //发送代办
                            NoticeBacklogRequestVo noticeBacklogRequestVo = NoticeBacklogRequestVo.builder()
                                    .backlogName(iceBox.getAssetId()+"_冰柜报警:"+IceAlarmTypeEnum.DISTANCE.getDesc())
                                    .noticeTypeEnum(NoticeTypeEnum.ICEBOX_ALARM)
                                    .relateCode(relateCode)
                                    .sendUserId(iceBox.getResponseManId())
                                    .build();
                            // 创建通知
                            feignOutBacklogClient.createNoticeBacklog(noticeBacklogRequestVo);
                        }
                    }

                }
                if(hisenseDTO.getLat().equals("") && hisenseDTO.getLng().equals("") && hisenseDTO.getDetailAddress().equals("")){
                    //冰柜离线
                    IceAlarm alarm = iceAlarmMapper.selectOne(Wrappers.<IceAlarm>lambdaQuery().eq(IceAlarm::getPutStoreNumber, putStoreNumber).eq(IceAlarm::getIceBoxAssetid, iceBox.getAssetId()).eq(IceAlarm::getSendUserId, iceBox.getResponseManId()).eq(IceAlarm::getAlarmType,IceAlarmTypeEnum.OUTLINE.getType()).last("limit 1"));
                    if(alarm != null){
                        return;
                    }
                    DateTime date = new DateTime();
                    String prefix = date.toString("yyyyMMddHHmmss");
                    String relateCode = iceBox.getResponseManId()+"_"+iceBox.getAssetId()+"_"+prefix;
                    IceAlarm iceAlarm = new IceAlarm();
                    iceAlarm.setPutStoreName(putStoreName).setPutStoreNumber(putStoreNumber).setRelateCode(relateCode).setIceBoxId(iceBox.getId()).setIceBoxAssetid(iceBox.getAssetId()).setAlarmType(IceAlarmTypeEnum.OUTLINE.getType()).setSendUserId(iceBox.getResponseManId()).setStatus(IceAlarmStatusEnum.NEWALARM.getType()).setCreateTime(new Date()).setUpdateTime(new Date());
                    iceAlarmMapper.insert(iceAlarm);
                    //发送代办
                    NoticeBacklogRequestVo noticeBacklogRequestVo = NoticeBacklogRequestVo.builder()
                            .backlogName(iceBox.getAssetId()+"_冰柜报警:"+IceAlarmTypeEnum.OUTLINE.getDesc())
                            .noticeTypeEnum(NoticeTypeEnum.ICEBOX_ALARM)
                            .relateCode(relateCode)
                            .sendUserId(iceBox.getResponseManId())
                            .build();
                    // 创建通知
                    feignOutBacklogClient.createNoticeBacklog(noticeBacklogRequestVo);
                }
            }

        }
    }

    public static double getDistance(double longitudeFrom, double latitudeFrom, double longitudeTo, double latitudeTo) {
        GlobalCoordinates source = new GlobalCoordinates(latitudeFrom, longitudeFrom);
        GlobalCoordinates target = new GlobalCoordinates(latitudeTo, longitudeTo);

        return new GeodeticCalculator().calculateGeodeticCurve(Ellipsoid.Sphere, source, target).getEllipsoidalDistance();
    }

}
