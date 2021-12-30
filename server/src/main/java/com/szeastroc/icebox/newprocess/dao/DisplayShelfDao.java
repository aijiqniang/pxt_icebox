package com.szeastroc.icebox.newprocess.dao;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.szeastroc.icebox.newprocess.consumer.common.ShelfPutDetailsMsg;
import com.szeastroc.icebox.newprocess.entity.DisplayShelf;
import com.szeastroc.icebox.newprocess.entity.DisplayShelfPutReport;
import com.szeastroc.icebox.newprocess.vo.request.DisplayShelfPage;
import org.apache.ibatis.annotations.Param;
import org.apache.shiro.crypto.hash.Hash;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * (DisplayShelf)表数据库访问层
 *
 * @author chenchao
 * @since 2021-05-28 09:36:31
 */
@Repository
public interface DisplayShelfDao extends BaseMapper<DisplayShelf> {

    IPage<DisplayShelf> selectDetailsPage(DisplayShelfPage page);
    //陈列架投放报表分页查询
    IPage<DisplayShelf> selectReportDetailsPage(DisplayShelfPage page);

    List<DisplayShelf> selectDetails(ShelfPutDetailsMsg shelfPutDetailsMsg);

    IPage<DisplayShelf> selectPage(DisplayShelfPage page);

    List<DisplayShelf.DisplayShelfType> selectType(@Param("supplierNumber") String supplierNumber);

    List<DisplayShelf> noPutShelves(@Param("serviceId") Integer serviceId, @Param("typeArr") String[] typeArr);

    List<DisplayShelf.DisplayShelfType> typeCount(@Param("customerNumber") String customerNumber);

    List<DisplayShelf.DisplayShelfType> customerDetail(@Param("customerNumber") String customerNumber);

    Integer selectByExportCount(@Param(Constants.WRAPPER) LambdaQueryWrapper<DisplayShelf> wrapper);

    List<DisplayShelf> InspectCount(@Param("applyNumber") String applyNumber);
    /**
     * 导出记录
     */
    void insertExportRecords(@Param("serialNum") String serialNum, @Param("jobName") String jobName,
                             @Param("userId") Integer userId, @Param("userName") String userName,
                             @Param("type") Byte type, @Param("requestTime") Date requestTime, @Param("param") String param);

//    Integer selectByExportCount(@Param(Constants.WRAPPER) Wrapper wrapper);
    void updateExportRecords(@Param("serialNum") String serialNum, @Param("netPath") String netPath, @Param("endRequestTime") Date endRequestTime);

}
