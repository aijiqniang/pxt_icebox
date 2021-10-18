package com.szeastroc.icebox.newprocess.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.szeastroc.icebox.newprocess.entity.IceAlarm;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.units.qual.A;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * TODO
 *
 * @author Aijinqiang
 * @version 1.0
 * @date 2021/9/24 15:50
 */
@Data
public class IceEventVo {


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IceboxList{
        @ApiModelProperty("冰柜id")
        Integer iceboxId;
        @ApiModelProperty("智能冰柜编号")
        String externalId;
        @ApiModelProperty("冰柜型号")
        String chestModel;
        @ApiModelProperty("冰柜名称")
        String chestName;
        @ApiModelProperty("投放时间")
        Date lastPutTime;
        @ApiModelProperty("保修起算时间")
        Date repairBeginTime;
        @ApiModelProperty("生产时间")
        Date createTime;
        @ApiModelProperty("押金")
        BigDecimal depositMoney;
        @ApiModelProperty("冰柜状态 0:异常，1:正常，2:报废，3:遗失，4:报修")
        Integer status;
        @ApiModelProperty("冰柜资产编号")
        String assetId;
        @ApiModelProperty("报警信息")
        List<IceAlarm> alarmList;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IceboxDetail{
        String storeName;
        String address;
        Double distance;



    }
}
