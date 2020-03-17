package com.szeastroc.icebox.newprocess.vo.request;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class IceExamineRequest extends Page {

    /**
     * 业务员id
     */
    private Integer createBy;

    /**
     * 门店编号
     */
    private String storeNumber;


    /**
     * 巡检日期
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date createTime;


    /**
     * 巡检
     */
    private Integer type;



}
