package com.szeastroc.icebox.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.entity.PactRecord;
import com.szeastroc.icebox.vo.ClientInfoRequest;

/**
 * Created by Tulane
 * 2019/5/29
 */
public interface PactRecordService extends IService<PactRecord>{

    CommonResponse<String> createPactRecord(ClientInfoRequest clientInfoRequest);

    CommonResponse<String> repairPactRecord();
}
