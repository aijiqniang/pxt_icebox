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
    private Integer chestModelId;

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
     * 最近投放编号
     */
    private String lastPutNumber;

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

    /**
     * 门店老板名字
     */
    private String memberName;

    /**
     * 门店老板手机号码
     */
    private String memberMobile;


    private Integer newSupplierId;
    /**
     * 经销商名字
     */
    private String newSupplierName;

    /**
     * 经销商编号
     */
    private String newSupplierNumber;

    /**
     * 退还类型
     */
    private Integer backType;


    /**
     * 免押类型
     */
    private Integer freeType;


    /**
     * 业务员用户id
     */
    private Integer userId;

}
