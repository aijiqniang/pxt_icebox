package com.szeastroc.icebox.newprocess.consumer.utils;

import com.szeastroc.common.constant.RegisterConstant;
import com.szeastroc.common.feign.visit.FeignExportRecordsClient;
import com.szeastroc.common.utils.ImageUploadUtil;
import com.szeastroc.icebox.newprocess.consumer.common.ExcelConstant;
import com.szeastroc.icebox.newprocess.dao.ExportRecordsDao;
import com.szeastroc.icebox.newprocess.enums.ExportRecordTypeEnum;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class PoiUtil {

    /**
     * 初始化EXCEL(sheet个数和标题)
     * @param totalRowCount 总记录数
     * @param titles        标题集合
     * @return XSSFWorkbook对象
     */
    public static SXSSFWorkbook initExcel(Integer totalRowCount, String[] titles) {
        // 在内存当中保持 100 行 , 超过的数据放到硬盘中在内存当中保持 100 行 , 超过的数据放到硬盘中
        SXSSFWorkbook wb = new SXSSFWorkbook(100);
        int sheetCount = ((totalRowCount % ExcelConstant.PER_SHEET_ROW_COUNT == 0) ?
                (totalRowCount / ExcelConstant.PER_SHEET_ROW_COUNT) : (totalRowCount / ExcelConstant.PER_SHEET_ROW_COUNT + 1));
        // 根据总记录数创建sheet并分配标题
        for (int i = 0; i < sheetCount; i++) {
            SXSSFSheet sheet = wb.createSheet("sheet" + (i + 1));
            SXSSFRow headRow = sheet.createRow(0);
            for (int j = 0; j < titles.length; j++) {
                SXSSFCell headRowCell = headRow.createCell(j);
                CellStyle cellStyle = wb.createCellStyle();
//                cellStyle.setFillBackgroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
                Font font = wb.createFont();
                font.setFontName("仿宋_GB2312");
                font.setBold(true);//粗体显示
                font.setFontHeightInPoints((short) 12);
                cellStyle.setFont(font);
                cellStyle.setAlignment(HorizontalAlignment.CENTER);
                cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
                headRowCell.setCellStyle(cellStyle);
                headRowCell.setCellValue(titles[j]);
            }
        }
        return wb;
    }

    /**
     * 初始化EXCEL(sheet个数和标题)
     * @param totalRowCount 总记录数
     * @param titles        标题集合
     * @return XSSFWorkbook对象
     */
    public static SXSSFWorkbook initExcel(Integer totalRowCount,Integer addSheet, String[] titles) {
        // 在内存当中保持 100 行 , 超过的数据放到硬盘中在内存当中保持 100 行 , 超过的数据放到硬盘中
        SXSSFWorkbook wb = new SXSSFWorkbook(100);
        int sheetCount = ((totalRowCount % ExcelConstant.PER_SHEET_ROW_COUNT == 0) ?
                (totalRowCount / ExcelConstant.PER_SHEET_ROW_COUNT) : (totalRowCount / ExcelConstant.PER_SHEET_ROW_COUNT + 1));
        // 根据总记录数创建sheet并分配标题
        for (int i = 0; i < sheetCount+addSheet; i++) {
            SXSSFSheet sheet = wb.createSheet("sheet" + (i + 1));
            SXSSFRow headRow = sheet.createRow(0);
            for (int j = 0; j < titles.length; j++) {
                SXSSFCell headRowCell = headRow.createCell(j);
                CellStyle cellStyle = wb.createCellStyle();
//                cellStyle.setFillBackgroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
                Font font = wb.createFont();
                font.setFontName("仿宋_GB2312");
                font.setBold(true);//粗体显示
                font.setFontHeightInPoints((short) 12);
                cellStyle.setFont(font);
                cellStyle.setAlignment(HorizontalAlignment.CENTER);
                cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
                headRowCell.setCellStyle(cellStyle);
                headRowCell.setCellValue(titles[j]);
            }
        }
        return wb;
    }

    /**
     * 下载EXCEL到本地指定的文件夹
     *
     * @param wb         EXCEL对象SXSSFWorkbook
     * @param file 文件
     */
    public static void downLoadExcelToLocalPath(SXSSFWorkbook wb, File file) {
        FileOutputStream fops = null;
        try {
            fops = new FileOutputStream(file);
            wb.write(fops);
        } catch (Exception e) {
            log.info(e.getMessage(),e);
        } finally {
            if (null != wb) {
                try {
                    wb.dispose();
                } catch (Exception e) {
                    log.info(e.getMessage(),e);
                }
            }
            if (null != fops) {
                try {
                    fops.close();
                } catch (Exception e) {
                    log.info(e.getMessage(),e);
                }
            }
        }
    }

    /**
     * 下载EXCEL到浏览器
     * @param wb       EXCEL对象XSSFWorkbook
     * @param response
     * @param fileName 文件名称
     */
    public static void downLoadExcelToWebsite(SXSSFWorkbook wb, HttpServletResponse response, String fileName) {
        response.setHeader("Content-disposition", "attachment; filename="
                + new String((fileName + ".xlsx").getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));//设置下载的文件名
        OutputStream outputStream = null;
        try {
            outputStream = response.getOutputStream();
            wb.write(outputStream);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != wb) {
                try {
                    wb.dispose();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (null != outputStream) {
                try {
                    outputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * 导出Excel到本地指定路径(报表)
     *
     * @param totalRowCount           总记录数
     * @param titles                  标题
     * @param exportPath              导出路径
     * @param writeExcelDataDelegated 向EXCEL写数据/处理格式的委托类 自行实现
     * @throws Exception
     */
    public static void exportReportExcelToLocalPath(Integer totalRowCount, String[] titles, String exportPath, ImageUploadUtil imageUploadUtil,
                                                    FeignExportRecordsClient feignExportRecordsClient, Integer recordsId, WriteExcelDataDelegated writeExcelDataDelegated) throws Exception {
        // 初始化EXCEL
        SXSSFWorkbook wb = PoiUtil.initExcel(totalRowCount, titles);
        // 调用委托类分批写数据
        int sheetCount = wb.getNumberOfSheets();
        for (int i = 0; i < sheetCount; i++) {
            SXSSFSheet eachSheet = wb.getSheetAt(i);
            for (int j = 1; j <= ExcelConstant.PER_SHEET_WRITE_COUNT; j++) {
                int currentPage = j;
                int pageSize = ExcelConstant.PER_WRITE_ROW_COUNT;
                double dPageSize = Double.parseDouble(pageSize+"");
                double dTotalRowCount = Double.parseDouble(totalRowCount+"");
                if(Math.ceil(dTotalRowCount/dPageSize) - currentPage < 0) {
                    continue;
                }
                int startRowCount = (j - 1) * ExcelConstant.PER_WRITE_ROW_COUNT + 1;
                int endRowCount = startRowCount + pageSize - 1;
                writeExcelDataDelegated.writeExcelData(wb,eachSheet, startRowCount, endRowCount, currentPage, pageSize);
            }
        }
        // 下载EXCEL
        File file = new File("poi_xlsx/");
        if(!file.exists()){
            file.mkdirs();
        }
        String xlsxPath = "poi_xlsx/"+exportPath;
        File allFile = new File(xlsxPath);
        allFile.createNewFile();
        PoiUtil.downLoadExcelToLocalPath(wb, allFile);
        log.info("xlsx文件已写入磁盘 [{}]", xlsxPath);
        // 上传到对象存储
        @Cleanup InputStream fileInputStream = new FileInputStream(xlsxPath);
        String uploadPath = imageUploadUtil.wechatUpload(fileInputStream, RegisterConstant.ICEBOX_REGISTER_NAME, "xlsx");
        log.info("文件下载地址 [{}]", uploadPath);
        // 删除临时文件
        CompletableFuture.runAsync(() -> {
            // 更新导出记录
            feignExportRecordsClient.updateExportRecord(uploadPath, ExportRecordTypeEnum.COMPLETED.getType(),recordsId);
            if(allFile.delete()) log.info("xlsx文件已删除 [{}]", xlsxPath);
        });
    }

    /**
     * 导出Excel到本地指定路径
     *
     * @param totalRowCount           总记录数
     * @param titles                  标题
     * @param exportPath              导出路径
     * @param writeExcelDataDelegated 向EXCEL写数据/处理格式的委托类 自行实现
     * @throws Exception
     */
    public static void exportExcelToLocalPath(Integer totalRowCount, String[] titles, String exportPath, ImageUploadUtil imageUploadUtil,
                                              ExportRecordsDao exportRecordsDao, String serialNum, WriteExcelDataDelegated writeExcelDataDelegated) throws Exception {
        // 初始化EXCEL
        SXSSFWorkbook wb = PoiUtil.initExcel(totalRowCount, titles);
        // 调用委托类分批写数据
        int sheetCount = wb.getNumberOfSheets();
        for (int i = 0; i < sheetCount; i++) {
            SXSSFSheet eachSheet = wb.getSheetAt(i);
            for (int j = 1; j <= ExcelConstant.PER_SHEET_WRITE_COUNT; j++) {
                int currentPage = j;
                int pageSize = ExcelConstant.PER_WRITE_ROW_COUNT;
                double dPageSize = Double.parseDouble(pageSize+"");
                double dTotalRowCount = Double.parseDouble(totalRowCount+"");
                if(Math.ceil(dTotalRowCount/dPageSize) - currentPage < 0) {
                    continue;
                }
                int startRowCount = (j - 1) * ExcelConstant.PER_WRITE_ROW_COUNT + 1;
                int endRowCount = startRowCount + pageSize - 1;
                writeExcelDataDelegated.writeExcelData(wb,eachSheet, startRowCount, endRowCount, currentPage, pageSize);
            }
        }
        // 下载EXCEL
        File file = new File("poi_xlsx/");
        if(!file.exists()){
            file.mkdirs();
        }
        String xlsxPath = "poi_xlsx/"+exportPath;
        File allFile = new File(xlsxPath);
        allFile.createNewFile();
        PoiUtil.downLoadExcelToLocalPath(wb, allFile);
        log.info("xlsx文件已写入磁盘 [{}]", xlsxPath);
        // 上传到对象存储
        @Cleanup InputStream fileInputStream = new FileInputStream(xlsxPath);
        String uploadPath = imageUploadUtil.wechatUpload(fileInputStream, RegisterConstant.ICEBOX_REGISTER_NAME, "xlsx");
        log.info("文件下载地址 [{}]", uploadPath);
        // 删除临时文件
        CompletableFuture.runAsync(() -> {
            // 更新导出记录
            exportRecordsDao.updateExportRecords(serialNum, uploadPath, new Date());
            if(allFile.delete()) log.info("xlsx文件已删除 [{}]", xlsxPath);
        });
    }

    /**
     * 导出Excel到本地指定路径,指定每次查询的数量
     *
     * @param totalRowCount           总记录数
     * @param titles                  标题
     * @param exportPath              导出路径
     * @param writeExcelDataDelegated 向EXCEL写数据/处理格式的委托类 自行实现
     * @throws Exception
     */
    public static void exportExcelToLocalPathByWriteRowCount(Integer totalRowCount,Integer writeRowCount, String[] titles, String exportPath, ImageUploadUtil imageUploadUtil,
                                                             ExportRecordsDao exportRecordsDao, String serialNum, WriteExcelDataDelegated writeExcelDataDelegated) throws Exception {
        // 初始化EXCEL
        SXSSFWorkbook wb = PoiUtil.initExcel(totalRowCount, titles);
        // 调用委托类分批写数据
        int sheetCount = wb.getNumberOfSheets();
        Integer sheetWriteCount = ExcelConstant.PER_SHEET_ROW_COUNT/writeRowCount;
        for (int i = 0; i < sheetCount; i++) {
            SXSSFSheet eachSheet = wb.getSheetAt(i);
            for (int j = 1; j <= sheetWriteCount; j++) {
                int currentPage = j;
                int pageSize = writeRowCount;
                double dPageSize = Double.parseDouble(pageSize+"");
                double dTotalRowCount = Double.parseDouble(totalRowCount+"");
                if(Math.ceil(dTotalRowCount/dPageSize) - currentPage < 0) {
                    continue;
                }
                int startRowCount = (j - 1) * writeRowCount + 1;
                int endRowCount = startRowCount + pageSize - 1;
                writeExcelDataDelegated.writeExcelData(wb,eachSheet, startRowCount, endRowCount, currentPage, pageSize);
            }
        }
        // 下载EXCEL
        File file = new File("poi_xlsx/");
        if(!file.exists()){
            file.mkdirs();
        }
        String xlsxPath = "poi_xlsx/"+exportPath;
        File allFile = new File(xlsxPath);
        allFile.createNewFile();
        PoiUtil.downLoadExcelToLocalPath(wb, allFile);
        log.info("xlsx文件已写入磁盘 [{}]", xlsxPath);
        // 上传到对象存储
        @Cleanup InputStream fileInputStream = new FileInputStream(xlsxPath);
        String uploadPath = imageUploadUtil.wechatUpload(fileInputStream, RegisterConstant.ICEBOX_REGISTER_NAME, "xlsx");
        log.info("文件下载地址 [{}]", uploadPath);
        // 删除临时文件
        CompletableFuture.runAsync(() -> {
            // 更新导出记录
            exportRecordsDao.updateExportRecords(serialNum, uploadPath, new Date());
            if(allFile.delete()) log.info("xlsx文件已删除 [{}]", xlsxPath);
        });
    }

    /**
     * 导出Excel到本地指定路径(计划明细报表)
     * 第一步操作，将当天的记录写入
     * @param totalRowCount           总记录数
     * @param writeExcelDataDelegated 向EXCEL写数据/处理格式的委托类 自行实现
     * @throws Exception
     */
    public static void exportExcelFirstForDetail(SXSSFWorkbook wb,Integer sheetCount,Integer totalRowCount,
                                              WriteExcelDataDelegated writeExcelDataDelegated) throws Exception {
        for (int i = 0; i < sheetCount; i++) {
            SXSSFSheet eachSheet = wb.getSheetAt(i);
            for (int j = 1; j <= ExcelConstant.PER_SHEET_WRITE_COUNT; j++) {
                int currentPage = j;
                int pageSize = ExcelConstant.PER_WRITE_ROW_COUNT;
                double dPageSize = Double.parseDouble(pageSize+"");
                double dTotalRowCount = Double.parseDouble(totalRowCount+"");
                if(Math.ceil(dTotalRowCount/dPageSize) - currentPage < 0) {
                    continue;
                }
                int startRowCount = (j - 1) * ExcelConstant.PER_WRITE_ROW_COUNT + 1;
                int endRowCount = startRowCount + pageSize - 1;
                writeExcelDataDelegated.writeExcelData(wb,eachSheet, startRowCount, endRowCount, currentPage, pageSize);
            }
        }
    }

    /**
     * 导出Excel到本地指定路径(计划明细报表)
     * 第二步操作，写detailExport中数据并下载
     * @param totalRowCount           总记录数
     * @param exportPath              导出路径
     * @param writeExcelDataDelegated 向EXCEL写数据/处理格式的委托类 自行实现
     * @throws Exception
     */
    public static void exportExcelSecondForDetail(SXSSFWorkbook wb,Integer startSheetCount,Integer totalRowCount, String exportPath, ImageUploadUtil imageUploadUtil,
                                                  ExportRecordsDao exportRecordsDao, String serialNum, WriteExcelDataDelegated writeExcelDataDelegated) throws Exception {

        // 调用委托类分批写数据
        int sheetCount = wb.getNumberOfSheets();
        for (int i = startSheetCount; i < sheetCount; i++) {
            SXSSFSheet eachSheet = wb.getSheetAt(i);
            for (int j = 1; j <= ExcelConstant.PER_SHEET_WRITE_COUNT; j++) {
                int currentPage = j;
                int pageSize = ExcelConstant.PER_WRITE_ROW_COUNT;
                double dPageSize = Double.parseDouble(pageSize+"");
                double dTotalRowCount = Double.parseDouble(totalRowCount+"");
                if(Math.ceil(dTotalRowCount/dPageSize) - currentPage < 0) {
                    continue;
                }
                int startRowCount = (j - 1) * ExcelConstant.PER_WRITE_ROW_COUNT + 1;
                int endRowCount = startRowCount + pageSize - 1;
                writeExcelDataDelegated.writeExcelData(wb,eachSheet, startRowCount, endRowCount, currentPage, pageSize);
            }
        }
        // 下载EXCEL
        File file = new File("poi_xlsx/");
        if(!file.exists()){
            file.mkdirs();
        }
        String xlsxPath = "poi_xlsx/"+exportPath;
        File allFile = new File(xlsxPath);
        allFile.createNewFile();
        PoiUtil.downLoadExcelToLocalPath(wb, allFile);
        log.info("xlsx文件已写入磁盘 [{}]", xlsxPath);
        // 上传到对象存储
        @Cleanup InputStream fileInputStream = new FileInputStream(xlsxPath);
        String uploadPath = imageUploadUtil.wechatUpload(fileInputStream, RegisterConstant.ICEBOX_REGISTER_NAME, "xlsx");
        log.info("文件下载地址 [{}]", uploadPath);
        // 删除临时文件
        if(null!=exportRecordsDao){
            CompletableFuture.runAsync(() -> {
                // 更新导出记录
                exportRecordsDao.updateExportRecords(serialNum, uploadPath, new Date());

                if(allFile.delete()) log.info("xlsx文件已删除 [{}]", xlsxPath);
            });
        }
    }

    /**
     * 导出Excel到浏览器
     *
     * @param response
     * @param totalRowCount           总记录数
     * @param fileName                文件名称
     * @param titles                  标题
     * @param writeExcelDataDelegated 向EXCEL写数据/处理格式的委托类 自行实现
     * @throws Exception
     */
    public static void exportExcelToWebsite(HttpServletResponse response, Integer totalRowCount, String fileName,
                                            String[] titles, WriteExcelDataDelegated writeExcelDataDelegated) throws Exception {
        // 初始化EXCEL
        SXSSFWorkbook wb = PoiUtil.initExcel(totalRowCount, titles);
        // 调用委托类分批写数据
        int sheetCount = wb.getNumberOfSheets();
        for (int i = 0; i < sheetCount; i++) {
            SXSSFSheet eachSheet = wb.getSheetAt(i);
            for (int j = 1; j <= ExcelConstant.PER_SHEET_WRITE_COUNT; j++) {
                int currentPage = (i * ExcelConstant.PER_SHEET_WRITE_COUNT + j - 1)*ExcelConstant.PER_WRITE_ROW_COUNT;
                if(currentPage>totalRowCount) continue;
                int pageSize = ExcelConstant.PER_WRITE_ROW_COUNT;
                int startRowCount = (j - 1) * ExcelConstant.PER_WRITE_ROW_COUNT + 1;
                int endRowCount = startRowCount + pageSize - 1;
                writeExcelDataDelegated.writeExcelData(wb,eachSheet, startRowCount, endRowCount, currentPage, pageSize);
            }
        }
        // 下载EXCEL
        PoiUtil.downLoadExcelToWebsite(wb, response, fileName);
    }

}