package com.szeastroc.icebox.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * TODO
 *
 * @author Aijinqiang
 * @version 1.0
 * @date 2021/4/27 9:55
 */
@Getter
@AllArgsConstructor
public enum IsSyncEnum {

        NO_SYNC(1,"未同步"),
        IS_SEND(2,"已发送"),
        IS_SYNC(3,"已同步");


    private Integer status;
    private String desc;
}
