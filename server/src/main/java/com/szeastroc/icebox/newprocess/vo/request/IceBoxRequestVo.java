package com.szeastroc.icebox.newprocess.vo.request;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class IceBoxRequestVo {

    /**
     * 门店编码
     */
    private String storeNumber;
    /**
     * 门店名称
     */
    private String storeName;
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
     * 冰柜型号
     */
    private String chestModel;
    /**
     * 拥有者的经销商id
     */
    private Integer supplierId;
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

}
