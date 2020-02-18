package com.szeastroc.icebox.newprocess.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "t_ice_back_order")
public class IceBackOrder {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 幂等key : order_id
     */
    @TableField(value = "resource_key")
    private String resourceKey;

    /**
     * 关联冰柜信息id
     */
    @TableField(value = "box_id")
    private Integer boxId;

    /**
     * 申请编号
     */
    @TableField(value = "apply_number")
    private String applyNumber;

    /**
     * 关联投放订单id
     */
    @TableField(value = "put_order_id")
    private Integer putOrderId;

    /**
     * 商户订单号(orderNum)
     */
    @TableField(value = "partner_trade_no")
    private String partnerTradeNo;

    @TableField(value = "openid")
    private String openid;

    @TableField(value = "amount")
    private BigDecimal amount;

    /**
     * 创建人
     */
    @TableField(value = "created_by")
    private Integer createdBy;

    /**
     * 创建时间
     */
    @TableField(value = "created_time")
    private Date createdTime;

    /**
     * 更新人
     */
    @TableField(value = "updated_by")
    private Integer updatedBy;

    /**
     * 更新时间
     */
    @TableField(value = "updated_time")
    private Date updatedTime;

    public static IceBackOrderBuilder builder() {
        return new IceBackOrderBuilder();
    }
}