package com.szeastroc.icebox.oldprocess.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.szeastroc.icebox.enums.FreePayTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by Tulane
 * 2019/5/21
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "t_ice_chest_info")
public class IceChestInfo {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String externalId;
    private String assetId;
    private String bluetoothId;
    private String bluetoothMac;
    private String qrCode;
    private String gpsMac;
    private String serialNumber;
    private String chestName;
    private String brandName;
    private String chestModel;
    private String chestNorm;
    private Integer chestStatus;
    private Integer lastExamineId;
    private Date lastExamineTime;
    private Integer putStatus;
    private Integer lastPutId;
    private Date lastPutTime;
    private BigDecimal chestMoney;
    private BigDecimal depositMoney;
    private Integer clientId;
    private String remark;
    private Date releaseTime;
    private Date repairBeginTime;
    private Integer freePayType;
    private Date createTime;
    private Date updateTime;

    //服务处
    private Integer marketAreaId;

    private Integer openTotal;

    public IceChestInfo(String externalId, String serialNumber, String chestName, String brandName, String chestModel, String chestNorm, Integer chestStatus, BigDecimal chestMoney, BigDecimal depositMoney, Integer clientId, String remark) {
        this.externalId = externalId;
        this.serialNumber = serialNumber;
        this.chestName = chestName;
        this.brandName = brandName;
        this.chestModel = chestModel;
        this.chestNorm = chestNorm;
        this.chestStatus = chestStatus;
        this.chestMoney = chestMoney;
        this.depositMoney = depositMoney;
        this.clientId = clientId;
        this.remark = remark;
        this.freePayType = FreePayTypeEnum.UN_FREE.getType();
        this.createTime = new Date();
        this.updateTime = this.createTime;
    }
}
