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

    @Select("SELECT * FROM `t_ice_box_assets_report` WHERE asset_id=#{assetId};")
    IceBoxAssetsReport readByAssetId(@Param("assetId") String assetId);

}
