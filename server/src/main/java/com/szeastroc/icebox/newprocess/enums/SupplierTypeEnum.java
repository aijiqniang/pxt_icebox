package com.szeastroc.icebox.newprocess.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created by Tulane
 * 2019/7/8
 */
@Getter
@AllArgsConstructor
public enum SupplierTypeEnum {

    IS_DEALER(1, "经销商"),
    IS_RESELLER(2, "分销商"),
    IS_POSTMAN(3, "邮差"),
    IS_WHOLESALER(4, "批发商"),
    IS_STORE(5,"门店");

    private Integer type;
    private String desc;

    public static String getDesc(String type) {
        for (SupplierTypeEnum supplierTypeEnum : SupplierTypeEnum.values()) {
            if (supplierTypeEnum.getType().equals(type)) {
                return supplierTypeEnum.getDesc();
            }
        }
        return "";
    }
}
