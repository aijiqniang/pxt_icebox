package com.szeastroc.icebox.newprocess.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.icebox.newprocess.dao.IceModelDao;
import com.szeastroc.icebox.newprocess.entity.IceModel;
import com.szeastroc.icebox.newprocess.service.IceModelService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class IceModelServiceImpl extends ServiceImpl<IceModelDao, IceModel> implements IceModelService {

    @Resource
    private IceModelDao iceModelDao;

    @Override
    public List<IceModel> getAllModel() {
        return iceModelDao.selectList(null);
    }
}


