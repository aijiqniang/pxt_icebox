package com.szeastroc.icebox.newprocess.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.icebox.newprocess.entity.IceBoxTransferHistory;
import com.szeastroc.icebox.newprocess.vo.IceBoxTransferHistoryVo;
import com.szeastroc.icebox.newprocess.vo.request.IceBoxTransferHistoryPage;

import java.util.List;

public interface IceBoxTransferHistoryService extends IService<IceBoxTransferHistory> {

    List<IceBoxTransferHistoryVo> findListBySupplierId(Integer supplierId);
}

