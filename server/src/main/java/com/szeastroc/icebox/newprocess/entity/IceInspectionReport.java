package com.szeastroc.icebox.newprocess.entity;

import cn.hutool.core.util.NumberUtil;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.szeastroc.icebox.newprocess.vo.InspectionReportVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName(value = "t_ice_inspection_report")
public class IceInspectionReport {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    /**
     * 本部id
     */
    private Integer headquartersDeptId;
    /**
     * 本部名称
     */
    private String headquartersDeptName;
    /**
     * 事业部id
     */
    private Integer businessDeptId;
    /**
     * 事业部名称
     */
    private String businessDeptName;
    /**
     * 大区id
     */
    private Integer regionDeptId;
    /**
     * 大区名称
     */
    private String regionDeptName;
    /**
     * 服务处id
     */
    private Integer serviceDeptId;
    /**
     * 服务处名称
     */
    private String serviceDeptName;
    /**
     * 组id
     */
    private Integer groupDeptId;
    /**
     * 组名称
     */
    private String groupDeptName;

    /**
     * 业务员id
     */
    private Integer userId;
    /**
     * 业务员
     */
    private String userName;
    /**
     * 投放数量
     */
    private Integer putCount;
    /**
     * 巡检数量
     */
    private Integer inspectionCount;
    /**
     * 遗失数量
     */
    private Integer lostScrapCount;
    /**
     * 巡检月份
     */
    private String inspectionDate;
    /**
     * 创建时间
     */
    private Date createdTime;
    /**
     * 更新时间
     */
    private Date updatedTime;


    public InspectionReportVO convertInspectionReportVO() {
        int noInspectionCount = this.putCount - this.lostScrapCount - this.inspectionCount;
        String percent = "-";
        if (0 != this.putCount) {
            percent = NumberUtil.formatPercent((float) this.inspectionCount / (this.putCount - this.lostScrapCount), 2);
        }
        return InspectionReportVO.builder()
                .inspection(this.inspectionCount)
                .putCount(this.putCount)
                .rate(percent)
                .noInspection(noInspectionCount)
                .name(this.userName)
                .userId(this.userId)
                .build();
    }
}