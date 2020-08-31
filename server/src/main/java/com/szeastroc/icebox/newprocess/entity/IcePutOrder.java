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
@TableName(value = "t_ice_put_order")
public class IcePutOrder {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 关联冰柜信息id
     */
    @TableField(value = "chest_id")
    private Integer chestId;

    /**
     * 申请编号
     */
    @TableField(value = "apply_number")
    private String applyNumber;

    /**
     * 订单号
     */
    @TableField(value = "order_num")
    private String orderNum;

    @TableField(value = "openid")
    private String openid;

    /**
     * 订单总金额
     */
    @TableField(value = "total_money")
    private BigDecimal totalMoney;

    /**
     * 实际支付金额
     */
    @TableField(value = "pay_money")
    private BigDecimal payMoney;

    /**
     * 流水号
     */
    @TableField(value = "pray_id")
    private String prayId;

    /**
     * 微信支付订单号
     */
    @TableField(value = "transaction_id")
    private String transactionId;

    /**
     * 交易状态-主动查询时会有 : 
SUCCESS—支付成功
REFUND—转入退款
NOTPAY—未支付
CLOSED—已关闭
REVOKED—已撤销（刷卡支付）
USERPAYING--用户支付中
PAYERROR--支付失败(其他原因，如银行返回失败)
     */
    @TableField(value = "trade_state")
    private String tradeState;

    @TableField(value = "trade_state_desc")
    private String tradeStateDesc;

    /**
     * 支付时间
     */
    @TableField(value = "pay_time")
    private Date payTime;

    /**
     * 状态 0.已关闭 1.支付中 2.已完成
     */
    @TableField(value = "status")
    private Integer status;

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

    /**
     * 更新时间
     */
    @TableField(value = "order_source")
    private Integer orderSource;
}