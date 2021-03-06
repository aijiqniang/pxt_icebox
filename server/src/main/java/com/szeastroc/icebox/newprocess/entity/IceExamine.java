package com.szeastroc.icebox.newprocess.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.szeastroc.icebox.newprocess.enums.IceBoxEnums;
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
     * 冰柜巡检编号
     */
    @TableField(value = "examine_number")
    private String examineNumber;

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
     * 资产编号图片的URL
     */
    @TableField(value = "asset_image")
    private String assetImage;

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


    /**
     * 经度
     */
    @TableField(value = "longitude")
    private String longitude;
    /**
     * 纬度
     */
    @TableField(value = "latitude")
    private String latitude;


    /**
     * 温度
     */
    @TableField(value = "temperature")
    private Double temperature;

    /**
     * 开关门次数
     */
    @TableField(value = "open_close_count")
    private Integer openCloseCount;

    /**
     * gps定位地址
     */
    @TableField(value = "gps_address")
    private String gpsAddress;

    /**
     * 冰柜状态
     */
    @TableField(value = "ice_status")
    private Integer iceStatus;

    /**
     * 巡检备注
     */
    @TableField(value = "examin_msg")
    private String examinMsg;

    /**
     * 审批状态：0-驳回，1-审核通过
     */
    @TableField(value = "examin_status")
    private Integer examinStatus;


    public boolean validate() {
        if(IceBoxEnums.StatusEnum.LOSE.getType().equals(iceStatus)){
            return iceBoxId != null && StringUtils.isNotBlank(storeNumber) && createBy != null;
        }else {
            return iceBoxId != null && StringUtils.isNotBlank(storeNumber) && StringUtils.isNotBlank(exteriorImage)
                    && StringUtils.isNotBlank(displayImage) && createBy != null;
        }
    }


    public IceExamineVo convert(IceExamine iceExamine, String realName, String storeName, String storeNumber) {
        return IceExamineVo.builder()
                .id(iceExamine.getId())
                .createBy(iceExamine.getCreateBy())
                .createName(realName)
                .displayImage(iceExamine.getDisplayImage())
                .exteriorImage(iceExamine.getExteriorImage())
                .assetImage(iceExamine.getAssetImage())
                .createTime(iceExamine.getCreateTime())
                .storeName(storeName)
                .storeNumber(storeNumber)
                .iceBoxId(iceExamine.getIceBoxId())
                .latitude(iceExamine.latitude)
                .longitude(iceExamine.longitude)
                .temperature(iceExamine.temperature)
                .openCloseCount(iceExamine.openCloseCount)
                .gpsAddress(iceExamine.gpsAddress)
                .build();
    }
}