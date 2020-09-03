package com.szeastroc.icebox.newprocess.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class IceBoxTransferHistoryVo {
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
     *申请人
     */
    private Integer createBy;
    /**
     *申请人姓名
     */
    private String createByName;
}