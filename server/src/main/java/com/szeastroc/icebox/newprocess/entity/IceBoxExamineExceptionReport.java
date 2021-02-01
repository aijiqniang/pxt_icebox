package com.szeastroc.icebox.newprocess.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "t_ice_box_examine_exception_report")
public class IceBoxExamineExceptionReport {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    /**
     *本部id
     */
    private Integer headquartersDeptId;
    /**
     *本部名称
     */
    private String headquartersDeptName;
    /**
     *事业部id
     */
    private Integer businessDeptId;
    /**
     *事业部名称
     */
    private String businessDeptName;
    /**
     *大区id
     */
    private Integer regionDeptId;
    /**
     *大区名称
     */
    private String regionDeptName;
    /**
     *服务处id
     */
    private Integer serviceDeptId;
    /**
     *服务处名称
     */
    private String serviceDeptName;
    /**
     *组id
     */
    private Integer groupDeptId;
    /**
     *组名称
     */
    private String groupDeptName;
    /**
     * 巡检编号
     */
    private String examineNumber;
    /**
     *所属经销商id
     */
    private Integer supplierId;
    /**
     *所属经销商编号
     */
    private String supplierNumber;
    /**
     *所属经销商名称
     */
    private String supplierName;
    /**
     *提交人id
     */
    private Integer submitterId;
    /**
     *提交人名称
     */
    private String submitterName;
    /**
     *提交人职位
     */
    private String submitterPosion;
    /**
     *提交日期
     */
    private Date submitTime;
    /**
     *投放客户编号
     */
    private String putCustomerNumber;
    /**
     *投放客户名称
     */
    private String putCustomerName;
    /**
     *投放客户类型
     */
    private Integer putCustomerType;
    /**
     *冰柜类型id
     */
    private Integer iceBoxModelId;
    /**
     *冰柜型号名称
     */
    private String iceBoxModelName;
    /**
     *冰柜资产编号
     */
    private String iceBoxAssetId;
    /**
     *押金
     */
    private BigDecimal depositMoney;
    /**
     *审核人id
     */
    private Integer examineUserId;
    /**
     *审核人名称
     */
    private String examineUserName;
    /**
     *审核备注
     */
    private String examineRemark;
    /**
     *审核时间
     */
    private Date examineTime;
    /**
     *  审批人职务
     */
    private String examineUserOfficeName;

    /**
     *状态 0:报备中 1:可提报  2:已提报 3:已报备 4：已驳回
     */
    private Integer status;
    /**
     *提报类型：2-报废，3-遗失
     */
    private Integer toOaType;
    /**
     *提报时间
     */
    private Date toOaTime;
    /**
     *提报单号
     */
    private String toOaNumber;
}
