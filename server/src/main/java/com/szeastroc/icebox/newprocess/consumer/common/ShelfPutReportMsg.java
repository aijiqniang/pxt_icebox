package com.szeastroc.icebox.newprocess.consumer.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ShelfPutReportMsg extends Page implements Serializable {

    private static final long serialVersionUID = -4750978713271531956L;
    /**
     * 下载任务id
     */
    private Integer recordsId;
    /**
     *本部id
     */
    private Integer headquartersDeptId;
    /**
     *事业部id
     */
    private Integer businessDeptId;
    /**
     *大区id
     */
    private Integer regionDeptId;
    /**
     *服务处id
     */
    private Integer serviceDeptId;
    /**
     *组id
     */
    private Integer groupDeptId;
    /**
     * 客户编号
     */
    private String customerNumber;
    /**
     * 商户编号
     */
    private String shNumber;
    /**
     * 客户名称
     */
    private String customerName;
    /**
     * 开始时间
     */
    private String startTime;
    /**
     * 结束时间
     */
    private String endTime;
    /**
     * 投放状态
     */
    private Integer putStatus;

}
