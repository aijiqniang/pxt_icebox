package com.szeastroc.icebox.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class IceBoxTransferHistoryVo {

    /**
     *转移批号
     */
    private String transferNumber;
    /**
     *转移前经销商id
     */
    private Integer oldSupplierId;
    /**
     *转移前经销商名称
     */
    private String oldSupplierName;
    /**
     *转移前经销商营销区域id
     */
    private Integer oldMarketAreaId;
    /**
     *转移后经销商id
     */
    private Integer newSupplierId;
    /**
     *转移后经销商名称
     */
    private String newSupplierName;
    /**
     *转移后经销商营销区域id
     */
    private Integer newMarketAreaId;
    /**
     *冰柜id
     */
    private List<Integer> iceBoxIds;
    /**
     *审批状态：0-未审核，1-审核中，2-通过，3-驳回
     */
    private Integer examineStatus;
    /**
     *申请人
     */
    private Integer createBy;
    /**
     *申请人姓名
     */
    private String createByName;
    /**
     *申请时间
     */
    private Date createTime;
}