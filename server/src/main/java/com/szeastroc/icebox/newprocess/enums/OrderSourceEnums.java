package com.szeastroc.icebox.newprocess.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单来源 1:otoc2:dms
 * Created by hbl
 * 2020.08.20
 */
@Getter
@AllArgsConstructor
public enum OrderSourceEnums {
    OTOC(1, "otoc"),
    DMS(2, "dms");

    private Integer type;
    private String desc;
}
