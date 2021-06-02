package com.szeastroc.icebox.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @ClassName: DisplayShelfTypeEnum
 * @Description:
 * @Author: 陈超
 * @Date: 2021/5/28 9:16
 **/
@AllArgsConstructor
public enum DisplayShelfTypeEnum {

    ENERGY_FOUR(1, "东鹏特饮四层陈列架"),
    LEMON_TEA_FOUR(2, "由柑柠檬茶四层陈列架"),
    SODA_FOUR(3, "东鹏加気四层陈列架");

    @Getter
    private final Integer type;
    @Getter
    private final String desc;


    public static DisplayShelfTypeEnum getByType(int type) {
        for (DisplayShelfTypeEnum displayShelfTypeEnum : DisplayShelfTypeEnum.values()) {
            if (displayShelfTypeEnum.getType().equals(type)) {
                return displayShelfTypeEnum;
            }
        }
        return null;
    }
}
