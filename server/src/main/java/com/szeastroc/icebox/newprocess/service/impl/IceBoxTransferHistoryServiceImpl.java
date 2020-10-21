package com.szeastroc.icebox.newprocess.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.icebox.newprocess.dao.IceBoxTransferHistoryDao;
import com.szeastroc.icebox.newprocess.entity.IceBoxTransferHistory;
import com.szeastroc.icebox.newprocess.service.IceBoxTransferHistoryService;
import com.szeastroc.icebox.newprocess.vo.request.IceTransferRecordPage;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IceBoxTransferHistoryServiceImpl extends ServiceImpl<IceBoxTransferHistoryDao, IceBoxTransferHistory> implements IceBoxTransferHistoryService {



    @Override
    public IPage<IceBoxTransferHistory> report(IceTransferRecordPage iceTransferRecordPage) {
        return null;
    }

    @Override
    public Void reportExport(IceTransferRecordPage iceTransferRecordPage) {
        return null;
    }

    @Override
    public List<IceBoxTransferHistory> findByIceBoxId(Integer iceBoxId) {
        return null;
    }
}
