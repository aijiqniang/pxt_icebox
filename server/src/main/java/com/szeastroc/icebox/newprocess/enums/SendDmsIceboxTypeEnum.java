package com.szeastroc.icebox.newprocess.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * TODO
 *
 * @author Aijinqiang
 * @version 1.0
 * @date 2021/5/17 16:45
 */
@Getter
@AllArgsConstructor
public enum SendDmsIceboxTypeEnum {
    PUT_CONFIRM(0,"冰柜投放接单确认"),
    PUT_ARRIVRD(1,"冰柜投放送达"),
    BACK_CONFIRM(2,"冰柜退还接单确认"),
    BACK_ARRIVED(3,"冰柜退还收柜");

    private final Integer code;

    private final String desc;
}
