package com.szeastroc.icebox.newprocess.vo.request;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class IceBoxRequestVo {

    /**
     * 投放客户编码
     */
    private String storeNumber;
    /**
     * 投放客户名称
     */
    private String storeName;
    /**
     * 投放客户类型
     */
    private Integer storeType;
    /**
     * 门店营销区域
     */
    private Integer marketAreaId;
    /**
     * 状态：0-已投放
     */
    private Integer type;
    /**
     * 冰柜名称
     */
    private String chestName;
    /**
     * 冰柜型号id
     */
    private Integer modelId;
    /**
     * 业务员id
     */
    private Integer userId;
    /**
     * 业务员营销区域
     */
    private Integer userMarketAreaId;
    /**
     * 冰柜型号
     */
    private String chestModel;
    /**
     * 拥有者的经销商id
     */
    private Integer supplierId;

    /**
     * 拥有者的经销商名称
     */
    private String supplierName;
    /**
     * 申请数量
     */
    private Integer applyCount;
    /**
     * 押金
     */
    private BigDecimal depositMoney;
    /**
     * 是否免押 0:不免押1:免押
     */
    private Integer freeType;

    /**
     * 查询条件
     */
    private String searchContent;

    /**
     * 备注
     */
    private String applyPit;
}
