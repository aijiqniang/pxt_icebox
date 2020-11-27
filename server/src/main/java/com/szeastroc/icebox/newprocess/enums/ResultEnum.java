package com.szeastroc.icebox.newprocess.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResultEnum {


    DUPLICATE_ASSET_NUMBER(4101, "资产编号重复"),
    EXCEPTION_REPORTING(4102, "异常报备中,不能变更冰柜信息"),
    CANNOT_CHANGE_CUSTOMER(4103,"正常的冰柜改为异常的冰柜时,不能变更使用客户"),
    CANNOT_CHANGE_ICEBOX(4104,"不能变更冰柜信息")
    ;

    private final Integer code;

    private final String message;
}
