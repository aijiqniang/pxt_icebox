package com.szeastroc.icebox.newprocess.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.common.entity.visit.ShelfPutModel;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.newprocess.consumer.common.ShelfPutReportMsg;
import com.szeastroc.icebox.newprocess.entity.DisplayShelfPutReport;
import com.szeastroc.icebox.newprocess.vo.ShelfPutReportVo;
import com.szeastroc.icebox.newprocess.vo.request.DisplayShelfPage;

/**
 * (DisplayShelfPutReport)表服务接口
 *
 * @author chenchao
 * @since 2021-06-07 10:26:39
 */
public interface DisplayShelfPutReportService extends IService<DisplayShelfPutReport> {

    void build(ShelfPutModel model);

    LambdaQueryWrapper<DisplayShelfPutReport> fillWrapper(ShelfPutReportMsg reportMsg);

    IPage<DisplayShelfPutReport> selectPage(ShelfPutReportMsg reportMsg);

    IPage<ShelfPutReportVo> selectPutPage(DisplayShelfPage reportMsg);

    Object detail(String applyNumber);

    CommonResponse export(ShelfPutReportMsg reportMsg);

    Integer selectByExportCount(LambdaQueryWrapper<DisplayShelfPutReport> wrapper);
}
