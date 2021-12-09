package com.szeastroc.icebox.config;

import com.google.common.collect.Range;
import org.apache.shardingsphere.api.sharding.standard.RangeShardingAlgorithm;
import org.apache.shardingsphere.api.sharding.standard.RangeShardingValue;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

/**
 * TODO
 *
 * @author Aijinqiang
 * @version 1.0
 * @date 2021/10/11 16:52
 */
public class EventShardingRangeAlgorithm implements RangeShardingAlgorithm<Date> {

    @Override
    public Collection<String> doSharding(Collection<String> collection, RangeShardingValue<Date> rangeShardingValue) {
        Collection<String> tabs = new ArrayList<>();
        Range<Date> timeRange = rangeShardingValue.getValueRange();

        Date startTime = timeRange.lowerEndpoint();
        Date endTime = timeRange.upperEndpoint();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startTime);

        while(calendar.getTime().before(endTime)){
            //倒序时间,顺序after改before其他相应的改动。
            try{
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                tabs.add("t_ice_event_record_"+sdf.format(calendar.getTime()));
                calendar.add(Calendar.DAY_OF_MONTH, 1);
            } catch(Exception e){
                e.printStackTrace();
            }
        }
        return tabs;
    }
}
