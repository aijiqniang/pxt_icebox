package com.szeastroc.icebox.newprocess.vo.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @ClassName: InvalidShelfApplyRequest
 * @Description:
 * @Author: 陈超
 * @Date: 2021/6/3 17:38
 **/
@Data
@ApiModel
public class InvalidShelfApplyRequest {
    @ApiModelProperty("用户姓名")
    private String userName;

    @ApiModelProperty("用户id")
    private Integer userId;

    @ApiModelProperty("申请编号")
    private String applyNumber;

    @ApiModelProperty("备注")
    private String remark;
}
