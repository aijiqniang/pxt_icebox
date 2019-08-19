package com.szeastroc.icebox.util.excel;

import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 
 * <一句话功能简述>
 * @author island(YQ)
 * @since   2018年10月20日
 */
public class ExcelUtil<E> {

    /** 日志常量类  **/
    private static final Logger LOGGER = LoggerFactory.getLogger(ExcelUtil.class);
    /** 日期常量类  **/
    private static final String DATE_FORMAT_YYYYMMDDHHMMSS = "yyyy-MM-dd HH:mm:ss";

    /** excel对象  **/
    private E excel;

    /**
     * <默认构造函数>
     */
    public ExcelUtil(E excel) {

        this.excel = excel;

    }

    /**
     * 获取对象实例
     * 
     * @return E对象实例
     * @throws InstantiationException InstantiationException
     * @throws IllegalAccessException IllegalAccessException
     */
    @SuppressWarnings("unchecked")
    public E get() throws InstantiationException, IllegalAccessException {

        return (E) excel.getClass().newInstance();

    }

    /**
     * * 将数据写入到EXCEL文档
     * 
     * @param filePath 文件路径
     * @param sheetName 标签页名
     * @param list 数据集合
     * @throws Exception Exception
     * @param <T> 泛型
     */
    public static <T> void writeToFile(String filePath, String sheetName, List<T> list) throws Exception {
        // 创建并获取工作簿对象
        Workbook wb = getWorkBook(list, sheetName);
        System.out.println(wb.toString());
        // 写入到文件
        FileOutputStream out = null;
        try{
            out= new FileOutputStream(filePath);
            wb.write(out);
            out.close();
        }
        catch (FileNotFoundException fe) {
            LOGGER.error("File not found.");
        }
        catch (IOException ioe) {
            LOGGER.error("IO exception.");
        }
        finally {
            if (null != out) {
                out.close();
            }
        }

    }
    
    /**
     * 创建excel文档，
     * @param list 数据
     * @param keys list中map的key数组集合
     * @param columnNames excel的列名
     * */
    public static Workbook createWorkBook(List<Map<String, Object>> list, String []keys, String columnNames[]) {
        // 创建excel工作簿
        Workbook wb = new HSSFWorkbook();
        // 创建第一个sheet（页），并命名
        Sheet sheet = wb.createSheet(list.get(0).get("sheetName").toString());
        // 手动设置列宽。第一个参数表示要为第几列设；，第二个参数表示列的宽度，n为列高的像素数。
        for(int i=0;i<keys.length;i++){
            sheet.setColumnWidth((short) i, (short) (35.7 * 150));
        }

        // 创建第一行
        Row row = sheet.createRow((short) 0);

        // 创建两种单元格格式
        CellStyle cs = wb.createCellStyle();
        CellStyle cs2 = wb.createCellStyle();

        // 创建两种字体
        Font f = wb.createFont();
        Font f2 = wb.createFont();

        // 创建第一种字体样式（用于列名）
        f.setFontHeightInPoints((short) 10);
        f.setColor(IndexedColors.BLACK.getIndex());
//        f.setBoldweight(Font.BOLDWEIGHT_BOLD);

        // 创建第二种字体样式（用于值）
        f2.setFontHeightInPoints((short) 10);
        f2.setColor(IndexedColors.BLACK.getIndex());

        // 设置第一种单元格的样式（用于列名）
        cs.setFont(f);
//        cs.setBorderLeft(CellStyle.BORDER_THIN);
//        cs.setBorderRight(CellStyle.BORDER_THIN);
//        cs.setBorderTop(CellStyle.BORDER_THIN);
//        cs.setBorderBottom(CellStyle.BORDER_THIN);
//        cs.setAlignment(CellStyle.ALIGN_CENTER);

        // 设置第二种单元格的样式（用于值）
        cs2.setFont(f2);
//        cs2.setBorderLeft(CellStyle.BORDER_THIN);
//        cs2.setBorderRight(CellStyle.BORDER_THIN);
//        cs2.setBorderTop(CellStyle.BORDER_THIN);
//        cs2.setBorderBottom(CellStyle.BORDER_THIN);
//        cs2.setAlignment(CellStyle.ALIGN_CENTER);

        cs2.setWrapText(true);
        DataFormat format = wb.createDataFormat();
        cs2.setDataFormat(format.getFormat("@"));
        cs.setDataFormat(format.getFormat("@"));

        //设置列名
        for(int i=0;i<columnNames.length;i++){
            Cell cell = row.createCell(i);
            cell.setCellValue(columnNames[i]);
            cell.setCellStyle(cs);
            cell.setCellType(Cell.CELL_TYPE_STRING);
        }
        //设置每行每列的值
        for (short i = 1; i < list.size(); i++) {
            // Row 行,Cell 方格 , Row 和 Cell 都是从0开始计数的
            // 创建一行，在页sheet上
            Row row1 = sheet.createRow((short) i);
            // 在row行上创建一个方格
            for(short j=0;j<keys.length;j++){
                Cell cell = row1.createCell(j);
                cell.setCellType(Cell.CELL_TYPE_STRING);
                if(keys[j].equals("orderContent")) {
                    cell.setCellValue(new HSSFRichTextString(list.get(i).get(keys[j]).toString()));
                }else {
                    cell.setCellValue(list.get(i).get(keys[j]) == null ? " " : list.get(i).get(keys[j]).toString());

                }
                cell.setCellStyle(cs2);
            }
        }
        return wb;
    }

    /**
     * 获得Workbook对象
     * <一句话功能简述>
     * @param list 数据列表
     * @param sheetName 标签名
     * @return Workbook对象
     * @throws Exception Exception
     * @param <T> 泛型
     */
    public static <T> Workbook getWorkBook(List<T> list, String sheetName) throws Exception {

        // 创建工作簿
        Workbook wb = new SXSSFWorkbook();
        if (list == null || list.size() == 0) {
            return wb;
        }
        // 创建一个工作表sheet
        Sheet sheet = wb.createSheet();
        wb.setSheetName(0, sheetName);

        // 申明行
        Row row = sheet.createRow(0);

        // 申明单元格
        Cell cell = null;
        CreationHelper createHelper = wb.getCreationHelper();
        Field[] fields = getClassFieldsAndSuperClassFields(list.get(0).getClass());
        XSSFCellStyle titleStyle = (XSSFCellStyle) wb.createCellStyle();

        int columnIndex = 0;
        Excel excel = null;
        for (Field field : fields) {
            field.setAccessible(true);
            excel = field.getAnnotation(Excel.class);
            if (excel == null || excel.skip()) {
                continue;
            }

            // 列宽注意乘256
            sheet.setColumnWidth(columnIndex, excel.width() * 256);

            // 写入标题
            cell = row.createCell(columnIndex);
            cell.setCellStyle(titleStyle);
            cell.setCellValue(excel.name());
            columnIndex++;
        }

        int rowIndex = 1;
        CellStyle cs = wb.createCellStyle();
        for (T t : list) {
            row = sheet.createRow(rowIndex);
            columnIndex = 0;
            Object o = null;
            for (Field field : fields) {
                field.setAccessible(true);
                // 忽略标记skip的字段
                excel = field.getAnnotation(Excel.class);
                if (excel == null || excel.skip()) {
                    continue;
                }

                // 数据
                cell = row.createCell(columnIndex);
                o = field.get(t);
                // 如果数据为空，跳过
                if (o == null) {
                    continue;
                }
                // 处理日期类型

                if (o instanceof Date) {
                    cs.setDataFormat(createHelper.createDataFormat().getFormat(DATE_FORMAT_YYYYMMDDHHMMSS));
                    cell.setCellStyle(cs);
                    cell.setCellValue((Date) field.get(t));
                }
                else if (o instanceof Double || o instanceof Float) {
                    cell.setCellValue((Double) field.get(t));
                }
                else if (o instanceof Boolean) {
                    cell.setCellValue((Boolean) field.get(t));
                }
                else if (o instanceof Integer) {
                    cell.setCellValue((Integer) field.get(t));
                }
                else {
                    cell.setCellValue(field.get(t).toString());
                }
                columnIndex++;
            }
            rowIndex++;
        }
        return wb;

    }

    /**
     * 从文件读取数据，最好是所有的单元格都是文本格式
     * 根据文件后缀判断是xls还是xlsx
     * 示例：List xx = new ExcelUtils(new Object()).readFromFile(file,0);
     * 在Object对象中添加@Excel(name = "导入列的列名")
     * @param file Excel文件
     * @param sheetIndex 第几个sheet 默认为第0个
     * @return 读取结果
     * @throws Exception Exception
     */
    public List<E> readFromFile(File file, int sheetIndex) throws Exception {

        if (file == null || null == file.getName() || "".equals(file.getName())){
            throw new Exception("输入文件为空，请输入正确文件");
        }
        
        boolean isXlsx = false;
        String[] tempArrayFileName = file.getName().split("\\.");
        //如果文件后缀名为xlsx，则取true
        if (tempArrayFileName.length > 0 && "xlsx".equalsIgnoreCase(tempArrayFileName[tempArrayFileName.length - 1])) {
            isXlsx = true;
        }
        return readFromFile(file, isXlsx, sheetIndex);
    }

    /**
     * 从文件读取数据，最好是所有的单元格都是文本格式
     * 示例：List xx = new ExcelUtils(new Object()).readFromFile(file,true,0);
     * 在Object对象中添加@Excel(name = "导入列的列名")
     * @param file Excel文件
     * @param isXlsx 是否xlsx格式(true:xlsx; false:xls)
     * @param sheetIndex 第几个sheet 默认为第0个
     * @return 读取结果
     * @throws Exception Exception
     */
    public List<E> readFromFile(File file, boolean isXlsx, int sheetIndex) throws Exception {

        return readFromFile(file, isXlsx, sheetIndex, -1, -1);
    }
    
    /**
     * 从文件读取数据，最好是所有的单元格都是文本格式
     * 取出第(startPage - 1) * pagePerSize + 1开始的pagePerSize条数据
     * 示例：List xx = new ExcelUtils(new Object()).readFromFile(file,true,0,1,100);
     * 在Object对象中添加@Excel(name = "导入列的列名")
     * @param file Excel文件
     * @param isXlsx 是否xlsx格式(true:xlsx; false:xls)
     * @param sheetIndex 第几个sheet 默认为第0个
     * @param startPage 开始页数 设置为0或负数则全量导入
     * @param pagePerSize 每页数据数
     * @return 读取结果
     * @throws Exception Exception
     */
    public List<E> readFromFile(File file, boolean isXlsx, int sheetIndex, int startPage, int pagePerSize) throws Exception {
        Field[] fields = getClassFieldsAndSuperClassFields(excel.getClass());
        Map<String, String> textToKey = new HashMap<String, String>();
        Excel excel = null;
        for (Field field : fields) {
            excel = field.getAnnotation(Excel.class);
            if (excel == null || excel.skip()) {
                continue;
            }
            textToKey.put(excel.name(), field.getName());
        }

        InputStream is = new FileInputStream(file);
        Workbook wb = null;
        if (isXlsx) {
            wb = new XSSFWorkbook(is);
        }
        else {
            wb = new HSSFWorkbook(is);
        }
        
        //若传入标签页小于0，取默认第0个
        if (sheetIndex < 0) {
            sheetIndex = 0;
        }
        
        Sheet sheet = wb.getSheetAt(sheetIndex);
        Row title = sheet.getRow(0);

        // 标题数组，后面用到，根据索引去标题名称，通过标题名称去字段名称用到 textToKey
        String[] titles = new String[title.getPhysicalNumberOfCells()];

        //获取列
        for (int i = 0; i < title.getPhysicalNumberOfCells(); i++) {
            if (null != title.getCell(i)) {
                titles[i] = title.getCell(i).getStringCellValue();
            }
        }

        List<E> list = new ArrayList<E>();
        E e = null;
        int rowIndex = 0;
        int columnCount = titles.length;
        Cell cell = null;
        Row row = null;
        
        Object object = null;
        
        //按行处理
        int start = (startPage - 1) * pagePerSize + 1;
        int end = startPage * pagePerSize;
        
        //逐行读取处理
        for (Iterator<Row> it = sheet.rowIterator(); it.hasNext();) {
            row = it.next();
            if (rowIndex == 0) {
                rowIndex++;
                continue;
            }
            else if (start > 0 && end >= start) {
                //根据起始行和结束行处理
                if (rowIndex < start) {
                    rowIndex++;
                    continue;
                }
                if (rowIndex > end){
                    break;
                }
            }
            // 确保序号增加，保证比较不会出现异常
            rowIndex++;

            if (row == null) {
                break;
            }

            e = get();
            //按列读取
            for (int i = 0; i < columnCount; i++) {
                cell = row.getCell(i);
                if (null != cell) {
                    object = readCellContent(cell);
                    if (null != object){
                        setFieldValue(textToKey.get(titles[i]), fields, e, object);
                    }
                }
            }
            list.add(e);

        }

        return list;

    }

    /**
     * 从单元格读取数据对象
     * 
     * @param cell 单元格对象
     * @return Object 单元格对象值 
     */
    public Object readCellContent(Cell cell) {
        Object o = null;
        switch (cell.getCellType()) {
            case XSSFCell.CELL_TYPE_BOOLEAN:
                o = cell.getBooleanCellValue();
                break;
            case XSSFCell.CELL_TYPE_NUMERIC:
                //日期类型转换
                if (HSSFDateUtil.isCellDateFormatted(cell)) {
                    o = DateUtil.getJavaDate(cell.getNumericCellValue());
                }
                else {
                    // 纯数字转换，防止字符串类型转换为科学计数或后面带.0
                    BigDecimal cellBig = new BigDecimal(cell.getNumericCellValue());
                    String tempTrim = cellBig.toString().trim();
                    o = tempTrim;
                    if (!"".equals(tempTrim)) {
                        String[] item = tempTrim.split("\\.");
                        if (item.length > 1 && "0".equals(item[1])) {
                            o = item[0];
                        }
                    }
                }
                break;
            case XSSFCell.CELL_TYPE_STRING:
                o = cell.getStringCellValue();
                break;
            case XSSFCell.CELL_TYPE_ERROR:
                o = cell.getErrorCellValue();
                break;
            case XSSFCell.CELL_TYPE_FORMULA:
                o = cell.getCellFormula();
                break;
            default:
                break;
        }

        return o;
    }
    
    /**
     * 根据对象转换设置值内容
     * @param key 映射转换KEY值
     * @param fields KEY值对应的
     * @param excel 表格对象
     * @param object 单元格对象
     * @author zhoufengfeng
     * @since   2015年7月16日
     */
    private void setFieldValue(String key, Field[] fields, E excel, Object object) {
        try {
            for (Field field : fields) {
                field.setAccessible(true);
                if (field.getName().equals(key)) {
                    if (field.getType().equals(object.getClass())) {
                        field.set(excel, object);
                    }
                    else {
                        // 根据不同单元络类型进行转换处理
                        if (field.getType().equals(Date.class)) {
                            field.set(excel, new SimpleDateFormat(DATE_FORMAT_YYYYMMDDHHMMSS).parse(object.toString()));
                        }
                        else if (field.getType().equals(String.class)) {
                            field.set(excel, object.toString());
                        }
                        else if (field.getType().equals(Long.class) || field.getType().equals(long.class)) {
                            if ("".equals(object.toString().toString())){
                                LOGGER.debug("Value is empty, pass.");
                            }
                            else {
                                field.set(excel, Double.valueOf(object.toString()).longValue());
                            }

                        }
                        else if (field.getType().equals(Integer.class) || field.getType().equals(int.class)) {
                            if ("".equals(object.toString().toString())){
                                LOGGER.debug("Value is empty, pass.");
                            }
                            else {
                                field.set(excel, Double.valueOf(object.toString()).intValue());
                            }
                        }
                        
                        else if (field.getType().equals(Boolean.class) || field.getType().equals(boolean.class)) {
                            field.set(excel, Boolean.parseBoolean(object.toString()));
                        }
                        else if (field.getType().equals(Float.class)  || field.getType().equals(float.class)) {
                            field.set(excel, Float.parseFloat(object.toString()));
                        }
                        else if (field.getType().equals(Double.class) || field.getType().equals(double.class)) {
                            field.set(excel, Double.parseDouble(object.toString()));
                        }
                        else if (field.getType().equals(BigDecimal.class)) {
                            field.set(excel, BigDecimal.valueOf(Double.parseDouble(object.toString())));
                        }
                        else {
                            LOGGER.info("Not exsite type: " + field.getType());
                        }
                    }

                }
            }
        }
        catch (RuntimeException e) {
            LOGGER.error("RuntimeException happen,please check.");
            throw e;
        }
        catch (Exception ex) {
            LOGGER.error("Set fieldValue exception.");
        }
    }

    /**
     * 通过反射获取所有的成员变量,包括父类
     * 
     * @param clazz 类名
     * @return 类中对象值
     * @throws Exception Exception
     * @param <T> 泛型 
     */
    public static <T> Field[] getClassFieldsAndSuperClassFields(Class<T> clazz) throws Exception {

        Field[] fields = clazz.getDeclaredFields();

        if (clazz.getSuperclass() == null) {
            throw new Exception(clazz.getName() + "为空");
        }

        Field[] superFields = clazz.getSuperclass().getDeclaredFields();

        Field[] allFields = new Field[fields.length + superFields.length];

        for (int i = 0; i < fields.length; i++) {
            allFields[i] = fields[i];
        }
        for (int i = 0; i < superFields.length; i++) {
            allFields[fields.length + i] = superFields[i];
        }

        return allFields;
    }
    
    /**
     * 替换Excel模板文件内容
     * 
     * @param map
     *            文档数据
     * @param sourceFilePath
     *            Excel模板文件路径
     * @param targetFilePath
     *            Excel生成文件路径
     * @return 是否成功
     */
    public static boolean replaceModel(Map<String, String> map, String sourceFilePath, String targetFilePath) {
        boolean bool = true;
        // 输出文件
        FileOutputStream fileOut = null;
        try {
            POIFSFileSystem fs = new POIFSFileSystem(new FileInputStream(sourceFilePath));
            HSSFWorkbook wb = new HSSFWorkbook(fs);
            HSSFSheet sheet = wb.getSheetAt(0);
            @SuppressWarnings("rawtypes")
            Iterator rows = sheet.rowIterator();
            while (rows.hasNext()) {
                HSSFRow row = (HSSFRow) rows.next();
                // 获取列
                for (int i = 0; i < row.getPhysicalNumberOfCells(); i++) {
                    if (null != row.getCell(i)) {
                        String cellValue = row.getCell(i).getStringCellValue();
                        if (cellValue != null && cellValue.trim().matches("(.*?)[{](.*?)[}](.*?)")) {
                            String[] arr = cellValue.trim().split("[$]");
                            StringBuffer sb = new StringBuffer();
                            for (int j = 0; j < arr.length; j++) {
                                if (arr[j].contains("{")) {
                                    String key = arr[j].substring(arr[j].indexOf("${") + 2,
                                            arr[j].indexOf("}", arr[j].indexOf("${") + 2));
                                    arr[j] = arr[j].replace("{" + key + "}", map.get(key));
                                }
                                sb.append(arr[j]);
                            }

                            row.getCell(i).setCellType(HSSFCell.CELL_TYPE_STRING);
                            row.getCell(i).setCellValue(sb.toString());
                        }
                    }
                }
            }

            // 输出文件
            fileOut = new FileOutputStream(targetFilePath);
            wb.write(fileOut);
            fileOut.close();

        }
        catch (FileNotFoundException e) {
            bool = false;
            LOGGER.error("文件未找到异常:" + e.getMessage());
        }
        catch (IOException e) {
            bool = false;
            LOGGER.error("IO异常:" + e.getMessage());
        }
        finally {
            if (null != fileOut) {
                try {
                    fileOut.close();
                }
                catch (IOException e) {
                    LOGGER.error("IO异常，无法关闭输出流");
                }
            }
        }
        return bool;
    }

}
