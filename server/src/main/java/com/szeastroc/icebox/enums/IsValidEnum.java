package com.szeastroc.icebox.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created by ljt
 * 2019/5/21
 */
@Getter
@AllArgsConstructor
public enum IsValidEnum {

    IS_VALID(1, "有效"),
    NO_VALID(0, "无效");

    private Integer status;
    private String desc;
}
