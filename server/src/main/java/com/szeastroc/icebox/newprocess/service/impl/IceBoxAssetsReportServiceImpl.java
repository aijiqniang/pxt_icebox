package com.szeastroc.icebox.newprocess.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.exception.NormalOptionException;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.customer.client.FeignSupplierClient;
import com.szeastroc.customer.common.vo.SubordinateInfoVo;
import com.szeastroc.icebox.newprocess.dao.IceBoxAssetsReportDao;
import com.szeastroc.icebox.newprocess.dao.IceBoxDao;
import com.szeastroc.icebox.newprocess.dao.IceBoxExtendDao;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.entity.IceBoxAssetsReport;
import com.szeastroc.icebox.newprocess.enums.IceBoxEnums;
import com.szeastroc.icebox.newprocess.enums.PutStatus;
import com.szeastroc.icebox.newprocess.service.IceBoxAssetsReportService;
import com.szeastroc.icebox.newprocess.vo.IceBoxAssetReportVo;
import com.szeastroc.user.client.FeignCacheClient;
import com.szeastroc.user.common.vo.SessionDeptInfoVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
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
        if (assetReportVo.getModelId() == null
                || assetReportVo.getNewPutStatus() == null
                || assetReportVo.getNewStatus() == null
                || assetReportVo.getSuppDeptId() == null
        ) {
            throw new NormalOptionException(Constants.API_CODE_FAIL, "参数不对");
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
    public List<Map<String, Object>> readReportJl(Integer deptId) {

        if (deptId == null) {
            return null;
        }

        List<IceBoxAssetsReport> assetsReportList = iceBoxAssetsReportDao.selectList(Wrappers.<IceBoxAssetsReport>lambdaQuery()
                .eq(IceBoxAssetsReport::getServiceDeptId, deptId)
                .orderByAsc(IceBoxAssetsReport::getSuppNumber)
        );
        if (CollectionUtils.isEmpty(assetsReportList)) {
            return null;
        }

        Map<String, List<IceBoxAssetsReport>> suppNumberMaps = Maps.newHashMap();
        for (IceBoxAssetsReport report : assetsReportList) {
            String suppNumber = report.getSuppNumber();
            List<IceBoxAssetsReport> assetsReports = suppNumberMaps.get(suppNumber);
            if (CollectionUtils.isEmpty(assetsReports)) {
                suppNumberMaps.put(suppNumber, Lists.newArrayList(report));
            } else {
                assetsReports.add(report);
                suppNumberMaps.put(suppNumber, assetsReports);
            }
        }


        List<Map<String, Object>> list = Lists.newArrayList();
        HashMap<String, Object> heJi_fwc = Maps.newHashMap();
        // 服务处合计
        Integer yiTou_fwc = 0;
        Integer zaiCang_fwc = 0;
        Integer yiShi_fwc = 0;
        Integer baoFei_fwc = 0;
        for (Map.Entry<String, List<IceBoxAssetsReport>> entry : suppNumberMaps.entrySet()) {
            String suppNumber = entry.getKey();
            // 经销商小计
            Integer yiTou_jxs = 0;
            Integer zaiCang_jxs = 0;
            Integer yiShi_jxs = 0;
            Integer baoFei_jxs = 0;

            String suppName = null;
            List<IceBoxAssetsReport> reportList = entry.getValue();
            for (IceBoxAssetsReport report : reportList) {
                HashMap<String, Object> hashMap = Maps.newHashMap();
                suppName = report.getSuppName();
                hashMap.put("suppNumber", suppNumber);
                hashMap.put("suppName", suppName);
                hashMap.put("xingHao", report.getXingHao());
                hashMap.put("fenPei", null);
                hashMap.put("yiTou", report.getYiTou());
                hashMap.put("zaiCang", report.getZaiCang());
                hashMap.put("yiShi", report.getYiShi());
                hashMap.put("baoFei", report.getBaoFei());
                yiTou_jxs += report.getYiTou();
                zaiCang_jxs += report.getZaiCang();
                yiShi_jxs += report.getYiShi();
                baoFei_jxs += report.getBaoFei();
                list.add(hashMap);
            }
            HashMap<String, Object> heJi_jxs = Maps.newHashMap();
            heJi_jxs.put("suppNumber", suppNumber);
            heJi_jxs.put("suppName", suppName==null?null:suppName+"经销商小计");
            heJi_jxs.put("xingHao", null);
            heJi_jxs.put("fenPei", null);
            heJi_jxs.put("yiTou", yiTou_jxs);
            heJi_jxs.put("zaiCang", zaiCang_jxs);
            heJi_jxs.put("yiShi", yiShi_jxs);
            heJi_jxs.put("baoFei", baoFei_jxs);
            list.add(heJi_jxs);
            yiTou_fwc += yiTou_jxs;
            zaiCang_fwc += zaiCang_jxs;
            yiShi_fwc += yiShi_jxs;
            baoFei_fwc += baoFei_jxs;
        }
        heJi_fwc.put("suppName", "服务处合计");
        heJi_fwc.put("xingHao", null);
        heJi_fwc.put("fenPei", null);
        heJi_fwc.put("yiTou", yiTou_fwc);
        heJi_fwc.put("zaiCang", zaiCang_fwc);
        heJi_fwc.put("yiShi", yiShi_fwc);
        heJi_fwc.put("baoFei", baoFei_fwc);

        list.add(heJi_fwc);
        return list;
    }

    @Override
    public List<Map<String, Object>> readReportDqzj(Integer deptId) {

        if(deptId==null){
            return null;
        }
        List<Map<String, Object>> list = iceBoxAssetsReportDao.readReportDqzj(deptId);
        return list;
    }

    @Override
    public void syncOldDatas() {

        Integer count = iceBoxDao.selectCount(null);
        log.info("查询出了那么多条数据:{}", count);
        List<String> failAssetIds = Lists.newArrayList();
        /**
         *  分页查找数据
         */
        int pageNum = 10; // 每页数量
        int totalPage = (count - 1) / pageNum + 1; // 总页数
        for (int i = 0; i < totalPage; i++) {
            Integer pageCode = i * pageNum;
            log.info("费用执行导出总页数->{}当前页码:{}", totalPage, i + 1);
            List<IceBox> iceBoxList = iceBoxDao.readLimitData(pageCode, pageNum);
            for (IceBox iceBox : iceBoxList) {
                try {
                    SubordinateInfoVo infoVo = FeignResponseUtil.getFeignData(feignSupplierClient.readId(iceBox.getSupplierId()));
                    IceBoxAssetReportVo assetReportVo = IceBoxAssetReportVo.builder()
                            .assetId(iceBox.getAssetId())
                            .modelId(iceBox.getModelId())
                            .modelName(iceBox.getModelName())
                            .suppName(infoVo == null ? null : infoVo.getName())
                            .suppNumber(infoVo == null ? null : infoVo.getNumber())
                            .oldPutStatus(null) // 投放状态 0: 未投放 1:已锁定(被业务员申请) 2:投放中 3:已投放
                            .oldStatus(null) // 冰柜状态 0:异常，1:正常，2:报废，3:遗失，4:报修
                            .newPutStatus(iceBox.getPutStatus())
                            .newStatus(iceBox.getStatus())
                            .suppDeptId(iceBox.getDeptId()).build();
                    iceBoxAssetsReportService.createOne(assetReportVo);
                } catch (Exception e) {
                    log.info("同步老数据失败", e);
                }
                // 存储同步失败的数据
                failAssetIds.add(iceBox.getAssetId());
            }
        }
        log.info("同步失败的冰柜老数据到报表中:{}", failAssetIds);
    }
}
