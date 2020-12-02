package com.szeastroc.icebox.newprocess.service;

import com.szeastroc.icebox.newprocess.entity.IcePutApply;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IcePutApplyService extends IService<IcePutApply> {

    /**
     * 冰柜投放数量
     * @param userId
     * @return
     */
    int getPutCount(Integer userId);

    int getCount(Integer userId,Integer status);

    List<Integer> getPutBoxIds(Integer userId);

    /**
     * 冰柜遗失数量
     * @param userId
     * @return
     */
    int getLostCount(Integer userId);

    List<Integer> getOwnerBoxIds(Integer userId);

    int getLostCountByDeptId(Integer deptId);

    int getLostCountByDeptIds(List<Integer> deptIds);

}


