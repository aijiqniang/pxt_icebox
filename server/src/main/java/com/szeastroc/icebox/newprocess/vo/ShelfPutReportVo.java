package com.szeastroc.icebox.newprocess.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class ShelfPutReportVo {

    //本部id
    private Integer headquartersDeptId;
    //本部名称
    private String headquartersDeptName;
    //事业部id
    private Integer businessDeptId;
    //事业部名称
    private String businessDeptName;
    //大区id
    private Integer regionDeptId;
    //大区名称
    private String regionDeptName;
    //服务处id
    private Integer serviceDeptId;
    //服务处名称
    private String serviceDeptName;
    //组id
    private Integer groupDeptId;
    //组名称
    private String groupDeptName;
    //申请编号
    private String applyNumber;

    //提交日期
    private Date submitTime;

    private String shNumber;
    //投放客户类型
    private Integer putCustomerType;

    //省份
    private String provinceName;
    //城市
    private String cityName;
    //区县
    private String districtName;
    //客户地址
    private String customerAddress;
    //拜访频率
    private String visitTypeName;
    //联系人
    private String linkmanName;
    //联系人电话
    private String linkmanMobile;
    //投放客户等级
    private String putCustomerLevel;

    private Date createTime;

    private Date updateTime;

    private String putRemark;

    private String name;
    private String size;
    private String putNumber;
    private String putName;
    private Integer signCount;
    private Integer putCount;
    private Integer scrapCount;
    private Integer loseCount;
}
