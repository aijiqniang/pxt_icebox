package com.szeastroc.icebox.newprocess.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created by Tulane
 * 2019/5/21
 */
@Getter
@AllArgsConstructor
public enum ServiceType {
    ENTER_WAREHOUSE(0, "入库"),
    IS_PUT(1, "投放"),
    IS_RETURN(2, "退还");

    private int type;
    private String desc;
}
