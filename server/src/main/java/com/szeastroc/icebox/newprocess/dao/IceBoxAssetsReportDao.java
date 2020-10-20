package com.szeastroc.icebox.newprocess.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.szeastroc.icebox.newprocess.entity.IceBoxAssetsReport;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * @Author xiao
 * @Date create in 2020/10/19 14:35
 * @Description:
 */
public interface IceBoxAssetsReportDao extends BaseMapper<IceBoxAssetsReport> {

    @Select("SELECT * FROM `t_ice_box_assets_report` WHERE supp_number=#{suppNumber} and xing_hao_id=#{modelId};")
    IceBoxAssetsReport readBySuppNumberAndModelId(@Param("suppNumber") String suppNumber,@Param("modelId") Integer modelId);

}
