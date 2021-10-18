package com.szeastroc.icebox.newprocess.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * TODO
 *
 * @author Aijinqiang
 * @version 1.0
 * @date 2021/10/8 17:21
 */
@Getter
@AllArgsConstructor
public enum IceAlarmTypeEnum {
    DISTANCE(1, "冰柜距离门店超过200m"),
    OUTLINE(2, "冰柜离线");

    private int type;
    private String desc;
}
