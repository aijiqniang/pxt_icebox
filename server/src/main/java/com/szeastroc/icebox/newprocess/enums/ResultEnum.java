package com.szeastroc.icebox.newprocess.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResultEnum {


    DUPLICATE_ASSET_NUMBER(4101, "资产编号重复"),
    EXCEPTION_REPORTING(4102, "异常报备中,不能变更冰柜信息"),
    CANNOT_CHANGE_CUSTOMER(4103,"不能更改客户")
    ;

    private final Integer code;

    private final String message;
}
