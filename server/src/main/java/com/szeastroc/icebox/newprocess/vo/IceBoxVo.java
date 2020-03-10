package com.szeastroc.icebox.newprocess.vo;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
public class IceBoxVo {

    private Integer iceBoxId;
    private String assetId;
    private String chestName;
    private String chestModel; //冰柜型号
    private String chestNorm;
    private String brandName;
    private BigDecimal depositMoney;
    private Integer openTotal;
    private String qrCode;
}
