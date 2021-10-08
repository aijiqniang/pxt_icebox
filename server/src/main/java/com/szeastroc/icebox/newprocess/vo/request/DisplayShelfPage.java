package com.szeastroc.icebox.newprocess.vo.request;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.szeastroc.icebox.newprocess.entity.DisplayShelf;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @ClassName: DisplayShelfPage
 * @Description:
 * @Author: 陈超
 * @Date: 2021/5/28 9:42
 **/
@Data
@ApiModel
public class DisplayShelfPage extends Page<DisplayShelf> {

    @ApiModelProperty(value = "部门id")
    private Integer deptId;
    @ApiModelProperty(value = "经销商编号")
    private String supplierNumber;
    @ApiModelProperty(value = "投放客户名称")
    private String putName;
    @ApiModelProperty(value = "投放客户编号")
    private String putNumber;
    @ApiModelProperty(value = "货架状态")
    private Integer status;
    @ApiModelProperty(value = "投放状态")
    private Integer putStatus;
    @ApiModelProperty(value = "签收状态")
    private Integer signStatus;
}
