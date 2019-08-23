package com.szeastroc.icebox.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.exception.DongPengException;
import com.szeastroc.commondb.config.redis.JedisClient;
import com.szeastroc.icebox.dao.IceChestInfoDao;
import com.szeastroc.icebox.dao.IceEventRecordDao;
import com.szeastroc.icebox.entity.IceChestInfo;
import com.szeastroc.icebox.entity.IceEventRecord;
import com.szeastroc.icebox.service.IceEventRecordService;
import com.szeastroc.icebox.vo.HisenseDTO;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author yuqi9
 * @since 2019/5/24
 */
@Service
public class IceEventRecordServiceImpl extends ServiceImpl<IceEventRecordDao, IceEventRecord> implements IceEventRecordService {

    @Resource
    private IceChestInfoDao iceChestInfoDao;

    @Resource
    private IceEventRecordDao iceEventRecordDao;

    @Resource
    private JedisClient jedisClient;

    private static final Integer EVENT_PUSH_TIME = 30;


    /**
     * 冰箱数据推送业务处理
     * @author island
     * @param hisenseDTO :
     * @return void
     * @since 2019/5/24
     */
    @Override
    @Transactional(value = "transactionManager")
    public void EventPush(HisenseDTO hisenseDTO){
        // 同一台设备+同一个事件时间+同一个事件类型
        if(StringUtils.isBlank(jedisClient.get(hisenseDTO.getOccurrenceTime().getTime()+hisenseDTO.getControlId()+ hisenseDTO.getType()))){
            jedisClient.set(hisenseDTO.getOccurrenceTime().getTime()+hisenseDTO.getControlId()+hisenseDTO.getType(),hisenseDTO.getControlId(),EVENT_PUSH_TIME, TimeUnit.SECONDS);
            //查询是否有对应冰箱数据
            IceChestInfo iceChestInfo = iceChestInfoDao.selectOne(Wrappers.<IceChestInfo>lambdaQuery().eq(IceChestInfo::getExternalId, hisenseDTO.getControlId()));
            if(null == iceChestInfo){
                throw new DongPengException("无效设备信息");
            }
            Map<String,Object> map = new HashMap<>();
            map.put("occurrence_time",hisenseDTO.getOccurrenceTime());
            map.put("asset_id",iceChestInfo.getAssetId());
            map.put("type",hisenseDTO.getType());
            List<IceEventRecord> list = iceEventRecordDao.selectByMap(map);
            if(null != list && list.size() > 0){
                return;
            }
            // 新增记录
            IceEventRecord iceEventRecord = new IceEventRecord();
            iceEventRecord.setAssetId(iceChestInfo.getAssetId());
            iceEventRecord.setCreateTime(new Date());
            BeanUtils.copyProperties(hisenseDTO,iceEventRecord);
            iceEventRecordDao.insert(iceEventRecord);
            //增加冰箱次数
            IceChestInfo iceChestInfo1 = new IceChestInfo();
            iceChestInfo1.setId(iceChestInfo.getId());
            Integer openTotal = iceChestInfo.getOpenTotal();
            if(null == openTotal){
                openTotal = 0;
            }
            iceChestInfo1.setOpenTotal(openTotal + hisenseDTO.getOpenCloseCount());
            iceChestInfoDao.updateById(iceChestInfo1);
        }
    }

}
