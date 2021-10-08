package com.szeastroc.icebox.newprocess.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.common.entity.icebox.vo.ShelfInspectRequest;
import com.szeastroc.common.entity.visit.ShelfInspectModel;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.consumer.common.ShelfInspectReportMsg;
import com.szeastroc.icebox.newprocess.entity.DisplayShelf;
import com.szeastroc.icebox.newprocess.entity.DisplayShelfInspectReport;
import com.szeastroc.icebox.newprocess.entity.DisplayShelfPutApply;
import com.szeastroc.icebox.newprocess.entity.DisplayShelfPutReport;

/**
 * (DisplayShelfInspectReport)表服务接口
 *
 * @author chenchao
 * @since 2021-06-11 09:38:04
 */
public interface DisplayShelfInspectReportService extends IService<DisplayShelfInspectReport> {

    Object selectPage(ShelfInspectReportMsg reportMsg);

    Object detail(String applyNumber);

    CommonResponse export(ShelfInspectReportMsg reportMsg);

    LambdaQueryWrapper<DisplayShelfInspectReport> fillWrapper(ShelfInspectReportMsg reportMsg);

    Integer selectByExportCount(LambdaQueryWrapper<DisplayShelfInspectReport> wrapper);

    void build(ShelfInspectModel model,  DisplayShelf displayShelf);

    void updateStatus(ShelfInspectRequest request);

}
