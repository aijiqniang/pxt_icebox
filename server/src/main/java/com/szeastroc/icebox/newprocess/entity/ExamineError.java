package com.szeastroc.icebox.newprocess.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;

import lombok.Data;

/**
 * 无法巡检记录
 * @TableName t_examine_error
 */
@TableName(value ="t_examine_error")
@Data
public class ExamineError implements Serializable {
    /**
     * 
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 冰柜id
     */
    @TableField(value = "box_id")
    private Integer boxId;

    /**
     * 资产id
     */
    @TableField(value = "box_assetId")
    private String boxAssetid;

    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;

    /**
     * 门店编号
     */
    @TableField(value = "store_number")
    private String storeNumber;

    /**
     * 
     */
    @TableField(value = "send_user_id_1")
    private Integer sendUserId1;

    /**
     * 
     */
    @TableField(value = "send_user_name_1")
    private String sendUserName1;

    /**
     * 
     */
    @TableField(value = "send_user_id_2")
    private Integer sendUserId2;

    /**
     * 
     */
    @TableField(value = "send_user_name_2")
    private String sendUserName2;

    /**
     * 0审核中  1通过 2驳回
     */
    @TableField(value = "pass_status")
    private Integer passStatus;

    /**
     * 
     */
    @TableField(value = "create_user_id")
    private Integer createUserId;

    /**
     * 
     */
    @TableField(value = "crete_user_name")
    private String creteUserName;

    /**
     * 
     */
    @TableField(value = "create_time")
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(value = "updatetime")
    private Date updatetime;

    /**
     * 创建人部门id
     */
    @TableField(value = "dept_id")
    private Integer deptId;

    /**
     * 创建人职位id
     */
    @TableField(value = "office_id")
    private Integer officeId;

    @TableField(exist = false)
    private Integer updateUserId;

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
        ExamineError other = (ExamineError) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getBoxId() == null ? other.getBoxId() == null : this.getBoxId().equals(other.getBoxId()))
            && (this.getBoxAssetid() == null ? other.getBoxAssetid() == null : this.getBoxAssetid().equals(other.getBoxAssetid()))
            && (this.getRemark() == null ? other.getRemark() == null : this.getRemark().equals(other.getRemark()))
            && (this.getStoreNumber() == null ? other.getStoreNumber() == null : this.getStoreNumber().equals(other.getStoreNumber()))
            && (this.getSendUserId1() == null ? other.getSendUserId1() == null : this.getSendUserId1().equals(other.getSendUserId1()))
            && (this.getSendUserName1() == null ? other.getSendUserName1() == null : this.getSendUserName1().equals(other.getSendUserName1()))
            && (this.getSendUserId2() == null ? other.getSendUserId2() == null : this.getSendUserId2().equals(other.getSendUserId2()))
            && (this.getSendUserName2() == null ? other.getSendUserName2() == null : this.getSendUserName2().equals(other.getSendUserName2()))
            && (this.getPassStatus() == null ? other.getPassStatus() == null : this.getPassStatus().equals(other.getPassStatus()))
            && (this.getCreateUserId() == null ? other.getCreateUserId() == null : this.getCreateUserId().equals(other.getCreateUserId()))
            && (this.getCreteUserName() == null ? other.getCreteUserName() == null : this.getCreteUserName().equals(other.getCreteUserName()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()))
            && (this.getUpdatetime() == null ? other.getUpdatetime() == null : this.getUpdatetime().equals(other.getUpdatetime()))
            && (this.getDeptId() == null ? other.getDeptId() == null : this.getDeptId().equals(other.getDeptId()))
            && (this.getOfficeId() == null ? other.getOfficeId() == null : this.getOfficeId().equals(other.getOfficeId()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getBoxId() == null) ? 0 : getBoxId().hashCode());
        result = prime * result + ((getBoxAssetid() == null) ? 0 : getBoxAssetid().hashCode());
        result = prime * result + ((getRemark() == null) ? 0 : getRemark().hashCode());
        result = prime * result + ((getStoreNumber() == null) ? 0 : getStoreNumber().hashCode());
        result = prime * result + ((getSendUserId1() == null) ? 0 : getSendUserId1().hashCode());
        result = prime * result + ((getSendUserName1() == null) ? 0 : getSendUserName1().hashCode());
        result = prime * result + ((getSendUserId2() == null) ? 0 : getSendUserId2().hashCode());
        result = prime * result + ((getSendUserName2() == null) ? 0 : getSendUserName2().hashCode());
        result = prime * result + ((getPassStatus() == null) ? 0 : getPassStatus().hashCode());
        result = prime * result + ((getCreateUserId() == null) ? 0 : getCreateUserId().hashCode());
        result = prime * result + ((getCreteUserName() == null) ? 0 : getCreteUserName().hashCode());
        result = prime * result + ((getCreateTime() == null) ? 0 : getCreateTime().hashCode());
        result = prime * result + ((getUpdatetime() == null) ? 0 : getUpdatetime().hashCode());
        result = prime * result + ((getDeptId() == null) ? 0 : getDeptId().hashCode());
        result = prime * result + ((getOfficeId() == null) ? 0 : getOfficeId().hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", boxId=").append(boxId);
        sb.append(", boxAssetid=").append(boxAssetid);
        sb.append(", remark=").append(remark);
        sb.append(", storeNumber=").append(storeNumber);
        sb.append(", sendUserId1=").append(sendUserId1);
        sb.append(", sendUserName1=").append(sendUserName1);
        sb.append(", sendUserId2=").append(sendUserId2);
        sb.append(", sendUserName2=").append(sendUserName2);
        sb.append(", passStatus=").append(passStatus);
        sb.append(", createUserId=").append(createUserId);
        sb.append(", creteUserName=").append(creteUserName);
        sb.append(", createTime=").append(createTime);
        sb.append(", upDate=").append(updatetime);
        sb.append(", deptId=").append(deptId);
        sb.append(", officeId=").append(officeId);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}