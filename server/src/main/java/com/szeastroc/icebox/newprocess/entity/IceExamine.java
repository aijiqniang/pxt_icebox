package com.szeastroc.icebox.newprocess.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "t_ice_examine")
public class IceExamine {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 冰柜的id
     */
    @TableField(value = "ice_box_id")
    private Integer iceBoxId;

    /**
     * 门店编号
     */
    @TableField(value = "store_number")
    private String storeNumber;

    /**
     * 照片的URL
     */
    @TableField(value = "image")
    private String image;

    /**
     * 巡检照片类型 1-冰柜外观照片 2-冰柜陈列照片
     */
    @TableField(value = "type")
    private Integer type;

    /**
     * 创建人
     */
    @TableField(value = "create_by")
    private Integer createBy;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time")
    private Date updateTime;
}