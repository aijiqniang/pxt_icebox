package com.szeastroc.icebox.newprocess.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * @Author xiao
 * @Date create in 2020/9/8 9:08
 * @Description:
 */
@Getter
@Setter
@Builder
public class CodeVo {

    @ColumnWidth(50)
    @ExcelProperty(value = "二维码")
    private String codeLink; // 二维码地址

}
