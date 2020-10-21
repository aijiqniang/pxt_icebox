package com.szeastroc.icebox.newprocess.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.entity.IceBoxAssetsReport;
import com.szeastroc.icebox.newprocess.vo.IceBoxAssetReportVo;
import com.szeastroc.icebox.newprocess.vo.request.IceBoxAssetReportPage;

import java.util.List;

public interface IceBoxAssetsReportService extends IService<IceBoxAssetsReport> {

    void createIceBoxAssetsReport(List<IceBoxAssetReportVo> lists);

    void createOne(IceBoxAssetReportVo assetReportVo);

    IPage readPage(IceBoxAssetReportPage reportPage);

    void syncOldDatas();

}








