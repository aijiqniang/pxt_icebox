package com.szeastroc.icebox.newprocess.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * @Author xiao
 * @Date create in 2020/5/6 17:24
 * @Description:
 */
@Getter
@Setter
@TableName(value = "t_export_records")
public class ExportRecords {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private String serialNum; // 任务流水号
    private String jobName; // 作业名称
    private Integer userId; // 用户id
    private Integer type; // 状态:0-处理中;1-已完成;
    private String netPath; // 文件网络地址
    private Date downloadTime; // 下载时间
    private String param; // 请求参数json串
    private Date requestTime; // 请求时间
    private String userName; // 用户名称
    private Date endRequestTime;// 请求结束时间

}
