package com.szeastroc.icebox.newprocess.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.util.Date;

/**
 * 申请编号关联 门店与冰柜型号关联信息
 */
@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "t_apply_relate_put_store_model")
public class ApplyRelatePutStoreModel {
    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 申请编号
     */
    @TableField(value = "apply_number")
    private String applyNumber;

    /**
     * 冰柜型号id
     */
    @TableField(value = "store_relate_model_id")
    private Integer storeRelateModelId;

    public static ApplyRelatePutStoreModel.ApplyRelatePutStoreModelBuilder builder() {
        return new ApplyRelatePutStoreModel.ApplyRelatePutStoreModelBuilder();
    }
}