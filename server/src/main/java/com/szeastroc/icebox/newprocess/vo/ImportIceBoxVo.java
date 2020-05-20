package com.szeastroc.icebox.newprocess.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * @Author xiao
 * @Date create in 2020/5/19 17:25
 * @Description:
 */
@Getter
@Setter
public class ImportIceBoxVo {

    @ExcelProperty("序号")
    private Integer serialNumber;
    @ExcelProperty("冰箱控制器ID")
    private String externalId;
    @ExcelProperty("设备编号")
    private String assetId;
    @ExcelProperty("蓝牙设备ID")
    private String bluetoothId;
    @ExcelProperty("蓝牙设备地址")
    private String bluetoothMac;
    @ExcelProperty("冰箱二维码链接")
    private String qrCode;
    @ExcelProperty("gps模块MAC地址")
    private String gpsMac;
    @ExcelProperty("设备名称")
    private String chestName;
    @ExcelProperty("生产厂家")
    private String brandName;
    @ExcelProperty("设备型号")
    private String modelStr;
    @ExcelProperty("设备规格")
    private String chestNorm;
    @ExcelProperty("冰柜价值")
    private Long chestMoney;
    @ExcelProperty("冰柜押金")
    private Long depositMoney;
    @ExcelProperty("经销商鹏讯通编号")
    private String supplierNumber;
    @ExcelProperty("生产日期")
    private Date releaseTime;
    @ExcelProperty("保修起算日期")
    private Date repairBeginTime;

}
