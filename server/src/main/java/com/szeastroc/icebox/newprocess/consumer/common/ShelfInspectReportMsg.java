package com.szeastroc.icebox.newprocess.consumer.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ShelfInspectReportMsg extends Page implements Serializable {

    private static final long serialVersionUID = -4750978713271531956L;
    @ApiModelProperty(value = "部门类型")
    private Integer deptType;
    @ApiModelProperty(value = "营销区域")
    private Integer marketAreaId;
    @ApiModelProperty(value = "货架类型")
    private String shelfType;
    @ApiModelProperty(value = "投放客户名称")
    private String customerName;
    @ApiModelProperty(value = "投放客户编号")
    private String customerNumber;
    @ApiModelProperty(value = "巡检人员")
    private String submitterName;
    @ApiModelProperty(value = "巡检日期")
    private String submitTime;


    /**
     * 下载任务id
     */
    private Integer recordsId;

    /**
     * 商户编号
     */
    private String shNumber;

    /**
     * 开始时间
     */
    private String startTime;
    /**
     * 结束时间
     */
    private String endTime;

}
