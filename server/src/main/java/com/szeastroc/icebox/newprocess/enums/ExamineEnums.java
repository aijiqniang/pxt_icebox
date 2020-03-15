package com.szeastroc.icebox.newprocess.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class ExamineEnums {

    @Getter
    @AllArgsConstructor
    public enum ExamineTime {

        FIRST_TIME(0, "第一次巡检"),
        LAST_TIME(1, "最后一次巡检");


        private Integer type;
        private String desc;

        public static String getDesc(Integer type) {
            for (ExamineTime examineTime : ExamineTime.values()) {
                if (examineTime.getType().equals(type)) {
                    return examineTime.getDesc();
                }
            }
            return "";
        }
    }
}
