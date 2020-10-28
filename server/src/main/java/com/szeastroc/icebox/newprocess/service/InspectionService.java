package com.szeastroc.icebox.newprocess.service;

import com.szeastroc.icebox.newprocess.vo.InspectionReportVO;

import java.util.List;

/**
 * @ClassName: InspectionService
 * @Description:
 * @Author: 陈超
 * @Date: 2020/10/27 10:39
 **/
public interface InspectionService {

    /**
     * 资产巡检报表
     *
     * @param deptId 部门id
     * @return
     */
    List<InspectionReportVO> report(Integer deptId);
}
