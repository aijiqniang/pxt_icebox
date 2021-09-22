package com.szeastroc.icebox.config;

import org.apache.shardingsphere.api.sharding.standard.PreciseShardingAlgorithm;
import org.apache.shardingsphere.api.sharding.standard.PreciseShardingValue;

import java.util.Collection;
import java.util.Date;

/**
 * TODO
 *
 * @author Aijinqiang
 * @version 1.0
 * @date 2021/9/18 16:27
 */
public class EventShardingTableAlgorithm implements PreciseShardingAlgorithm<Date> {
    @Override
    public String doSharding(Collection<String> collection, PreciseShardingValue<Date> preciseShardingValue) {
        // 根据当前日期来分表
        Date date = preciseShardingValue.getValue();
        String year = String.format("%tY", date);
        String mon =String.valueOf(Integer.parseInt(String.format("%tm", date))); // 去掉前缀0
        //String mon =String.format("%tm", date);
        String dat = String.format("%td", date);
        String tableName = "t_ice_event_record"+"_" + year + mon + dat;
        System.out.println("tb_name:" + tableName);
        return tableName;
    }
}
