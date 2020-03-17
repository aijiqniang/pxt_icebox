package com.szeastroc.icebox.newprocess.vo;

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


}
