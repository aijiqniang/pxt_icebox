package com.szeastroc.icebox.newprocess.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class SimpleIceBoxDetailVo {

    /**
     * 冰柜表id
     */
    private Integer id;



    /**
     * 东鹏资产id
     */
    private String assetId;

    /**
     * 冰柜型号
     */
    private String chestModel;

    /**
     * 冰柜名称
     */
    private String chestName;

    /**
     * 押金
     */
    private BigDecimal depositMoney;

    /**
     * 最近投放日期
     */
    private Date lastPutTime;


    /**
     * 投放的门店number
     */
    private String putStoreNumber;


    /**
     * 门店地址
     */
    private String storeAddress;


    


    /**
     * 拥有者的经销商
     */
    private Integer supplierId;


    /**
     * 冰柜所属部门id
     */
    private Integer deptId;

}
