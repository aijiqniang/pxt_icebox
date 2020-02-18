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
@TableName(value = "t_ice_apply_by_user")
public class IceApplyByUser {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 业务员id
     */
    @TableField(value = "user_id")
    private Integer userId;

    /**
     * 冰柜型号id
     */
    @TableField(value = "model_id")
    private Integer modelId;

    /**
     * 是否免押 0:不免押1:免押
     */
    @TableField(value = "is_free")
    private Integer isFree;

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

    public static IceApplyByUserBuilder builder() {
        return new IceApplyByUserBuilder();
    }
}