package com.szeastroc.icebox.newprocess.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.exception.NormalOptionException;
import com.szeastroc.customer.client.FeignSupplierClient;
import com.szeastroc.icebox.newprocess.dao.IceBoxAssetsReportDao;
import com.szeastroc.icebox.newprocess.dao.IceBoxDao;
import com.szeastroc.icebox.newprocess.dao.IceBoxExtendDao;
import com.szeastroc.icebox.newprocess.entity.IceBoxAssetsReport;
import com.szeastroc.icebox.newprocess.service.IceBoxAssetsReportService;
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
    private final IceBoxDao iceBoxDao;
    private final IceBoxExtendDao iceBoxExtendDao;
    private final FeignSupplierClient feignSupplierClient;


    @Override
    public void createIceBoxAssetsReport(List<Map<String ,Object>> lists) {

        if(CollectionUtils.isEmpty(lists)){
            return;
        }
        for (Map<String ,Object> map:lists){
            try {
                createOne(map);
            } catch (Exception e) {
                log.info("更新数据失败",e);
            }
        }
    }

    @Transactional
    @Override
    public void createOne(Map<String ,Object>map) {

        String  suppName = (String) map.get("suppName"); // 经销商名称
        String  suppNumber = (String) map.get("suppNumber"); // 经销商编号
        String  assetId = (String) map.get("assetId"); // 资产id
        String  modelName = (String) map.get("modelName"); // 型号名称
        Integer  modelId = (Integer) map.get("modelId"); // 型号id
        log.info("冰柜资产报表导入数据:suppName {},suppNumber {},assetId {},modelName {},modelId {}",suppName,suppNumber,assetId,modelName,modelId);
        if(StringUtils.isBlank(suppName)
                ||StringUtils.isBlank(suppNumber)
                ||StringUtils.isBlank(assetId)
                ||StringUtils.isBlank(modelName)
                ||modelId==null
        ){
            throw new NormalOptionException(Constants.API_CODE_FAIL,Constants.ErrorMsg.REQUEST_PARAM_ERROR);
        }

        IceBoxAssetsReport iceBoxAssetsReport = iceBoxAssetsReportDao.readByAssetId(assetId);
        iceBoxAssetsReport.setAssetId(assetId)
              .setSuppNumber(suppNumber)
              .setSuppName(suppName)
              .setXingHao(modelName)
              .setXingHaoId(modelId);
        Integer id = iceBoxAssetsReport.getId();

        if (id == null) {
            iceBoxAssetsReportDao.insert(iceBoxAssetsReport);
        } else {
            iceBoxAssetsReportDao.updateById(iceBoxAssetsReport);
        }
    }
}
