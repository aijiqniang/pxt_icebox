package com.szeastroc.icebox.util;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.szeastroc.common.feign.visit.FeignExportRecordsClient;
import com.szeastroc.common.utils.ImageUploadUtil;
import com.szeastroc.common.utils.SpringContextUtil;
import com.szeastroc.icebox.constant.IceBoxConstant;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class NewExcelUtil<T> {

    @Resource
    private final ImageUploadUtil imageUploadUtil = SpringContextUtil.getBean(ImageUploadUtil.class);
    @Resource
    private final FeignExportRecordsClient feignExportRecordsClient = SpringContextUtil.getBean(FeignExportRecordsClient.class);

    /**
     * 仅作兼容老的导出报表
     *
     * @param fileName  文件名称
     * @param titleName sheet名称
     * @param headers   excel 第一行的头
     * @param data      导出的数据
     * @param response
     */
    public void oldExportExcel(String fileName, String titleName, String[] headers, List<T> data, HttpServletResponse response) {
        try {
            response.setContentType("application/vnd.ms-excel;charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8") + ".xlsx");
            WriteSheet writeSheet = EasyExcel.writerSheet(titleName).build();

            List<List<String>> list = new ArrayList<>();
            for (String header : headers) {
                List<String> headList = new ArrayList<>();
                headList.add(header);
                list.add(headList);
            }
            ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream()).head(list).build();
            excelWriter.write(data, writeSheet);
            excelWriter.finish();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 不传入表的头信息，在对象中定义好头的信息和位置
     *
     * @param fileName
     * @param sheetName
     * @param data
     * @param response
     */
    public void exportExcel(String fileName, String sheetName, List<T> data, HttpServletResponse response) {
        try {
            response.setContentType("application/vnd.ms-excel;charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8") + ".xlsx");
            WriteSheet writeSheet = EasyExcel.writerSheet(sheetName).build();
            ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream()).build();
            excelWriter.write(data, writeSheet);
            excelWriter.finish();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void asyncExportExcel(String fileName, String sheetName, String[] headers, List<T> data, Integer exportRecordsId) throws Exception {
        List<List<String>> list = new ArrayList<>();
        for (String header : headers) {
            List<String> headList = new ArrayList<>();
            headList.add(header);
            list.add(headList);
        }
        WriteSheet writeSheet = EasyExcel.writerSheet(sheetName).build();
        String xlsxPath = CreatePathUtil.creatDocPath();
        ExcelWriter excelWriter = EasyExcel.write(xlsxPath).head(list).build();
        excelWriter.write(data, writeSheet);
        excelWriter.finish();
        File xlsxFile = new File(xlsxPath);

        @Cleanup InputStream in = new FileInputStream(xlsxFile);
        try {

            String frontName = new DateTime().toString("yyyy-MM-dd-HH-mm-ss");
            // todo 上传临时文件到网络
            String imgUrl = imageUploadUtil.wechatUpload(in, IceBoxConstant.ICE_BOX, "BGDC" + frontName, "xlsx");
            // 更新下载列表中的数据
            feignExportRecordsClient.updateExportRecord(imgUrl, 1, exportRecordsId);
        } catch (Exception e) {
            log.error("报表导出excel错误", e);
            log.error("报表导出excel错误,exportRecordsId-->[{}]", exportRecordsId);
        } finally {
            // 删除临时目录
            if (StringUtils.isNotBlank(xlsxPath)) {
                FileUtils.deleteQuietly(xlsxFile);
            }
        }
    }

    public void asyncExportExcelOther(String fileName, String sheetName, List<T> data, Integer exportRecordsId) throws Exception {
        List<List<String>> list = new ArrayList<>();

        WriteSheet writeSheet = EasyExcel.writerSheet(sheetName).build();
        String xlsxPath = CreatePathUtil.creatDocPath();
        ExcelWriter excelWriter = EasyExcel.write(xlsxPath, data.get(0).getClass()).build();
        excelWriter.write(data, writeSheet);
        excelWriter.finish();
        File xlsxFile = new File(xlsxPath);

        @Cleanup InputStream in = new FileInputStream(xlsxFile);
        try {

            String frontName = new DateTime().toString("yyyy-MM-dd-HH-mm-ss");
            // todo 上传临时文件到网络
            String imgUrl = imageUploadUtil.wechatUpload(in, IceBoxConstant.ICE_BOX, "BGDC" + frontName, "xlsx");
            // 更新下载列表中的数据
            feignExportRecordsClient.updateExportRecord(imgUrl, 1, exportRecordsId);
        } catch (Exception e) {
            log.error("报表导出excel错误", e);
            log.error("报表导出excel错误,exportRecordsId-->[{}]", exportRecordsId);
        } finally {
            // 删除临时目录
            if (StringUtils.isNotBlank(xlsxPath)) {
                FileUtils.deleteQuietly(xlsxFile);
            }
        }
    }
}
