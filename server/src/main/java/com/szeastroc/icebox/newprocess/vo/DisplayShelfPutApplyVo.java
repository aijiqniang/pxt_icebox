package com.szeastroc.icebox.newprocess.vo;

import com.szeastroc.common.entity.visit.SessionExamineVo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * @ClassName: DisplayShelfPutApplyVo
 * @Description:
 * @Author: 陈超
 * @Date: 2021/6/3 15:56
 **/
@Data
@ApiModel
public class DisplayShelfPutApplyVo {

    @ApiModelProperty("申请编号")
    private String applyNumber;

    @ApiModelProperty("申请时间")
    private Date createTime;

    @ApiModelProperty("当前所在地")
    private String customerName;

    @ApiModelProperty("审批节点")
    private List<SessionExamineVo.VisitExamineNodeVo> visitExamineNodes;

    @ApiModelProperty("货架信息")
    private List<SupplierDisplayShelfVO> shelfList;
}
