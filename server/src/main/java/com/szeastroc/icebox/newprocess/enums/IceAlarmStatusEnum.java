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
public enum IceAlarmStatusEnum {
    NEWALARM(1, "新增报警"),
    FEEDBACKED(2, "已反馈"),
    AUTO(3, "自动消除");

    private int type;
    private String desc;
}
