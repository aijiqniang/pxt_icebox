package com.szeastroc.icebox.newprocess.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

/**
 * TODO
 *
 * @author Aijinqiang
 * @version 1.0
 * @date 2021/9/24 15:50
 */
@Data
public class IceEventVo {


    @Data
    @Builder
    public static class IceboxList{
        @ApiModelProperty("冰柜id")
        Integer iceboxId;
        @ApiModelProperty("智能冰柜编号")
        String externalId;

    }
}
