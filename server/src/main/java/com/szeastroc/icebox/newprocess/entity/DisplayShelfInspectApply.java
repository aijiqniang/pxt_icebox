package com.szeastroc.icebox.newprocess.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 陈列架巡检(DisplayShelfInspectApply)表实体类
 *
 * @author chenchao
 * @since 2021-06-07 14:41:14
 */
@SuppressWarnings("serial")
@Data
@Accessors(chain = true)
@TableName("t_display_shelf_inspect_apply")
public class DisplayShelfInspectApply extends Model<DisplayShelfInspectApply> {

    @TableId(type = IdType.AUTO)
    private Integer id;


    /**
     * 申请编号 关联字段,随机生成
     */

    private String applyNumber;

    /**
     * 投放编号
     */
    private String putNumber;


    /**
     * 业务员id
     */

    private Integer userId;


    /**
     * 投放的客户编号
     */

    private String customerNumber;


    private Integer customerType;


    /**
     * 审批流状态 0:未审核1:审核中2:通过3:驳回
     */

    private Integer examineStatus;


    /**
     * 审批流ID
     */

    private Integer examineId;


    /**
     * 备注
     */

    private String remark;

    private String imageUrl;


    /**
     * 部门id
     */

    private Integer deptId;


    /**
     * 创建人
     */

    private Integer createdBy;


    /**
     * 创建时间
     */

    private Date createdTime;


    /**
     * 更新人
     */

    private Integer updatedBy;


    /**
     * 修改时间
     */

    private Date updateTime;

    @TableField(exist = false)
    private String createName;
    @TableField(exist = false)
    private String customerName;


    /**
     * 获取主键值
     *
     * @return 主键值
     */
    @Override
    protected Serializable pkVal() {
        return this.id;
    }
}
