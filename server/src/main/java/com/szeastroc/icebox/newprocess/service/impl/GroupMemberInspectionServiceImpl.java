package com.szeastroc.icebox.newprocess.service.impl;

import cn.hutool.core.util.NumberUtil;
import com.google.common.collect.Lists;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.customer.client.FeignStoreClient;
import com.szeastroc.icebox.newprocess.dao.PutStoreRelateModelDao;
import com.szeastroc.icebox.newprocess.factory.InspectionServiceFactory;
import com.szeastroc.icebox.newprocess.service.IceBoxExamineExceptionReportService;
import com.szeastroc.icebox.newprocess.service.IceBoxPutReportService;
import com.szeastroc.icebox.newprocess.service.IceBoxService;
import com.szeastroc.icebox.newprocess.service.IceExamineService;
import com.szeastroc.icebox.newprocess.service.InspectionService;
import com.szeastroc.icebox.newprocess.service.PutStoreRelateModelService;
import com.szeastroc.icebox.newprocess.vo.InspectionReportVO;
import com.szeastroc.user.client.FeignDeptClient;
import com.szeastroc.user.client.FeignUserClient;
import com.szeastroc.user.common.session.UserManageVo;
import com.szeastroc.user.common.vo.SimpleUserInfoVo;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: CrewInspectionImpl
 * @Description: 业代巡检报表
 * @Author: 陈超
 * @Date: 2020/10/27 15:12
 **/
@Service
public class GroupMemberInspectionServiceImpl implements InspectionService, InitializingBean {

    @Autowired
    private FeignUserClient feignUserClient;

    @Autowired
    FeignDeptClient deptClient;
    @Autowired
    private IceExamineService iceExamineService;
    @Autowired
    private PutStoreRelateModelService putStoreRelateModelService;

    @Autowired
    FeignStoreClient feignStoreClient;

    @Override
    public List<InspectionReportVO> report(Integer deptId) {
        ArrayList<InspectionReportVO> list = Lists.newArrayList();
        UserManageVo userManageVo = FeignResponseUtil.getFeignData(feignUserClient.getSessionUserInfo());
        list.add(this.getByUserId(userManageVo.getSessionUserInfoVo().getId()));
        return list;
    }


    public InspectionReportVO getByUserId(Integer userId){
        Integer inspectionCount = iceExamineService.getCurrentMonthInspectionCount(userId).size();
        Integer putCount = putStoreRelateModelService.getCurrentMonthPutCount(userId);
        String percent = NumberUtil.formatPercent((float) inspectionCount / putCount, 2);
        Integer noInspectionCount = putCount-inspectionCount;
        SimpleUserInfoVo user = FeignResponseUtil.getFeignData(feignUserClient.findSimpleUserById(userId));
        return InspectionReportVO.builder()
                .inspection(inspectionCount)
                .putCount(putCount)
                .rate(percent)
                .noInspection(noInspectionCount)
                .name(user.getRealname())
                .build();
    }

    @Override
    public void afterPropertiesSet() {
        InspectionServiceFactory.register(1,this);
    }
}
