package com.szeastroc.icebox.vo;

import lombok.Setter;

@Setter
public class IceDepositResponse {

    // 客户编号
    private String clientNumber;
    // 客户名称
    private String clientName;
    // 联系人
    private String contactName;
    // 联系电话
    private String contactMobile;
    // 门店地址
    private String clientPlace;
    // 服务处
    private String marketAreaName;

    // 设备型号
    private String chestModel;
    // 设备名称
    private String chestName;
    // 资产编号
    private String assetId;

    // 支付金额
    private String payMoney;
    // 支付时间
    private String payTime;
    // 交易号
    private String orderNum;
    // 设备价值
    private String chestMoney;
}
