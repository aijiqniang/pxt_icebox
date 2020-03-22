package com.szeastroc.icebox.newprocess.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.szeastroc.icebox.newprocess.vo.IceExamineVo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;

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
     * 外观照片的URL
     */
    @TableField(value = "exterior_image")
    private String exteriorImage;


    /**
     * 陈列照片的URL
     */
    @TableField(value = "display_image")
    private String displayImage;

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


    public boolean validate() {
        return iceBoxId != null && StringUtils.isNotBlank(storeNumber) && StringUtils.isNotBlank(exteriorImage)
                && StringUtils.isNotBlank(displayImage) && createBy != null;
    }


    public IceExamineVo convert(IceExamine iceExamine,String realName,String storeName,String storeNumber) {
        return IceExamineVo.builder()
                .id(iceExamine.getId())
                .createBy(iceExamine.getCreateBy())
                .createName(realName)
                .displayImage(iceExamine.getDisplayImage())
                .exteriorImage(iceExamine.getExteriorImage())
                .createTime(iceExamine.getCreateTime())
                .storeName(storeName)
                .storeNumber(storeNumber)
                .iceBoxId(iceExamine.getIceBoxId())
                .build();
    }
}