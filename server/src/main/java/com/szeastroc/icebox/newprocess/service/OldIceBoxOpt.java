package com.szeastroc.icebox.newprocess.service;

import com.szeastroc.icebox.newprocess.vo.IceBoxAssetReportVo;
import com.szeastroc.icebox.newprocess.vo.OldIceBoxImportVo;

import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface OldIceBoxOpt {

    List<IceBoxAssetReportVo> opt(List<OldIceBoxImportVo> oldIceBoxImportVoList);

}
