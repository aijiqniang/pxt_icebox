package com.szeastroc.icebox.vo.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.szeastroc.icebox.entity.IceChestPutRecord;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
public class IceDepositPage extends Page {

    // 客户编号
    private String clientNumber;
    // 客户名称
    private String clientName;
    // 联系电话
    private String contactMobile;

    // 设备型号
    private String chestModel;
    // 资产编号
    private String assetId;

    // 服务处
    private Integer marketAreaId;

    // 查询: 支付开始时间
    private String payStartTime;
    // 查询: 支付结束时间
    private String payEndTime;

}
