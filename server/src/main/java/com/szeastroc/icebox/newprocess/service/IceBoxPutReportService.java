package com.szeastroc.icebox.newprocess.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.consumer.common.IceBoxPutReportMsg;
import com.szeastroc.icebox.newprocess.entity.IceBoxPutReport;

import java.util.List;

public interface IceBoxPutReportService extends IService<IceBoxPutReport> {


    IPage<IceBoxPutReport> findByPage(IceBoxPutReportMsg reportMsg);

    CommonResponse<IceBoxPutReport> sendExportMsg(IceBoxPutReportMsg reportMsg);

    Integer selectByExportCount(LambdaQueryWrapper<IceBoxPutReport> wrapper);

    void dealHistoryData();

    void syncPutDataToReport(List<Integer> ids);

    void repairIceBoxColumns();

    void repairIceBoxCycleType();
}








