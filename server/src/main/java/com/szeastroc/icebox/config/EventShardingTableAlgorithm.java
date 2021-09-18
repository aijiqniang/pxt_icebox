package com.szeastroc.icebox.config;

import org.apache.shardingsphere.api.sharding.standard.PreciseShardingAlgorithm;
import org.apache.shardingsphere.api.sharding.standard.PreciseShardingValue;

import java.util.Collection;

/**
 * TODO
 *
 * @author Aijinqiang
 * @version 1.0
 * @date 2021/9/18 16:27
 */
public class EventShardingTableAlgorithm implements PreciseShardingAlgorithm<Integer> {
    @Override
    public String doSharding(Collection<String> collection, PreciseShardingValue<Integer> preciseShardingValue) {
        return null;
    }
}
