package com.szeastroc.icebox.newprocess.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.icebox.newprocess.entity.IceBoxChangeHistory;
import com.szeastroc.icebox.newprocess.vo.request.IceChangeHistoryPage;

public interface IceBoxChangeHistoryService extends IService<IceBoxChangeHistory> {


    IPage<IceBoxChangeHistory> iceBoxChangeHistoryService(IceChangeHistoryPage iceChangeHistoryPage);
}

