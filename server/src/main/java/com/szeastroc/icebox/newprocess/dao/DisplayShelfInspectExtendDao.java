package com.szeastroc.icebox.newprocess.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.szeastroc.icebox.newprocess.entity.DisplayShelfInspectExtend;
import com.szeastroc.icebox.newprocess.vo.request.ShelfInspectPage;
import com.szeastroc.icebox.newprocess.vo.request.ShelfInspectVo;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DisplayShelfInspectExtendDao extends BaseMapper<DisplayShelfInspectExtend> {
    List<ShelfInspectVo> selectHistoryPage(ShelfInspectPage shelfInspectPage);
}
