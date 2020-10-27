package com.szeastroc.icebox.newprocess.service;

import com.szeastroc.icebox.newprocess.vo.InspectionReportVO;

/**
 * @ClassName: InspectionService
 * @Description:
 * @Author: 陈超
 * @Date: 2020/10/27 10:39
 **/
public interface InspectionService {

    /**
     * 资产巡检
     *
     * @param deptId 部门id
     * @return
     */
    InspectionReportVO query(Integer deptId);
}
