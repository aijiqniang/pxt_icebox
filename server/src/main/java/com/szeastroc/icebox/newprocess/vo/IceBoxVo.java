package com.szeastroc.icebox.newprocess.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IceBoxVo {

    /**
     * 冰柜名称
     */
    private String chestName;
    /**
     * 冰柜型号id
     */
    private Integer modelId;

    /**
     * 冰柜型号名称
     */
    private String chestModel;
    /**
     * 资产编号
     */
    private String assetId;

    /**
     * 押金
     */
    private BigDecimal depositMoney;
    /**
     * 拥有者的经销商id
     */
    private Integer supplierId;
    /**
     * 拥有者的经销商名称
     */
    private String supplierName;
    /**
     * 拥有者的经销商地址
     */
    private String supplierAddress;
    /**
     * 拥有者的经销商联系人
     */
    private String linkman;
    /**
     * 拥有者的经销商联系人手机号
     */
    private String linkmanMobile;
    /**
     * 库存数量
     */
    private Integer iceBoxCount;
    /**
     * 投放日期
     */
    private Date lastPutTime;
    /**
     * 投放日期
     */
    private String lastPutTimeStr;
    /**
     * 处理状态：退押中，申请中
     */
    private String statusStr;

    /**
     * 审批流节点
     */
    private List<ExamineNodeVo> examineNodeVoList;

    private Integer iceBoxId;

    private String chestNorm;
    private String brandName;
    private Integer openTotal;
    private String qrCode;

    private Integer freeType;
}
