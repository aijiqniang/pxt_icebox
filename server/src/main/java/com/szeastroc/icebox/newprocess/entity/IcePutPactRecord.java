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
@TableName(value = "t_ice_put_pact_record")
public class IcePutPactRecord {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 投放的门店编号
     */
    @TableField(value = "store_number")
    private String storeNumber;

    /**
     * 冰柜id
     */
    @TableField(value = "box_id")
    private Integer boxId;

    /**
     * 申请编号
     */
    @TableField(value = "apply_number")
    private String applyNumber;

    /**
     * 投放时间 用于记录协议生效时间
     */
    @TableField(value = "put_time")
    private Date putTime;

    /**
     * 协议到期时间
     */
    @TableField(value = "put_expire_time")
    private Date putExpireTime;

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
     * 更新时间
     */
    @TableField(value = "updated_time")
    private Date updatedTime;
}