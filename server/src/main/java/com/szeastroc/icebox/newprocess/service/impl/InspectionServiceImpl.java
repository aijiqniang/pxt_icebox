package com.szeastroc.icebox.newprocess.service.impl;

import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.icebox.newprocess.enums.DeptTypeEnum;
import com.szeastroc.icebox.newprocess.service.IceBoxService;
import com.szeastroc.icebox.newprocess.service.IceExamineService;
import com.szeastroc.icebox.newprocess.service.InspectionService;
import com.szeastroc.icebox.newprocess.vo.InspectionReportVO;
import com.szeastroc.user.client.FeignDeptClient;
import com.szeastroc.user.client.FeignUserClient;
import com.szeastroc.user.common.session.UserManageVo;
import com.szeastroc.user.common.vo.SimpleUserRelateDeptVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    @Autowired
    private FeignUserClient feignUserClient;

    @Autowired
    FeignDeptClient deptClient;
    @Autowired
    private IceExamineService iceExamineService;
    @Autowired
    private IceBoxService iceBoxService;

    @Override
    public InspectionReportVO query(Integer deptId) {
        UserManageVo userManageVo = FeignResponseUtil.getFeignData(feignUserClient.getSessionUserInfo());
        Integer deptType = userManageVo.getSessionUserInfoVo().getDeptType();
        InspectionReportVO vo = new InspectionReportVO();
        if(DeptTypeEnum.SERVICE.getType().equals(deptType)||DeptTypeEnum.LARGE_AREA.getType().equals(deptType)){


        }else{
            SimpleUserRelateDeptVo deptVo = userManageVo.getSimpleUserRelateDeptVos().stream().filter(one -> one.getDeptId().equals(deptId)).findFirst().orElse(null);
            if(Objects.nonNull(deptVo)){
                List<Integer> userIds = new ArrayList<>();
                if(Integer.valueOf(1).equals(deptVo.getIsLeader())){
                    userIds = FeignResponseUtil.getFeignData(feignUserClient.getUserIdsByDeptInfoId(deptId));
                }else{
                    userIds.add(userManageVo.getSessionUserInfoVo().getId());
                }
                Integer inspectionCount = iceExamineService.getCurrentMonthInspectionCount(userIds);
                int putCount = iceBoxService.getCurrentMonthPutCount(userIds);
                vo.setInspection(inspectionCount);
                vo.setPutCount(putCount);
//                vo.setRate(inspectionCount/putCount);
            }

        }
        return null;
    }
}
