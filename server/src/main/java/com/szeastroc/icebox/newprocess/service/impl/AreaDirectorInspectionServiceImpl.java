package com.szeastroc.icebox.newprocess.service.impl;

import cn.hutool.core.util.NumberUtil;
import com.szeastroc.common.feign.user.FeignDeptClient;
import com.szeastroc.icebox.newprocess.factory.InspectionServiceFactory;
import com.szeastroc.icebox.newprocess.service.IceInspectionReportService;
import com.szeastroc.icebox.newprocess.service.InspectionService;
import com.szeastroc.icebox.newprocess.vo.InspectionReportVO;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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
    private IceInspectionReportService iceInspectionReportService;

    @Override
    public List<InspectionReportVO> report(Integer deptId) {
        List<InspectionReportVO> reports = iceInspectionReportService.getServiceReports(deptId);
        for (InspectionReportVO report : reports) {
            String percent = "-";
            if(0!=report.getPutCount()){
                percent = NumberUtil.formatPercent((float) report.getInspection() / (report.getNoInspection()+report.getInspection()), 2);
            }
            report.setRate(percent);
        }
        return reports;
    }


    @Override
    public void afterPropertiesSet() {
        InspectionServiceFactory.register(4, this);
    }
}
