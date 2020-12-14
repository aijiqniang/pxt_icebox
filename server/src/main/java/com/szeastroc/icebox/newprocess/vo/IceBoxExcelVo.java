package com.szeastroc.icebox.newprocess.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Getter;
import lombok.Setter;

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
    @ExcelProperty(value = "所属经销商编号")
    private String suppNumber; // 所属经销商编号
    @ColumnWidth(50)
    @ExcelProperty(value = "所属经销商名称")
    private String suppName; // 所属经销商名称
    @ColumnWidth(50)
    @ExcelProperty(value = "资产编号")
    private String oldAssetId; // 资产编号
    @ColumnWidth(50)
    @ExcelProperty(value = "资产编号(修改)")
    private String assetId; // 资产编号(修改)
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
    private String putStatusStr; // 冰柜状态
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

}
