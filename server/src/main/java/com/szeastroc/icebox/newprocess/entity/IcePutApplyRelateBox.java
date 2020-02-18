package com.szeastroc.icebox.newprocess.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
@TableName(value = "t_ice_put_apply_relate_box")
public class IcePutApplyRelateBox {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 申请编号
     */
    @TableField(value = "apply_number")
    private String applyNumber;

    /**
     * 冰柜ID (t_ice_box)
     */
    @TableField(value = "box_id")
    private Integer boxId;

    /**
     * 型号ID (t_ice_model)
     */
    @TableField(value = "model_id")
    private Integer modelId;

    /**
     * 免押类型 0:不免押1:免押
     */
    @TableField(value = "free_type")
    private Integer freeType;

    public static IcePutApplyRelateBoxBuilder builder() {
        return new IcePutApplyRelateBoxBuilder();
    }
}