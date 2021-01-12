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

    private Integer id;
    /**
     * 冰柜名称
     */
    private String chestName;
    /**
     * 申请编号
     */
    private String applyNumber;
    /**
     * 冰柜型号id
     */
    private Integer modelId;

    /**
     * 冰柜型号名称
     */
    private String chestModel;
    /**
     * 冰柜价值
     */
    private BigDecimal chestMoney;
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
     * 投放状态：0: 未投放 1:已锁定(被业务员申请)  2:投放中 3:已投放',
     */
    private Integer putStatus;

    /**
     * -1 当前没有退还记录 0未审核 1:审核中 2:审核通过 3:审核驳回";
     */
    private Integer backStatus;

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

    private String applyTimeStr;
    private String detailAddress;
    private Integer applyCount;

    /**
     * 操作的业务员名称
     */
    private Integer userId;

    /**
     * 操作的业务员名称
     */
    private String userName;

    /**
     * 作废原因
     */
    private String cancelMsg;

    /**
     * 巡检审批状态 0:未审核1:审核中2:通过3:驳回
     */
    private Integer examineStatus;
    /**
     * 巡检编号
     */
    private String examineNumber;
    /**
     * 冰柜状态 0:异常，1:正常，2:报废，3:遗失，4:报修
     */
    private Integer iceStatus;

    /**
     * 拜访频率
     */
    private String visitTypeName;
}
