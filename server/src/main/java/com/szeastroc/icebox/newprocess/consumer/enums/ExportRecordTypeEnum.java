package com.szeastroc.icebox.newprocess.consumer.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ExportRecordTypeEnum{
    FAIL((byte)-1, "处理失败"),
    PROCESSING((byte)0, "处理中"),
    COMPLETED((byte)1, "已完成");

    private Byte type;
    private String desc;
}