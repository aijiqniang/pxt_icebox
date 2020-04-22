package com.szeastroc.icebox.newprocess.vo.request;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @Author xiao
 * @Date create in 2020/4/21 16:59
 * @Description:
 */
@Getter
@Setter
public class IceBoxPage extends Page {

    private Integer deptId; // 营销区域id
    private String bluetoothId; // 设备编号(蓝牙设备ID)
    private String belongObjNumber; // 所在对象编号
    private String belongObjName; // 所在对象名称
    private Integer status; // 设备状态 (冰柜状态 1:正常 0:异常)
    private Integer belongObj; // 所在对象  (put_status  投放状态 0: 未投放 1:已锁定(被业务员申请) 2:投放中 3:已投放; 当经销商时为 0-未投放;当门店时为非未投放状态;)
    private List<Integer> deptIds; // 营销区域id集合
    private List<Integer> supplierIdList; // 拥有者的经销商
    private List<String> putStoreNumberList; // 投放的门店number

}
