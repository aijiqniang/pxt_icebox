package com.szeastroc.icebox.newprocess.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created by Tulane
 * 2019/5/21
 */
@Getter
@AllArgsConstructor
public enum PutStatus {

    NO_PUT(0, "未投放"),
    LOCK_PUT(1, "已锁定(被业务员申请)"),
    DO_PUT(2, "投放中"),
    FINISH_PUT(3, "已投放"),
    NO_PASS(4, "已驳回"),
    DO_BACK(5,"退还中"),
    /**
     * 投放报表使用
     */
    IS_CANCEL(5, "已作废"),
    IS_ACCEPT(6,"配送中"),
    IS_ARRIVED(7,"待签收");

    private Integer status;
    private String desc;

    public static PutStatus convertEnum(int status){
        PutStatus[] putStatuses = PutStatus.values();
        for (PutStatus putStatus : putStatuses){
            if(putStatus.getStatus() == status){
                return putStatus;
            }
        }
        return null;
    }
}
