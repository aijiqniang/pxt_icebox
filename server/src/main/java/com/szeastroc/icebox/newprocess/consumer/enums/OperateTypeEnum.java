package com.szeastroc.icebox.newprocess.consumer.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OperateTypeEnum {
    INSERT(1, "新增"),
    UPDATE(2, "更新"),
    SELECT(3, "查询");

    private Integer type;
    private String desc;
}