package com.szeastroc.icebox.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created by Tulane
 * 2019/5/23
 */
@Getter
@AllArgsConstructor
public enum OrderStatus {

    IS_CANCEL(0, "已取消"),
    IS_PAY_ING(1, "支付中"),
    IS_FINISH(2, "已完成");

    private Integer status;
    private String desc;
}
