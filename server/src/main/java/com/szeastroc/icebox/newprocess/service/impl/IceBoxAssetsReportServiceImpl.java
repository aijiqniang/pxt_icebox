package com.szeastroc.icebox.newprocess.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.exception.NormalOptionException;
import com.szeastroc.customer.client.FeignSupplierClient;
import com.szeastroc.icebox.newprocess.dao.IceBoxAssetsReportDao;
import com.szeastroc.icebox.newprocess.dao.IceBoxDao;
import com.szeastroc.icebox.newprocess.dao.IceBoxExtendDao;
import com.szeastroc.icebox.newprocess.entity.IceBoxAssetsReport;
import com.szeastroc.icebox.newprocess.enums.IceBoxEnums;
import com.szeastroc.icebox.newprocess.service.IceBoxAssetsReportService;
import com.szeastroc.icebox.newprocess.vo.IceBoxAssetReportVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    private final IceBoxDao iceBoxDao;
    private final IceBoxExtendDao iceBoxExtendDao;
    private final FeignSupplierClient feignSupplierClient;


    @Override
    public void createIceBoxAssetsReport(List<IceBoxAssetReportVo> lists) {

        if (CollectionUtils.isEmpty(lists)) {
            return;
        }
        for (IceBoxAssetReportVo assetReportVo : lists) {
            try {
                createOne(assetReportVo);
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
        ) {
            throw new NormalOptionException(Constants.API_CODE_FAIL, Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }
        IceBoxAssetsReport iceBoxAssetsReport = iceBoxAssetsReportDao.readBySuppNumber(assetReportVo.getSuppNumber());
        if (iceBoxAssetsReport == null) {
            iceBoxAssetsReport = new IceBoxAssetsReport();
        }
        iceBoxAssetsReport
                .setSuppNumber(assetReportVo.getSuppNumber())
                .setSuppName(assetReportVo.getSuppName())
                .setXingHao(assetReportVo.getModelName())
                .setXingHaoId(assetReportVo.getModelId());

        Integer oldStatus = assetReportVo.getOldStatus();
        Integer newStatus = assetReportVo.getNewStatus();
        Integer newPutStatus = assetReportVo.getNewPutStatus();
        if (oldStatus == null) { // 此次数据属于新增
            /**
             * @Date: 2020/10/20 10:51 xiao
             *  报表中只需要关心以下状态
             */
//            if(IceBoxEnums.StatusEnum.ABNORMAL.getType()){ // 异常,
//
//            }
        }
        if (IceBoxEnums.StatusEnum.ABNORMAL.getType().equals(oldStatus)) { // 此次数据属于将 异常状态的数据更新成其他状态

        }
        if (IceBoxEnums.StatusEnum.NORMAL.getType().equals(oldStatus)) { // 此次数据属于将 正常状态的数据更新成其他状态

        }

        Integer id = iceBoxAssetsReport.getId();

        if (id == null) {
            iceBoxAssetsReportDao.insert(iceBoxAssetsReport);
        } else {
            iceBoxAssetsReportDao.updateById(iceBoxAssetsReport);
        }
    }
}
