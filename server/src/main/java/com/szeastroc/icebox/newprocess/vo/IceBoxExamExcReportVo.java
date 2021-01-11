package com.szeastroc.icebox.newprocess.vo;

import com.szeastroc.icebox.newprocess.entity.IceBoxExamineExceptionReport;
import lombok.Getter;
import lombok.Setter;

/**
 * @Author xiao
 * @Date create in 2020/12/28 14:15
 * @Description:
 */
@Getter
@Setter
public class IceBoxExamExcReportVo extends IceBoxExamineExceptionReport {

    private String shenHeRenZhiWu; //审核人职务
    private String shenHeBeiZhu; // 审核备注

}
