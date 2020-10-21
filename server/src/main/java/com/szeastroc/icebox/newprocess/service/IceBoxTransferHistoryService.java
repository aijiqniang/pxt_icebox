package com.szeastroc.icebox.newprocess.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.icebox.newprocess.entity.IceBoxTransferHistory;
import com.szeastroc.icebox.newprocess.vo.request.IceTransferRecordPage;

import java.util.List;

public interface IceBoxTransferHistoryService extends IService<IceBoxTransferHistory> {


    IPage<IceBoxTransferHistory> report(IceTransferRecordPage iceTransferRecordPage);

    Void reportExport(IceTransferRecordPage iceTransferRecordPage);

    List<IceBoxTransferHistory> findByIceBoxId(Integer iceBoxId);
}

