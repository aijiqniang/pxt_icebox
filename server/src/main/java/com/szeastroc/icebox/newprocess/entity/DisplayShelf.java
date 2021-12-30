package com.szeastroc.icebox.newprocess.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * (DisplayShelf)表实体类
 *
 * @author chenchao
 * @since 2021-05-28 09:36:31
 */
@SuppressWarnings("serial")
@Data
@TableName("t_display_shelf")
@ApiModel
public class DisplayShelf extends Model<DisplayShelf> {


    @TableId(type = IdType.AUTO)
    private Integer id;
    private String applyNumber;
    /**
     * 货架名称
     */
    @ApiModelProperty("货架名称")
    private String name;
    /**
     * 货架类型 1东鹏特饮四层陈列架 2由柑柠檬茶四层陈列架 3东鹏加気四层陈列架
     */
    @ApiModelProperty("货架类型 1东鹏特饮四层陈列架 2由柑柠檬茶四层陈列架 3东鹏加気四层陈列架")
    private Integer type;
    @ApiModelProperty("货架的尺寸")
    private String size;
    private Integer headquartersDeptId;
    @ApiModelProperty("本部")
    private String headquartersDeptName;
    private Integer businessDeptId;
    @ApiModelProperty("事业部")
    private String businessDeptName;
    private Integer regionDeptId;
    @ApiModelProperty("大区")
    private String regionDeptName;
    private Integer serviceDeptId;
    @ApiModelProperty("服务处")
    private String serviceDeptName;
    private Integer groupDeptId;
    @ApiModelProperty("组")
    private String groupDeptName;
    /**
     * 经销商编号
     */
    @ApiModelProperty("经销商编号")
    private String supplierNumber;
    @ApiModelProperty("经销商类型 1经销商 2特约经销商")
    private Integer supplierType;
    @ApiModelProperty("经销商名称")
    private String supplierName;
    /**
     * 投放客户编号
     */
    @ApiModelProperty("投放客户编号")
    private String putNumber;
    @ApiModelProperty("投放客户名称")
    private String putName;
    @ApiModelProperty("客户类型")
    @TableField(updateStrategy = FieldStrategy.IGNORED)
    private Integer customerType;

    /**
     * 投放状态 0: 未投放 1:已锁定(被业务员申请)  2:投放中 3:已投放
     */
    @ApiModelProperty("投放状态 0: 未投放 1:已锁定(被业务员申请)  2:投放中 3:已投放")
    private Integer putStatus;
    /**
     * 签收状态
     */
    @ApiModelProperty("签收状态")
    private Integer signStatus;
    /**
     * 设备状态 0异常 1正常
     */
    @ApiModelProperty("0异常 1正常")
    private Integer status;
    @ApiModelProperty("责任人id")
    private Integer responseManId;
    @ApiModelProperty("责任人姓名")
    private String responseMan;
    private Date createTime;
    private Date updateTime;

    @TableField(exist = false)
    @ApiModelProperty("总货架数量")
    private Integer count;

    @TableField(exist = false)
    @ApiModelProperty("已投放货架数量")
    private Integer putCount;

    @TableField(exist = false)
    @ApiModelProperty("类型集合")
    private List<DisplayShelf.DisplayShelfType> list;


    @Data
    @ApiModel
    public static class DisplayShelfType {
        @ApiModelProperty("类型名称")
        private String typeName;
        @ApiModelProperty("数量")
        private Integer count;
        @ApiModelProperty("状态")
        private Integer status;
        @ApiModelProperty("签收状态")
        private Integer signStatus;
        @ApiModelProperty("尺寸")
        private String size;
        @ApiModelProperty("类型")
        private Integer type;
    }

    @Data
    public static class DisplayShelfData {

        @ExcelProperty("事业部*")
        private String businessDeptName;
        @ExcelProperty("大区*")
        private String regionDeptName;
        @ExcelProperty("服务处*")
        private String serviceDeptName;
        @ExcelProperty("尺寸*")
        private String size;
        @ExcelProperty("货架类型*")
        private String shelfType;
        @ExcelProperty("数量")
        private Integer repertoryCount;

    }

    /**
     * 获取主键值
     *
     * @return 主键值
     */
    @Override
    protected Serializable pkVal() {
        return this.id;
    }
}
