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
     *转移后经销商id
     */
    private Integer newSupplierId;
    /**
     *转移后经销商名称
     */
    private String newSupplierName;
    /**
     *冰柜id
     */
    private Integer iceBoxId;
    /**
     *申请人
     */
    private Integer createBy;
    /**
     *申请人
     */
    private String createByName;
    /**
     *申请时间
     */
    private Date createTime;
}