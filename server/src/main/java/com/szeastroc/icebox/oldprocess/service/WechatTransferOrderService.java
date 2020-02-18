package com.szeastroc.icebox.oldprocess.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.oldprocess.entity.WechatTransferOrder;

/**
 * Created by Tulane
 * 2019/8/19
 */
public interface WechatTransferOrderService extends IService<WechatTransferOrder>{

    CommonResponse<String> takeBackIceChest(Integer iceChestId, Integer clientId) throws Exception;
}
