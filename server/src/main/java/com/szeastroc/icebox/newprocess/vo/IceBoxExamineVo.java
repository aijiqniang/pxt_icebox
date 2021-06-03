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
public class IceBoxExamineVo {

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
     *冰柜型号名称
     */
    private String iceBoxModelName;
    /**
     *冰柜资产编号
     */
    private String iceBoxAssetId;

    /**
     *冰柜状态
     */
    private String statusStr;
    /**
     * 外观照片的URL
     */
    private String exteriorImage;
    /**
     * 陈列照片的URL
     */
    private String displayImage;

    /**
     * 资产编号图片的URL
     */
    private String assetImage;
    /**
     * 巡检备注
     */
    private String examinMsg;

    /**
     * 商户编号
     */
    private String merchantNumber;
}
