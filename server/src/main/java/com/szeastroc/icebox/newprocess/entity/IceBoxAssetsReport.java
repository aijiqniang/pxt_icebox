package com.szeastroc.icebox.newprocess.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Date;

@Getter
@Setter
@Accessors(chain = true)
@TableName(value = "t_ice_box_assets_report")
public class IceBoxAssetsReport {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private String suppName;
    private String suppNumber;
    private Integer xingHaoId;
    private String xingHao; // 型号
    private Integer fenPeiLiang; // 分配量
    private Integer yiTou; // 已投
    private Integer zaiCang; // 在仓
    private Integer yiShi; // 遗失
    private Integer baoFei; // 报废
    private Date createdTime;
    private Date updatedTime;
    private Integer headquartersDeptId; // 本部
    private String headquartersDeptName; // 本都
    private Integer businessDeptId; // 事业部
    private String businessDeptName; // 事业部
    private Integer regionDeptId; // 大区
    private String regionDeptName; // 大区
    private Integer serviceDeptId; // 服务处
    private String serviceDeptName; // 服务处
    private Integer groupDeptId; // 组
    private String groupDeptName; // 组

}