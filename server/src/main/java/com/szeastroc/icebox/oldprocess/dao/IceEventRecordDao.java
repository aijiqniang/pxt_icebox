package com.szeastroc.icebox.oldprocess.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.szeastroc.icebox.oldprocess.entity.IceEventRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.Date;

@Repository
public interface IceEventRecordDao extends BaseMapper<IceEventRecord> {

    @Select("SELECT SUM(open_close_count)FROM `t_ice_event_record` WHERE asset_id=#{assetId}\n" +
            "AND occurrence_time BETWEEN #{startTime} AND #{endTime};")
    Integer sumOpenCloseCount(@Param("assetId") String assetId, @Param("startTime") Date startTime, @Param("endTime") Date endTime);

    @Select("SELECT SUM(open_close_count)FROM `t_ice_event_record` WHERE asset_id=#{assetId};")
    Integer sumTotalOpenCloseCount(@Param("assetId") String assetId);

}
