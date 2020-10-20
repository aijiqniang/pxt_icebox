package com.szeastroc.icebox.newprocess.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.exception.NormalOptionException;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.customer.client.FeignSupplierClient;
import com.szeastroc.icebox.newprocess.dao.IceBoxAssetsReportDao;
import com.szeastroc.icebox.newprocess.dao.IceBoxDao;
import com.szeastroc.icebox.newprocess.dao.IceBoxExtendDao;
import com.szeastroc.icebox.newprocess.entity.IceBoxAssetsReport;
import com.szeastroc.icebox.newprocess.enums.IceBoxEnums;
import com.szeastroc.icebox.newprocess.enums.PutStatus;
import com.szeastroc.icebox.newprocess.service.IceBoxAssetsReportService;
import com.szeastroc.icebox.newprocess.vo.IceBoxAssetReportVo;
import com.szeastroc.icebox.newprocess.vo.request.IceBoxAssetReportPage;
import com.szeastroc.user.client.FeignCacheClient;
import com.szeastroc.user.common.vo.SessionDeptInfoVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * @Author xiao
 * @Date create in 2020/10/19 14:37
 * @Description:
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @_(@Autowired))
public class IceBoxAssetsReportServiceImpl extends ServiceImpl<IceBoxAssetsReportDao, IceBoxAssetsReport> implements IceBoxAssetsReportService {

    private final IceBoxAssetsReportDao iceBoxAssetsReportDao;
    private final FeignCacheClient feignCacheClient;
    private final IceBoxDao iceBoxDao;
    private final IceBoxExtendDao iceBoxExtendDao;
    private final FeignSupplierClient feignSupplierClient;
    private IceBoxAssetsReportService iceBoxAssetsReportService;

    // set 注入
    @Autowired
    public void setIceBoxAssetsReportService(IceBoxAssetsReportService iceBoxAssetsReportService) {
        this.iceBoxAssetsReportService = iceBoxAssetsReportService;
    }

    @Override
    public void createIceBoxAssetsReport(List<IceBoxAssetReportVo> lists) {

        if (CollectionUtils.isEmpty(lists)) {
            return;
        }
        for (IceBoxAssetReportVo assetReportVo : lists) {
            try {
                iceBoxAssetsReportService.createOne(assetReportVo);
            } catch (Exception e) {
                log.info("更新数据失败", e);
            }
        }
    }

    @Transactional
    @Override
    public void createOne(IceBoxAssetReportVo assetReportVo) {

        if (assetReportVo == null) {
            return;
        }
        log.info("冰柜资产报表导入数据:{}", JSON.toJSONString(assetReportVo));
        if (StringUtils.isBlank(assetReportVo.getSuppNumber())
                || assetReportVo.getModelId() == null
                || assetReportVo.getNewPutStatus() == null
                || assetReportVo.getNewStatus() == null
                || assetReportVo.getSuppDeptId() == null
        ) {
            throw new NormalOptionException(Constants.API_CODE_FAIL, Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }
        IceBoxAssetsReport iceBoxAssetsReport = iceBoxAssetsReportDao.readBySuppNumberAndModelId(assetReportVo.getSuppNumber(), assetReportVo.getModelId());
        if (iceBoxAssetsReport == null) {
            iceBoxAssetsReport = new IceBoxAssetsReport();
        }

        // 获取五级营销区域
        Map<Integer, SessionDeptInfoVo> fiveDeptInfoVoMap = FeignResponseUtil.getFeignData(feignCacheClient.getFiveLevelDept(assetReportVo.getSuppDeptId()));
        if (fiveDeptInfoVoMap != null) {
            SessionDeptInfoVo groupVo = fiveDeptInfoVoMap.get(1); // 组
            SessionDeptInfoVo serviceVo = fiveDeptInfoVoMap.get(2); // 服务处
            SessionDeptInfoVo regionVo = fiveDeptInfoVoMap.get(3); // 大区
            SessionDeptInfoVo businessVo = fiveDeptInfoVoMap.get(4); // 事业部
            SessionDeptInfoVo headquartersVo = fiveDeptInfoVoMap.get(5); // 本都
            if (groupVo != null) {
                iceBoxAssetsReport.setGroupDeptId(groupVo.getId()).setGroupDeptName(groupVo.getName());
            }
            if (serviceVo != null) {
                iceBoxAssetsReport.setServiceDeptId(serviceVo.getId()).setServiceDeptName(serviceVo.getName());
            }
            if (regionVo != null) {
                iceBoxAssetsReport.setRegionDeptId(regionVo.getId()).setRegionDeptName(regionVo.getName());
            }
            if (businessVo != null) {
                iceBoxAssetsReport.setBusinessDeptId(businessVo.getId()).setBusinessDeptName(businessVo.getName());
            }
            if (headquartersVo != null) {
                iceBoxAssetsReport.setHeadquartersDeptId(headquartersVo.getId()).setHeadquartersDeptName(headquartersVo.getName());
            }
        }

        iceBoxAssetsReport
                .setSuppNumber(assetReportVo.getSuppNumber())
                .setSuppName(assetReportVo.getSuppName())
                .setXingHao(assetReportVo.getModelName())
                .setXingHaoId(assetReportVo.getModelId());

        Integer oldStatus = assetReportVo.getOldStatus();
        Integer oldPutStatus = assetReportVo.getOldPutStatus();
        Integer newStatus = assetReportVo.getNewStatus();
        Integer newPutStatus = assetReportVo.getNewPutStatus();
        /**
         * @Date: 2020/10/20 10:51 xiao
         *  报表中只需要关心以下状态  已投/在仓(未投放)/遗失/报废
         */
        /**------------     已投      ----------*/
        if (PutStatus.FINISH_PUT.getStatus().equals(oldPutStatus)) {
            iceBoxAssetsReport.setYiTou(iceBoxAssetsReport.getYiTou() == null ? 0 : iceBoxAssetsReport.getYiTou() - 1);
        }
        if (PutStatus.FINISH_PUT.getStatus().equals(newPutStatus)) {
            iceBoxAssetsReport.setYiTou(iceBoxAssetsReport.getYiTou() == null ? 1 : iceBoxAssetsReport.getYiTou() + 1);
        }
        /**------------     在仓(未投放)      ----------*/
        if (PutStatus.NO_PUT.getStatus().equals(oldPutStatus)) {
            iceBoxAssetsReport.setZaiCang(iceBoxAssetsReport.getZaiCang() == null ? 0 : iceBoxAssetsReport.getZaiCang() - 1);
        }
        if (PutStatus.NO_PUT.getStatus().equals(newPutStatus)) {
            iceBoxAssetsReport.setZaiCang(iceBoxAssetsReport.getZaiCang() == null ? 1 : iceBoxAssetsReport.getZaiCang() + 1);
        }
        /**------------     遗失      ----------*/
        if (IceBoxEnums.StatusEnum.LOSE.getType().equals(oldStatus)) {
            iceBoxAssetsReport.setYiShi(iceBoxAssetsReport.getYiShi() == null ? 0 : iceBoxAssetsReport.getYiShi() - 1);
        }
        if (IceBoxEnums.StatusEnum.LOSE.getType().equals(newStatus)) {
            iceBoxAssetsReport.setYiShi(iceBoxAssetsReport.getYiShi() == null ? 1 : iceBoxAssetsReport.getYiShi() + 1);
        }
        /**------------     报废      ----------*/
        if (IceBoxEnums.StatusEnum.SCRAP.getType().equals(oldStatus)) {
            iceBoxAssetsReport.setBaoFei(iceBoxAssetsReport.getBaoFei() == null ? 0 : iceBoxAssetsReport.getBaoFei() - 1);
        }
        if (IceBoxEnums.StatusEnum.SCRAP.getType().equals(newStatus)) {
            iceBoxAssetsReport.setBaoFei(iceBoxAssetsReport.getBaoFei() == null ? 1 : iceBoxAssetsReport.getBaoFei() + 1);
        }


        Integer id = iceBoxAssetsReport.getId();
        if (id == null) {
            iceBoxAssetsReportDao.insert(iceBoxAssetsReport);
        } else {
            iceBoxAssetsReportDao.updateById(iceBoxAssetsReport);
        }
    }

    @Override
    public IPage readPage(IceBoxAssetReportPage reportPage) {

        IPage page = iceBoxAssetsReportDao.selectPage(reportPage,
                Wrappers.<IceBoxAssetsReport>lambdaQuery().orderByDesc(IceBoxAssetsReport::getId));
        return page;
    }
}
