package com.szeastroc.icebox.dao;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.szeastroc.icebox.entity.IceChestPutRecord;
import com.szeastroc.icebox.vo.query.IceDepositPage;
import org.apache.ibatis.annotations.Param;

public interface IceChestPutRecordDao extends BaseMapper<IceChestPutRecord> {

    IPage<IceChestPutRecord> customSelectPage(Page<IceChestPutRecord> page,
                                              @Param(Constants.WRAPPER)Wrapper wrapper,
                                              @Param("query")IceDepositPage iceDepositPage);
}