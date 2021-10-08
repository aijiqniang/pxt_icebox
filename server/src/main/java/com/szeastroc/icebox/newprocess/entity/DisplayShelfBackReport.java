package com.szeastroc.icebox.newprocess.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName(value = "t_display_shelf_back_report")
public class DisplayShelfBackReport {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    /**
     * 申请编号 关联字段,随机生成
     */
    private String applyNumber;

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
     * 所属经销商编号
     */
    private String dealerNumber;
    /**
     * 所属经销商名称
     */
    private String dealerName;

    /**
     * 退还客户编号
     */
    private String customerNumber;
    /**
     * 退还客户名称
     */
    private String customerName;
    /**
     * 客户地址
     */
    private String customerAddress;
    /**
     * 退还客户类型
     */
    private Integer customerType;
    /**
     * 客户联系人
     */
    private String linkMan;
    /**
     * 客户联系电话
     */
    private String linkMobile;
    /**
     * 退还日期
     */
    private Date backDate;

    private Integer checkPersonId;
    /**
     * 审核人员
     */
    private String checkPerson;
    /**
     * 审核职位名称
     */
    private String checkOfficeName;
    /**
     * 审核日期
     */
    private Date checkDate;
    /**
     * 审批流id
     */
    private Integer examineId;
    /**
     * 退还状态 1:退还中 2:已退还 3:驳回
     */
    private Integer examineStatus;
    /**
     * 押金
     */
    private BigDecimal depositMoney;
    /**
     * 提交人id
     */
    private Integer submitterId;
    /**
     * 提交人名称
     */
    private String submitterName;
    /**
     * 提交人电话
     */
    private String submitterMobile;

    private String province;

    private String city;

    private String area;
    //退还备注
    private String backRemarks;
    //审批备注
    private String reason;
    /**
     * 创建时间
     */
    private Date createdTime;
    /**
     * 更新时间
     */
    private Date updatedTime;

    /**
     * 商户编号
     */
    private String merchantNumber;

    private Integer dptyShelf;
    private Integer ygnmcShelf;
    private Integer dpjqShelf;
}