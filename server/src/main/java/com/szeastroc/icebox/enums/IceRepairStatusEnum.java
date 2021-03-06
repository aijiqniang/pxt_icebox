package com.szeastroc.icebox.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @ClassName: IceRepairStatusEnum
 * @Description:
 * @Author: 陈超
 * @Date: 2021/1/25 10:10
 **/
@Getter
@AllArgsConstructor
public enum IceRepairStatusEnum {
    REPAIRING(0,"报修中"),
    NO_ARRANGE(1, "待派工"),
    ARRANGED(2, "已派工"),
    ACCEPTED(3, "已接受"),
    LINK_CUSTOMER(4, "已联系用户"),
    ARRANGE_ENGINEER(5, "已指派工程师"),
    FEEDBACK(6, "已反馈"),
    ACCEPT_FEEDBACK(7, "已接单反馈"),
    ORDER_FINISH(8, "已结单"),
    AUTO_AUDIT(9, "已自动审核"),
    PASS_INTERVIEW(10, "回访通过"),
    CLOSE(20, "已关闭"),
    CANCEL(50, "已取消"),
    CANCEL_INTERVIEW(51, "回访取消"),
    CONFIRM_CANCEL(52, "已确认取消"),
    ;

    private Integer status;
    private String desc;

    public static IceRepairStatusEnum convertVo(int status) {
        IceRepairStatusEnum[] enums = IceRepairStatusEnum.values();
        for (IceRepairStatusEnum statusEnum : enums) {
            if (statusEnum.getStatus() == status) {
                return statusEnum;
            }
        }
        return null;
    }

    public static String getDesc(Integer status) {
        for (IceRepairStatusEnum enu : IceRepairStatusEnum.values()) {
            if (enu.getStatus().equals(status)) {
                return enu.getDesc();
            }
        }
        return "";
    }
}
