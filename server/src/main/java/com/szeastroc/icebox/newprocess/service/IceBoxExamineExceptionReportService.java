package com.szeastroc.icebox.newprocess.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.consumer.common.IceBoxExamineExceptionReportMsg;
import com.szeastroc.icebox.newprocess.consumer.common.IceBoxPutReportMsg;
import com.szeastroc.icebox.newprocess.entity.IceBoxExamineExceptionReport;
import com.szeastroc.icebox.newprocess.entity.IceBoxPutReport;

public interface IceBoxExamineExceptionReportService extends IService<IceBoxExamineExceptionReport> {


    IPage<IceBoxExamineExceptionReport> findByPage(IceBoxExamineExceptionReportMsg reportMsg);

    CommonResponse<IceBoxExamineExceptionReport> sendExportMsg(IceBoxExamineExceptionReportMsg reportMsg);

    Integer selectByExportCount(LambdaQueryWrapper<IceBoxExamineExceptionReport> wrapper);
}








