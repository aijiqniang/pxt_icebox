package com.szeastroc.icebox.newprocess.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @ClassName: DisplayShelfVO
 * @Description:
 * @Author: 陈超
 * @Date: 2021/5/31 11:13
 **/
@Data
@ApiModel
public class SupplierDisplayShelfVO {

    @ApiModelProperty("经销商编号")
    private String supplierNumber;

    @ApiModelProperty("经销商名称")
    private String supplierName;

    @ApiModelProperty("经销商联系人")
    private String linkMan;

    @ApiModelProperty("经销商联系电话")
    private String linkMobile;

    @ApiModelProperty("经销商联系地址")
    private String linkAddress;

    @ApiModelProperty("服务处id")
    private Integer serviceDeptId;

    @ApiModelProperty("服务处名称")
    private String serviceDeptName;

    @ApiModelProperty("货架名称")
    private String name;

    @ApiModelProperty("货架类型")
    private Integer type;

    @ApiModelProperty("货架数量")
    private Integer count;

    @ApiModelProperty("尺寸大小")
    private String size;

    @ApiModelProperty("拜访频率")
    private String visitTypeName;

    @ApiModelProperty("客户名称")
    private String customerName;

    @ApiModelProperty("客户等级")
    private String customerLevel;

    @ApiModelProperty("客户联系人")
    private String customerLinkMan;

    @ApiModelProperty("客户联系人")
    private String customerLinkMobile;

    @ApiModelProperty("客户联系人")
    private String customerLinkAddress;

}
