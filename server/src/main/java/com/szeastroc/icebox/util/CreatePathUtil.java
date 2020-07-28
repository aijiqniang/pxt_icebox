package com.szeastroc.icebox.util;

import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;

import java.io.File;

/**
 * <Description> <br>
 *
 * @author xiao<br>
 * @createDate 2019/11/19 16:45 <br>
 * @see com.szeastroc.esign <br>
 */
@Slf4j
public class CreatePathUtil {

    /**
     * 创建文件上传路径
     * @return
     */
    public static String creatDocPath() {
        // 文件夹前缀  prefix
        DateTime dateTime = new DateTime();
        // 当前程序所在文件目录
        String ROOT_FOLDER = new File("").getAbsolutePath();
        //文件地址前缀拼接（可根据实际场景自定义）
        String PATH_PREFEX = ROOT_FOLDER + File.separator + "html" + File.separator;
        // 填充后的 xlsx 文件路径   E:\soft\idea\workspace\pxt_cloud\html\东鹏合同(江西重点客户服务处20191116).xlsx
        String dest = PATH_PREFEX + dateTime.getMillis() + ".xlsx";
        File dstFile = new File(dest);
        //文件目录
        File destDirectory = new File(dstFile.getParent());
        if (!destDirectory.exists()) {
            destDirectory.mkdirs();
        }
        return dest;
    }
}
