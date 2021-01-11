package com.szeastroc.icebox.newprocess.service.impl;

import cn.hutool.core.util.NumberUtil;
import com.szeastroc.common.feign.user.FeignDeptClient;
import com.szeastroc.common.feign.user.FeignUserClient;
import com.szeastroc.icebox.newprocess.entity.IceInspectionReport;
import com.szeastroc.icebox.newprocess.factory.InspectionServiceFactory;
import com.szeastroc.icebox.newprocess.service.IceInspectionReportService;
import com.szeastroc.icebox.newprocess.service.InspectionService;
import com.szeastroc.icebox.newprocess.vo.InspectionReportVO;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @ClassName: ServiceManagerInspectionImpl
 * @Description: 服务处经理巡检
 * @Author: 陈超
 * @Date: 2020/10/27 15:14
 **/
@Service
public class ServiceManagerInspectionServiceImpl implements InspectionService, InitializingBean {
    @Autowired
    FeignDeptClient feignDeptClient;
    @Autowired
    FeignUserClient feignUserClient;
    @Autowired
    private IceInspectionReportService iceInspectionReportService;

    @Override
    public List<InspectionReportVO> report(Integer deptId) {
        List<InspectionReportVO> reports = iceInspectionReportService.getGroupReports(deptId);
        for (InspectionReportVO report : reports) {
            String percent = "-";
            if(0!=report.getPutCount()){
                percent = NumberUtil.formatPercent((float) report.getInspection() / (report.getNoInspection()+report.getInspection()), 2);
            }
            report.setRate(percent);
        }
        List<IceInspectionReport> inService = iceInspectionReportService.getInService(deptId);
        List<InspectionReportVO> serviceReports = inService.stream().map(IceInspectionReport::convertInspectionReportVO).collect(Collectors.toList());
        reports.addAll(serviceReports);
        return reports;
    }


    @Override
    public void afterPropertiesSet() {
        InspectionServiceFactory.register(3, this);
    }
}
