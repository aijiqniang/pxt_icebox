package com.szeastroc.icebox.newprocess.vo;

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
public class IceBoxPutReportExcelVo {
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
     *申请编号
     */
    private String applyNumber;
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
     *免押类型：1-不免押，2-免押
     */
    private String freeType;
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
     *投放状态 0: 未投放 1:已锁定(被业务员申请)  2:投放中 3:已投放
     */
    private String putStatus;

}
