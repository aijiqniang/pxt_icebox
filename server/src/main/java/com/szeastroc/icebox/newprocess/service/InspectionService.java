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
     * @param type 1业代 2组长 3服务处经理 4大区总
     * @return
     */
    InspectionReportVO query(Integer type);
}
