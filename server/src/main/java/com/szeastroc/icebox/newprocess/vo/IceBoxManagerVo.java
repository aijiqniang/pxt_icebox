package com.szeastroc.icebox.newprocess.vo;

import com.szeastroc.icebox.newprocess.entity.IceBox;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IceBoxManagerVo {


    private Integer iceBoxId;
    /**
     * 资产编号
     */
    private String assetId;
    /**
     * 冰柜名称
     */
    private String chestName;
    /**
     * 冰柜型号id
     */
    private Integer modelId;
    /**
     * 冰柜型号
     */
    private String modelName;
    /**
     * 冰柜价值
     */
    private BigDecimal chestMoney;
    /**
     * 押金
     */
    private BigDecimal depositMoney;
    /**
     * 拥有者的经销商id
     */
    private Integer supplierId;

    /**
     * 拥有者的经销商number
     */
    private String supplierNumber;
    /**
     * 冰柜状态
     */
    private Integer status;

    /**
     * 冰柜备注
     */
    private String remark;
    /**
     * 冰柜规格
     */
    private String chestNorm;
    /**
     * 冰柜品牌
     */
    private String brandName;
    /**
     * 操作的业务员名称
     */
    private Integer userId;
    /**
     * 操作的业务员名称
     */
    private String userName;
    /**
     * 所属部门id
     */
    private Integer deptId;
    /**
     * 修改部门
     */
    private boolean modifyDept;
    /**
     * 修改经销商
     */
    private boolean modifySupplier;
    /**
     * 修改客户
     */
    private boolean modifyCustomer;
    /**
     * 修改客户类型
     */
    private Integer modifyCustomerType;
    /**
     * 客户编号
     */
    private String customerNumber;


    public boolean validateMain() {
        return null != iceBoxId && StringUtils.isNotBlank(assetId) && null != modelId
                && StringUtils.isNotBlank(modelName) && null != supplierId && StringUtils.isNotBlank(supplierNumber)
                && null != status && StringUtils.isNotBlank(chestNorm) && StringUtils.isNotBlank(brandName) && null != deptId;
    }

    public IceBox convertToIceBox() {
        return IceBox.builder()
                .assetId(this.assetId)
                .modelId(this.modelId)
                .modelName(this.modelName)
                .brandName(this.brandName)
                .chestNorm(this.chestNorm)
                .chestName(this.chestName)
                .chestMoney(this.chestMoney)
                .depositMoney(this.depositMoney)
                .supplierId(this.supplierId)
                .deptId(this.deptId)
                .remark(this.remark)
                .status(this.status)
                .build();
    }


}
