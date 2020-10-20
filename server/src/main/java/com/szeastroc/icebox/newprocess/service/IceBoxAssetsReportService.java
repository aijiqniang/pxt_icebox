package com.szeastroc.icebox.newprocess.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.icebox.newprocess.entity.IceBoxAssetsReport;

import java.util.List;
import java.util.Map;

public interface IceBoxAssetsReportService extends IService<IceBoxAssetsReport> {

    void createIceBoxAssetsReport(List<Map<String ,Object>> lists);

    void createOne(Map<String,Object>map);

}








