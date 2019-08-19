package com.szeastroc.icebox.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created by Tulane
 * 2019/5/21
 */
@Getter
@AllArgsConstructor
public enum  PutStatus {

    IS_PUT(1, "已投放"),
    NO_PUT(0, "未投放");

    private Integer status;
    private String desc;
}
