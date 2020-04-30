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
@TableName(value = "t_ice_back_apply_relate_box")
public class IceBackApplyRelateBox {
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
     * 退还类型 1:退押金2:不退押金
     */
    @TableField(value = "back_type")
    private Integer backType;

    /**
     * 免押类型 1:不免押2:免押
     */
    @TableField(value = "free_type")
    private Integer freeType;

    /**
     * 退还的经销商
     */
    @TableField(value = "back_supplier_id")
    private Integer backSupplierId;




    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private Integer createTime;


    /**
     * 更新时间
     */
    @TableField(value = "update_time")
    private Integer updateTime;


}