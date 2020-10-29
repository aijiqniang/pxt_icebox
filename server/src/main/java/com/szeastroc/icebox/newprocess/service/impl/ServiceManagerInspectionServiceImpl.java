package com.szeastroc.icebox.newprocess.service.impl;

import cn.hutool.core.util.NumberUtil;
import com.google.common.collect.Lists;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.icebox.newprocess.factory.InspectionServiceFactory;
import com.szeastroc.icebox.newprocess.service.IceBoxService;
import com.szeastroc.icebox.newprocess.service.IceExamineService;
import com.szeastroc.icebox.newprocess.service.IcePutApplyService;
import com.szeastroc.icebox.newprocess.service.InspectionService;
import com.szeastroc.icebox.newprocess.service.PutStoreRelateModelService;
import com.szeastroc.icebox.newprocess.vo.InspectionReportVO;
import com.szeastroc.user.client.FeignDeptClient;
import com.szeastroc.user.client.FeignUserClient;
import com.szeastroc.user.common.vo.SessionDeptInfoVo;
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
    private PutStoreRelateModelService putStoreRelateModelService;
    @Autowired
    private IcePutApplyService icePutApplyService;

    @Override
    public List<InspectionReportVO> report(Integer deptId) {
        List<InspectionReportVO> list = Lists.newArrayList();
        List<SessionDeptInfoVo> childDepts = FeignResponseUtil.getFeignData(feignDeptClient.findNormalChildDeptInfosByParentId(deptId));
        for (SessionDeptInfoVo childDept : childDepts) {
            List<Integer> userIds = FeignResponseUtil.getFeignData(feignUserClient.getUserIdsByDeptInfoId(childDept.getId()));
            Integer inspectionCount = iceExamineService.getInspectionBoxes(userIds).size();
            Integer putCount = putStoreRelateModelService.getCurrentMonthPutCount(userIds);
            Integer lostCount =icePutApplyService.getLostCountByDeptId(childDept.getId());
            String percent = "-";
            if(0!=putCount){
                percent = NumberUtil.formatPercent((float) inspectionCount / putCount-lostCount, 2);
            }
            Integer noInspectionCount = putCount-inspectionCount;
            InspectionReportVO vo = InspectionReportVO.builder()
                    .inspection(inspectionCount)
                    .putCount(putCount)
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
