package com.szeastroc.icebox.newprocess.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * 业务员申请关联冰柜表
 * </p>
 *
 * @author 陈超
 * @since 2021-06-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("t_display_shelf_put_apply_relate")
@ApiModel(value = "ShelfPutApplyRelate对象", description = "业务员申请关联陈列架表 ")
public class DisplayShelfPutApplyRelate implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 申请编号
     */
    @ApiModelProperty(value = "申请编号")
    @TableField("apply_number")
    private String applyNumber;

    /**
     * 货架id
     */
    @ApiModelProperty(value = "货架id")
    @TableField("shelf_id")
    private Integer shelfId;

    @ApiModelProperty(value = "插入数据的时间")
    @TableField("update_time")
    private Date updateTime;

}
