package com.szeastroc.icebox.newprocess.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.icebox.newprocess.entity.PutStoreRelateModel;

import java.util.List;

/**
 * @ClassName: PutStoreRelateModelService
 * @Description:
 * @Author: 陈超
 * @Date: 2020/10/27 19:27
 **/
public interface PutStoreRelateModelService extends IService<PutStoreRelateModel> {

    Integer getCurrentMonthPutCount(Integer userId);
    Integer getCurrentMonthPutCount(List<Integer> userIds);

}
