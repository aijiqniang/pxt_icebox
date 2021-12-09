package com.szeastroc.icebox.newprocess.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    MOVE_1(1,"1-1","冰柜确实被移动"),
    MOVE_2(1,"1-2","冰柜实际未被移动（门店定位异常）"),
    MOVE_3(1,"1-3","冰柜实际未被移动（冰柜定位异常）"),
    OUTLINE_1(2,"2-1","冰柜确实被断电(正常经营)"),
    OUTLINE_2(2,"2-2","冰柜确实被断电(非正常经营)"),
    OUTLINE_3(2,"2-3","冰柜实际未断电"),
    TEMPTUREATURE_1(3,"3-1","冰柜确实不制冷"),
    TEMPTUREATURE_2(3,"3-2","冰柜被人为调温"),
    PERSON_1(4,"4-1","冰柜开门感应故障"),
    PERSON_2(4,"4-2","冰柜确实人流少（调整位置）"),
    PERSON_3(4,"4-3","冰柜确实人流少(退柜)");


    private Integer type;
    private String littleType;
    private String desc;

    public static Map<String,String> getDesc(Integer type){
        Map<String,String> map = new HashMap<>();
        for(IceAlarmFeedBackEnum statusEnum: IceAlarmFeedBackEnum.values()){
            if(statusEnum.getType().equals(type)){
                map.put(statusEnum.getLittleType(),statusEnum.getDesc());
            }
        }
        return map;
    }
}
