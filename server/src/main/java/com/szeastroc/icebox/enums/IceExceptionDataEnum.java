package com.szeastroc.icebox.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum IceExceptionDataEnum {
    ICE_RETURN(1, "退仓"),
    ICE_SCRAP(2, "报废"),
    ICE_LOSE(3, "遗失"),
    ICE_ADD(4, "新增"),
    ICE_RETURN_SCRAP(5, "退仓&报废"),
    ICE_SYSTEM_ADD(6, "系统新增"),
    ICE_ADD_SCRAP(7, "系统新增&报废"),
    ICE_ADD_LOSE(8, "系统新增&遗失");

    private Integer type;
    private String desc;



    public static Integer getEnumType(String desc) {
        for (IceExceptionDataEnum enu : IceExceptionDataEnum.values()) {
            if (enu.getDesc().equals(desc)) {
                return enu.getType();
            }
        }
        return null;
    }
}
