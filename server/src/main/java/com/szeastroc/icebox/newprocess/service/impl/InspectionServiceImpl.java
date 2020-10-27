package com.szeastroc.icebox.newprocess.service.impl;

import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.icebox.newprocess.service.InspectionService;
import com.szeastroc.icebox.newprocess.vo.InspectionReportVO;
import com.szeastroc.user.client.FeignUserClient;
import com.szeastroc.user.common.session.UserManageVo;
import com.szeastroc.user.common.vo.SessionDeptInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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


    @Override
    public InspectionReportVO query(Integer type) {
        UserManageVo userManageVo = FeignResponseUtil.getFeignData(feignUserClient.getSessionUserInfo());
        List<SessionDeptInfoVo> sessionDeptInfoVos = userManageVo.getSessionDeptInfoVos();
        for (SessionDeptInfoVo sessionDeptInfoVo : sessionDeptInfoVos) {
            Integer id = sessionDeptInfoVo.getId();
            List<Integer> userIds = FeignResponseUtil.getFeignData(feignUserClient.getUserIdsByDeptInfoId(id));


        }
        
        return null;
    }
}
