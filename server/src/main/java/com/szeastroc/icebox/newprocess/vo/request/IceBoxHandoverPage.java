package com.szeastroc.icebox.newprocess.vo.request;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

/**
 *
 *
 * @author Aijinqiang
 * @version 1.0
 * @date 2021/6/7 14:50
 */
@Data
public class IceBoxHandoverPage extends Page {
    /**
     *本部id
     */
    private Integer headquartersDeptId;
    /**
     *本部名称
     */
    private String headquartersDeptName;
    /**
     *事业部id
     */
    private Integer businessDeptId;
    /**
     *事业部名称
     */
    private String businessDeptName;
    /**
     *大区id
     */
    private Integer regionDeptId;
    /**
     *大区名称
     */
    private String regionDeptName;
    /**
     *服务处id
     */
    private Integer serviceDeptId;
    /**
     *服务处名称
     */
    private String serviceDeptName;
    /**
     *组id
     */
    private Integer groupDeptId;
    /**
     *组名称
     */
    private String groupDeptName;
    /**
     * 冰柜编号
     */
    private String iceBoxAssetid;

    /**
     * 冰柜状态
     */
    private Integer iceboxStatus;

    /**
     * 交接业务员name
     */
    private String sendUserName;

    /**
     * 接收业务员name
     */
    private String receiveUserName;

    /**
     *交接时间
     */
    private Date handoverTime;

    /**
     * 1交接中 2已交接 3已驳回
     */
    private Integer handoverStatus;

    private Date startTime;

    private Date endTime;
    /**
     * 操作类型
     */
    private Integer operateType;

    private Integer recordsId;

    private String operateName;
}
