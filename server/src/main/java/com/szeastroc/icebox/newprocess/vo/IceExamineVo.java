package com.szeastroc.icebox.newprocess.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;


@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class IceExamineVo {

    private Integer id;

    /**
     * 冰柜的id
     */
    private Integer iceBoxId;

    /**
     * 门店编号
     */
    private String storeNumber;


    /**
     * 门店名称
     */
    private String storeName;


    /**
     * 门店营销区域
     */
    private Integer marketAreaId;


    /**
     * 外观照片的URL
     */
    private String exteriorImage;


    /**
     * 陈列照片的URL
     */
    private String displayImage;

    /**
     * 创建人
     */

    private Integer createBy;


    /**
     * 创建人名称
     */
    private String  createName;

    /**
     * 创建时间
     */
    private Date createTime;


    /**
     * 经度
     */
    private String longitude;
    /**
     * 纬度
     */
    private String latitude;


    /**
     * 温度
     */
    private Double temperature;

    /**
     * 开关门次数
     */
    private Integer openCloseCount;

    /**
     * GPS定位地址
     */
    private String gpsAddress;

    /**
     * 冰柜状态
     */
    private Integer iceStatus;

    /**
     * 冰柜巡检的状态
     */
    private Integer iceExamineStatus;

    /**
     * 巡检备注
     */
    private String examinMsg;


}
