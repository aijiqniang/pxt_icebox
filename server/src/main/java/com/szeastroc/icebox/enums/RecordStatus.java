package com.szeastroc.icebox.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 记录状态枚举
 * Created by Tulane
 * 2019/5/21
 */
@Getter
@AllArgsConstructor
public enum RecordStatus {

    APPLY_ING(1, "申请中"),
    SEND_ING(2, "发出中"),
    RECEIVE_FINISH(3, "已接收");

    private Integer status;
    private String desc;

}
