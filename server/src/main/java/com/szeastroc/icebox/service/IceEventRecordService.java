package com.szeastroc.icebox.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.icebox.entity.IceEventRecord;
import com.szeastroc.icebox.vo.HisenseDTO;
import org.springframework.stereotype.Repository;

@Repository
public interface IceEventRecordService extends IService<IceEventRecord> {

    void EventPush(HisenseDTO hisenseDTO);

}
