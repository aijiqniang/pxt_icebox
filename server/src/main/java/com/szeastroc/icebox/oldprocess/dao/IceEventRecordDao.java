package com.szeastroc.icebox.oldprocess.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.szeastroc.icebox.newprocess.vo.IceEventVo;
import com.szeastroc.icebox.oldprocess.entity.IceEventRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface IceEventRecordDao extends BaseMapper<IceEventRecord> {

    @Select("SELECT SUM(open_close_count)FROM `t_ice_event_record` WHERE asset_id=#{assetId}\n" +
            "AND occurrence_time BETWEEN #{startTime} AND #{endTime};")
    Integer sumOpenCloseCount(@Param("assetId") String assetId, @Param("startTime") Date startTime, @Param("endTime") Date endTime);

    @Select("SELECT SUM(open_close_count)FROM `t_ice_event_record` WHERE asset_id=#{assetId};")
    Integer sumTotalOpenCloseCount(@Param("assetId") String assetId);

    @Update("create TABLE  ${tn}\n" +
            "      (\n" +
            "        `id` bigint(20) NOT NULL AUTO_INCREMENT,\n" +
            "        `type` int(4) DEFAULT NULL COMMENT '推送事件类型 1.普通定时推送 2.温度变化  3.发生断点  4.GPS位置变化',\n" +
            "        `asset_id` varchar(64) COLLATE utf8mb4_german2_ci DEFAULT NULL COMMENT '资产id',\n" +
            "        `occurrence_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '事件发生时间',\n" +
            "        `temperature` double(11,2) DEFAULT NULL COMMENT '温度',\n" +
            "        `open_close_count` int(11) DEFAULT NULL COMMENT ' 开关门次数',\n" +
            "        `lng` varchar(64) COLLATE utf8mb4_german2_ci DEFAULT NULL COMMENT '经度',\n" +
            "        `lat` varchar(64) COLLATE utf8mb4_german2_ci DEFAULT NULL COMMENT '纬度',\n" +
            "        `detail_address` varchar(255) COLLATE utf8mb4_german2_ci DEFAULT NULL,\n" +
            "        `create_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,\n" +
            "        PRIMARY KEY (`id`) USING BTREE,\n" +
            "        KEY `asset_index` (`asset_id`) USING BTREE,\n" +
            "        KEY `occurrence_index` (`occurrence_time`) USING BTREE,\n" +
            "        KEY `type_index` (`type`) USING BTREE,\n" +
            "        KEY `asset_id` (`asset_id`,`occurrence_time`) USING BTREE\n" +
            "      ) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_german2_ci COMMENT='设备信息记录表';")
    void createTableMySelf(@Param("tn") String tn);

    List<IceEventVo.IceboxList> getIntelIceboxs(@Param("userId") Integer userId,@Param("assetId") String assetId);

}
