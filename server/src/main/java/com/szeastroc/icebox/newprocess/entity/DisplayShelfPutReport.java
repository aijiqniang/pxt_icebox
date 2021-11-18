package com.szeastroc.icebox.newprocess.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * (DisplayShelfPutReport)表实体类
 *
 * @author chenchao
 * @since 2021-06-07 10:26:35
 */
@SuppressWarnings("serial")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("t_display_shelf_put_report")
public class DisplayShelfPutReport extends Model<DisplayShelfPutReport> {

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
     * 申请编号
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

    private String shNumber;


    /**
     * 投放客户名称
     */

    private String putCustomerName;


    /**
     * 投放客户类型
     */

    private Integer putCustomerType;


    /**
     * 审核人id
     */

    private Integer examineUserId;


    /**
     * 审核人名称
     */

    private String examineUserName;


    private Date examineTime;


    /**
     * 投放状态 0: 未投放 1:已锁定(被业务员申请)  2:投放中 3:已投放
     */

    private Integer putStatus;


    /**
     * 省份
     */

    private String provinceName;


    /**
     * 城市
     */

    private String cityName;


    /**
     * 区县
     */

    private String districtName;


    /**
     * 客户地址
     */

    private String customerAddress;


    /**
     * 拜访频率
     */

    private String visitTypeName;


    /**
     * 提交人电话
     */

    private String submitterMobile;


    /**
     * 联系人
     */

    private String linkmanName;


    /**
     * 联系人电话
     */

    private String linkmanMobile;


    /**
     * 审批人职务
     */

    private String examineUserPosion;


    /**
     * 审批备注
     */

    private String examineRemark;


    /**
     * 投放客户等级
     */

    private String putCustomerLevel;


    /**
     * 签收时间
     */

    private Date signTime;

    private Date createTime;

    private Date updateTime;

    private String putRemark;


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
