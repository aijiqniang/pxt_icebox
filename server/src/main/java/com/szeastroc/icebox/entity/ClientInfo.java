package com.szeastroc.icebox.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.szeastroc.icebox.enums.FreePayTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Created by Tulane
 * 2019/5/21
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "t_client_info")
public class ClientInfo {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String clientName;
    private Integer clientType;
    private String clientNumber;
    private String clientPlace;
    private String clientLevel;
    private Integer clientStatus;
    private String contactName;
    private String contactMobile;
    private Integer marketAreaId;
    private Date createTime;
    private Date updateTime;

    public ClientInfo(String clientName, Integer clientType, String clientNumber, String clientPlace, String clientLevel, Integer clientStatus, String contactName, String contactMobile, Integer marketAreaId) {
        this.clientName = clientName;
        this.clientType = clientType;
        this.clientNumber = clientNumber;
        this.clientPlace = clientPlace;
        this.clientLevel = clientLevel;
        this.clientStatus = clientStatus;
        this.contactName = contactName;
        this.contactMobile = contactMobile;
        this.marketAreaId = marketAreaId;
        this.createTime = new Date();
        this.updateTime = this.createTime;
    }
}
