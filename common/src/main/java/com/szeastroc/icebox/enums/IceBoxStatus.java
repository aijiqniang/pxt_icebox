package com.szeastroc.icebox.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created by hbl
 * 2020.04.22
 */
@Getter
@AllArgsConstructor
public enum IceBoxStatus {
    NO_PUT(0, "未投放"),
    IS_LOCK(1, "已锁定"),
    IS_PUTING(2, "投放中"),
    IS_PUTED(3, "已投放");

    private Integer status;
    private String desc;
}
