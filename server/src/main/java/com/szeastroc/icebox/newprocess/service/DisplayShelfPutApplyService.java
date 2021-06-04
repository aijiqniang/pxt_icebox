package com.szeastroc.icebox.newprocess.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.common.entity.icebox.vo.IceBoxRequest;
import com.szeastroc.icebox.newprocess.entity.DisplayShelfPutApply;
import com.szeastroc.icebox.newprocess.vo.DisplayShelfPutApplyVo;
import com.szeastroc.icebox.newprocess.vo.request.InvalidShelfApplyRequest;
import com.szeastroc.icebox.newprocess.vo.request.SignShelfRequest;

import java.util.List;

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

    void sign(SignShelfRequest request);

    List<DisplayShelfPutApplyVo> putList(String customerNumber);

    List<DisplayShelfPutApplyVo> processing(String customerNumber);

    void invalid(InvalidShelfApplyRequest request);
}
