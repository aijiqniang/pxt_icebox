package com.szeastroc.icebox.newprocess.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.szeastroc.icebox.newprocess.entity.ExportRecords;
import com.szeastroc.icebox.newprocess.entity.IceBoxPutReport;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;

/**
 * Created by hbl
 * 2019/8/21 0021 17:17
 */
@Repository
public interface ExportRecordsDao extends BaseMapper<ExportRecords> {

    /**
     * 导出记录
     */
    void insertExportRecords(@Param("serialNum") String serialNum, @Param("jobName") String jobName,
                             @Param("userId") Integer userId, @Param("userName") String userName,
                             @Param("type") Integer type, @Param("requestTime") Date requestTime);

    void updateExportRecords(@Param("serialNum") String serialNum, @Param("netPath") String netPath, @Param("endRequestTime") Date endRequestTime);

}
