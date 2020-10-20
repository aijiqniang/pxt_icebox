package com.szeastroc.icebox.newprocess.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum  ExamineExceptionStatusEnums {

    /**
     * 状态 0:报备中 1:可提报  2:已提报 3:已报备(仅针对报修) 4：已驳回
     */
    is_reporting(0, "报备中"),
    allow_report(1, "可提报"),
    is_reported(2, "已提报"),
    is_repaired(3, "已报备"),
    is_unpass(4, "已驳回");

    private Integer status;
    private String desc;

    public static String getDesc(Integer status) {
        for (ExamineExceptionStatusEnums examineTime : ExamineExceptionStatusEnums.values()) {
            if (examineTime.getStatus().equals(status)) {
                return examineTime.getDesc();
            }
        }
        return "";
    }

}
