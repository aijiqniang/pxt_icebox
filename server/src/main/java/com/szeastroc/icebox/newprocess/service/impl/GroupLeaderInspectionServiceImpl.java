package com.szeastroc.icebox.newprocess.service.impl;

import com.google.common.collect.Lists;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.icebox.newprocess.factory.InspectionServiceFactory;
import com.szeastroc.icebox.newprocess.service.InspectionService;
import com.szeastroc.icebox.newprocess.vo.InspectionReportVO;
import com.szeastroc.icebox.newprocess.vo.StoreVO;
import com.szeastroc.user.client.FeignUserClient;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: GroupLeaderInspectionImpl
 * @Description: 组长巡检
 * @Author: 陈超
 * @Date: 2020/10/27 15:14
 **/
@Service
public class GroupLeaderInspectionServiceImpl implements InspectionService, InitializingBean {
    @Autowired
    private GroupMemberInspectionServiceImpl groupMemberInspectionService;
    @Autowired
    private FeignUserClient feignUserClient;
    @Override
    public List<InspectionReportVO> report(Integer deptId) {
        ArrayList<InspectionReportVO> list = Lists.newArrayList();
        //组员id
        List<Integer> userIds = FeignResponseUtil.getFeignData(feignUserClient.getUserIdsByDeptInfoId(deptId));
        for (Integer userId : userIds) {
            InspectionReportVO reportVO = groupMemberInspectionService.getByUserId(userId);
            list.add(reportVO);
        }
        return list;
    }


    @Override
    public void afterPropertiesSet() {
        InspectionServiceFactory.register(2,this);
    }

}
