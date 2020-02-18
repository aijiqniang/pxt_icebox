package com.szeastroc.icebox.oldprocess.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.util.Date;

import com.szeastroc.common.enums.CommonStatus;
import com.szeastroc.icebox.enums.FreePayTypeEnum;
import com.szeastroc.icebox.enums.RecordStatus;
import com.szeastroc.icebox.enums.ServiceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "t_ice_chest_put_record")
public class IceChestPutRecord {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 关联冰柜id
     */
    @TableField(value = "chest_id")
    private Integer chestId;

    /**
     * 申请人企业微信号
     */
    @TableField(value = "applicant_qywechat_id")
    private String applicantQywechatId;

    /**
     * 申请时间
     */
    @TableField(value = "apply_time")
    private Date applyTime;

    /**
     * 对应发出客户信息主键id
     */
    @TableField(value = "send_client_id")
    private Integer sendClientId;

    @TableField(value = "send_qywechat_id")
    private Integer sendQywechatId;

    /**
     * 发出时间
     */
    @TableField(value = "send_time")
    private Date sendTime;

    /**
     * 对应接收客户信息主键id
     */
    @TableField(value = "receive_client_id")
    private Integer receiveClientId;

    /**
     * 接收人企业微信id
     */
    @TableField(value = "receive_qywechat_id")
    private Integer receiveQywechatId;

    /**
     * 接收时间
     */
    @TableField(value = "receive_time")
    private Date receiveTime;

    /**
     * 记录状态: 1:申请中 2:发出中 3:已接收
     */
    @TableField(value = "record_status")
    private Integer recordStatus;

    /**
     * 业务类型 1:投放 2:入库
     */
    @TableField(value = "service_type")
    private Integer serviceType;

    /**
     * 押金
     */
    @TableField(value = "deposit_money")
    private BigDecimal depositMoney;

    /**
     * 免押类型 1:不免押 2:免押
     */
    @TableField(value = "free_pay_type")
    private Integer freePayType;

    /**
     * 状态 1:正常 0:关闭
     */
    @TableField(value = "status")
    private Integer status;

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

    public IceChestPutRecord(Integer chestId, String applicantQywechatId, Date applyTime, Integer sendClientId, Integer receiveClientId, BigDecimal depositMoney) {
        this.chestId = chestId;
        this.applicantQywechatId = applicantQywechatId;
        this.applyTime = applyTime;
        this.sendClientId = sendClientId;
        this.receiveClientId = receiveClientId;
        this.depositMoney = depositMoney;
        this.createTime = new Date();
        this.updateTime = this.createTime;
        this.recordStatus = RecordStatus.APPLY_ING.getStatus();
        this.serviceType = ServiceType.IS_PUT.getType();
        this.status = CommonStatus.VALID.getStatus();
        this.freePayType = FreePayTypeEnum.UN_FREE.getType();
    }

    public IceChestPutRecord(Integer chestId, String applicantQywechatId, Date applyTime, Integer sendClientId, Integer receiveClientId, BigDecimal depositMoney, Integer recordStatus) {
        this.chestId = chestId;
        this.applicantQywechatId = applicantQywechatId;
        this.applyTime = applyTime;
        this.sendClientId = sendClientId;
        this.receiveClientId = receiveClientId;
        this.depositMoney = depositMoney;
        this.createTime = new Date();
        this.updateTime = this.createTime;
        this.recordStatus = recordStatus;
        this.serviceType = ServiceType.IS_PUT.getType();
        this.status = CommonStatus.VALID.getStatus();
        this.freePayType = FreePayTypeEnum.UN_FREE.getType();
    }

    public IceChestPutRecord(Integer chestId, String applicantQywechatId, Date applyTime, Integer sendClientId, Integer receiveClientId, BigDecimal depositMoney, Integer recordStatus, Integer serviceType) {
        this.chestId = chestId;
        this.applicantQywechatId = applicantQywechatId;
        this.applyTime = applyTime;
        this.sendClientId = sendClientId;
        this.receiveClientId = receiveClientId;
        this.depositMoney = depositMoney;
        this.createTime = new Date();
        this.updateTime = this.createTime;
        this.recordStatus = recordStatus;
        this.serviceType = serviceType;
        this.status = CommonStatus.VALID.getStatus();
        this.freePayType = FreePayTypeEnum.UN_FREE.getType();
    }
}