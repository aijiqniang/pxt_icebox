package com.szeastroc.icebox.newprocess.service.impl;

import cn.hutool.core.util.NumberUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Lists;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.customer.client.FeignStoreClient;
import com.szeastroc.customer.common.vo.SimpleStoreVo;
import com.szeastroc.icebox.newprocess.dao.IceBoxDao;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.entity.IceExamine;
import com.szeastroc.icebox.newprocess.factory.InspectionServiceFactory;
import com.szeastroc.icebox.newprocess.service.IceExamineService;
import com.szeastroc.icebox.newprocess.service.IcePutApplyService;
import com.szeastroc.icebox.newprocess.service.InspectionService;
import com.szeastroc.icebox.newprocess.vo.InspectionReportVO;
import com.szeastroc.icebox.newprocess.vo.StoreVO;
import com.szeastroc.user.client.FeignDeptClient;
import com.szeastroc.user.client.FeignUserClient;
import com.szeastroc.user.common.session.UserManageVo;
import com.szeastroc.user.common.vo.SimpleUserInfoVo;
import com.szeastroc.visit.client.FeignVisitInfoClient;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

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
    private IcePutApplyService icePutApplyService;
    @Autowired
    FeignStoreClient feignStoreClient;
    @Resource
    private IceBoxDao iceBoxDao;
    @Autowired
    private FeignVisitInfoClient feignVisitInfoClient;

    @Override
    public List<InspectionReportVO> report(Integer deptId) {
        ArrayList<InspectionReportVO> list = Lists.newArrayList();
        UserManageVo userManageVo = FeignResponseUtil.getFeignData(feignUserClient.getSessionUserInfo());
        list.add(this.getByUserId(userManageVo.getSessionUserInfoVo().getId()));
        return list;
    }


    public InspectionReportVO getByUserId(Integer userId){
        int inspectionCount = iceExamineService.getInspectionBoxes(userId).size();
        int putCount = icePutApplyService.getPutCount(userId);

        int lostCount = icePutApplyService.getLostCount(userId);
        String percent = "-";
        if(0!=putCount){
            percent = NumberUtil.formatPercent((float) inspectionCount / putCount-lostCount, 2);
        }
        Integer noInspectionCount = iceExamineService.getNoInspectionBoxes(putCount,userId);
        SimpleUserInfoVo user = FeignResponseUtil.getFeignData(feignUserClient.findSimpleUserById(userId));
        return InspectionReportVO.builder()
                .inspection(inspectionCount)
                .putCount(putCount)
                .rate(percent)
                .noInspection(noInspectionCount)
                .name(user.getRealname())
                .userId(userId)
                .build();
    }

    @Override
    public void afterPropertiesSet() {
        InspectionServiceFactory.register(1,this);
    }

    public List<StoreVO> getStoreByUserId(Integer userId){
        List<Integer> putBoxIds = icePutApplyService.getPutBoxIds(userId);
        List<IceExamine> inspectionBoxes = iceExamineService.getInspectionBoxes(userId);
        HashSet<Integer> idSet = new HashSet<>();
        for (Integer putBoxId : putBoxIds) {
            for (IceExamine inspectionBox : inspectionBoxes) {
                if(!putBoxId.equals(inspectionBox.getIceBoxId())){
                    idSet.add(putBoxId);
                }
            }
        }
        if(CollectionUtils.isEmpty(idSet)){
            return Lists.newArrayList();
        }
        LambdaQueryWrapper<IceBox> wrapper = Wrappers.<IceBox>lambdaQuery();
        wrapper.in(IceBox::getId,idSet).eq(IceBox::getPutStatus,3).eq(IceBox::getStatus,1);
        List<IceBox> iceBoxes = iceBoxDao.selectList(wrapper);
        List<String> storeNumbers = iceBoxes.stream().map(IceBox::getPutStoreNumber).distinct().collect(Collectors.toList());
        List<SimpleStoreVo> stores = FeignResponseUtil.getFeignData(feignStoreClient.getSimpleStoreByNumbers(storeNumbers));
        List<StoreVO> vos = stores.stream().map(one -> {
            StoreVO storeVO = new StoreVO();
            storeVO.setCustomerNumber(one.getStoreNumber());
            storeVO.setStoreName(one.getStoreName());
            String day = FeignResponseUtil.getFeignData(feignVisitInfoClient.getStoreWeekIndex(one.getStoreNumber()));
            storeVO.setDay(day);
            return storeVO;
        }).collect(Collectors.toList());
        return vos;
    }
}
