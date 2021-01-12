package com.szeastroc.icebox.newprocess.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

@Getter
@AllArgsConstructor
public enum VisitCycleEnum {

    AMONDAYVISIT(1, "一周一访"),
    TWOMONDAYVISIT(2, "两周一访"),
    THREEMONDAYVISIT(3, "三周一访"),
    AMONTHVISIT(4, "四周一访");

    private int result;
    private String code;

    public static String getDescByCode(Integer result){
        VisitCycleEnum[] visitCycleEnums = VisitCycleEnum.values();
        for (VisitCycleEnum visitCycleEnum:visitCycleEnums){
            if(Objects.nonNull(result) && visitCycleEnum.getResult()==result){
                return visitCycleEnum.getCode();
            }
        }
        return "";
    }
}
