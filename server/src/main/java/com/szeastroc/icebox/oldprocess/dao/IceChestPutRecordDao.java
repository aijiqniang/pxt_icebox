package com.szeastroc.icebox.oldprocess.dao;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.szeastroc.icebox.oldprocess.entity.IceChestPutRecord;
import com.szeastroc.icebox.oldprocess.vo.query.IceDepositPage;
import org.apache.ibatis.annotations.Param;

public interface IceChestPutRecordDao extends BaseMapper<IceChestPutRecord> {

    IPage<IceChestPutRecord> customSelectPage(Page<IceChestPutRecord> page,
                                              @Param(Constants.WRAPPER)Wrapper wrapper,
                                              @Param("query")IceDepositPage iceDepositPage);
}