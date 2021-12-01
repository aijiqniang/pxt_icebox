package com.szeastroc.icebox.newprocess.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * TODO
 *
 * @author Aijinqiang
 * @version 1.0
 * @date 2021/10/8 17:21
 */
@Getter
@AllArgsConstructor
public enum IceAlarmTypeEnum {
    DISTANCE(1, "位移报警"),
    OUTLINE(2, "断电报警"),
    OVER_TEMPERTURE(3,"超温报警"),
    PERSON(4,"人流量报警");

    private int type;
    private String desc;


    public static String getDesc(int type){
        for(IceAlarmTypeEnum iceAlarmTypeEnum : IceAlarmTypeEnum.values()){
            if(iceAlarmTypeEnum.getType() == type){
                return iceAlarmTypeEnum.getDesc();
            }
        }
        return null;
    };
}
