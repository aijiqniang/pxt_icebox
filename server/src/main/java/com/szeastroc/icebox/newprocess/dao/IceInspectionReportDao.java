package com.szeastroc.icebox.newprocess.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.szeastroc.icebox.newprocess.entity.IceInspectionReport;
import com.szeastroc.icebox.newprocess.vo.InspectionReportVO;

import java.util.List;

/**
 * 冰柜巡检报表 (TIceInspectionReport)表数据库访问层
 *
 * @author chenchao
 * @since 2020-12-16 16:46:21
 */
public interface IceInspectionReportDao extends BaseMapper<IceInspectionReport> {

    List<InspectionReportVO> getGroupReports(Integer deptId);

    List<InspectionReportVO> getServiceReports(Integer deptId);
}