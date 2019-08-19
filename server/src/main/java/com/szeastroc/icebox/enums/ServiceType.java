package com.szeastroc.icebox.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created by Tulane
 * 2019/5/21
 */
@Getter
@AllArgsConstructor
public enum ServiceType {
    IS_PUT(1, "投放"),
    ENTER_WAREHOUSE(2, "入库");

    private int type;
    private String desc;
}
