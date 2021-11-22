package com.szeastroc.icebox.oldprocess.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.exception.DongPengException;
import com.szeastroc.commondb.config.redis.JedisClient;
import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.icebox.newprocess.dao.IceBoxExtendDao;
import com.szeastroc.icebox.newprocess.entity.IceBoxExtend;
import com.szeastroc.icebox.oldprocess.dao.IceChestInfoDao;
import com.szeastroc.icebox.oldprocess.dao.IceEventRecordDao;
import com.szeastroc.icebox.oldprocess.entity.IceEventRecord;
import com.szeastroc.icebox.oldprocess.service.IceEventRecordService;
import com.szeastroc.icebox.oldprocess.vo.HisenseDTO;
import com.szeastroc.icebox.util.MD5;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
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
        }
    }

}
