package com.szeastroc.icebox.newprocess.vo;

import lombok.Data;

/**
 * @ClassName: InspectionReportVO
 * @Description:
 * @Author: 陈超
 * @Date: 2020/10/27 10:40
 **/
@Data
public class InspectionReportVO {

    /**
     * 投放数量
     */
    private Integer putCount;
    /**
     * 巡检数量
     */
    private Integer inspection;
    /**
     * 巡检率
     */
    private String rate;
    /**
     * 未投放
     */
    private Integer noInspection;
}
