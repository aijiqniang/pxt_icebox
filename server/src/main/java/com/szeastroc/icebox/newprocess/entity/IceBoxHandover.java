package com.szeastroc.icebox.newprocess.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

import lombok.Data;

/**
 * aijinqiang
 * @TableName t_ice_box_handover
 */
@TableName(value ="t_ice_box_handover")
@Data
public class IceBoxHandover implements Serializable {
    /**
     * 
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 冰柜id
     */
    private Integer iceBoxId;

    /**
     * 资产编号
     */
    private String iceBoxAssetid;

    /**
     * 
     */
    private Integer iceboxStatus;
    /**
     *本部id
     */
    private Integer headquartersDeptId;
    /**
     *本部名称
     */
    private String headquartersDeptName;
    /**
     *事业部id
     */
    private Integer businessDeptId;
    /**
     *事业部名称
     */
    private String businessDeptName;
    /**
     *大区id
     */
    private Integer regionDeptId;
    /**
     *大区名称
     */
    private String regionDeptName;
    /**
     *服务处id
     */
    private Integer serviceDeptId;
    /**
     *服务处名称
     */
    private String serviceDeptName;
    /**
     *组id
     */
    private Integer groupDeptId;
    /**
     *组名称
     */
    private String groupDeptName;

    /**
     * 交接业务员id
     */
    private Integer sendUserId;

    /**
     * 交接业务员name
     */
    private String sendUserName;

    /**
     * 交接人职务
     */
    private String sendUserOfficeName;

    /**
     * 接受业务员id
     */
    private Integer receiveUserId;

    /**
     * 接收业务员name
     */
    private String receiveUserName;

    /**
     * 接收人职务
     */
    private String receiveUserOfficeName;


    /**
     * 门店信息
     */
    private String storeNumber;

    /**
     * 供应商id
     */
    private Integer supplierId;

    /**
     * 供应商名称
     */
    private String supplierName;

    /**
     * 型号id
     */
    private Integer modelId;

    /**
     * 型号名称
     */
    private String modelName;

    /**
     *  1不免押 2 免押
     */
    private Integer freetype;

    /**
     * 押金 
     */
    private BigDecimal depositMoney;

    /**
     * 1有效 2无效
     */
    private Integer status;

    /**
     * 1交接中 2已交接 3已驳回
     */
    private Integer handoverStatus;

    /**
     * 
     */
    private Date createTime;

    /**
     *交接时间
     */
    private Date handoverTime;

    /**
     *
     */
    private Date updateTime;

    private String relateCode;

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
        IceBoxHandover other = (IceBoxHandover) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getIceBoxId() == null ? other.getIceBoxId() == null : this.getIceBoxId().equals(other.getIceBoxId()))
            && (this.getIceBoxAssetid() == null ? other.getIceBoxAssetid() == null : this.getIceBoxAssetid().equals(other.getIceBoxAssetid()))
            && (this.getIceboxStatus() == null ? other.getIceboxStatus() == null : this.getIceboxStatus().equals(other.getIceboxStatus()))
            && (this.getSendUserId() == null ? other.getSendUserId() == null : this.getSendUserId().equals(other.getSendUserId()))
            && (this.getReceiveUserId() == null ? other.getReceiveUserId() == null : this.getReceiveUserId().equals(other.getReceiveUserId()))
            && (this.getStoreNumber() == null ? other.getStoreNumber() == null : this.getStoreNumber().equals(other.getStoreNumber()))
            && (this.getSupplierId() == null ? other.getSupplierId() == null : this.getSupplierId().equals(other.getSupplierId()))
            && (this.getSupplierName() == null ? other.getSupplierName() == null : this.getSupplierName().equals(other.getSupplierName()))
            && (this.getModelId() == null ? other.getModelId() == null : this.getModelId().equals(other.getModelId()))
            && (this.getModelName() == null ? other.getModelName() == null : this.getModelName().equals(other.getModelName()))
            && (this.getFreetype() == null ? other.getFreetype() == null : this.getFreetype().equals(other.getFreetype()))
            && (this.getDepositMoney() == null ? other.getDepositMoney() == null : this.getDepositMoney().equals(other.getDepositMoney()))
            && (this.getHandoverStatus() == null ? other.getHandoverStatus() == null : this.getHandoverStatus().equals(other.getHandoverStatus()))
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
        result = prime * result + ((getIceboxStatus() == null) ? 0 : getIceboxStatus().hashCode());
        result = prime * result + ((getSendUserId() == null) ? 0 : getSendUserId().hashCode());
        result = prime * result + ((getReceiveUserId() == null) ? 0 : getReceiveUserId().hashCode());
        result = prime * result + ((getStoreNumber() == null) ? 0 : getStoreNumber().hashCode());
        result = prime * result + ((getSupplierId() == null) ? 0 : getSupplierId().hashCode());
        result = prime * result + ((getSupplierName() == null) ? 0 : getSupplierName().hashCode());
        result = prime * result + ((getModelId() == null) ? 0 : getModelId().hashCode());
        result = prime * result + ((getModelName() == null) ? 0 : getModelName().hashCode());
        result = prime * result + ((getFreetype() == null) ? 0 : getFreetype().hashCode());
        result = prime * result + ((getDepositMoney() == null) ? 0 : getDepositMoney().hashCode());
        result = prime * result + ((getHandoverStatus() == null) ? 0 : getHandoverStatus().hashCode());
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
        sb.append(", iceboxStatus=").append(iceboxStatus);
        sb.append(", sendUserId=").append(sendUserId);
        sb.append(", receiveUserId=").append(receiveUserId);
        sb.append(", storeNumber=").append(storeNumber);
        sb.append(", supplierId=").append(supplierId);
        sb.append(", supplierName=").append(supplierName);
        sb.append(", modelId=").append(modelId);
        sb.append(", modelName=").append(modelName);
        sb.append(", freetype=").append(freetype);
        sb.append(", depositMoney=").append(depositMoney);
        sb.append(", handoverStatus=").append(handoverStatus);
        sb.append(", createTime=").append(createTime);
        sb.append(", updateTime=").append(updateTime);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}