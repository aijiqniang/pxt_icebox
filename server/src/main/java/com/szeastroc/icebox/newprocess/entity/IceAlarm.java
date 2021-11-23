package com.szeastroc.icebox.newprocess.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.bouncycastle.asn1.x509.V2AttributeCertificateInfoGenerator;

/**
 * 
 * @TableName t_ice_alarm
 */
@TableName(value ="t_ice_alarm")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class IceAlarm implements Serializable {
    /**
     * 
     */
    @TableId(value = "id",type = IdType.AUTO)
    private Integer id;

    /**
     * 冰柜id
     */
    @TableField(value = "ice_box_id")
    private Integer iceBoxId;

    /**
     * 冰柜资产id
     */
    @TableField(value = "ice_box_assetid")
    private String iceBoxAssetid;

    /**
     * 报警类型 1离门店超200m  2冰柜离线
     */
    @TableField(value = "alarm_type")
    private Integer alarmType;

    /**
     * 通知关联code
     */
    @TableField(value = "relate_code")
    private String relateCode;

    /**
     * 通知业务员
     */
    @TableField(value = "send_user_id")
    private Integer sendUserId;

    /**
     * 1新增报警  2已反馈  3自动消除
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 反馈
     */
    @TableField(value = "feedback")
    private String feedback;

    /**
     * 反馈附件
     */
    @TableField(value = "feedback_img")
    private String feedbackImg;

    /**
     * 
     */
    @TableField(value = "create_time")
    private Date createTime;

    /**
     * 
     */
    @TableField(value = "update_time")
    private Date updateTime;

    @TableField(value = "put_store_number")
    private String putStoreNumber;

    @TableField(value = "put_store_name")
    private String putStoreName;

    @TableField(value = "alarm_remove_time")
    private Date alarmRemoveTime;

    @TableField(value = "remark")
    private String remark;

    @TableField(value = "repair_time")
    private Date repairTime;

    @TableField(value = "outline_count")
    private Integer outlineCount;

    @TableField(value = "outline_limit")
    private Integer outlineLimit;

    @TableField(value = "overTep_wd")
    private Integer overTepWd;

    @TableField(value = "overTep_count")
    private Integer overTepCount;

    @TableField(value = "overTep_limit")
    private Integer overTepLimit;

    @TableField(value = "person_count")
    private Integer personCount;

    @TableField(value = "person_time")
    private Integer personTime;

    @TableField(value = "person_limit")
    private Integer personLimit;

    @TableField(value = "openCount_id")
    private Integer openCountId;

    @TableField(value = "apply_type")
    private Integer applyType;


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PageRequest extends Page {
        Integer boxId;
    }


    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        IceAlarm other = (IceAlarm) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getIceBoxId() == null ? other.getIceBoxId() == null : this.getIceBoxId().equals(other.getIceBoxId()))
            && (this.getIceBoxAssetid() == null ? other.getIceBoxAssetid() == null : this.getIceBoxAssetid().equals(other.getIceBoxAssetid()))
            && (this.getAlarmType() == null ? other.getAlarmType() == null : this.getAlarmType().equals(other.getAlarmType()))
            && (this.getRelateCode() == null ? other.getRelateCode() == null : this.getRelateCode().equals(other.getRelateCode()))
            && (this.getSendUserId() == null ? other.getSendUserId() == null : this.getSendUserId().equals(other.getSendUserId()))
            && (this.getStatus() == null ? other.getStatus() == null : this.getStatus().equals(other.getStatus()))
            && (this.getFeedback() == null ? other.getFeedback() == null : this.getFeedback().equals(other.getFeedback()))
            && (this.getFeedbackImg() == null ? other.getFeedbackImg() == null : this.getFeedbackImg().equals(other.getFeedbackImg()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()))
            && (this.getUpdateTime() == null ? other.getUpdateTime() == null : this.getUpdateTime().equals(other.getUpdateTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getIceBoxId() == null) ? 0 : getIceBoxId().hashCode());
        result = prime * result + ((getIceBoxAssetid() == null) ? 0 : getIceBoxAssetid().hashCode());
        result = prime * result + ((getAlarmType() == null) ? 0 : getAlarmType().hashCode());
        result = prime * result + ((getRelateCode() == null) ? 0 : getRelateCode().hashCode());
        result = prime * result + ((getSendUserId() == null) ? 0 : getSendUserId().hashCode());
        result = prime * result + ((getStatus() == null) ? 0 : getStatus().hashCode());
        result = prime * result + ((getFeedback() == null) ? 0 : getFeedback().hashCode());
        result = prime * result + ((getFeedbackImg() == null) ? 0 : getFeedbackImg().hashCode());
        result = prime * result + ((getCreateTime() == null) ? 0 : getCreateTime().hashCode());
        result = prime * result + ((getUpdateTime() == null) ? 0 : getUpdateTime().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", iceBoxId=").append(iceBoxId);
        sb.append(", iceBoxAssetid=").append(iceBoxAssetid);
        sb.append(", alarmType=").append(alarmType);
        sb.append(", relateCode=").append(relateCode);
        sb.append(", sendUserId=").append(sendUserId);
        sb.append(", status=").append(status);
        sb.append(", feedback=").append(feedback);
        sb.append(", feedbackImg=").append(feedbackImg);
        sb.append(", createTime=").append(createTime);
        sb.append(", updateTime=").append(updateTime);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}