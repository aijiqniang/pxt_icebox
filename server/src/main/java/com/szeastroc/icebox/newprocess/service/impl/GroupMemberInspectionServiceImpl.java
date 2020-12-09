package com.szeastroc.icebox.newprocess.service.impl;

import cn.hutool.core.util.NumberUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Lists;
import com.szeastroc.common.entity.customer.vo.SimpleStoreVo;
import com.szeastroc.common.entity.customer.vo.SubordinateInfoVo;
import com.szeastroc.common.entity.user.session.UserManageVo;
import com.szeastroc.common.entity.user.vo.SimpleUserInfoVo;
import com.szeastroc.common.feign.customer.FeignStoreClient;
import com.szeastroc.common.feign.customer.FeignSupplierClient;
import com.szeastroc.common.feign.user.FeignDeptClient;
import com.szeastroc.common.feign.user.FeignUserClient;
import com.szeastroc.common.feign.visit.FeignVisitInfoClient;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.entity.IceExamine;
import com.szeastroc.icebox.newprocess.factory.InspectionServiceFactory;
import com.szeastroc.icebox.newprocess.service.IceBoxService;
import com.szeastroc.icebox.newprocess.service.IceExamineService;
import com.szeastroc.icebox.newprocess.service.InspectionService;
import com.szeastroc.icebox.newprocess.vo.InspectionReportVO;
import com.szeastroc.icebox.newprocess.vo.StoreVO;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    FeignSupplierClient feignSupplierClient;
    @Autowired
    FeignDeptClient deptClient;
    @Autowired
    private IceExamineService iceExamineService;
    @Autowired
    FeignStoreClient feignStoreClient;
    @Resource
    private IceBoxService iceBoxService;
    @Autowired
    private FeignVisitInfoClient feignVisitInfoClient;


    @Override
    public List<InspectionReportVO> report(Integer deptId) {
        ArrayList<InspectionReportVO> list = Lists.newArrayList();
        UserManageVo userManageVo = FeignResponseUtil.getFeignData(feignUserClient.getSessionUserInfo());
        list.add(this.getByUserId(userManageVo.getSessionUserInfoVo().getId()));
        return list;
    }


    public InspectionReportVO getByUserId(Integer userId) {
        List<Integer> putBoxIds = iceBoxService.getPutBoxIds(userId);
        int inspectionCount = iceExamineService.getInspectionBoxes(putBoxIds,userId).size();
        int putCount = putBoxIds.size();
        int lostCount = iceBoxService.getLostCount(userId,putBoxIds);
        String percent = "-";
        if (0 != putCount) {
            percent = NumberUtil.formatPercent((float) inspectionCount / putCount - lostCount, 2);
        }
        Integer noInspectionCount = iceExamineService.getNoInspectionBoxes(putCount, userId);
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
        InspectionServiceFactory.register(1, this);
    }

    public List<StoreVO> getStoreByUserId(Integer userId) {
        List<Integer> putBoxIds = iceBoxService.getPutBoxIds(userId);
        List<IceExamine> inspectionBoxes = iceExamineService.getInspectionBoxes(putBoxIds,userId);
        Set<Integer> idSet = new HashSet<>();
        if (CollectionUtils.isEmpty(inspectionBoxes)) {
            idSet = new HashSet<>(putBoxIds);
        } else {
            putBoxIds.removeAll(inspectionBoxes.stream().map(IceExamine::getIceBoxId).collect(Collectors.toList()));
            idSet = new HashSet<>(putBoxIds);

        }
        if (CollectionUtils.isEmpty(idSet)) {
            return Lists.newArrayList();
        }
        LambdaQueryWrapper<IceBox> wrapper = Wrappers.<IceBox>lambdaQuery();
        wrapper.in(IceBox::getId, idSet).eq(IceBox::getPutStatus, 3).eq(IceBox::getStatus, 1);
        List<IceBox> iceBoxes = iceBoxService.list(wrapper);
        if(CollectionUtils.isEmpty(iceBoxes)){
            return Lists.newArrayList();
        }
        List<String> storeNumbers = iceBoxes.stream().map(IceBox::getPutStoreNumber).distinct().collect(Collectors.toList());
        if(CollectionUtils.isEmpty(storeNumbers)){
            return Lists.newArrayList();
        }
        //门店
        List<SimpleStoreVo> stores = FeignResponseUtil.getFeignData(feignStoreClient.getSimpleStoreByNumbers(storeNumbers));
        List<StoreVO> vos = new ArrayList<>();
        if(Objects.nonNull(stores)){
             vos = stores.stream().map(one -> {
                StoreVO storeVO = new StoreVO();
                storeVO.setCustomerNumber(one.getStoreNumber());
                storeVO.setStoreName(one.getStoreName());
                String day = FeignResponseUtil.getFeignData(feignVisitInfoClient.getStoreWeekIndex(one.getStoreNumber()));
                storeVO.setDay(day);
                return storeVO;
            }).collect(Collectors.toList());
        }
        //配送商
        Map<String, SubordinateInfoVo> suppliers = FeignResponseUtil.getFeignData(feignSupplierClient.getCustomersByNumberList(storeNumbers));
        if(Objects.nonNull(suppliers)&&!suppliers.isEmpty()){
            for (SubordinateInfoVo supplier : suppliers.values()) {
                StoreVO storeVO = new StoreVO();
                storeVO.setCustomerNumber(supplier.getNumber());
                storeVO.setStoreName(supplier.getName());
                String day = FeignResponseUtil.getFeignData(feignVisitInfoClient.getStoreWeekIndex(supplier.getNumber()));
                storeVO.setDay(day);
                vos.add(storeVO);
            }
        }
        return vos;
    }
}
