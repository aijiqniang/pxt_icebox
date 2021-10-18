package com.szeastroc.icebox.config;


import com.szeastroc.commondb.config.mybatis.Datasources;
import org.apache.shardingsphere.api.config.masterslave.MasterSlaveRuleConfiguration;
import org.apache.shardingsphere.api.config.sharding.ShardingRuleConfiguration;
import org.apache.shardingsphere.api.config.sharding.TableRuleConfiguration;
import org.apache.shardingsphere.api.config.sharding.strategy.StandardShardingStrategyConfiguration;
import org.apache.shardingsphere.shardingjdbc.api.ShardingDataSourceFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.*;

@Configuration
public class ShardingDataSourceConfig {

    @Resource
    @Qualifier(Datasources.MASTER_DB)
    private DataSource masterDB;

    @Resource
    @Qualifier(Datasources.SLAVE_DB)
    private DataSource slaveDB;

    @Bean(name = "shardingDataSource")
    public DataSource shardingDataSource() throws SQLException {
        // 配置真实数据源
        Map<String, DataSource> dataSourceMap = new HashMap<>();


        // 配置主库
        dataSourceMap.put("ds_master", masterDB);

        // 配置第一个从库
        dataSourceMap.put("ds_slave0", masterDB);

        // 配置读写分离规则
        MasterSlaveRuleConfiguration masterSlaveRuleConfig = new MasterSlaveRuleConfiguration("ds_master_slave", "ds_master", Arrays.asList("ds_slave0"));
        List<MasterSlaveRuleConfiguration> masterSlaveRuleConfigs = new ArrayList<>();
        masterSlaveRuleConfigs.add(masterSlaveRuleConfig);

        // 配置分片规则
        ShardingRuleConfiguration shardingRuleConfig = new ShardingRuleConfiguration();

        shardingRuleConfig.setBindingTableGroups(Arrays.asList("t_ice_event_record"));
        //shardingRuleConfig.getTableRuleConfigs().addAll(Arrays.asList(getTaskPointTableRuleConfiguration(),getIncentiveTaskIndexUserTableRuleConfiguration(),getIncentiveIndexCompleteTableRuleConfiguration(),getIncentiveTaskIndexUserDetailTableRuleConfiguration()));
        shardingRuleConfig.getTableRuleConfigs().addAll(Arrays.asList((getEventRuleConfiguration())));
        shardingRuleConfig.setMasterSlaveRuleConfigs(masterSlaveRuleConfigs);
        // 获取数据源对象
        DataSource dataSource = ShardingDataSourceFactory.createDataSource(dataSourceMap, shardingRuleConfig, new Properties());

        return dataSource;
    }

    @Bean
    public TableRuleConfiguration getEventRuleConfiguration() {
        TableRuleConfiguration result = new TableRuleConfiguration("t_ice_event_record","ds_master_slave.t_ice_event_record_${2021..2030}${1..12}${1..31}");
        StandardShardingStrategyConfiguration standardStrategy = new StandardShardingStrategyConfiguration("occurrence_time",new EventShardingTableAlgorithm(),new EventShardingRangeAlgorithm());
        result.setTableShardingStrategyConfig(standardStrategy);
        return result;
    }

    @Primary
    @Bean(name = "shardingTransactionManager")
    public DataSourceTransactionManager transactionManager() throws SQLException {
        return new DataSourceTransactionManager(shardingDataSource());
    }
}
