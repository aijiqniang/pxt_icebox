package com.szeastroc.icebox.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created by Tulane
 * 2019/9/2
 */
@Getter
@AllArgsConstructor
public enum ExamineStatusEnum {

    NO_DEFAULT(0, "未审批"),
    IS_DEFAULT(1, "审批中"),
    IS_PASS(2, "通过"),
    UN_PASS(3, "驳回");

    private final Integer status;
    private final String desc;


    public static String convertVo(int type) {
        for (ExamineStatusEnum examineStatusEnum : ExamineStatusEnum.values()) {
            if (examineStatusEnum.getStatus().equals(type)) {
                return examineStatusEnum.getDesc();
            }
        }
        return "";
    }
}
