package com.szeastroc.icebox.newprocess.service;

import com.alibaba.fastjson.JSONObject;
import com.szeastroc.icebox.newprocess.vo.IceBoxAssetReportVo;
import com.szeastroc.icebox.newprocess.vo.OldIceBoxImportVo;

import java.util.List;

@FunctionalInterface
public interface OldIceBoxOpt {

    List<JSONObject> opt(List<OldIceBoxImportVo> oldIceBoxImportVoList);

}
