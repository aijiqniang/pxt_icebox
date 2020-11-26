package com.szeastroc.icebox.newprocess.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Lists;
import com.szeastroc.icebox.newprocess.dao.IceBoxDao;
import com.szeastroc.icebox.newprocess.dao.IcePutApplyRelateBoxDao;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.entity.IcePutApplyRelateBox;
import com.szeastroc.icebox.newprocess.service.IcePutApplyRelateBoxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.icebox.newprocess.dao.IcePutApplyDao;
import com.szeastroc.icebox.newprocess.entity.IcePutApply;
import com.szeastroc.icebox.newprocess.service.IcePutApplyService;

@Service
public class IcePutApplyServiceImpl extends ServiceImpl<IcePutApplyDao, IcePutApply> implements IcePutApplyService {

    @Resource
    private IcePutApplyRelateBoxDao icePutApplyRelateBoxDao;
    @Resource
    private IceBoxDao iceBoxDao;


    @Override
    public int getPutCount(Integer userId) {
        return getCount(userId,null);
    }

    @Override
    public int getLostCount(Integer userId) {
        return getCount(userId,3);
    }

    @Override
    public int getCount(Integer userId, Integer status) {
        LambdaQueryWrapper<IceBox> iceBoxWrapper = buildWrapper(userId, status);
        if(Objects.isNull(iceBoxWrapper)){
            return 0;
        }
        return iceBoxDao.selectCount(iceBoxWrapper);
    }

    private LambdaQueryWrapper<IceBox> buildWrapper(Integer userId, Integer status) {
        LambdaQueryWrapper<IceBox> iceBoxWrapper = Wrappers.<IceBox>lambdaQuery();
        List<Integer> ownerBoxIds = this.getOwnerBoxIds(userId);
        if(CollectionUtils.isEmpty(ownerBoxIds)){
            return null;
        }
        iceBoxWrapper.in(IceBox::getId, ownerBoxIds)
                .eq(IceBox::getPutStatus,3);
        if(Objects.nonNull(status)){
            iceBoxWrapper.eq(IceBox::getStatus,status);
        }
        return iceBoxWrapper;
    }

    @Override
    public List<Integer> getOwnerBoxIds(Integer userId) {
        LambdaQueryWrapper<IcePutApply> wrapper = Wrappers.<IcePutApply>lambdaQuery();
        wrapper.eq(IcePutApply::getCreatedBy,userId).eq(IcePutApply::getExamineStatus,2).eq(IcePutApply::getStoreSignStatus,1);
        List<IcePutApply> icePutApplies = this.baseMapper.selectList(wrapper);
        if(CollectionUtils.isEmpty(icePutApplies)){
            return Lists.newArrayList();
        }
        LambdaQueryWrapper<IcePutApplyRelateBox> queryWrapper = Wrappers.<IcePutApplyRelateBox>lambdaQuery();
        queryWrapper.in(IcePutApplyRelateBox::getApplyNumber,icePutApplies.stream().map(IcePutApply::getApplyNumber).collect(Collectors.toList()));
        List<IcePutApplyRelateBox> relateBoxes = icePutApplyRelateBoxDao.selectList(queryWrapper);
        return relateBoxes.stream().map(IcePutApplyRelateBox::getBoxId).collect(Collectors.toList());
    }

    @Override
    public List<Integer> getPutBoxIds(Integer userId) {
        LambdaQueryWrapper<IceBox> wrapper = buildWrapper(userId, null);
        return iceBoxDao.selectList(wrapper).stream().map(IceBox::getId).collect(Collectors.toList());
    }

    @Override
    public int getLostCountByDeptId(Integer deptId) {
        LambdaQueryWrapper<IceBox> iceBoxWrapper = Wrappers.<IceBox>lambdaQuery();
        iceBoxWrapper.eq(IceBox::getPutStatus,3).eq(IceBox::getDeptId,deptId).eq(IceBox::getStatus,3);
        return iceBoxDao.selectCount(iceBoxWrapper);
    }


    @Override
    public int getLostCountByDeptIds(List<Integer> deptIds) {
        if(CollectionUtils.isEmpty(deptIds)){
            return 0;
        }
        LambdaQueryWrapper<IceBox> iceBoxWrapper = Wrappers.<IceBox>lambdaQuery();
        iceBoxWrapper.eq(IceBox::getPutStatus,3).in(IceBox::getDeptId,deptIds).eq(IceBox::getStatus,3);
        return iceBoxDao.selectCount(iceBoxWrapper);
    }
}


