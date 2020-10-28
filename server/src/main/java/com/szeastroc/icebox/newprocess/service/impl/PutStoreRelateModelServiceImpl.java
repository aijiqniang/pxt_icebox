package com.szeastroc.icebox.newprocess.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.icebox.newprocess.dao.PutStoreRelateModelDao;
import com.szeastroc.icebox.newprocess.entity.PutStoreRelateModel;
import com.szeastroc.icebox.newprocess.service.PutStoreRelateModelService;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @ClassName: PutStoreRelateModelServiceImpl
 * @Description:
 * @Author: 陈超
 * @Date: 2020/10/27 19:27
 **/
@Service
public class PutStoreRelateModelServiceImpl extends ServiceImpl<PutStoreRelateModelDao, PutStoreRelateModel> implements PutStoreRelateModelService {
    @Override
    public Integer getCurrentMonthPutCount(Integer userId) {
        LambdaQueryWrapper<PutStoreRelateModel> wrapper = Wrappers.<PutStoreRelateModel>lambdaQuery();
        wrapper.eq(PutStoreRelateModel::getPutStatus,3)
                .eq(PutStoreRelateModel::getCreateBy,userId);
//                .apply("date_format(create_timed,'%Y-%m-%d') = '" + new DateTime().toString("yyyy-MM-dd")+"'");
        return this.baseMapper.selectCount(wrapper);
    }

    @Override
    public Integer getCurrentMonthPutCount(List<Integer> userIds) {
        LambdaQueryWrapper<PutStoreRelateModel> wrapper = Wrappers.<PutStoreRelateModel>lambdaQuery();
        wrapper.eq(PutStoreRelateModel::getPutStatus,3)
                .in(PutStoreRelateModel::getCreateBy,userIds);
//                .apply("date_format(create_timed,'%Y-%m-%d') = '" + new DateTime().toString("yyyy-MM-dd")+"'");
        return this.baseMapper.selectCount(wrapper);
    }
}
