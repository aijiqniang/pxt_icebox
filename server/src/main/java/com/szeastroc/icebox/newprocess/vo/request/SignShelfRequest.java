package com.szeastroc.icebox.newprocess.vo.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @ClassName: StoreSignShelfRequest
 * @Description:
 * @Author: 陈超
 * @Date: 2021/6/3 10:21
 **/
@Data
@ApiModel
public class SignShelfRequest {

    @ApiModelProperty("客户编号")
    private String customerNumber;
    @ApiModelProperty("申请编号")
    private String applyNumber;
    @ApiModelProperty("货架数量")
    private List<Shelf> shelfList;

    @Data
    @ApiModel
    public static class Shelf {
        @ApiModelProperty("签收数量")
        private Integer count;

        @ApiModelProperty("货架类型")
        private Integer type;

        @ApiModelProperty("尺寸大小")
        private Integer size;

    }

}
