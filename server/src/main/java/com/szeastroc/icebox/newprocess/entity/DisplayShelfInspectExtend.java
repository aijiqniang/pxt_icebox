package com.szeastroc.icebox.newprocess.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
* 陈列架巡检扩展表
* */

@SuppressWarnings("serial")
@Data
@TableName("t_display_shelf_inspect_extend")
@ApiModel
@Accessors(chain = true)
public class DisplayShelfInspectExtend {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String applyNumber;
    @ApiModelProperty("货架名称")
    private String name;
    @ApiModelProperty("巡检数量")
    private Integer count;
    @ApiModelProperty("货架类型 1东鹏特饮四层陈列架 2由柑柠檬茶四层陈列架 3东鹏加気四层陈列架")
    private Integer type;
    @ApiModelProperty("货架的尺寸")
    private String size;
    @ApiModelProperty("投放客户编号")
    private String putNumber;
    @ApiModelProperty("投放客户名称")
    private String putName;
    @ApiModelProperty("巡检状态")
    private Integer status;
    private Date createTime;
    private Date updateTime;
}
