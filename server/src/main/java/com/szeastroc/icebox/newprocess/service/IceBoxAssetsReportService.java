package com.szeastroc.icebox.newprocess.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.icebox.newprocess.entity.IceBoxAssetsReport;
import com.szeastroc.icebox.newprocess.vo.IceBoxAssetReportVo;

import java.util.List;
import java.util.Map;

public interface IceBoxAssetsReportService extends IService<IceBoxAssetsReport> {

    void createIceBoxAssetsReport(List<IceBoxAssetReportVo> lists);

    void createOne(IceBoxAssetReportVo assetReportVo);

    List<Map<String, Object>> readReportJl(Integer deptId);

    List<Map<String, Object>> readReportDqzj(Integer deptId);

    void syncOldDatas();

}








