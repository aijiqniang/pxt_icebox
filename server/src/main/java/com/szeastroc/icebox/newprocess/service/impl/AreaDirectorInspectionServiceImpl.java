package com.szeastroc.icebox.newprocess.service.impl;

import cn.hutool.core.util.NumberUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Lists;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.icebox.newprocess.entity.IceBoxExamineExceptionReport;
import com.szeastroc.icebox.newprocess.entity.IceBoxPutReport;
import com.szeastroc.icebox.newprocess.factory.InspectionServiceFactory;
import com.szeastroc.icebox.newprocess.service.IceBoxExamineExceptionReportService;
import com.szeastroc.icebox.newprocess.service.IceBoxPutReportService;
import com.szeastroc.icebox.newprocess.service.IcePutApplyService;
import com.szeastroc.icebox.newprocess.service.InspectionService;
import com.szeastroc.icebox.newprocess.vo.InspectionReportVO;
import com.szeastroc.user.client.FeignDeptClient;
import com.szeastroc.user.common.vo.SessionDeptInfoVo;
import org.joda.time.DateTime;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @ClassName: AreaDirectorInspectionImpl
 * @Description: 大区总巡检
 * @Author: 陈超
 * @Date: 2020/10/27 15:16
 **/
@Service
public class AreaDirectorInspectionServiceImpl implements InspectionService, InitializingBean {

    @Autowired
    FeignDeptClient feignDeptClient;
    @Autowired
    private IceBoxPutReportService iceBoxPutReportService;
    @Autowired
    private IceBoxExamineExceptionReportService iceBoxExamineExceptionReportService;
    @Autowired
    private IcePutApplyService icePutApplyService;

    @Override
    public List<InspectionReportVO> report(Integer deptId) {
        List<InspectionReportVO> list = Lists.newArrayList();
        List<SessionDeptInfoVo> childDepts = FeignResponseUtil.getFeignData(feignDeptClient.findNormalChildDeptInfosByParentId(deptId));
        for (SessionDeptInfoVo childDept : childDepts) {
            Integer id = childDept.getId();
            LambdaQueryWrapper<IceBoxPutReport> wrapper = Wrappers.<IceBoxPutReport>lambdaQuery();
            LambdaQueryWrapper<IceBoxExamineExceptionReport> exceptionReportWrapper = Wrappers.<IceBoxExamineExceptionReport>lambdaQuery();
            wrapper.eq(IceBoxPutReport::getServiceDeptId, id);
            exceptionReportWrapper.eq(IceBoxExamineExceptionReport::getServiceDeptId, id);
            String firstDay = new DateTime().dayOfMonth().withMinimumValue().toString("yyyy-MM-dd");
            String lastDay = new DateTime().dayOfMonth().withMaximumValue().toString("yyyy-MM-dd");
            wrapper.ge(IceBoxPutReport::getSubmitTime, firstDay + " 00:00:00");
            wrapper.le(IceBoxPutReport::getSubmitTime, lastDay + " 23:59:59");
            Integer putCount = iceBoxPutReportService.selectByExportCount(wrapper);

            exceptionReportWrapper.ge(IceBoxExamineExceptionReport::getSubmitTime, firstDay + " 00:00:00");
            exceptionReportWrapper.le(IceBoxExamineExceptionReport::getSubmitTime, lastDay + " 23:59:59");
            Integer inspectionCount = iceBoxExamineExceptionReportService.selectByExportCount(exceptionReportWrapper);
            List<SessionDeptInfoVo> groups = FeignResponseUtil.getFeignData(feignDeptClient.findNormalChildDeptInfosByParentId(id));
            int lostCount = icePutApplyService.getLostCountByDeptIds(groups.stream().map(SessionDeptInfoVo::getId).collect(Collectors.toList()));
            String percent = "-";
            if(0!=putCount){
                percent = NumberUtil.formatPercent((float) inspectionCount / putCount-lostCount, 2);
            }
            Integer noInspection = putCount - inspectionCount;
            InspectionReportVO vo = InspectionReportVO.builder()
                    .inspection(inspectionCount)
                    .putCount(putCount)
                    .rate(percent)
                    .noInspection(noInspection)
                    .deptId(childDept.getId())
                    .deptName(childDept.getName())
                    .build();
            list.add(vo);
        }
        return list;
    }


    @Override
    public void afterPropertiesSet() {
        InspectionServiceFactory.register(4, this);
    }
}
