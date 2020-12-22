package com.szeastroc.icebox.newprocess.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "t_ice_back_apply_report")
public class IceBackApplyReport {
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
    private String backCustomerNumber;
    /**
     * 退还客户名称
     */
    private String backCustomerName;
    /**
     * 退还客户类型
     */
    private Integer backCustomerType;
    /**
     * 退还日期
     */
    private Date backDate;
    /**
     * 冰柜id
     */
    private Integer boxId;
    /**
     * 冰柜型号
     */
    private Integer modelId;
    /**
     * 冰柜名称
     */
    private String modelName;
    /**
     * 资产编号
     */
    private String assetId;
    /**
     * 是否免押
     */
    private Integer freeType;

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
     * 创建时间
     */
    private Date createdTime;
    /**
     * 更新时间
     */
    private Date updatedTime;
}