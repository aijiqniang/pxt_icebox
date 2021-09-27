package com.szeastroc.icebox.newprocess.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.common.entity.visit.ShelfBackModel;
import com.szeastroc.icebox.newprocess.entity.DisplayShelfBackReport;

public interface DisplayShelfBackReportService extends IService<DisplayShelfBackReport> {
    void buildBackReport(ShelfBackModel model);
}
