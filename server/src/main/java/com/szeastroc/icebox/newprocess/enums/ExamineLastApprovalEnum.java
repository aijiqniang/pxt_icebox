package com.szeastroc.icebox.newprocess.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ExamineLastApprovalEnum {


    GROUP(1, "组"),
    SERVICE(2, "服务处"),
    LARGE_AREA(3, "大区"),
    BUSINESS_UNIT(4, "事业部"),

    ;

    private Integer type;
    private String desc;


}
