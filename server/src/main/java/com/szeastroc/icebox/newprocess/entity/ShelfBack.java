package com.szeastroc.icebox.newprocess.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@TableName("t_shelf_back")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ApiModel
public class ShelfBack {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private String uuid;
    //陈列架id
    private Integer shelfId;
    //申请的客户编码
    private String customerNumber;
    //营销区域id
    private Integer marketArea;
    //门店的名称
    private String customerName;
    //服务处id
    private Integer serviceDeptId;
    //服务处名称
    private String serviceDeptName;
    //申请编号
    private String applyNumber;
    //货架的类型
    private Integer shelfType;
    //货架名称
    private String shelfName;
    //申请的数量
    private Integer applyCount;
    //签收的状态 0:未退还 1：已退还
    private Integer signStatus;
    //退还的时间
    private Date backTime;
    //尺寸的大小
    private String shelfSize;
}
