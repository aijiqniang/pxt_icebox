package com.szeastroc.icebox.newprocess.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author xiao
 * @Date create in 2020/4/22 19:35
 * @Description:
 */
public class IceBoxEnums {

    /**
     * 冰柜状态 1:正常 0:异常
     */
    @Getter
    @AllArgsConstructor
    public enum StatusEnum {

        NORMAL(1, "正常"),
        ABNORMAL(0, "异常");

        private Integer type;
        private String desc;

        public static String getDesc(Integer type) {
            for (StatusEnum enu : StatusEnum.values()) {
                if (enu.getType().equals(type)) {
                    return enu.getDesc();
                }
            }
            return "";
        }
    }
}
