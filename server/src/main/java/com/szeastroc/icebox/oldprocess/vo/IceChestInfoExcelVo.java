package com.szeastroc.icebox.oldprocess.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.szeastroc.icebox.util.excel.Excel;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by Tulane
 * 2019/5/21
 */
@Data
public class IceChestInfoExcelVo {

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
    @Excel(name = "冰柜价值")
    private BigDecimal chestMoney;
    @Excel(name = "冰柜押金")
    private BigDecimal depositMoney;
    @Excel(name = "备注")
    private String remark;
    @Excel(name = "生产日期")
    private Date releaseTime;
    @Excel(name = "保修起算日期")
    private Date repairBeginTime;

    //鹏讯通id
    @Excel(name = "经销商鹏讯通编号")
    private String pxtId;

    @Excel(name="所属服务处")
    private String marketAreaName;

    @Excel(name="经销商名称")
    private String jxsName;

    @Excel(name="经销商地址")
    private String jxsAddress;

    @TableField(exist = false)
    @Excel(name="经销商联系人")
    private String jxsContact;

    @Excel(name="经销商联系人电话")
    private String jxsContactMobile;

}
