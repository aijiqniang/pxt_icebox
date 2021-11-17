package com.szeastroc.icebox.newprocess.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * TODO
 *
 * @author Aijinqiang
 * @version 1.0
 * @date 2021/11/16 15:25
 */
@Getter
@AllArgsConstructor
public enum IceAlarmOpencountEnum {
    WAIT_RUN(1, "待结算"),
    UN_VALID(2, "已失效"),
    SUC_ALARM(3,"已报警");

    private int type;
    private String desc;
}
