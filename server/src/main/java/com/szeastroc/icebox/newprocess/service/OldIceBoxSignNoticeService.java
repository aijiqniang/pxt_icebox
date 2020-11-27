package com.szeastroc.icebox.newprocess.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.icebox.newprocess.entity.OldIceBoxSignNotice;

import java.util.List;

public interface OldIceBoxSignNoticeService extends IService<OldIceBoxSignNotice> {


    List<OldIceBoxSignNotice> findListByPxtNumber(String pxtNumber);
}








