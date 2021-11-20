package com.szeastroc.icebox.newprocess.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * (IceAlarmOpencount)表实体类
 *
 * @author aijinqiang
 * @since 2021-11-16 15:19:43
 */
@SuppressWarnings("serial")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName(value = "t_ice_alarm_opencount")
public class IceAlarmOpencount{

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    //冰柜id
    private String boxId;
    //资产id
    private String boxAssetid;
    //门店编号
    private String putStoreNumber;
    //报警规则id
    private Integer iceAlarmRuleDetailId;
    //规则开关门次数
    private Integer limitCount;
    //持续天数
    private Integer keepTime;
    //今日开关门次数
    private Integer todayCount;
    //1 待结算 2已失效 3已报警
    private Integer status;

    private Date createTime;

    private Date updateTime;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getBoxId() {
        return boxId;
    }

    public void setBoxId(String boxId) {
        this.boxId = boxId;
    }

    public String getBoxAssetid() {
        return boxAssetid;
    }

    public void setBoxAssetid(String boxAssetid) {
        this.boxAssetid = boxAssetid;
    }

    public String getPutStoreNumber() {
        return putStoreNumber;
    }

    public void setPutStoreNumber(String putStoreNumber) {
        this.putStoreNumber = putStoreNumber;
    }

    public Integer getIceAlarmRuleDetailId() {
        return iceAlarmRuleDetailId;
    }

    public void setIceAlarmRuleDetailId(Integer iceAlarmRuleDetailId) {
        this.iceAlarmRuleDetailId = iceAlarmRuleDetailId;
    }

    public Integer getLimitCount() {
        return limitCount;
    }

    public void setLimitCount(Integer limitCount) {
        this.limitCount = limitCount;
    }

    public Integer getKeepTime() {
        return keepTime;
    }

    public void setKeepTime(Integer keepTime) {
        this.keepTime = keepTime;
    }

    public Integer getTodayCount() {
        return todayCount;
    }

    public void setTodayCount(Integer todayCount) {
        this.todayCount = todayCount;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }


}
