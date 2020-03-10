package com.szeastroc.icebox.newprocess.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 冰柜签收状态Vo
 */
@Getter
@Setter
public class IceBoxStatusVo {

    private boolean signFlag; // 是否允许下一步门店签收

    /**
     * 1: 正常 2: 冰柜已投放 3: 当前门店未申请该冰柜 4: 申请审批未完成  5: 冰柜不存在(二维码未找到)
     */
    private int status;
    private String message;
}
