package com.szeastroc.icebox.newprocess.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 退还类型 1:退押金2:不退押金
 * Created by Tulane
 * 2019/5/21
 */
@Getter
@AllArgsConstructor
public enum BackType {
    BACK_MONEY(1, "退押金"),
    BACK_WITHOUT_MONEY(2, "不退押金");

    private int type;
    private String desc;
}
