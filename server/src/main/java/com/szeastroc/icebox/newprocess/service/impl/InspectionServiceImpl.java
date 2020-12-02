package com.szeastroc.icebox.newprocess.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.icebox.newprocess.entity.IceBoxExamineExceptionReport;
import com.szeastroc.icebox.newprocess.entity.IceBoxPutReport;
import com.szeastroc.icebox.newprocess.enums.DeptTypeEnum;
import com.szeastroc.icebox.newprocess.service.IceBoxExamineExceptionReportService;
import com.szeastroc.icebox.newprocess.service.IceBoxPutReportService;
import com.szeastroc.icebox.newprocess.service.IceBoxService;
import com.szeastroc.icebox.newprocess.service.IceExamineService;
import com.szeastroc.icebox.newprocess.service.InspectionService;
import com.szeastroc.icebox.newprocess.vo.InspectionReportVO;
import com.szeastroc.user.client.FeignDeptClient;
import com.szeastroc.user.client.FeignUserClient;
import com.szeastroc.user.common.session.UserManageVo;
import com.szeastroc.user.common.vo.SimpleUserRelateDeptVo;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @ClassName: InspectionServiceImpl
 * @Description:
 * @Author: 陈超
 * @Date: 2020/10/27 10:39
 **/
@Slf4j
@Service
public class InspectionServiceImpl implements InspectionService {

//    @Autowired
//    private FeignUserClient feignUserClient;
//
//    @Autowired
//    FeignDeptClient deptClient;
//    @Autowired
//    private IceExamineService iceExamineService;
//    @Autowired
//    private IceBoxService iceBoxService;
//
//    @Autowired
//    private IceBoxPutReportService iceBoxPutReportService;
//    @Autowired
//    private IceBoxExamineExceptionReportService iceBoxExamineExceptionReportService;
//
//
//    @Override
//    public List<InspectionReportVO> report(Integer deptId) {
//        UserManageVo userManageVo = FeignResponseUtil.getFeignData(feignUserClient.getSessionUserInfo());
//        Integer deptType = userManageVo.getSessionUserInfoVo().getDeptType();
//        int inspectionCount=0;
//        int putCount = 0;
//        LambdaQueryWrapper<IceBoxPutReport> wrapper = Wrappers.<IceBoxPutReport>lambdaQuery();
//        LambdaQueryWrapper<IceBoxExamineExceptionReport> exceptionReportWrapper = Wrappers.<IceBoxExamineExceptionReport>lambdaQuery();
//        if(DeptTypeEnum.LARGE_AREA.getType().equals(deptType)||DeptTypeEnum.SERVICE.getType().equals(deptType)){
//           if(DeptTypeEnum.LARGE_AREA.getType().equals(deptType)){
//               wrapper.eq(IceBoxPutReport::getRegionDeptId,deptId);
//               exceptionReportWrapper.eq(IceBoxExamineExceptionReport::getRegionDeptId,deptId);
//           }else{
//               wrapper.eq(IceBoxPutReport::getServiceDeptId,deptId);
//               exceptionReportWrapper.eq(IceBoxExamineExceptionReport::getServiceDeptId,deptId);
//           }
//            String firstDay = new DateTime().dayOfMonth().withMinimumValue().toString("yyyy-MM-dd");
//            String lastDay = new DateTime().dayOfMonth().withMaximumValue().toString("yyyy-MM-dd");
//            wrapper.ge(IceBoxPutReport::getSubmitTime, firstDay+" 00:00:00");
//            wrapper.le(IceBoxPutReport::getSubmitTime, lastDay+" 23:59:59");
//            putCount = iceBoxPutReportService.selectByExportCount(wrapper);
//            exceptionReportWrapper.ge(IceBoxExamineExceptionReport::getSubmitTime, firstDay+" 00:00:00");
//            exceptionReportWrapper.le(IceBoxExamineExceptionReport::getSubmitTime, lastDay+" 23:59:59");
//            inspectionCount = iceBoxExamineExceptionReportService.selectByExportCount(exceptionReportWrapper);
//        }else{
//            SimpleUserRelateDeptVo deptVo = userManageVo.getSimpleUserRelateDeptVos().stream().filter(one -> one.getDeptId().equals(deptId)).findFirst().orElse(null);
//            if(Objects.nonNull(deptVo)){
//                List<Integer> userIds = new ArrayList<>();
//                if(Integer.valueOf(1).equals(deptVo.getIsLeader())){
//                    userIds = FeignResponseUtil.getFeignData(feignUserClient.getUserIdsByDeptInfoId(deptId));
//                }else{
//                    userIds.add(userManageVo.getSessionUserInfoVo().getId());
//                }
//                inspectionCount = iceExamineService.getCurrentMonthInspectionCount(userIds);
//                putCount = iceBoxService.getCurrentMonthPutCount(userIds);
//            }
//
//        }
//        DecimalFormat df = new DecimalFormat("0.00");
//        String rate = df.format((float)inspectionCount/putCount);
//        int noInspection = putCount-inspectionCount;
//        InspectionReportVO.builder().inspection(inspectionCount).putCount(putCount).rate(rate).noInspection(noInspection).build()
//        return null;
//    }


    @Override
    public List<InspectionReportVO> report(Integer deptId) {
        return null;
    }
}
