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
    ICE_RETURN_SCRAP(5, "退仓&报废");

    private Integer type;
    private String desc;
}
