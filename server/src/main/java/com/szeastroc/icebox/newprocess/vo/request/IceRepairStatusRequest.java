package com.szeastroc.icebox.newprocess.vo.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * @ClassName: RepairIceDTO
 * @Description:
 * @Author: 陈超
 * @Date: 2021/1/12 14:40
 **/
@Data
@ApiModel
public class IceRepairStatusRequest {

    /**
     * 订单编号
     */
    @ApiModelProperty(value = "订单编号",required = true)
    private String orderNumber;

    /**
     * 完成状态
     */
    @ApiModelProperty(value = "完成状态")
    private String finishStatus;

    /**
     * 工单状态
     */
    @ApiModelProperty(value = "工单状态")
    private Integer status;
    /**
     * 服务提供商编号
     */
    @ApiModelProperty(value = "服务提供商编号")
    private String serviceProviderCode;

    /**
     * 服务提供商名称
     */
    @ApiModelProperty(value = "服务提供商名称")
    private String serviceProviderName;
    /**
     * 受理时间
     */
    @ApiModelProperty(value = "受理时间")
    private Date acceptTime;
    /**
     * 故障原因
     */
    @ApiModelProperty(value = "故障原因")
    private String cause;
    /**
     * 维修措施
     */
    @ApiModelProperty(value = "维修措施")
    private String repairMethod;
    /**
     * 实际服务类型
     */
    @ApiModelProperty(value = "实际服务类型")
    private String factServiceType;
    /**
     * 实际服务方式
     */
    @ApiModelProperty(value = "实际服务方式")
    private String factServiceMethod;
    /**
     * 中间结果描述
     */
    @ApiModelProperty(value = "中间结果描述")
    private String result;
    /**
     * 反馈备注
     */
    @ApiModelProperty(value = "反馈备注")
    private String feedback;
    /**
     * 服务完成时间
     */
    @ApiModelProperty(value = "服务完成时间")
    private Date finishTime;
    /**
     * 工程师
     */
    @ApiModelProperty(value = "工程师")
    private String engineer;
}
