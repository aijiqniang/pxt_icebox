package com.szeastroc.icebox.newprocess.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CommonIsCheckEnum {

	NO_CHECK(0, "未审核"),
	IS_CHECKING(-1, "审核中"),
	IS_HANGUP(-2, "已撤回"),
	IS_CHECK(1, "已审核"),
	NO_PASS(2, "已驳回");
	
    private Integer status;
    private String desc;

    public static String getDesc(Integer status){
    	for(CommonIsCheckEnum statusEnum: CommonIsCheckEnum.values()){
    		if(statusEnum.getStatus().equals(status)){
    			return statusEnum.getDesc();
			}
		}
		return "";
	}
}
