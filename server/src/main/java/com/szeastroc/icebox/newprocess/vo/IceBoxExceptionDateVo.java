package com.szeastroc.icebox.newprocess.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
@Data
public class IceBoxExceptionDateVo {
    //序号 所属经销商编号  冰柜编号 冰柜名称  品牌 冰柜型号  冰柜规格  押金金额 现投放门店编号 现投放门店名称 导入类型
    @ExcelProperty("所属经销商编号")
    private String number;
    @ExcelProperty("冰柜编号")
    private String assetId;
    @ExcelProperty("冰柜名称")
    private String chestName;
    @ExcelProperty("品牌")
    private String brandName;
    @ExcelProperty("冰柜型号")
    private String iceModel;
    @ExcelProperty("冰柜规格")
    private String chestNorm;
    @ExcelProperty("押金金额")
    private BigDecimal chestMoney;
    @ExcelProperty("现投放门店编号")
    private String storeNumber;
    @ExcelProperty("现投放门店名称")
    private String storeName;
    @ExcelProperty("导入类型")
    private String importType;
}
