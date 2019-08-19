package com.szeastroc.icebox.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created by Tulane
 * 2019/5/22
 */
@Getter
@AllArgsConstructor
public enum ClientType {

    IS_STORE(1, "门店"),
    IS_DEALER(2, "经销商");

    private int type;
    private String desc;
}
