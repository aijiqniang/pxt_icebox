package com.szeastroc.icebox.newprocess.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 冰柜投放 客户信息
 */
@Getter
@Setter
public class IceBoxCustomerVo {
    // 营销区域
    Integer marketArea;
    // 客户名称
    String customerName;
    // 客户编号
    String customerNumber;
    // 客户类型
    Integer supplierType;
    // 客户主业务员id
    Integer mainSalesmanId;
    // 客户主业务员名称
    String mainSalesmanName;
}
