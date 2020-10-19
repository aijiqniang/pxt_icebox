package com.szeastroc.icebox.newprocess.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.icebox.newprocess.dao.OldIceBoxSignNoticeDao;
import com.szeastroc.icebox.newprocess.entity.OldIceBoxSignNotice;
import com.szeastroc.icebox.newprocess.enums.OldIceBoxSignNoticeStatusEnums;
import com.szeastroc.icebox.newprocess.service.OldIceBoxSignNoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OldIceBoxSignNoticeServiceImpl extends ServiceImpl<OldIceBoxSignNoticeDao, OldIceBoxSignNotice> implements OldIceBoxSignNoticeService {

    @Autowired
    private OldIceBoxSignNoticeDao oldIceBoxSignNoticeDao;
    @Override
    public List<OldIceBoxSignNotice> findListByPxtNumber(String pxtNumber) {
        List<OldIceBoxSignNotice> list = oldIceBoxSignNoticeDao.selectList(Wrappers.<OldIceBoxSignNotice>lambdaQuery().eq(OldIceBoxSignNotice::getPutStoreNumber, pxtNumber).eq(OldIceBoxSignNotice::getStatus, OldIceBoxSignNoticeStatusEnums.NO_SIGN.getStatus()));
        return list;
    }
}


