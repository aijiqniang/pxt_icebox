package com.szeastroc.icebox.newprocess.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.szeastroc.icebox.newprocess.entity.IceBoxExamineExceptionReport;
import com.szeastroc.icebox.newprocess.entity.IceBoxPutReport;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IceBoxExamineExceptionReportDao extends BaseMapper<IceBoxExamineExceptionReport> {

    Integer selectByExportCount(@Param(Constants.WRAPPER) LambdaQueryWrapper<IceBoxExamineExceptionReport> wrapper);
}