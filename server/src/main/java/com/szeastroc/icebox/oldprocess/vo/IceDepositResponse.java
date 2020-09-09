package com.szeastroc.icebox.oldprocess.vo;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IceDepositResponse {

    // 客户编号
    @ExcelProperty(value = "客户编号", index = 0)
    private String clientNumber;
    // 客户名称
    @ExcelProperty(value = "客户名称", index = 1)
    private String clientName;
    // 联系人
    @ExcelProperty(value = "联系人", index = 2)
    private String contactName;
    // 联系电话
    @ExcelProperty(value = "联系电话", index = 3)
    private String contactMobile;
    // 门店地址
    @ExcelProperty(value = "门店地址", index = 4)
    private String clientPlace;
    // 服务处
    @ExcelProperty(value = "服务处", index = 5)
    private String marketAreaName;

    // 设备型号
    @ExcelProperty(value = "设备型号", index = 6)
    private String chestModel;
    // 设备名称
    @ExcelProperty(value = "设备名称", index = 7)
    private String chestName;
    // 资产编号
    @ExcelProperty(value = "资产编号", index = 8)
    private String assetId;

    // 支付金额
    @ExcelProperty(value = "支付金额", index = 9)
    private String payMoney;
    // 支付时间
    @ExcelIgnore
    private long payTime;
    // 交易号
    @ExcelProperty(value = "交易号", index = 11)
    private String orderNum;
    // 设备价值
    @ExcelProperty(value = "设备价值", index = 12)
    private String chestMoney;

    @ExcelProperty(value = "支付时间", index = 10)
    private String payTimeStr;
}
