package com.szeastroc.icebox.newprocess.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.icebox.newprocess.entity.IceInspectionReport;
import com.szeastroc.icebox.newprocess.vo.InspectionReportVO;

import java.util.List;

/**
 * 冰柜巡检报表 (TIceInspectionReport)表服务接口
 *
 * @author chenchao
 * @since 2020-12-16 16:46:21
 */
public interface IceInspectionReportService extends IService<IceInspectionReport> {


    IceInspectionReport getCurrentMonthReport(Integer userId);

    List<IceInspectionReport> getInService(Integer deptId);

    List<InspectionReportVO> getGroupReports(Integer deptId);

    List<InspectionReportVO> getServiceReports(Integer deptId);

    void truncate();
}