package com.szeastroc.icebox.newprocess.consumer.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class IceBackApplyReportMsg extends Page implements Serializable {

    private static final long serialVersionUID = -4750978713271531956L;
    /**
     * 下载任务id
     */
    private Integer recordsId;
    /**
     *本部id
     */
    private Integer headquartersDeptId;
    /**
     *事业部id
     */
    private Integer businessDeptId;
    /**
     *大区id
     */
    private Integer regionDeptId;
    /**
     *服务处id
     */
    private Integer serviceDeptId;
    /**
     *组id
     */
    private Integer groupDeptId;
    /**
     *申请编号
     */
    private String applyNumber;
    /**
     * 所属经销商名称
     */
    private String dealerName;
    /**
     *所属经销商编号
     */
    private String dealerNumber;
    /**
     * 退回客户编号
     */
    private String backCustomerNumber;
    /**
     * 退回客户名称
     */
    private String backCustomerName;
    /**
     * 资产编号
     */
    private String assetId;
    /**
     * 开始时间
     */
    private String startTime;
    /**
     * 结束时间
     */
    private String endTime;
    /**
     * 退还状态
     */
    private Integer examineStatus;

}
