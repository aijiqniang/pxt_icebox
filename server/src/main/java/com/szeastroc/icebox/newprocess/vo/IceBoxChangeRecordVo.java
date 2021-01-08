package com.szeastroc.icebox.newprocess.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * @Author xiao
 * @Date create in 2021/1/7 13:41
 * @Description:
 */
@Getter
@Setter
@Accessors(chain = true)
public class IceBoxChangeRecordVo {

    @ColumnWidth(50)
    @ExcelProperty(value = "操作人")
    private String createByName;
    @ColumnWidth(50)
    @ExcelProperty(value = "操作时间")
    private String createTimeStr;
    @ColumnWidth(100)
    @ExcelProperty({"变更后信息","营销区域"})
    private String newMarkAreaName;
    @ColumnWidth(50)
    @ExcelProperty({"变更后信息","资产编号"})
    private String newAssetId;
    @ColumnWidth(50)
    @ExcelProperty({"变更后信息","所属经销商"})
    private String newSupplierName;
    @ColumnWidth(50)
    @ExcelProperty({"变更后信息","所属经销商编号"})
    private String newSupplierNumber;
    @ColumnWidth(50)
    @ExcelProperty({"变更后信息","客户信息"})
    private String newStoreName;
    @ColumnWidth(50)
    @ExcelProperty({"变更后信息","品牌"})
    private String newBrandName;
    @ColumnWidth(50)
    @ExcelProperty({"变更后信息","押金"})
    private BigDecimal newChestDepositMoney;
    @ColumnWidth(50)
    @ExcelProperty({"变更后信息","价值"})
    private BigDecimal newChestMoney;
    @ColumnWidth(50)
    @ExcelProperty({"变更后信息","冰箱名称"})
    private String newChestName;
    @ColumnWidth(50)
    @ExcelProperty({"变更后信息","规格"})
    private String newChestNorm;
    @ColumnWidth(50)
    @ExcelProperty({"变更后信息","型号"})
    private String newModelName;
    @ColumnWidth(50)
    @ExcelProperty({"变更后信息","备注"})
    private String newRemake;
    @ColumnWidth(50)
    @ExcelProperty({"变更后信息","状态"})
    private String newStatusStr;


    @ColumnWidth(100)
    @ExcelProperty({"变更前信息","营销区域"})
    private String oldMarkAreaName;
    @ColumnWidth(50)
    @ExcelProperty({"变更前信息","资产编号"})
    private String oldAssetId;
    @ColumnWidth(50)
    @ExcelProperty({"变更前信息","所属经销商"})
    private String oldSupplierName;
    @ColumnWidth(50)
    @ExcelProperty({"变更前信息","所属经销商编号"})
    private String oldSupplierNumber;
    @ColumnWidth(50)
    @ExcelProperty({"变更前信息","客户信息"})
    private String oldStoreName;
    @ColumnWidth(50)
    @ExcelProperty({"变更前信息","品牌"})
    private String oldBrandName;
    @ColumnWidth(50)
    @ExcelProperty({"变更前信息","押金"})
    private BigDecimal oldChestDepositMoney;
    @ColumnWidth(50)
    @ExcelProperty({"变更前信息","价值"})
    private BigDecimal oldChestMoney;
    @ColumnWidth(50)
    @ExcelProperty({"变更前信息","冰箱名称"})
    private String oldChestName;
    @ColumnWidth(50)
    @ExcelProperty({"变更前信息","规格"})
    private String oldChestNorm;
    @ColumnWidth(50)
    @ExcelProperty({"变更前信息","型号"})
    private String oldModelName;
    @ColumnWidth(50)
    @ExcelProperty({"变更前信息","备注"})
    private String oldRemake;
    @ColumnWidth(50)
    @ExcelProperty({"变更前信息","状态"})
    private String oldStatusStr;

}
