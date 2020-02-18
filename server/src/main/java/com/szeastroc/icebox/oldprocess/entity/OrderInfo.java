package com.szeastroc.icebox.oldprocess.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.szeastroc.icebox.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by Tulane
 * 2019/5/23
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "t_order_info")
public class OrderInfo {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer chestId;
    private Integer chestPutRecordId;
    private String orderNum;
    private String openid;
    private BigDecimal totalMoney;
    private BigDecimal payMoney;
    private String prayId;
    private String transactionId;
    private String tradeState;
    private String tradeStateDesc;
    private Date payTime;
    private Integer status;
    private Date createTime;
    private Date updateTime;

    public OrderInfo(Integer chestId, Integer chestPutRecordId, String orderNum, String openid, BigDecimal totalMoney, String prayId) {
        this.chestId = chestId;
        this.chestPutRecordId = chestPutRecordId;
        this.orderNum = orderNum;
        this.openid = openid;
        this.totalMoney = totalMoney;
        this.prayId =  prayId;
        this.createTime = new Date();
        this.updateTime = this.createTime;
        this.status = OrderStatus.IS_PAY_ING.getStatus();
    }

    public OrderInfo(Integer chestId, String orderNum, String openid, BigDecimal totalMoney, String prayId) {
        this.chestId = chestId;
        this.orderNum = orderNum;
        this.openid = openid;
        this.totalMoney = totalMoney;
        this.prayId =  prayId;
        this.createTime = new Date();
        this.updateTime = this.createTime;
        this.status = OrderStatus.IS_PAY_ING.getStatus();
    }

}
