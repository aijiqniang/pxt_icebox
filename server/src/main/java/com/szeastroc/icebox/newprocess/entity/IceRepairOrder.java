package com.szeastroc.icebox.newprocess.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 冰柜维修订单表(IceRepairOrder)表实体类
 *
 * @author chenchao
 * @since 2021-01-12 15:58:23
 */
@SuppressWarnings("serial")
@Data
@Builder
@Accessors
@TableName("t_ice_repair_order")
@ApiModel
public class IceRepairOrder extends Model<IceRepairOrder> {

    @ApiModelProperty(value = "id")
    private Integer id;

    @ApiModelProperty(value = "订单编号")
    private String orderNumber;

    @ApiModelProperty(value = "本部id")
    private Integer headquartersDeptId;
    @ApiModelProperty(value = "本部")
    private String headquartersDeptName;
    @ApiModelProperty(value = "事业部id")

    private Integer businessDeptId;
    @ApiModelProperty(value = "事业部名称")

    private String businessDeptName;
    @ApiModelProperty(value = "大区id")

    private Integer regionDeptId;
    @ApiModelProperty(value = "大区事业部")

    private String regionDeptName;
    @ApiModelProperty(value = "服务处id")

    private Integer serviceDeptId;

    @ApiModelProperty(value = "服务处")
    private String serviceDeptName;

    @ApiModelProperty(value = "组id")
    private Integer groupDeptId;

    @ApiModelProperty(value = "组")
    private String groupDeptName;

    @ApiModelProperty(value = "客户编号")
    /**
     * 客户编号
     */

    private String customerNumber;
    @ApiModelProperty(value = "客户名称")

    /**
     * 客户名称
     */

    private String customerName;

    @ApiModelProperty(value = "客户地址")
    /**
     * 客户地址
     */

    private String customerAddress;
    @ApiModelProperty(value = "客户类型")

    /**
     * 客户类型
     */
    private Integer customerType;


    /**
     * 联系人
     */
    @ApiModelProperty(value = "联系人")
    private String linkMan;


    /**
     * 联系人电话
     */
    @ApiModelProperty(value = "联系人电话")
    private String linkMobile;

    @ApiModelProperty(value = "冰箱id")
    private Integer boxId;

    @ApiModelProperty(value = "冰箱型号id")
    private Integer modelId;


    /**
     * 型号
     */
    @ApiModelProperty(value = "冰箱型号")
    private String modelName;


    /**
     * 资产编号
     */
    @ApiModelProperty(value = "资产编号")
    private String assetId;


    /**
     * 省份
     */
    @ApiModelProperty(value = "省份")
    private String province;


    /**
     * 城市
     */
    @ApiModelProperty(value = "城市")
    private String city;


    /**
     * 区
     */
    @ApiModelProperty(value = "区")
    private String area;


    /**
     * 问题描述
     */
    @ApiModelProperty(value = "问题描述")
    private String description;


    /**
     * 备注
     */
    @ApiModelProperty(value = "备注")
    private String remark;


    /**
     * 订单状态
     */
    @ApiModelProperty(value = "订单状态")
    private Integer status;


    /**
     * 创建时间
     */

    private Date createdTime;


    /**
     * 更新时间
     */

    private Date updatedTime;


    /**
     * 获取主键值
     *
     * @return 主键值
     */
    @Override
    protected Serializable pkVal() {
        return this.id;
    }
}