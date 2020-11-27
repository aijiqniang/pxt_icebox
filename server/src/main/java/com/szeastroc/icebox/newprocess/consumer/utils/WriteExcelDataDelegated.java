package com.szeastroc.icebox.newprocess.consumer.utils;

import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

@FunctionalInterface
public interface WriteExcelDataDelegated {

    /**
     * 写表格数据
     * @param eachSheet 写入的sheet
     * @param startRowCount 开始行
     * @param endRowCount 结束行
     * @param currentPage 查询开始页
     * @param pageSize 查询数量
     * @throws Exception
     */
    void writeExcelData(SXSSFWorkbook wb, SXSSFSheet eachSheet, Integer startRowCount, Integer endRowCount, Integer currentPage, Integer pageSize) throws Exception;
}