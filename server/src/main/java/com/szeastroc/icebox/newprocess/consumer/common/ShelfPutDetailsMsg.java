package com.szeastroc.icebox.newprocess.consumer.common;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.szeastroc.icebox.newprocess.entity.DisplayShelf;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;


@Getter
@Setter
@Accessors(chain = true)
public class ShelfPutDetailsMsg implements Serializable {
    private static final long serialVersionUID = 1L;
    private LambdaQueryWrapper<DisplayShelf> shelfLambdaQueryWrapper;
    private List<DisplayShelf> displayShelfList;
    private Integer userId;
    private String realName;
    private String serialNum;
    private Integer labelId;

    /**
     * 下载任务id
     */
    private Integer recordsId;
    //deptType  1:服务处 2:大区 3:事业部 4:本部 5:组
    @ApiModelProperty(value = "部门类型")
    private Integer deptType;
    @ApiModelProperty(value = "营销区域")
    private Integer marketAreaId;
    @ApiModelProperty(value = "货架类型")
    private String shelfType;
}