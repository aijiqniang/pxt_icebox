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
@TableName(value = "t_ice_transfer_record")
public class IceTransferRecord {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 申请编号
     */
    @TableField(value = "apply_number")
    private String applyNumber;

    /**
     * 业务类型 0:入库 1:投放 2:退还
     */
    @TableField(value = "service_type")
    private Integer serviceType;

    /**
     * 关联冰柜ID (t_ice_box)
     */
    @TableField(value = "box_id")
    private Integer boxId;

    /**
     * 参与的经销商ID
     */
    @TableField(value = "supplier_id")
    private Integer supplierId;

    /**
     * 参与的门店Number
     */
    @TableField(value = "store_number")
    private String storeNumber;

    /**
     * 发出时间
     */
    @TableField(value = "send_time")
    private Date sendTime;

    /**
     * 接收时间
     */
    @TableField(value = "receive_time")
    private Date receiveTime;

    /**
     * 经办人ID (t_sys_user_info)
     */
    @TableField(value = "apply_user_id")
    private Integer applyUserId;

    /**
     * 经办时间
     */
    @TableField(value = "apply_time")
    private Date applyTime;

    /**
     * 交易金额
     */
    @TableField(value = "transfer_money")
    private BigDecimal transferMoney;

    /**
     * 记录状态 1:进行中2:已完成3:已取消
     */
    @TableField(value = "record_status")
    private Integer recordStatus;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private Date createTime;

    /**
     * 修改时间
     */
    @TableField(value = "update_time")
    private Date updateTime;
}