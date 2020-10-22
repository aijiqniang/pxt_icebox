package com.szeastroc.icebox.newprocess.vo;

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
public class IceBoxExamineExceptionReportExcelVo {
    /**
     *本部名称
     */
    private String headquartersDeptName;
    /**
     *事业部名称
     */
    private String businessDeptName;
    /**
     *大区名称
     */
    private String regionDeptName;
    /**
     *服务处名称
     */
    private String serviceDeptName;
    /**
     *组名称
     */
    private String groupDeptName;
    /**
     *所属经销商编号
     */
    private String supplierNumber;
    /**
     *所属经销商名称
     */
    private String supplierName;
    /**
     *提交人名称
     */
    private String submitterName;
    /**
     *提交日期
     */
    private String submitTime;
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
    private String putCustomerType;
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
     *审核人名称
     */
    private String examineUserName;
    /**
     *审核时间
     */
    private String examineTime;
    /**
     *状态 0:报备中 1:可提报  2:已提报 3:已报备 4：已驳回
     */
    private String status;
    /**
     *提报类型：2-报废，3-遗失
     */
    private String toOaType;
    /**
     *提报时间
     */
    private String toOaTime;
    /**
     *提报单号
     */
    private String toOaNumber;

}