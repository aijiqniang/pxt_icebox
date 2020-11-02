package com.szeastroc.icebox.newprocess.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum IceBoxReprotTypeEnum {
	//报表类型：1-异常报备，2-冰柜巡检
	EXCEPTION(1, "异常报备"),
	EXAMINE(2, "冰柜巡检");
	
    private Integer type;
    private String desc;

    public static String getDesc(Integer type){
    	for(IceBoxReprotTypeEnum typeEnum: IceBoxReprotTypeEnum.values()){
    		if(typeEnum.getType().equals(type)){
    			return typeEnum.getDesc();
			}
		}
		return "";
	}
}
