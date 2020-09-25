package com.szeastroc.icebox.newprocess.service;

import com.szeastroc.icebox.newprocess.vo.OldIceBoxImportVo;

import java.util.List;

@FunctionalInterface
public interface OldIceBoxOpt {

    void opt(List<OldIceBoxImportVo> oldIceBoxImportVoList);
}
