package com.szeastroc.icebox.newprocess.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "t_ice_box_extend")
public class IceBoxExtend {
    /**
     * 冰柜表id
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Integer id;

    /**
     * 外部厂商id  (冰箱控制器ID)
     */
    @TableField(value = "external_id")
    private String externalId;

    /**
     * 东鹏资产id
     */
    @TableField(value = "asset_id")
    private String assetId;

    /**
     * 蓝牙设备ID
     */
    @TableField(value = "bluetooth_id")
    private String bluetoothId;

    /**
     * 蓝牙设备地址
     */
    @TableField(value = "bluetooth_mac")
    private String bluetoothMac;

    /**
     * 冰箱二维码
     */
    @TableField(value = "qr_code")
    private String qrCode;

    /**
     * gps模块MAC
     */
    @TableField(value = "gps_mac")
    private String gpsMac;

    /**
     * 对应最近巡检记录id
     */
    @TableField(value = "last_examine_id")
    private Integer lastExamineId;

    /**
     * 最近巡检日期
     */
    @TableField(value = "last_examine_time")
    private Date lastExamineTime;

    /**
     * 对应最近投放记录id
     */
    @TableField(value = "last_put_id")
    private Integer lastPutId;

    /**
     * 最近一次申请编号
     */
    @TableField(value = "last_apply_number")
    private String lastApplyNumber;

    /**
     * 最近投放日期
     */
    @TableField(value = "last_put_time")
    private Date lastPutTime;

    /**
     * 出厂日期
     */
    @TableField(value = "release_time")
    private Date releaseTime;

    /**
     * 保修起算日期
     */
    @TableField(value = "repair_begin_time")
    private Date repairBeginTime;

    /**
     * 开门次数
     */
    @TableField(value = "open_total")
    private Integer openTotal;

    public static IceBoxExtendBuilder builder() {
        return new IceBoxExtendBuilder();
    }
}