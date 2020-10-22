package com.szeastroc.icebox.newprocess.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.icebox.newprocess.entity.IceBoxChangeHistory;

import java.util.List;

public interface IceBoxChangeHistoryService extends IService<IceBoxChangeHistory> {


    List<IceBoxChangeHistory> iceBoxChangeHistoryService(Integer iceBoxId);
}

