package com.szeastroc.icebox.newprocess.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.szeastroc.icebox.newprocess.entity.IceExamine;
import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.icebox.newprocess.vo.IceExamineVo;
import com.szeastroc.icebox.newprocess.vo.request.IceExamineRequest;

import java.util.List;

public interface IceExamineService extends IService<IceExamine>{


    void doExamine(IceExamine iceExamine);

    IPage<IceExamineVo> findExamine(IceExamineRequest iceExamineRequest);

    IceExamineVo findOneExamine(IceExamineRequest iceExamineRequest);

    Integer inspectionCount(List<Integer> userIds);

}
