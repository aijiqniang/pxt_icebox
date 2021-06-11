package com.szeastroc.icebox.newprocess.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * TODO
 *
 * @author Aijinqiang
 * @version 1.0
 * @date 2021/6/4 15:17
 */
@Getter
@AllArgsConstructor
public enum HandOverEnum {

    DO_HANDOVER(1,"交接中"),
    PASS_HANDOVER(2,"已交接"),
    REJECT_HANOVER(3,"已驳回");

    private Integer type;
    private String desc;
}
