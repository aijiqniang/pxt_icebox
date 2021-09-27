package com.szeastroc.icebox.newprocess.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.common.entity.icebox.vo.IceBoxRequest;
import com.szeastroc.common.entity.visit.SessionExamineVo;
import com.szeastroc.common.entity.visit.ShelfBackModel;
import com.szeastroc.icebox.newprocess.entity.DisplayShelfBackApply;
import com.szeastroc.icebox.newprocess.entity.ShelfBack;

import java.util.List;

public interface DisplayShelfBackApplyService extends IService<DisplayShelfBackApply> {

    List<SessionExamineVo.VisitExamineNodeVo> shelfBack(ShelfBackModel model);

    void updateBackStatus(IceBoxRequest request);

    void shelfBacklog(ShelfBackModel model);

    List<ShelfBack> shelfBackDetails(String uuid);
}
