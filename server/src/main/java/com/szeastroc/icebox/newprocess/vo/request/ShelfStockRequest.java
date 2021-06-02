package com.szeastroc.icebox.newprocess.vo.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @ClassName: ShelfStockRequest
 * @Description:
 * @Author: 陈超
 * @Date: 2021/6/2 11:07
 **/
@Data
@ApiModel
public class ShelfStockRequest {

    @ApiModelProperty("客户编号")
    private String customerNumber;

    @ApiModelProperty("营小区域id")
    private Integer marketAreaId;

    @ApiModelProperty(value = "查询类型",notes = "0已投放 1可供申请 2处理中 ")
    private Integer type;
}
