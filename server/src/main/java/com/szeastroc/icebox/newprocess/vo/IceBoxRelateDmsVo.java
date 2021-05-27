package com.szeastroc.icebox.newprocess.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import java.math.BigDecimal;
import java.util.Date;

/**
 *
 *
 * @author Aijinqiang
 * @version 1.0
 * @date 2021/5/11 10:17
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IceBoxRelateDmsVo {

    /**
     *
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 1投放 2退还
     */
    @ApiModelProperty(value ="配送类型 1投放 2退还")
    private Integer type;

    /**
     * 投放/退还编号
     */
    @ApiModelProperty(value ="投放/退还编号")
    private String relateNumber;

    /**
     *
     */
    @ApiModelProperty(value ="putStoreRelateModelId")
    private Integer putStoreRelateModelId;

    /**
     * 冰柜id
     */
    @ApiModelProperty(value ="冰柜id")
    private Integer iceBoxId;

    /**
     * 0 旧冰柜 1冰柜
     */
    @ApiModelProperty(value ="0 旧冰柜 1冰柜")
    private Integer iceBoxType;

    /**
     * 冰柜assetid
     */
    @ApiModelProperty(value ="冰柜assetid")
    private String iceBoxAssetId;

    /**
     * 当前定位
     */
    @ApiModelProperty(value ="当前定位")
    private String currentPlace;

    /**
     * 预计送达时间
     */
    @ApiModelProperty(value = "预计送达时间")
    private Date expectArrviedDate;

    /**
     * 储运备注
     */
    @ApiModelProperty(value = "储运备注")
    private String remark;

    /**
     * 店招照片
     */
    @ApiModelProperty(value = "店招照片")
    private String photo;

    /**
     * 审批备注
     */
    @ApiModelProperty(value = "审批备注")
    private String examineRemark;

    /**
     * 审批流id
     */
    @ApiModelProperty(value = "审批流id")
    private Integer examineId;

    /**
     * 投放门店信息
     */
    @ApiModelProperty(value = "投放门店信息")
    private String putStoreNumber;

    /**
     * 供应商id
     */
    @ApiModelProperty(value = "供应商id")
    private Integer supplierId;

    /**
     * 冰柜型号id
     */
    @ApiModelProperty(value = "冰柜型号id")
    private Integer modelId;

    /**
     * 2投放中 6配送中  7待签收
     */
    @ApiModelProperty(value = "2投放中 6配送中  7待签收")
    private Integer putstatus;

    /**
     *
     */
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /**
     *
     */
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    /**
     * 设备名称
     */
    @ApiModelProperty(value = "设备名称")
    private String chestName;
    /**
     * 品牌
     */
    @ApiModelProperty(value = "品牌")
    private String brandName;
    /**
     * 冰柜规格
     */
    @ApiModelProperty(value = "冰柜规格")
    private String chestNorm;
    /**
     * 冰柜型号名
     */
    @ApiModelProperty(value = "冰柜型号名")
    private String modelName;
    /**
     * 投放方式
     */
    @ApiModelProperty(value = "投放方式")
    private String putType;

    /**
     * 投放门店名
     */
    @ApiModelProperty(value = "投放门店名")
    private String putStoreName;

    /**
     * 门店信息地址
     */
    @ApiModelProperty(value = "门店信息地址")
    private String address;

    /**
     * 店主
     */
    @ApiModelProperty(value = "店主")
    private String shopkeeper;

    /**
     * 店主手机号
     */
    @ApiModelProperty(value = "店主手机号")
    private String shopkeeperPhoneNumber;

    /**
     * 业务员id
     */
    @ApiModelProperty(value = "业务员id")
    private Integer saleManId;
    /**
     * 业务员名称
     */
    @ApiModelProperty(value = "业务员名称")
    private String saleManName;
    /**
     * 业务员手机号
     */
    @ApiModelProperty(value = "业务员手机号")
    private String saleManPhoneNumber;
    /**
     * 免押类型：1-不免押，2-免押
     */
    @ApiModelProperty(value = "免押类型：1-不免押，2-免押")
    private Integer freeType;
    /**
     * 押金
     */
    @ApiModelProperty(value = "押金")
    private BigDecimal depositMoney;

    /**
     * 本部名称
     */
    @ApiModelProperty(value = "本部名称")
    private String headquartersDeptName;
    /**
     * 事业部名称
     */
    @ApiModelProperty(value = "事业部名称")
    private String businessDeptName;
    /**
     * 大区名称
     */
    @ApiModelProperty(value = "大区名称")
    private String regionDeptName;
    /**
     * 服务处名称
     */
    @ApiModelProperty(value = "服务处名称")
    private String serviceDeptName;
    /**
     * 组名称
     */
    @ApiModelProperty(value = "组名称")
    private String groupDeptName;

    /**
     * 接单时间
     */
    @ApiModelProperty(value = "接单时间")
    private Date acceptTime;

    /**
     * 送达时间
     */
    @ApiModelProperty(value = "送达时间")
    private Date arrviedTime;

    /**
     * 1退还中 2已退还 3已驳回  4已接单 5已收柜
     */
    @ApiModelProperty(value = "退还状态:1退还中 2已退还 3已驳回  4已接单 5已收柜")
    private Integer backstatus;

    @ApiModelProperty(value = "经度")
    private String longitude;

    @ApiModelProperty(value = "纬度")
    private String latitude;


}
