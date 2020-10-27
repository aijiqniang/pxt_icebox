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
     * 冰柜状态 0:异常，1:正常，2:报废，3:遗失，4:报修
     */
    @Getter
    @AllArgsConstructor
    public enum StatusEnum {

        ABNORMAL(0, "异常"),
        NORMAL(1, "正常"),
        SCRAP(2, "报废"),
        LOSE(3, "遗失"),
        REPAIR(4, "报修"),
        IS_SCRAPING(5, "报废中"),
        IS_LOSEING(6, "遗失中");

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

    /**
     * 冰柜类型 0:旧冰柜，1:新冰柜
     */
    @Getter
    @AllArgsConstructor
    public enum TypeEnum {

        OLD_ICE_BOX(0, "旧冰柜"),
        NEW_ICE_BOX(1, "新冰柜");

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

    /**
     * 冰柜类型 0:旧冰柜，1:新冰柜
     */
    @Getter
    @AllArgsConstructor
    public enum ChangeSourceTypeEnum {
        SFA(0, "SFA"),
        BACKSTAGE_MANAGEMENT(1, "后台管理");
        private Integer type;
        private String desc;
        public static String getDesc(Integer type) {
            for (ChangeSourceTypeEnum enu : ChangeSourceTypeEnum.values()) {
                if (enu.getType().equals(type)) {
                    return enu.getDesc();
                }
            }
            return "";
        }
    }
}
