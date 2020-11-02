package com.szeastroc.icebox.newprocess.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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
@TableName(value = "t_ice_box_change_history")
public class IceBoxChangeHistory {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 冰柜id
     */
    private Integer iceBoxId;
    /**
     * 转移前经销商id
     */
    private Integer oldSupplierId;
    /**
     * 转移前经销商名称
     */
    private String oldSupplierName;
    /**
     * 转移前经销商名称
     */
    private String oldSupplierNumber;
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
     * 转移前经销商名称
     */
    private String newSupplierNumber;
    /**
     * 转移后经销商营销区域
     */
    private Integer newMarketAreaId;


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
     * 变更前冰柜状态
     */
    private Integer oldStatus;

    /**
     * 变更前冰柜备注
     */
    private String oldRemake;

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
     * 变更后冰柜状态
     */
    private Integer newStatus;


    /**
     * 变更后冰柜备注
     */
    private String newRemake;

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


    @TableField(exist = false)
    private String oldMarkAreaName;

    @TableField(exist = false)
    private String newMarkAreaName;


    @TableField(exist = false)
    private String oldStoreName;

    @TableField(exist = false)
    private String newStoreName;

    @TableField(exist = false)
    private String oldStatusStr;

    @TableField(exist = false)
    private String newStatusStr;

}