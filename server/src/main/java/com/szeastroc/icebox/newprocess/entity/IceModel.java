package com.szeastroc.icebox.newprocess.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
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
@TableName(value = "t_ice_model")
public class IceModel {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 冰柜型号
     */
    @TableField(value = "chest_model")
    private String chestModel;

    /**
     * 冰柜名称
     */
    @TableField(value = "chest_name")
    private String chestName;

    /**
     * 冰柜押金
     */
    @TableField(value = "deposit_money")
    private BigDecimal depositMoney;

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

    public static IceModelBuilder builder() {
        return new IceModelBuilder();
    }
}