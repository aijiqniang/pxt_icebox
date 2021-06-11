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
import lombok.experimental.Accessors;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@TableName(value = "t_ice_box")
public class IceBox {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 冰柜名称
     */
    @TableField(value = "chest_name")
    private String chestName;

    /**
     * 东鹏资产id
     */
    @TableField(value = "asset_id")
    private String assetId;

    /**
     * 冰柜型号
     */
    @TableField(value = "model_id")
    private Integer modelId;

    /**
     * 冰柜型号
     */
    @TableField(value = "model_name")
    private String modelName;

    /**
     * 品牌
     */
    @TableField(value = "brand_name")
    private String brandName;

    /**
     * 冰柜规格
     */
    @TableField(value = "chest_norm")
    private String chestNorm;

    /**
     * 冰柜价值
     */
    @TableField(value = "chest_money")
    private BigDecimal chestMoney;

    /**
     * 押金
     */
    @TableField(value = "deposit_money")
    private BigDecimal depositMoney;

    /**
     * 拥有者的经销商
     */
    @TableField(value = "supplier_id")
    private Integer supplierId;

    /**
     * 投放的门店number
     */
    @TableField(value = "put_store_number")
    private String putStoreNumber;

    /**
     * 冰柜所属部门id
     */
    @TableField(value = "dept_id")
    private Integer deptId;

    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;

    /**
     * 投放状态 0: 未投放 1:已锁定(被业务员申请)  2:投放中 3:已投放
     */
    @TableField(value = "put_status")
    private Integer putStatus;

    /**
     * 冰柜状态 1:正常 0:异常
     */
    @TableField(value = "status")
    private Integer status;

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

    /**
     * 冰柜类型：0-旧冰柜，1-新冰柜
     */
    @TableField(value = "ice_box_type")
    private Integer iceBoxType;

    /**
     * 东鹏旧资产id
     */
    @TableField(value = "old_asset_id")
    private String oldAssetId;

    @TableField(value = "responseMan_id")
    private Integer responseManId;
    /**
     * 责任人
     */
    @TableField(value = "responseMan")
    private String responseMan;

    public static IceBoxBuilder builder() {
        return new IceBoxBuilder();
    }
}