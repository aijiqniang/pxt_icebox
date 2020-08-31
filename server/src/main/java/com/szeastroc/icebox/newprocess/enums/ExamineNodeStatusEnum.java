package com.szeastroc.icebox.newprocess.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created by Tulane
 * 2019/9/2
 */
@Getter
@AllArgsConstructor
public enum ExamineNodeStatusEnum {

    IS_DEFAULT(0, "审批中"),
    IS_PASS(1, "批准"),
    UN_PASS(2, "驳回");

    private Integer status;
    private String desc;
    }
