package com.szeastroc.icebox.newprocess.service;

import com.szeastroc.icebox.newprocess.vo.OldIceBoxImportVo;
import com.szeastroc.icebox.vo.IceBoxAssetReportVo;

import java.util.List;

@FunctionalInterface
public interface OldIceBoxOpt {

    List<IceBoxAssetReportVo> opt(List<OldIceBoxImportVo> oldIceBoxImportVoList);

}
