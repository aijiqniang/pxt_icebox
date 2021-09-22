package com.szeastroc.icebox.newprocess.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * null
 * @TableName t_ice_box_relate_dms
 */
@TableName(value ="t_ice_box_relate_dms")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IceBoxRelateDms implements Serializable {
    /**
     *
     *
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 1投放 2退还
     */
    private Integer type;

    /**
     * 投放/退还编号
     */
    private String relateNumber;

    /**
     *
     *
     */
    private Integer putStoreRelateModelId;

    /**
     * 冰柜id
     */
    private Integer iceBoxId;

    /**
     * 0 旧冰柜 1冰柜
     */
    private Integer iceBoxType;

    /**
     * 冰柜assetid
     */
    private String iceBoxAssetId;

    /**
     * 当前定位
     */
    private String currentPlace;

    /**
     * 预计送达时间
     */
    private Date expectArrviedDate;

    /**
     * 储运备注
     */
    private String remark;

    /**
     * 店招照片
     */
    private String photo;

    /**
     * 审批备注
     */
    private String examineRemark;

    /**
     * 审批流id
     */
    private Integer examineId;

    /**
     * 投放门店信息
     */
    private String putStoreNumber;

    /**
     * 供应商id
     */
    private Integer supplierId;

    /**
     * 冰柜型号id
     */
    private Integer modelId;

    /**
     * 2投放中 6配送中  7待签收
     */
    private Integer putstatus;

    /**
     *
     *
     */
    private Date createTime;

    /**
     *
     *
     */
    private Date updateTime;

    /**
     * 免押类型：1-不免押，2-免押
     */
    private Integer freeType;
    /**
     * 押金
     */
    private BigDecimal depositMoney;

    /**
     * 接单时间
     */
    private Date acceptTime;

    /**
     * 送达时间
     */
    private Date arrviedTime;

    /**
     * 1退还中 2已退还 3已驳回  4已接单 5已收柜
     */
    private Integer backstatus;
    /**
     * 经度
     */
    private String longitude;

    /**
     * 纬度
     */
    private String latitude;

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
        IceBoxRelateDms other = (IceBoxRelateDms) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getType() == null ? other.getType() == null : this.getType().equals(other.getType()))
            && (this.getRelateNumber() == null ? other.getRelateNumber() == null : this.getRelateNumber().equals(other.getRelateNumber()))
            && (this.getPutStoreRelateModelId() == null ? other.getPutStoreRelateModelId() == null : this.getPutStoreRelateModelId().equals(other.getPutStoreRelateModelId()))
            && (this.getIceBoxId() == null ? other.getIceBoxId() == null : this.getIceBoxId().equals(other.getIceBoxId()))
            && (this.getIceBoxType() == null ? other.getIceBoxType() == null : this.getIceBoxType().equals(other.getIceBoxType()))
            && (this.getIceBoxAssetId() == null ? other.getIceBoxAssetId() == null : this.getIceBoxAssetId().equals(other.getIceBoxAssetId()))
            && (this.getCurrentPlace() == null ? other.getCurrentPlace() == null : this.getCurrentPlace().equals(other.getCurrentPlace()))
            && (this.getExpectArrviedDate() == null ? other.getExpectArrviedDate() == null : this.getExpectArrviedDate().equals(other.getExpectArrviedDate()))
            && (this.getRemark() == null ? other.getRemark() == null : this.getRemark().equals(other.getRemark()))
            && (this.getPhoto() == null ? other.getPhoto() == null : this.getPhoto().equals(other.getPhoto()))
            && (this.getExamineRemark() == null ? other.getExamineRemark() == null : this.getExamineRemark().equals(other.getExamineRemark()))
            && (this.getExamineId() == null ? other.getExamineId() == null : this.getExamineId().equals(other.getExamineId()))
            && (this.getPutStoreNumber() == null ? other.getPutStoreNumber() == null : this.getPutStoreNumber().equals(other.getPutStoreNumber()))
            && (this.getSupplierId() == null ? other.getSupplierId() == null : this.getSupplierId().equals(other.getSupplierId()))
            && (this.getModelId() == null ? other.getModelId() == null : this.getModelId().equals(other.getModelId()))
            && (this.getPutstatus() == null ? other.getPutstatus() == null : this.getPutstatus().equals(other.getPutstatus()))
            && (this.getCreateTime() == null ? other.getCreateTime() == null : this.getCreateTime().equals(other.getCreateTime()))
            && (this.getUpdateTime() == null ? other.getUpdateTime() == null : this.getUpdateTime().equals(other.getUpdateTime()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getType() == null) ? 0 : getType().hashCode());
        result = prime * result + ((getRelateNumber() == null) ? 0 : getRelateNumber().hashCode());
        result = prime * result + ((getPutStoreRelateModelId() == null) ? 0 : getPutStoreRelateModelId().hashCode());
        result = prime * result + ((getIceBoxId() == null) ? 0 : getIceBoxId().hashCode());
        result = prime * result + ((getIceBoxType() == null) ? 0 : getIceBoxType().hashCode());
        result = prime * result + ((getIceBoxAssetId() == null) ? 0 : getIceBoxAssetId().hashCode());
        result = prime * result + ((getCurrentPlace() == null) ? 0 : getCurrentPlace().hashCode());
        result = prime * result + ((getExpectArrviedDate() == null) ? 0 : getExpectArrviedDate().hashCode());
        result = prime * result + ((getRemark() == null) ? 0 : getRemark().hashCode());
        result = prime * result + ((getPhoto() == null) ? 0 : getPhoto().hashCode());
        result = prime * result + ((getExamineRemark() == null) ? 0 : getExamineRemark().hashCode());
        result = prime * result + ((getExamineId() == null) ? 0 : getExamineId().hashCode());
        result = prime * result + ((getPutStoreNumber() == null) ? 0 : getPutStoreNumber().hashCode());
        result = prime * result + ((getSupplierId() == null) ? 0 : getSupplierId().hashCode());
        result = prime * result + ((getModelId() == null) ? 0 : getModelId().hashCode());
        result = prime * result + ((getPutstatus() == null) ? 0 : getPutstatus().hashCode());
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
        sb.append(", type=").append(type);
        sb.append(", relateNumber=").append(relateNumber);
        sb.append(", putStoreRelateModelId=").append(putStoreRelateModelId);
        sb.append(", iceBoxId=").append(iceBoxId);
        sb.append(", iceBoxType=").append(iceBoxType);
        sb.append(", iceBoxAssetId=").append(iceBoxAssetId);
        sb.append(", currentPlace=").append(currentPlace);
        sb.append(", expectArrviedDate=").append(expectArrviedDate);
        sb.append(", remark=").append(remark);
        sb.append(", photo=").append(photo);
        sb.append(", examineRemark=").append(examineRemark);
        sb.append(", examineId=").append(examineId);
        sb.append(", putStoreNumber=").append(putStoreNumber);
        sb.append(", supplierId=").append(supplierId);
        sb.append(", modelId=").append(modelId);
        sb.append(", putstatus=").append(putstatus);
        sb.append(", createTime=").append(createTime);
        sb.append(", updateTime=").append(updateTime);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}