package com.szeastroc.icebox.newprocess.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.vo.request.IceBoxPage;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IceBoxDao extends BaseMapper<IceBox> {

    List<IceBox> findPage(IceBoxPage iceBoxPage);

}