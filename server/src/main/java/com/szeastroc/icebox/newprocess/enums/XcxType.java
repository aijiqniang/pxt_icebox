package com.szeastroc.icebox.newprocess.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * SFA小程序列表状态 0: 已投放 1:可供申请  2:处理中
 */
@Getter
@AllArgsConstructor
public enum XcxType {

    IS_PUTED(0, "已投放"),
    NO_PUT(1, "可供申请"),
    IS_PUTING(2, "处理中");

    private Integer status;
    private String desc;
}
