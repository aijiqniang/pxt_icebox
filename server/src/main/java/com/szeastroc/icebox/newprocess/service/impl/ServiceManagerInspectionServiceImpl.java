package com.szeastroc.icebox.newprocess.service.impl;

import cn.hutool.core.util.NumberUtil;
import com.google.common.collect.Lists;
import com.szeastroc.common.entity.user.vo.SessionDeptInfoVo;
import com.szeastroc.common.feign.user.FeignDeptClient;
import com.szeastroc.common.feign.user.FeignUserClient;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.icebox.newprocess.factory.InspectionServiceFactory;
import com.szeastroc.icebox.newprocess.service.IceBoxService;
import com.szeastroc.icebox.newprocess.service.IceExamineService;
import com.szeastroc.icebox.newprocess.service.InspectionService;
import com.szeastroc.icebox.newprocess.vo.InspectionReportVO;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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
    private IceExamineService iceExamineService;
    @Autowired
    private IceBoxService iceBoxService;

    @Override
    public List<InspectionReportVO> report(Integer deptId) {
        List<InspectionReportVO> list = Lists.newArrayList();
        List<SessionDeptInfoVo> childDepts = FeignResponseUtil.getFeignData(feignDeptClient.findNormalChildDeptInfosByParentId(deptId));
        for (SessionDeptInfoVo childDept : childDepts) {
            List<Integer> userIds = FeignResponseUtil.getFeignData(feignUserClient.getUserIdsByDeptInfoId(childDept.getId()));
            Integer allInspectionCount = 0;
            Integer allPutCount = 0;
            for (Integer userId : userIds) {
                List<Integer> boxIds = iceBoxService.getPutBoxIds(userId);
                int inspectionCount = iceExamineService.getInspectionBoxes(boxIds,userId).size();
                int putCount = boxIds.size();

                allInspectionCount += inspectionCount;
                allPutCount+=putCount;
            }
            int lostCount =iceBoxService.getLostCountByDeptId(childDept.getId());
            String percent = "-";
            if(0!=allPutCount){
                percent = NumberUtil.formatPercent((float) allInspectionCount / allPutCount-lostCount, 2);
            }
            Integer noInspectionCount = allPutCount-allInspectionCount;
            InspectionReportVO vo = InspectionReportVO.builder()
                    .inspection(allInspectionCount)
                    .putCount(allPutCount)
                    .rate(percent)
                    .noInspection(noInspectionCount)
                    .deptName(childDept.getName())
                    .deptId(childDept.getId())
                    .build();
            list.add(vo);
        }
        return list;
    }


    @Override
    public void afterPropertiesSet() {
        InspectionServiceFactory.register(3, this);
    }
}
