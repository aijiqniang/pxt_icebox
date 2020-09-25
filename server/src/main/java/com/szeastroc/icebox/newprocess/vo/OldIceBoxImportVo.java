package com.szeastroc.icebox.newprocess.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OldIceBoxImportVo {

    @ExcelProperty("事业部")
    private String division;
    @ExcelProperty("大区")
    private String region;
    @ExcelProperty("服务处")
    private String service;
    @ExcelProperty("所属经销商编号")
    private String supplierNumber;
    @ExcelProperty("所属经销商名称")
    private String supplierName;
    @ExcelProperty("冰柜编号")
    private String assetId;
    @ExcelProperty("冰柜名称")
    private String chestName;
    @ExcelProperty("品牌")
    private String brandName;
    @ExcelProperty("冰柜型号")
    private String modelName;
    @ExcelProperty("冰柜规格")
    private String chestNorm;
    @ExcelProperty("押金金额")
    private BigDecimal depositMoney;
    @ExcelProperty("现投放门店编号")
    private String storeNumber;
    @ExcelProperty("现投放门名称")
    private String storeName;
    @ExcelProperty("冰柜状态")
    private String status;
    @ExcelProperty("导入类型")
    private String type;


}
