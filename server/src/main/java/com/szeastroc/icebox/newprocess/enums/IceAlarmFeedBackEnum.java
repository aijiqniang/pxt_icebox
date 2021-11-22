package com.szeastroc.icebox.newprocess.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO
 *
 * @author Aijinqiang
 * @version 1.0
 * @date 2021/11/17 11:26
 */
@Getter
@AllArgsConstructor
public enum IceAlarmFeedBackEnum {

    MOVE_1(1,"冰柜确实被移动"),
    MOVE_2(1,"冰柜实际未被移动（门店定位异常）"),
    MOVE_3(1,"冰柜实际未被移动（冰柜定位异常）"),
    OUTLINE_1(2,"冰柜确实被断电(正常经营)"),
    OUTLINE_2(2,"冰柜确实被断电(非正常经营)"),
    OUTLINE_3(2,"冰柜实际未断电"),
    TEMPTUREATURE_1(3,"冰柜确实不制冷"),
    TEMPTUREATURE_2(3,"冰柜被人为调温"),
    PERSON_1(4,"冰柜开门感应故障"),
    PERSON_2(4,"冰柜确实人流少（调整位置）"),
    PERSON_3(4,"冰柜确实人流少(退柜)");


    private Integer type;
    private String desc;

    public static List<String> getDesc(Integer type){
        List<String> str = new ArrayList<>();
        for(IceAlarmFeedBackEnum statusEnum: IceAlarmFeedBackEnum.values()){
            if(statusEnum.getType().equals(type)){
                str.add(statusEnum.getDesc());
            }
        }
        return str;
    }
}
