package com.szeastroc.icebox.newprocess.service.impl;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.customer.client.FeignStoreClient;
import com.szeastroc.customer.client.FeignSupplierClient;
import com.szeastroc.customer.common.vo.StoreInfoDtoVo;
import com.szeastroc.customer.common.vo.SubordinateInfoVo;
import com.szeastroc.icebox.newprocess.dao.IceBoxChangeHistoryDao;
import com.szeastroc.icebox.newprocess.entity.IceBoxChangeHistory;
import com.szeastroc.icebox.newprocess.service.IceBoxChangeHistoryService;
import com.szeastroc.icebox.newprocess.vo.request.IceChangeHistoryPage;
import com.szeastroc.user.client.FeignCacheClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class IceBoxChangeHistoryServiceImpl extends ServiceImpl<IceBoxChangeHistoryDao, IceBoxChangeHistory> implements IceBoxChangeHistoryService {


    @Resource
    private IceBoxChangeHistoryDao iceBoxChangeHistoryDao;
    @Resource
    private FeignCacheClient feignCacheClient;
    @Resource
    private FeignStoreClient feignStoreClient;
    @Resource
    private FeignSupplierClient feignSupplierClient;

    @Override
    public IPage<IceBoxChangeHistory> iceBoxChangeHistoryService(IceChangeHistoryPage iceChangeHistoryPage) {

        Integer iceBoxId = iceChangeHistoryPage.getIceBoxId();

        IPage<IceBoxChangeHistory> iPage = iceBoxChangeHistoryDao.selectPage(iceChangeHistoryPage, Wrappers.<IceBoxChangeHistory>lambdaQuery().eq(IceBoxChangeHistory::getIceBoxId, iceBoxId).orderByDesc(IceBoxChangeHistory::getCreateTime));

        iPage.convert(iceBoxChangeHistory -> {

            Integer oldMarketAreaId = iceBoxChangeHistory.getOldMarketAreaId();
            Integer newMarketAreaId = iceBoxChangeHistory.getNewMarketAreaId();

            String oldMarkerAreaName = FeignResponseUtil.getFeignData(feignCacheClient.getForMarketAreaName(oldMarketAreaId));
            String newMarkerAreaName = FeignResponseUtil.getFeignData(feignCacheClient.getForMarketAreaName(newMarketAreaId));
            iceBoxChangeHistory.setOldMarkAreaName(oldMarkerAreaName);
            iceBoxChangeHistory.setNewMarkAreaName(newMarkerAreaName);

            String oldPutStoreNumber = iceBoxChangeHistory.getOldPutStoreNumber();
            String newPutStoreNumber = iceBoxChangeHistory.getNewPutStoreNumber();
            String oldStoreMsg = "";
            String newStoreMsg = "";
            if (null != oldPutStoreNumber) {
                StoreInfoDtoVo oldStoreInfoDtoVo = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(oldPutStoreNumber));
                if (null != oldStoreInfoDtoVo && oldStoreInfoDtoVo.getId() != null) {
                    oldStoreMsg = oldStoreInfoDtoVo.getStoreName() + "(" + oldStoreInfoDtoVo.getStoreNumber() + ")";
                } else {
                    SubordinateInfoVo subordinateInfoVo = FeignResponseUtil.getFeignData(feignSupplierClient.findByNumber(oldPutStoreNumber));
                    if (null != subordinateInfoVo && StringUtils.isNotBlank(subordinateInfoVo.getNumber())) {
                        oldStoreMsg = subordinateInfoVo.getName() + "(" + subordinateInfoVo.getNumber() + ")";
                    }
                }
            }


            if (null != newPutStoreNumber) {
                StoreInfoDtoVo newStoreInfoDtoVo = FeignResponseUtil.getFeignData(feignStoreClient.getByStoreNumber(newPutStoreNumber));
                if (null != newStoreInfoDtoVo && newStoreInfoDtoVo.getId() != null) {
                    newStoreMsg = newStoreInfoDtoVo.getStoreName() + "(" + newStoreInfoDtoVo.getStoreNumber() + ")";
                } else {
                    SubordinateInfoVo subordinateInfoVo = FeignResponseUtil.getFeignData(feignSupplierClient.findByNumber(newPutStoreNumber));
                    if (null != subordinateInfoVo && StringUtils.isNotBlank(subordinateInfoVo.getNumber())) {
                        newStoreMsg = subordinateInfoVo.getName() + "(" + subordinateInfoVo.getNumber() + ")";
                    }
                }
            }

            iceBoxChangeHistory.setOldStoreName(oldStoreMsg);
            iceBoxChangeHistory.setNewStoreName(newStoreMsg);
            return iceBoxChangeHistory;
        });

        return iPage;
    }
}
