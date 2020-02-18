package com.szeastroc.icebox.newprocess.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 审批流状态 0:未审核1:审核中2:通过3:驳回
 */
@Getter
@AllArgsConstructor
public enum ExamineStatus {

    DEFAULT_EXAMINE(0, "未审核"),
    DOING_EXAMINE(1, "审核中"),
    PASS_EXAMINE(2, "通过"),
    REJECT_EXAMINE(3, "驳回");

    private Integer status;
    private String desc;
}
