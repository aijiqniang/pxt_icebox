package com.szeastroc.icebox.newprocess.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "t_ice_box_transfer_history")
public class IceBoxTransferHistory {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 本部id
     */
    private Integer headquartersDeptId;
    /**
     * 本部名称
     */
    private String headquartersDeptName;
    /**
     * 事业部id
     */
    private Integer businessDeptId;
    /**
     * 事业部名称
     */
    private String businessDeptName;
    /**
     * 大区id
     */
    private Integer regionDeptId;
    /**
     * 大区名称
     */
    private String regionDeptName;
    /**
     * 服务处id
     */
    private Integer serviceDeptId;
    /**
     * 服务处名称
     */
    private String serviceDeptName;
    /**
     * 组id
     */
    private Integer groupDeptId;
    /**
     * 组名称
     */
    private String groupDeptName;
    
    /**
     * 转移批号
     */
    private String transferNumber;
    /**
     * 转移前经销商id
     */
    private Integer oldSupplierId;
    /**
     * 转移前经销商名称
     */
    private String oldSupplierName;

    /**
     * 转移前经销商编号
     */
    private String oldSupplierNumber;
    /**
     * 转移前经销商营销区域
     */
    private Integer oldMarketAreaId;
    /**
     * 转移后经销商id
     */
    private Integer newSupplierId;
    /**
     * 转移后经销商名称
     */
    private String newSupplierName;

    /**
     * 转移后经销商编号
     */
    private String newSupplierNumber;
    /**
     * 转移后经销商营销区域
     */
    private Integer newMarketAreaId;
    /**
     * 冰柜id
     */
    private Integer iceBoxId;
    /**
     * 审批状态：0-未审核，1-审核中，2-通过，3-驳回
     */
    private Integer examineStatus;
    /**
     * 申请人
     */
    private Integer createBy;
    /**
     * 申请人
     */
    private String createByName;
    /**
     * 申请时间
     */
    private Date createTime;
    /**
     * 更新时间
     */
    private Date updateTime;


    /**
     * 是否审批：0-不审批，1-审批
     */
    private Integer isCheck;

    /**
     * 资产编号
     */
    private String assetId;

    /**
     * 审核人id
     */
    private Integer reviewerId;

    /**
     * 审核人姓名
     */
    private Integer reviewerName;
}