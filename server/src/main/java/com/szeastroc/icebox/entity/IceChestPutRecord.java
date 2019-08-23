package com.szeastroc.icebox.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.szeastroc.common.enums.CommonStatus;
import com.szeastroc.icebox.enums.FreePayTypeEnum;
import com.szeastroc.icebox.enums.RecordStatus;
import com.szeastroc.icebox.enums.ServiceType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by Tulane
 * 2019/5/21
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "t_ice_chest_put_record")
public class IceChestPutRecord {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer chestId;
    private String applicantQywechatId;
    private Date applyTime;
    private Integer sendClientId;
    private Integer sendQywechatId;
    private Date sendTime;
    private Integer receiveClientId;
    private Integer receiveQywechatId;
    private Date receiveTime;
    private Integer recordStatus;
    private Integer serviceType;
    private BigDecimal depositMoney;
    private Integer freePayType;
    private Integer status;
    private Date createTime;
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
