package com.szeastroc.icebox.newprocess.vo.request;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * @Author xiao
 * @Date create in 2020/4/23 17:27
 * @Description:
 */
@Getter
@Setter
public class IceTransferRecordPage extends Page {

    private Integer iceBoxId;

    private Integer deptId;

    private String oldSupplierNumber;
    private String newSupplierNumber;

    private String oldSupplierName;
    private String newSupplierName;


    private String userMessage;

    private Date startTime;
    private Date endTime;




}
