package com.szeastroc.icebox.newprocess.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.vo.request.IceBoxPage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface IceBoxDao extends BaseMapper<IceBox> {

    List<IceBox> findPage(IceBoxPage iceBoxPage);

    List<IceBox> exportExcel(Map<String, Object> param);

    Integer exportExcelCount(Map<String, Object> param);

    @Select("SELECT * FROM `t_ice_box` LIMIT #{pageCode},#{pageNum};")
    List<IceBox> readLimitData(@Param("pageCode") Integer pageCode, @Param("pageNum") Integer pageNum);

}