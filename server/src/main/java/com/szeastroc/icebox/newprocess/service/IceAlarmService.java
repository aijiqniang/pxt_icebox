package com.szeastroc.icebox.newprocess.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.szeastroc.icebox.newprocess.entity.IceAlarm;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 *
 */
public interface IceAlarmService extends IService<IceAlarm> {

    List<IceAlarm> getAlarmList(Integer boxId);

    IPage<IceAlarm> findByPage(IceAlarm.PageRequest boxId);
}
