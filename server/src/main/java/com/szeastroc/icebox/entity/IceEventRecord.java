package com.szeastroc.icebox.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @author yuqi9
 * @since 2019/5/24
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "t_ice_event_record")
public class IceEventRecord {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer type;

    private String assetId;

    private Date occurrenceTime;

    private Double temperature;

    private Integer openCloseCount;

    private String lng;

    private String lat;

    private String detailAddress;

    private Date createTime;
}
