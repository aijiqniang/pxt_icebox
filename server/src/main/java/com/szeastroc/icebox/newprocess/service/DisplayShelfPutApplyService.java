package com.szeastroc.icebox.newprocess.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.common.entity.icebox.vo.IceBoxRequest;
import com.szeastroc.icebox.newprocess.entity.DisplayShelfPutApply;

/**
 * <p>
 * 业务员申请表  服务类
 * </p>
 *
 * @author 陈超
 * @since 2021-06-01
 */
public interface DisplayShelfPutApplyService extends IService<DisplayShelfPutApply> {
    void updateStatus(IceBoxRequest request);
}
