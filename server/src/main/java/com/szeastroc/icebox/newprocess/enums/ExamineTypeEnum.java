package com.szeastroc.icebox.newprocess.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created by Tulane
 * 2019/9/2
 */
@Getter
@AllArgsConstructor
public enum ExamineTypeEnum {

    BACK_GOODS(1, "退货单"),
    ACTIVITY_MATERIALS(2, "活动领料单"),
    COST_RETURN(3, "付费陈列费用返还确认单"),
    CUSTOM_CHANGE(4, "修改门店详情"),
    FLIT(5, "调拨单"),
    CUSTOM_ADD(6, "新增门店"),
    SIGN_ERR(7, "异常签到"),
    LOCATION_ERR(8, "门店纠偏"),
    PD(9, "盘点单"),
    ICEBOX_REFUND(10,"冰柜退还"),
    ICEBOX_PUT(11,"冰柜投放"),
    RECALL_BACK_GOODS(12,"撤回退货单"),
    INVENTORY(13,"盘点"),
    FAKE_STORE(14,"虚假门店"),
    DISTRIBUTOR_ADD(15,"新增配送商"),
    DISTRIBUTOR_CHANGE(16, "修改配送商详情"),
    ICEBOX_TRANSFER(17,"冰柜转移"),
    ICEBOX_NORMAL(18,"冰柜正常"),
    ICEBOX_SCRAP(19,"冰柜报废"),
    ICEBOX_LOSE(20,"冰柜遗失");

    private int type;
    private String desc;

    public static ExamineTypeEnum convertVo(int type){
        ExamineTypeEnum[] examineTypeEnums = ExamineTypeEnum.values();
        for (ExamineTypeEnum examineTypeEnum : examineTypeEnums){
            if(examineTypeEnum.getType() == type){
                return examineTypeEnum;
            }
        }
        return null;
    }
}
