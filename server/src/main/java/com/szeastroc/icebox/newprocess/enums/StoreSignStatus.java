package com.szeastroc.icebox.newprocess.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 门店签收状态 0:未签收1:已签收2:拒绝
 */
@Getter
@AllArgsConstructor
public enum StoreSignStatus {

    DEFAULT_SIGN(0, "未签收"),
    ALREADY_SIGN(1, "已签收"),
    REFUSE_SIGN(2, "拒绝");

    private Integer status;
    private String desc;
}
