package com.szeastroc.icebox.oldprocess.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by Tulane
 * 2019/7/19
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "t_wechat_transfer_order")
public class WechatTransferOrder {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String resourceKey;
    private Integer chestId;
    private Integer chestPutRecordId;
    private Integer orderId;
    private String partnerTradeNo;
    private String openid;
    private BigDecimal amount;
    private Date createTime;
    private Date updateTime;

    public WechatTransferOrder(String resourceKey, Integer chestId, Integer chestPutRecordId, Integer orderId, String openid, BigDecimal amount) {
        this.resourceKey = resourceKey;
        this.chestId = chestId;
        this.chestPutRecordId = chestPutRecordId;
        this.orderId = orderId;
        this.openid = openid;
        this.amount = amount;
        this.createTime = new Date();
        this.updateTime = this.createTime;
    }

}
