package com.szeastroc.icebox.newprocess.consumer.common;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class IceBoxPutReportMsg  extends Page implements Serializable {

    private static final long serialVersionUID = -4750978713271531956L;

    private Integer id;
    /**
     * 下载任务id
     */
    private Integer recordsId;
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
     *申请编号
     */
    private String applyNumber;
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
     *提交日期
     */
    private Date submitTime;
    /**
     *提交日期
     */
    private Date submitEndTime;
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
     *免押类型：1-不免押，2-免押
     */
    private Integer freeType;
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
     *审核时间
     */
    private Date examineTime;
    /**
     *投放状态 0: 未投放 1:已锁定(被业务员申请)  2:投放中 3:已投放
     */
    private Integer putStatus;

    /**
     *操作类型：1-新增，2-更新，3-查询
     */
    private Integer operateType;

    /**
     * 操作人
     */
    private String operateName;
}
