package com.szeastroc.icebox.oldprocess.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.icebox.oldprocess.entity.IceEventRecord;
import com.szeastroc.icebox.oldprocess.vo.HisenseDTO;
import org.springframework.stereotype.Repository;

@Repository
public interface IceEventRecordService extends IService<IceEventRecord> {

    void EventPush(HisenseDTO hisenseDTO);

}
