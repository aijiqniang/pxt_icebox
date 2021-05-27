package com.szeastroc.icebox.newprocess.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created by Tulane
 * 2019/7/8
 */
@Getter
@AllArgsConstructor
public enum IceBackStatusEnum {

    BACK_ING(1, "退还中"),
    BACK_SUCCESS(2, "已退还"),
    BACK_REJECT(3, "已驳回"),
    IS_ACEPTD(4,"已接单"),
    IS_ARRVIED(5,"已收柜");

    private Integer type;
    private String desc;

    public static String getDesc(Integer type) {
        for (IceBackStatusEnum supplierTypeEnum : IceBackStatusEnum.values()) {
            if (supplierTypeEnum.getType().equals(type)) {
                return supplierTypeEnum.getDesc();
            }
        }
        return "";
    }
}
