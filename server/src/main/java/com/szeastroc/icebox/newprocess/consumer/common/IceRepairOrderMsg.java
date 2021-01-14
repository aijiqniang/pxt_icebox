package com.szeastroc.icebox.newprocess.consumer.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ApiModel
public class IceRepairOrderMsg extends Page implements Serializable {

    private static final long serialVersionUID = -4750978713271531956L;

    /**
     * 下载任务id
     */
    @ApiModelProperty(value = "下载记录id")
    private Integer recordsId;
    /**
     *本部id
     */
    @ApiModelProperty(value = "本部id")
    private Integer headquartersDeptId;
    /**
     *事业部id
     */
    @ApiModelProperty(value = "事业部id")
    private Integer businessDeptId;
    /**
     *大区id
     */
    @ApiModelProperty(value = "大区id")
    private Integer regionDeptId;
    /**
     *服务处id
     */
    @ApiModelProperty(value = "服务处id")
    private Integer serviceDeptId;
    /**
     *组id
     */
    @ApiModelProperty(value = "组id")
    private Integer groupDeptId;
    /**
     *订单编号
     */
    @ApiModelProperty(value = "订单编号")
    private String orderNumber;
    /**
     *客户名称
     */
    @ApiModelProperty(value = "客户名称")
    private String customerName;
    /**
     *资产编号
     */
    @ApiModelProperty(value = "资产编号")
    private String assetId;

    @ApiModelProperty(value = "订单状态")
    private Integer status;

}
