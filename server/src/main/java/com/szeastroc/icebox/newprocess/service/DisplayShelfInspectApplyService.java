package com.szeastroc.icebox.newprocess.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.common.entity.icebox.vo.ShelfInspectRequest;
import com.szeastroc.common.entity.visit.SessionExamineVo;
import com.szeastroc.common.entity.visit.ShelfInspectModel;
import com.szeastroc.icebox.newprocess.entity.DisplayShelfInspectApply;
import com.szeastroc.icebox.newprocess.vo.request.ShelfInspectPage;
import com.szeastroc.icebox.newprocess.vo.request.ShelfInspectVo;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 陈列架巡检(DisplayShelfInspectApply)表服务接口
 *
 * @author chenchao
 * @since 2021-06-07 14:41:15
 */
public interface DisplayShelfInspectApplyService extends IService<DisplayShelfInspectApply> {

    List<SessionExamineVo.VisitExamineNodeVo> shelfInspect(ShelfInspectModel model);

    List<SessionExamineVo.VisitExamineNodeVo> submitShelfInspectDetails(ShelfInspectModel model);

    void doInspect(ShelfInspectRequest request);

    IPage<DisplayShelfInspectApply> history(ShelfInspectPage page);

    List<ShelfInspectModel> submitted( String customerNumber);

    IPage<ShelfInspectVo> inspectHistory(ShelfInspectPage page);
}
