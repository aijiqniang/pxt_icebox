package com.szeastroc.icebox.newprocess.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.feign.customer.FeignStoreClient;
import com.szeastroc.common.feign.customer.FeignSupplierClient;
import com.szeastroc.icebox.newprocess.dao.IceBoxDao;
import com.szeastroc.icebox.newprocess.entity.IceAlarm;
import com.szeastroc.icebox.newprocess.service.IceAlarmService;
import com.szeastroc.icebox.newprocess.dao.IceAlarmMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 *
 */
@Service
public class IceAlarmServiceImpl extends ServiceImpl<IceAlarmMapper, IceAlarm>
implements IceAlarmService{

    @Resource
    private IceAlarmMapper iceAlarmMapper;
    @Resource
    private IceBoxDao iceBoxDao;
    @Resource
    private FeignSupplierClient feignSupplierClient;
    @Resource
    private FeignStoreClient feignStoreClient;

    @Override
    public List<IceAlarm> getAlarmList(Integer boxId) {
        List<IceAlarm> iceAlarms = iceAlarmMapper.selectList(Wrappers.<IceAlarm>lambdaQuery().eq(IceAlarm::getIceBoxId,boxId));
        return iceAlarms;
    }

    @Override
    public IPage<IceAlarm> findByPage(IceAlarm.PageRequest pageRequest) {
        IPage<IceAlarm> iceAlarmIPage = iceAlarmMapper.selectPage(pageRequest,Wrappers.<IceAlarm>lambdaQuery().eq(IceAlarm::getIceBoxId,pageRequest.getBoxId()));
        return iceAlarmIPage;
    }
}




