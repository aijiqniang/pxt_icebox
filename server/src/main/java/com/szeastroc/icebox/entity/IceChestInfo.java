package com.szeastroc.icebox.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.szeastroc.icebox.enums.FreePayTypeEnum;
import com.szeastroc.icebox.util.excel.Excel;
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
    @Excel(name = "冰箱控制器ID")
    private String externalId;
    @Excel(name = "设备编号")
    private String assetId;
    @Excel(name = "蓝牙设备ID")
    private String bluetoothId;
    @Excel(name = "蓝牙设备地址")
    private String bluetoothMac;
    @Excel(name = "冰箱二维码链接")
    private String qrCode;
    @Excel(name = "gps模块mac地址")
    private String gpsMac;
    @Excel(name = "冰柜编号")
    private String serialNumber;
    @Excel(name = "设备名称")
    private String chestName;
    @Excel(name = "生产厂家")
    private String brandName;
    @Excel(name = "设备型号")
    private String chestModel;
    @Excel(name = "设备规格")
    private String chestNorm;
    private Integer chestStatus;
    private Integer lastExamineId;
    private Integer lastExamineTime;
    private Integer putStatus;
    private Integer lastPutId;
    private Date lastPutTime;
    @Excel(name = "冰柜价值")
    private BigDecimal chestMoney;
    @Excel(name = "冰柜押金")
    private BigDecimal depositMoney;
    private Integer clientId;
    @Excel(name = "备注")
    private String remark;
    @Excel(name = "生产日期")
    private Date releaseTime;
    @Excel(name = "保修起算日期")
    private Date repairBeginTime;
    private Integer freePayType;
    private Date createTime;
    private Date updateTime;

    //鹏讯通id
    @TableField(exist = false)
    @Excel(name = "经销商鹏讯通编号")
    private String pxtId;
    //服务处
    private Integer marketAreaId;

    @TableField(exist = false)
    @Excel(name="所属服务处")
    private String marketAreaName;

    @TableField(exist = false)
    @Excel(name="经销商名称")
    private String jxsName;

    @TableField(exist = false)
    @Excel(name="经销商地址")
    private String jxsAddress;

    @TableField(exist = false)
    @Excel(name="经销商联系人")
    private String jxsContact;

    @TableField(exist = false)
    @Excel(name="经销商联系人电话")
    private String jxsContactMobile;


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
