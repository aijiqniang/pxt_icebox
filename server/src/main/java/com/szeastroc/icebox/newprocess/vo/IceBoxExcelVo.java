package com.szeastroc.icebox.newprocess.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * @Author xiao
 * @Date create in 2020/4/3 11:16
 * @Description:
 */
@Getter
@Setter
public class IceBoxExcelVo {

    //    @ColumnWidth(50)
//    @ExcelProperty(value = "营销区域")
//    private String deptStr; // 营销区域
    @ColumnWidth(50)
    @ExcelProperty(value = "事业部")
    private String sybStr; // 事业部
    @ColumnWidth(50)
    @ExcelProperty(value = "大区")
    private String dqStr; // 大区
    @ColumnWidth(50)
    @ExcelProperty(value = "服务处")
    private String fwcStr; // 服务处
    @ColumnWidth(50)
    @ExcelProperty(value = "组")
    private String groupStr; // 组


    @ColumnWidth(50)
    @ExcelProperty(value = "所属经销商编号")
    private String suppNumber; // 所属经销商编号
    @ColumnWidth(50)
    @ExcelProperty(value = "所属经销商名称")
    private String suppName; // 所属经销商名称
    @ColumnWidth(50)
    @ExcelProperty(value = "资产编号(修改)")
    private String xiuGaiAssetId; // 资产编号(修改)
    @ColumnWidth(50)
    @ExcelProperty(value = "资产编号")
    private String assetId;
    @ColumnWidth(50)
    @ExcelProperty(value = "冰柜型号")
    private String chestModel; // 冰柜型号
    @ColumnWidth(50)
    @ExcelProperty(value = "押金收取金额")
    private String depositMoney; // 押金收取金额
    @ColumnWidth(50)
    @ExcelProperty(value = "现投放客户类型")
    private String storeTypeName; // 现投放客户类型
    @ColumnWidth(50)
    @ExcelProperty(value = "现投放客户级别")
    private String storeLevel; // 现投放客户级别
    @ColumnWidth(50)
    @ExcelProperty(value = "现投放客户编号")
    private String storeNumber; // 现投放客户编号
    @ColumnWidth(50)
    @ExcelProperty(value = "现投放客户名称")
    private String storeName; // 现投放客户名称
    @ColumnWidth(50)
    @ExcelProperty(value = "客户负责人手机号")
    private String mobile; // 客户负责人手机号
    @ColumnWidth(50)
    @ExcelProperty(value = "现投放客户地址")
    private String address; // 现投放客户地址
    @ColumnWidth(50)
    @ExcelProperty(value = "客户状态")
    private String statusStr; // 客户状态


    @ColumnWidth(50)
    @ExcelProperty(value = "冰柜状态")
    private String iceStatusStr; // 冰柜状态
    @ColumnWidth(50)
    @ExcelProperty(value = "冰柜投放状态")
    private String putStatusStr; // 冰柜投放状态
    @ColumnWidth(50)
    @ExcelProperty(value = "负责业务员姓名")
    private String realName; // 负责业务员姓名
    @ColumnWidth(50)
    @ExcelProperty(value = "投放日期")
    private String lastPutTimeStr; // 投放日期
    @ColumnWidth(50)
    @ExcelProperty(value = "最后一次巡检时间")
    private String lastExamineTimeStr; // 最后一次巡检时间
    @ColumnWidth(50)
    @ExcelProperty(value = "冰柜备注")
    private String remark; // 冰柜备注
    /**
     *冰柜生产日期
     */
    @ColumnWidth(50)
    @ExcelProperty(value = "生产日期")
    private String releaseTimeStr;
    /**
     *冰柜保修起算日期
     */
    @ColumnWidth(50)
    @ExcelProperty(value = "保修起算日期")
    private String repairBeginTimeStr;
    /**
     *冰柜年份
     */
    @ColumnWidth(50)
    @ExcelProperty(value = "冰柜年份")
    private String iceboxYear;

    /**
     * 冰柜温度
     */
    @ColumnWidth(50)
    @ExcelProperty(value = "温度")
    private String temperature;

    /**
     * 定位地址
     */
    @ColumnWidth(50)
    @ExcelProperty(value = "冰柜GPS定位位置")
    private String gpsAddress;

    /**
     * 定位地址
     */
    @ColumnWidth(50)
    @ExcelProperty(value = "最近采集时间")
    private Date occurrenceTime;
    /**
     * 开关门累计总数
     */
    @ColumnWidth(50)
    @ExcelProperty(value = "开关门累计总数")
    private Integer totalSum;
    /**
     * 开关门月累计
     */
    @ColumnWidth(50)
    @ExcelProperty(value = "开关门月累计")
    private Integer monthSum;
    /**
     * 开关门今日累计
     */
    @ColumnWidth(50)
    @ExcelProperty(value = "开关门今日累计")
    private Integer todaySum;
    /**
     * 直线距离
     */
    @ColumnWidth(50)
    @ExcelProperty(value = "直线距离(米)")
    private double distance;

    /**
     * 商户编号
     */
    @ColumnWidth(50)
    @ExcelProperty(value = "商户编号")
    private String merchantNumber;
}
