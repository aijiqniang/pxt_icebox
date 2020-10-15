package com.szeastroc.icebox.newprocess.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 旧冰柜通知状态 0:未签收，1:已签收
 */
@Getter
@AllArgsConstructor
public enum OldIceBoxSignNoticeStatusEnums {

    NO_SIGN(0, "未签收"),
    IS_SIGNED(1, "已签收");

    private Integer status;
    private String desc;
}
