package com.szeastroc.icebox.newprocess.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.consumer.common.IceBackApplyReportMsg;
import com.szeastroc.icebox.newprocess.entity.IceBackApplyReport;

/**
 * 冰柜退还表 (TIceBackApplyReport)表服务接口
 *
 * @author chenchao
 * @since 2020-12-16 16:41:06
 */
public interface IceBackApplyReportService extends IService<IceBackApplyReport> {

    IPage<IceBackApplyReport> findByPage(IceBackApplyReportMsg reportMsg);

    CommonResponse<IceBackApplyReport> sendExportMsg(IceBackApplyReportMsg reportMsg);

    Integer selectByExportCount(LambdaQueryWrapper<IceBackApplyReport> wrapper);

    LambdaQueryWrapper<IceBackApplyReport> fillWrapper(IceBackApplyReportMsg reportMsg);
}