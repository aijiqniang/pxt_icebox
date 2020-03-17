package com.szeastroc.icebox.newprocess.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamineNodeVo {

    private Integer id;
    private Integer examineId;
    private Integer userId;
    private Integer examineStatus;
    private Integer orderNumber;
    private Date createTime;
    private Date updateTime;

    private String userIdStr;
    private String officeName;
}