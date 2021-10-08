package com.szeastroc.icebox.newprocess.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 业务员退还表
 *
 *
 * */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Builder
@TableName("t_display_shelf_back_apply")
@ApiModel(value = "ShelfBackModel对象", description = "业务员退还表 ")
public class DisplayShelfBackApply implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 申请编号 关联字段,随机生成
     */
    @ApiModelProperty(value = "申请编号 关联字段,随机生成")
    @TableField("apply_number")
    private String applyNumber;

    /**
     * 业务员id
     */
    @ApiModelProperty(value = "业务员id")
    @TableField("user_id")
    private Integer userId;

    /**
     * 投放的客户编号
     */
    @ApiModelProperty(value = "投放的客户编号")
    @TableField("put_customer_number")
    private String putCustomerNumber;


    /**
     * 投放的客户类型
     */
    @ApiModelProperty(value = "投放的客户类型")
    @TableField("put_customer_type")
    private Integer putCustomerType;

    /**
     * 签收状态 0:未签收1:已签收2:拒绝
     */
    @ApiModelProperty(value = "签收状态 0:未签收1:已签收2:拒绝")
    @TableField("sign_status")
    private Integer signStatus;

    /**
     * 审批流状态 0:未审核1:审核中2:通过3:驳回
     */
    @ApiModelProperty(value = "审批流状态 0:未审核1:审核中2:通过3:驳回")
    @TableField("examine_status")
    private Integer examineStatus;

    /**
     * 审批流ID
     */
    @ApiModelProperty(value = "审批流ID")
    @TableField("examine_id")
    private Integer examineId;

    /**
     * remark
     */
    @ApiModelProperty(value = "备注")
    @TableField("remark")
    private String remark;

    /**
     * 部门id
     */
    @ApiModelProperty(value = "部门id")
    @TableField("dept_id")
    private Integer deptId;

    /**
     * 创建人
     */
    @ApiModelProperty(value = "创建人")
    @TableField("created_by")
    private Integer createdBy;

    /**
     * 创建时间
     */
    @ApiModelProperty(value = "创建时间")
    @TableField("created_time")
    private Date createdTime;

    /**
     * 更新人
     */
    @ApiModelProperty(value = "更新人")
    @TableField("updated_by")
    private Integer updatedBy;

    /**
     * 修改时间
     */
    @ApiModelProperty(value = "修改时间")
    @TableField("update_time")
    private Date updateTime;

    /**
     * 退还状态
     */
    @ApiModelProperty(value = "退还状态 0未退还 1已退还")
    @TableField("back_status")
    private Integer backStatus;
}
