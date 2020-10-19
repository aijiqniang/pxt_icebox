package com.szeastroc.icebox.newprocess.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ExportRecordTypeEnum {
    PROCESSING(0, "处理中"),
    COMPLETED(1, "已完成");

    private Integer type;
    private String desc;
}