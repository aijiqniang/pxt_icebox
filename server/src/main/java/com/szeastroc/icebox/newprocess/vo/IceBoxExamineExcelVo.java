package com.szeastroc.icebox.newprocess.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class IceBoxExamineExcelVo {

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
     *冰柜资产编号
     */
    private String iceBoxAssetId;
    /**
     *冰柜型号名称
     */
    private String iceBoxModelName;

    /**
     *所属经销商编号
     */
    private String supplierNumber;
    /**
     *所属经销商名称
     */
    private String supplierName;
    /**
     *投放客户编号
     */
    private String putCustomerNumber;
    /**
     *投放客户名称
     */
    private String putCustomerName;
    /**
     *冰柜状态
     */
    private String statusStr;
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
    private String submitTime;

    /**
     * 照片的URL
     */
    private String ImageUrl;
    /**
     * 巡检备注
     */
    private String examinMsg;

}
