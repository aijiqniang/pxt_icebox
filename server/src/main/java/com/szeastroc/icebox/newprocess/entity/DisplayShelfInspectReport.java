package com.szeastroc.icebox.newprocess.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * (DisplayShelfInspectReport)表实体类
 *
 * @author chenchao
 * @since 2021-06-11 09:38:03
 */
@SuppressWarnings("serial")
@Data
@Accessors(chain = true)
@TableName("t_display_shelf_inspect_report")
public class DisplayShelfInspectReport extends Model<DisplayShelfInspectReport> {


    @TableId(type = IdType.AUTO)
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
     * 巡检编号
     */

    private String applyNumber;


    /**
     * 所属经销商编号
     */

    private String supplierNumber;


    /**
     * 所属经销商名称
     */

    private String supplierName;


    /**
     * 所属经销商类型
     */

    private Integer supplierType;


    /**
     * 提交人id
     */

    private Integer submitterId;


    /**
     * 提交人名称
     */

    private String submitterName;


    /**
     * 提交日期
     */

    private Date submitTime;


    /**
     * 投放客户编号
     */

    private String putCustomerNumber;

    /**
     * 商户编号
     */
    private String shNumber;


    /**
     * 投放客户名称
     */

    private String putCustomerName;


    /**
     * 投放客户类型
     */

    private Integer putCustomerType;


    private String putCustomerLevel;


    /**
     * 审核人id
     */

    private Integer examineUserId;


    /**
     * 审核人名称
     */

    private String examineUserName;


    /**
     * 审核人职务
     */

    private String examineUserOfficeName;


    private Date examineTime;


    /**
     * 状态 0:报备中 1:可提报  2:已提报 3:已报备 4：已驳回
     */

    private Integer status;


    /**
     * 提交人职位
     */

    private String submitterPosition;


    /**
     * 审批备注
     */

    private String examineRemark;

    private String inspectRemark;

    private Date createTime;

    private Date updateTime;


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
