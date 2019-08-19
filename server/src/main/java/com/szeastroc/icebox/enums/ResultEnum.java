package com.szeastroc.icebox.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created by Tulane
 * 2019/6/20
 */
@Getter
@AllArgsConstructor
public enum ResultEnum {
    CLIENT_IS_NOT_REGISTER(1001, "客户未注册"),
    CLIENT_ICECHEST_IS_NOT_PUT(1002, "该客户未投放冰柜"),
    ICE_CHEST_IS_NOT_UN_PUT(1003, "该冰柜不是未投放状态, 无法绑定"),
    CLIENT_HAVE_ICECHEST_NOW(1004, "该客户已存在绑定的冰柜, 无法继续绑定"),
    ICE_CHEST_IS_HAVE_PUT_ING(1005, "该冰柜存在正在投放的客户, 无法绑定"),
    ORDER_IS_CANCEL_AND_RETRY_NEW_ORDER(2001, "订单已取消, 请重新下单"),
    LOOP_LONG_CONNECTION_FINISH_AND_RETRY_AGIN(2002, "查询订单状态长链接时间超时, 跳出循环等待再次请求"),
    TAKE_BAKE_ERR_WITH_EXPIRE_TIME(3001, "您使用的冰柜在正常协议期内，如特殊情况需退还，请联系您的业务员");

    private Integer code;
    private String message;
}
