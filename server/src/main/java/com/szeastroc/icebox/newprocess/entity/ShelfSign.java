package com.szeastroc.icebox.newprocess.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@TableName("t_shelf_sign")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ApiModel
public class ShelfSign {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    //陈列架的id
    private Integer shelfId;
    //申请的单号
    private String applyNumber;
    //尺寸的大小
    private String shelfSize;
    //货架类型
    private Integer shelfType;
    //货架名称
    private String shelfName;
    //签收的标题
    private String informName;
    //未签收的数量
    private Integer defaultSign;
    //签收的客户号码
    private String customerNumber;
    //签收的客户名称
    private String customerName;
    //签收的状态 0:未签收 1：已签收
    private Integer signStatus;
    //签收的时间
    private Date createDate;
    //修改的时间
    private Date updateDate;

}
