package com.szeastroc.icebox.newprocess.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BackEnum {
    //0：待接单 1：审批中 2：配送中 3：未投放 4：已作废 5：驳回
    WAIT_ORDER(0,"待接单"),  //退还申请审核通过后
    IS_DEFAULT(1, "审批中"), //商户退还提交后
    IS_ACCEPT(2,"配送中"),   //配送员已经接单确认
    NO_PUT(3, "未投放"),     //配送商已经确认收柜
    IS_CANCEL(4, "已作废"),  //提交人自己作废
    UN_PASS(5, "驳回");      //审批人驳回

    private int status;
    private String desc;

}
