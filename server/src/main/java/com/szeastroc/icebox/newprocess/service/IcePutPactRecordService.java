package com.szeastroc.icebox.newprocess.service;

import com.szeastroc.icebox.newprocess.entity.IcePutPactRecord;
import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.icebox.oldprocess.vo.ClientInfoRequest;

public interface IcePutPactRecordService extends IService<IcePutPactRecord>{

    void createPactRecord(ClientInfoRequest clientInfoRequest);

    boolean checkPactRecordByBoxId(Integer iceBoxId);
}
