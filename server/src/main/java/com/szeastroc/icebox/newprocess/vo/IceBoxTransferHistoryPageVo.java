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
public class IceBoxTransferHistoryPageVo {

    /**
     * 事业部
     */
    @ExcelProperty(value = "转出事业部", index = 0)
    private String oldBusinessDept;

    /**
     * 大区
     */
    @ExcelProperty(value = "转出大区", index = 1)
    private String oldRegionDept;

    /**
     * 服务处
     */
    @ExcelProperty(value = "转出服务处", index = 2)
    private String oldServiceDept;

    /**
     * 转移前经销商编号
     */
    @ExcelProperty(value = "转出经销商编号", index = 3)
    private String oldSupplierNumber;

    /**
     * 转移前经销商名称
     */
    @ExcelProperty(value = "转出经销商名称", index = 4)
    private String oldSupplierName;

    /**
     * 资产编号
     */
    @ExcelProperty(value = "冰柜资产编号", index = 5)
    private String assetId;

    /**
     * 冰柜型号
     */
    @ExcelProperty(value = "冰柜型号", index = 6)
    private String modelName;

    /**
     * 冰柜押金
     */
    @ExcelProperty(value = "冰柜押金", index = 7)
    private BigDecimal depositMoney;


    /**
     * 审批状态：0-未审核，1-审核中，2-通过，3-驳回
     */
    @ExcelProperty(value = "转移状态", index = 8)
    private String examineStatusStr;

    /**
     * 事业部
     */
    @ExcelProperty(value = "转入事业部", index = 9)
    private String newBusinessDept;

    /**
     * 大区
     */
    @ExcelProperty(value = "转入大区", index = 10)
    private String newRegionDept;

    /**
     * 服务处
     */
    @ExcelProperty(value = "转入服务处", index = 11)
    private String newServiceDept;

    /**
     * 转移后经销商编号
     */
    @ExcelProperty(value = "转入经销商编号", index = 12)
    private String newSupplierNumber;
    /**
     * 转移后经销商名称
     */
    @ExcelProperty(value = "转入经销商名称", index = 13)
    private String newSupplierName;

    /**
     * 申请人姓名
     */
    @ExcelProperty(value = "提交人", index = 14)
    private String createByName;
    /**
     * 申请日期
     */
    @ExcelProperty(value = "申请日期", index = 15)
    private String createTimeStr;


    /**
     * 审核人
     */
    @ExcelProperty(value = "审核人", index = 16)
    private String reviewer;

    /**
     * 审核人
     */
    @ExcelProperty(value = "审核人职务", index = 17)
    private String reviewerOfficeName;

    /**
     * 审核日期
     */
    @ExcelProperty(value = "审核日期", index = 18)
    private String reviewTimeStr;
}