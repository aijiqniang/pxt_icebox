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
@TableName(value = "t_ice_box_transfer_history")
public class IceBoxTransferHistory {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    /**
     * 转移批号
     */
    private String transferNumber;
    /**
     * 转移前经销商id
     */
    private Integer oldSupplierId;
    /**
     * 转移前经销商名称
     */
    private String oldSupplierName;
    /**
     * 转移前经销商营销区域
     */
    private Integer oldMarketAreaId;
    /**
     * 转移后经销商id
     */
    private Integer newSupplierId;
    /**
     * 转移后经销商名称
     */
    private String newSupplierName;
    /**
     * 转移后经销商营销区域
     */
    private Integer newMarketAreaId;
    /**
     * 冰柜id
     */
    private Integer iceBoxId;
    /**
     * 审批状态：0-未审核，1-审核中，2-通过，3-驳回
     */
    private Integer examineStatus;
    /**
     * 申请人
     */
    private Integer createBy;
    /**
     * 申请人
     */
    private String createByName;
    /**
     * 申请时间
     */
    private Date createTime;
    /**
     * 更新时间
     */
    private Date updateTime;


    /**
     * 变更前资产编号
     */
    private String oldAssetId;

    /**
     * 变更前型号id
     */
    private Integer oldModelId;

    /**
     * 变更前型号名称
     */
    private String oldModelName;

    /**
     * 变更前品牌
     */
    private String oldBrandName;

    /**
     * 变更前冰柜名称
     */
    private String oldChestName;

    /**
     * 变更前规格
     */
    private String oldChestNorm;

    /**
     * 变更前价值
     */
    private BigDecimal oldChestMoney;

    /**
     * 变更前押金
     */
    private BigDecimal oldChestDepositMoney;

    /**
     * 变更前投放编号
     */
    private String oldPutStoreNumber;

    /**
     * 变更后资产编号
     */
    private String newAssetId;

    /**
     * 变更后型号id
     */
    private Integer newModelId;

    /**
     * 变更后型号名称
     */
    private String newModelName;

    /**
     * 变更后品牌
     */
    private String newBrandName;

    /**
     * 变更后冰柜名称
     */
    private String newChestName;

    /**
     * 变更后规格
     */
    private String newChestNorm;

    /**
     * 变更后价值
     */
    private BigDecimal newChestMoney;

    /**
     * 变更后押金
     */
    private BigDecimal newChestDepositMoney;


    /**
     * 变更后投放编号
     */
    private String newPutStoreNumber;

    /**
     * 是否审批：0-不审批，1-审批
     */
    private Integer isCheck;
}