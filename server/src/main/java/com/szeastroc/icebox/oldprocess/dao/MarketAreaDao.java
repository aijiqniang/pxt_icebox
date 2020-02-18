package com.szeastroc.icebox.oldprocess.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.szeastroc.icebox.oldprocess.entity.MarketArea;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * Created by Tulane
 * 2019/5/28
 */
public interface MarketAreaDao extends BaseMapper<MarketArea>{

    @Update("update t_market_area set id = #{newId} where id = #{oldId}")
    int updateNewKey(@Param("oldId") Integer oldId, @Param("newId") Integer newId);
}
