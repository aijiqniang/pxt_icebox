package com.szeastroc.icebox.newprocess.service;

import com.szeastroc.icebox.newprocess.vo.OldIceBoxImportVo;

import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface OldIceBoxOpt {

    List<Map<String ,Object>> opt(List<OldIceBoxImportVo> oldIceBoxImportVoList);

}
