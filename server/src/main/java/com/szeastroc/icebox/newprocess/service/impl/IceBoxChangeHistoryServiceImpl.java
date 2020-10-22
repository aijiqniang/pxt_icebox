package com.szeastroc.icebox.newprocess.service.impl;


import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.icebox.newprocess.dao.IceBoxChangeHistoryDao;
import com.szeastroc.icebox.newprocess.entity.IceBoxChangeHistory;
import com.szeastroc.icebox.newprocess.service.IceBoxChangeHistoryService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class IceBoxChangeHistoryServiceImpl extends ServiceImpl<IceBoxChangeHistoryDao, IceBoxChangeHistory> implements IceBoxChangeHistoryService {


    @Resource
    private IceBoxChangeHistoryDao iceBoxChangeHistoryDao;

    @Override
    public List<IceBoxChangeHistory> iceBoxChangeHistoryService(Integer iceBoxId) {
        return iceBoxChangeHistoryDao.selectList(Wrappers.<IceBoxChangeHistory>lambdaQuery().eq(IceBoxChangeHistory::getIceBoxId, iceBoxId));
    }
}
