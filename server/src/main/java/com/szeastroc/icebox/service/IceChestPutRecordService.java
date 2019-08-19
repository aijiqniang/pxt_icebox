package com.szeastroc.icebox.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.entity.IceChestPutRecord;
import com.szeastroc.icebox.vo.ClientInfoRequest;
import com.szeastroc.icebox.vo.OrderPayResponse;

/**
 * Created by Tulane
 * 2019/5/21
 */
public interface IceChestPutRecordService extends IService<IceChestPutRecord> {

    CommonResponse<OrderPayResponse> applyPayIceChest(ClientInfoRequest clientInfoRequest) throws Exception;

}
