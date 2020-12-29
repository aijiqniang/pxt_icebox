package com.szeastroc.icebox.newprocess.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.szeastroc.icebox.newprocess.entity.IceExamine;
import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.icebox.newprocess.vo.IceExamineVo;
import com.szeastroc.icebox.newprocess.vo.request.IceExamineRequest;

import java.util.List;
import java.util.Map;

public interface IceExamineService extends IService<IceExamine>{


    void doExamine(IceExamine iceExamine);

    IPage<IceExamineVo> findExamine(IceExamineRequest iceExamineRequest);

    IceExamineVo findOneExamine(IceExamineRequest iceExamineRequest);

    List<IceExamine> getInspectionBoxes(List<Integer> userIds);

    List<IceExamine> getInspectionBoxes(Integer userId);

    Integer getNoInspectionBoxes(Integer putCount, Integer userId);

    Map<String, Object> doExamineNew(IceExamineVo iceExamineVo);

    void dealIceExamineCheck(String redisKey, Integer status, Integer updateBy,String examineRemark);

    IceExamineVo findExamineByNumber(String examineNumber);

    void syncExamineDataToReport(List<Integer> ids);
}
