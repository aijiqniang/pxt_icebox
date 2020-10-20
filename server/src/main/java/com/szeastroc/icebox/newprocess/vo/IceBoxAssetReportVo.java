package com.szeastroc.icebox.newprocess.vo;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * @Author xiao
 * @Date create in 2020/10/20 9:50
 * @Description:
 */
@Getter
@Setter
@Builder
public class IceBoxAssetReportVo {

    private String suppName; // 经销商名称
    private String suppNumber; // 经销商编号
    private String modelName; // 型号名称
    private Integer modelId; // 型号id
    private Integer oldPutStatus; // 旧的冰柜投放状态
    private Integer oldStatus; // 旧的冰柜状态
    private Integer newPutStatus; // 新的冰柜投放状态
    private Integer newStatus; // 新的冰柜状态
    private String assetId; // 资产id

}
