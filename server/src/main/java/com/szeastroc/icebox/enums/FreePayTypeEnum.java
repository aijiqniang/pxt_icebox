package com.szeastroc.icebox.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by Tulane
 * 2019/8/21
 */
@Getter
@AllArgsConstructor
public enum FreePayTypeEnum {

    UN_FREE(1, "不免押"),
    IS_FREE(2, "免押");

    private int type;
    private String desc;

    public static FreePayTypeEnum convertVo(int type){
        FreePayTypeEnum[] freePayTypeEnums = FreePayTypeEnum.values();
        for (FreePayTypeEnum freePayTypeEnum : freePayTypeEnums){
            if(freePayTypeEnum.getType() == type){
                return freePayTypeEnum;
            }
        }
        return null;
    }
}
