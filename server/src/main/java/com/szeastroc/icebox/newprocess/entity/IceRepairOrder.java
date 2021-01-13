package com.szeastroc.icebox.newprocess.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
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
public class IceRepairOrder extends Model<IceRepairOrder> {


    private Integer id;


    private String orderNumber;


    private Integer headquartersDeptId;


    private String headquartersDeptName;


    private Integer businessDeptId;


    private String businessDeptName;


    private Integer regionDeptId;


    private String regionDeptName;


    private Integer serviceDeptId;


    private String serviceDeptName;


    private Integer groupDeptId;


    private String groupDeptName;


    /**
     * 客户编号
     */

    private String customerNumber;


    /**
     * 客户名称
     */

    private String customerName;


    /**
     * 客户地址
     */

    private String customerAddress;


    /**
     * 客户类型
     */

    private Integer customerType;


    /**
     * 联系人
     */

    private String linkMan;


    /**
     * 联系人电话
     */

    private String linkMobile;


    private Integer boxId;


    private Integer modelId;


    /**
     * 型号
     */

    private String modelName;


    /**
     * 资产编号
     */

    private String assetId;


    /**
     * 省份
     */

    private String province;


    /**
     * 城市
     */

    private String city;


    /**
     * 区
     */

    private String area;



    /**
     * 问题描述
     */

    private String description;


    /**
     * 备注
     */

    private String remark;


    /**
     * 订单状态
     */
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