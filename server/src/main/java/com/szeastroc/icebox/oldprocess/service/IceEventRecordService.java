package com.szeastroc.icebox.oldprocess.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.icebox.newprocess.vo.IceEventVo;
import com.szeastroc.icebox.oldprocess.entity.IceEventRecord;
import com.szeastroc.icebox.oldprocess.vo.HisenseDTO;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IceEventRecordService extends IService<IceEventRecord> {

    void EventPush(HisenseDTO hisenseDTO);

    void newEventPush(List<HisenseDTO> hisenseDTOList);

    void eventPushConsumer(HisenseDTO hisenseDTO);

    void createTable(String startTime, String endTime);

    List<IceEventVo.IceboxList> xfaList(Integer userId, String assetId,String relateCode);

    IceEventVo.IceboxDetail boxDetail(String boxId);

    void sychAlarm(Integer alarmId);

    void createTableMonth();

    void sychAlarmPerson(Integer alarmId);
}
