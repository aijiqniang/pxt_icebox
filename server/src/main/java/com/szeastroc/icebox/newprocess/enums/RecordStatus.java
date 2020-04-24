package com.szeastroc.icebox.newprocess.enums;

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

    APPLY_ING(1, "进行中"),
    SEND_ING(2, "已完成"),
    RECEIVE_FINISH(3, "已取消");

    private Integer status;
    private String desc;

    public static String getDesc(Integer type) {
        for (RecordStatus enu : RecordStatus.values()) {
            if (enu.getStatus().equals(type)) {
                return enu.getDesc();
            }
        }
        return "";
    }

}
