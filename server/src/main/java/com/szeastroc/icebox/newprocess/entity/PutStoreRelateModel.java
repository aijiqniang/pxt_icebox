package com.szeastroc.icebox.newprocess.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.util.Date;

/**
 * 门店关联投放冰柜型号
 */
@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "t_put_store_relate_model")
public class PutStoreRelateModel {
    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 投放的门店编号
     */
    @TableField(value = "put_store_number")
    private String putStoreNumber;

    /**
     * 冰柜型号id
     */
    @TableField(value = "model_id")
    private Integer modelId;

    /**
     * 冰柜所属经销id
     */
    @TableField(value = "supplier_id")
    private Integer supplierId;

    /**
     * 冰柜投放状态
     */
    @TableField(value = "put_status")
    private Integer putStatus;

    /**
     * 审批流状态 0:未审核1:审核中2:通过3:驳回
     */
    @TableField(value = "examine_status")
    private Integer examineStatus;

    /**
     * 审批备注
     */
    @TableField(value = "examine_remark")
    private String examineRemark;

    /**
     * 业务员id
     */
    @TableField(value = "create_by")
    private Integer createBy;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private Date createTime;

    /**
     * 更新人id
     */
    @TableField(value = "update_by")
    private Integer updateBy;
    /**
     * 更新人名称
     */
    @TableField(value = "update_by_name")
    private String updateByName;
    /**
     * 更新时间
     */
    @TableField(value = "update_time")
    private Date updateTime;

    /**
     * 申请状态：0-无效；1-有效
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 作废原因
     */
    @TableField(value = "cancel_msg")
    private String cancelMsg;

    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;




    public static PutStoreRelateModel.PutStoreRelateModelBuilder builder() {
        return new PutStoreRelateModel.PutStoreRelateModelBuilder();
    }
}