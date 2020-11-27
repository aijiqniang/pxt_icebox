package com.szeastroc.icebox.newprocess.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.entity.IceBoxPutReport;
import com.szeastroc.icebox.newprocess.vo.request.IceBoxPage;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface IceBoxPutReportDao extends BaseMapper<IceBoxPutReport> {

    Integer selectByExportCount(@Param(Constants.WRAPPER) LambdaQueryWrapper<IceBoxPutReport> wrapper);
}