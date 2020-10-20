package com.szeastroc.icebox.newprocess.vo;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IceBoxDetailVo {

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
     * 出场日期
     */
    private Date releaseTime;


    /**
     * 保修起算日期
     */
    private Date repairBeginTime;


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
     * 开门次数
     */
    private Integer openTotal;


    /**
     * 拥有者的经销商
     */
    private Integer supplierId;


    /**
     * 冰柜所属部门id
     */
    private Integer deptId;


    /**
     * 第一次巡检记录
     */
    private IceExamineVo firstExamine;



    /**
     * 最近一次巡检记录
     */
    private IceExamineVo lastExamine;



    /**
     * 冰柜类型：0-旧冰柜，1-新冰柜
     */
    private Integer iceBoxType;
}
