package com.szeastroc.icebox.newprocess.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
@TableName(value = "t_ice_put_apply")
public class IcePutApply {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 申请编号 关联字段,随机生成
     */
    @TableField(value = "apply_number")
    private String applyNumber;

    /**
     * 业务员id
     */
    @TableField(value = "user_id")
    private Integer userId;

    /**
     * 投放的门店Number
     */
    @TableField(value = "put_store_number")
    private String putStoreNumber;

    /**
     * 门店签收状态 0:未签收1:已签收2:拒绝
     */
    @TableField(value = "store_sign_status")
    private Integer storeSignStatus;

    /**
     * 审批流状态 0:未审核1:审核中2:通过3:驳回
     */
    @TableField(value = "examine_status")
    private Integer examineStatus;

    /**
     * 审批流ID
     */
    @TableField(value = "examine_id")
    private Integer examineId;

    /**
     * 旧投放表ID
     */
    @TableField(value = "old_put_id")
    private Integer oldPutId;

    /**
     * 创建人
     */
    @TableField(value = "created_by")
    private Integer createdBy;

    /**
     * 创建时间
     */
    @TableField(value = "created_time")
    private Date createdTime;

    /**
     * 更新人
     */
    @TableField(value = "updated_by")
    private Integer updatedBy;

    /**
     * 修改时间
     */
    @TableField(value = "update_time")
    private Date updateTime;

    public static IcePutApplyBuilder builder() {
        return new IcePutApplyBuilder();
    }
}