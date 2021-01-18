package com.szeastroc.icebox.newprocess.vo;

import com.szeastroc.icebox.newprocess.entity.IceRepairOrder;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @ClassName: IceRepairOrderVO
 * @Description:
 * @Author: 陈超
 * @Date: 2021/1/18 9:21
 **/
@Data
@ApiModel
public class IceRepairOrderVO extends IceRepairOrder{

    @ApiModelProperty(value = "冰箱名称")
    private String chestName;
    @ApiModelProperty(value = "冰箱规格")
    private String chestNorm;
    @ApiModelProperty(value = "冰箱品牌")
    private String brandName;

}
