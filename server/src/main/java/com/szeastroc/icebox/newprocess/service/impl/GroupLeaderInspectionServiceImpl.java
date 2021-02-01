package com.szeastroc.icebox.newprocess.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.szeastroc.icebox.newprocess.entity.IceInspectionReport;
import com.szeastroc.icebox.newprocess.factory.InspectionServiceFactory;
import com.szeastroc.icebox.newprocess.service.IceInspectionReportService;
import com.szeastroc.icebox.newprocess.service.InspectionService;
import com.szeastroc.icebox.newprocess.vo.InspectionReportVO;
import org.joda.time.DateTime;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @ClassName: GroupLeaderInspectionImpl
 * @Description: 组长巡检
 * @Author: 陈超
 * @Date: 2020/10/27 15:14
 **/
@Service
public class GroupLeaderInspectionServiceImpl implements InspectionService, InitializingBean {

    @Autowired
    private IceInspectionReportService iceInspectionReportService;
    @Override
    public List<InspectionReportVO> report(Integer deptId) {
        //组员id
        LambdaQueryWrapper<IceInspectionReport> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(IceInspectionReport::getGroupDeptId,deptId).eq(IceInspectionReport::getInspectionDate,new DateTime().toString("yyyy-MM"));
        List<IceInspectionReport> reports = iceInspectionReportService.list(wrapper);
        return reports.stream().map(IceInspectionReport::convertInspectionReportVO).collect(Collectors.toList());
    }

    @Override
    public void afterPropertiesSet() {
        InspectionServiceFactory.register(2,this);
    }

}
