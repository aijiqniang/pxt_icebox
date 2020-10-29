package com.szeastroc.icebox.newprocess.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 部门类型 0:其他 1:服务处 2:大区 3:事业部 4:本部 5:组 6：外联部门
 */
@Getter
@AllArgsConstructor
public enum DeptTypeEnum {

    OTHER(0, "其他"),
    SERVICE(1, "服务处"),
    LARGE_AREA(2, "大区"),
    BUSINESS_UNIT(3, "事业部"),
    THIS_PART(4, "本部"),
    GROUP(5, "组"),
    CUS_DEPT(6, "外联部门"),

    ;

    private Integer type;
    private String desc;
}
