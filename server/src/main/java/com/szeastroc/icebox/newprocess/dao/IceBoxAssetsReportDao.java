package com.szeastroc.icebox.newprocess.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.szeastroc.icebox.newprocess.entity.IceBoxAssetsReport;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * @Author xiao
 * @Date create in 2020/10/19 14:35
 * @Description:
 */
public interface IceBoxAssetsReportDao extends BaseMapper<IceBoxAssetsReport> {

    @Select("SELECT * FROM `t_ice_box_assets_report` WHERE supp_id=#{suppId} and xing_hao_id=#{modelId};")
    IceBoxAssetsReport readBySuppIdAndModelId(@Param("suppId") Integer suppId,@Param("modelId") Integer modelId);

    @Select("SELECT service_dept_name,service_dept_id,xing_hao,SUM(yi_tou)yiTou,SUM(zai_cang)zaiCang,SUM(yi_shi)yiShi,SUM(bao_fei)baoFei\n" +
            "FROM `t_ice_box_assets_report` WHERE region_dept_id=#{deptId}\n" +
            "GROUP BY service_dept_id,xing_hao;")
    List<Map<String ,Object>>readReportDqzj(@Param("deptId") Integer deptId);

}
