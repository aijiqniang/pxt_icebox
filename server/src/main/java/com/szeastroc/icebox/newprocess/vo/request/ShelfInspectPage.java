package com.szeastroc.icebox.newprocess.vo.request;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.szeastroc.icebox.newprocess.entity.DisplayShelfInspectApply;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Data
public class ShelfInspectPage extends Page<DisplayShelfInspectApply> {

    /**
     * 投放编号
     */
    @NotEmpty(message = "投放编号不能为空")
    private String putNumber;

    /**
     * 业务员id
     */
    @NotNull(message = "业务员id不能为空")
    private Integer createBy;

    @NotEmpty(message = "业务员姓名不能为空")
    private String createName;

    /**
     * 巡检日期
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date createTime;




}
