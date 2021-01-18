package com.szeastroc.icebox.newprocess.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
@NoArgsConstructor
@AllArgsConstructor
public class IceRepairOrder extends Model<IceRepairOrder> {

    @ApiModelProperty(value = "id")
    @TableId(type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "订单编号")
    @TableField(value = "order_number")
    private String orderNumber;

    @ApiModelProperty(value = "本部id")
    @TableField(value = "headquarters_dept_id")
    private Integer headquartersDeptId;
    @ApiModelProperty(value = "本部")
    @TableField(value = "headquarters_dept_name")
    private String headquartersDeptName;
    @TableField(value = "business_dept_id")
    @ApiModelProperty(value = "事业部id")
    private Integer businessDeptId;
    @TableField(value = "business_dept_name")
    @ApiModelProperty(value = "事业部名称")
    private String businessDeptName;
    @ApiModelProperty(value = "大区id")
    @TableField(value = "region_dept_id")
    private Integer regionDeptId;
    @ApiModelProperty(value = "大区事业部")
    @TableField(value = "region_dept_name")
    private String regionDeptName;
    @ApiModelProperty(value = "服务处id")
    @TableField(value = "service_dept_id")
    private Integer serviceDeptId;
    @TableField(value = "service_dept_name")
    @ApiModelProperty(value = "服务处")
    private String serviceDeptName;
    @TableField(value = "group_dept_id")
    @ApiModelProperty(value = "组id")
    private Integer groupDeptId;
    @TableField(value = "group_Dept_name")
    @ApiModelProperty(value = "组")
    private String groupDeptName;
    @ApiModelProperty(value = "客户编号")
    /**
     * 客户编号
     */
    @TableField(value = "customer_number")
    private String customerNumber;
    @ApiModelProperty(value = "客户名称")

    /**
     * 客户名称
     */
    @TableField(value = "customer_name")
    private String customerName;
    @ApiModelProperty(value = "客户地址")
    /**
     * 客户地址
     */
    @TableField(value = "customer_address")
    private String customerAddress;
    @ApiModelProperty(value = "客户类型")

    /**
     * 客户类型
     */
    @TableField(value = "customer_type")
    private Integer customerType;


    /**
     * 联系人
     */
    @TableField(value = "link_man")
    @ApiModelProperty(value = "联系人")
    private String linkMan;


    /**
     * 联系人电话
     */
    @TableField(value = "link_mobile")
    @ApiModelProperty(value = "联系人电话")
    private String linkMobile;
    @TableField(value = "box_id")
    @ApiModelProperty(value = "冰箱id")
    private Integer boxId;
    @TableField(value = "model_id")
    @ApiModelProperty(value = "冰箱型号id")
    private Integer modelId;
    /**
     * 型号
     */
    @TableField(value = "model_name")
    @ApiModelProperty(value = "冰箱型号")
    private String modelName;


    /**
     * 资产编号
     */
    @TableField(value = "asset_id")
    @ApiModelProperty(value = "资产编号")
    private String assetId;


    /**
     * 省份
     */
    @TableField(value = "province")
    @ApiModelProperty(value = "省份")
    private String province;


    /**
     * 城市
     */
    @TableField(value = "city")
    @ApiModelProperty(value = "城市")
    private String city;


    /**
     * 区
     */
    @TableField(value = "area")
    @ApiModelProperty(value = "区")
    private String area;


    /**
     * 问题描述
     */
    @TableField(value = "description")
    @ApiModelProperty(value = "问题描述")
    private String description;


    /**
     * 备注
     */
    @TableField(value = "remark")
    @ApiModelProperty(value = "备注")
    private String remark;


    /**
     * 订单状态
     */
    @TableField(value = "status")
    @ApiModelProperty(value = "订单状态")
    private Integer status;


    /**
     * 创建时间
     */
    @TableField(value = "created_time")
    private Date createdTime;


    /**
     * 更新时间
     */
    @TableField(value = "updated_time")
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