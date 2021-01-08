package com.szeastroc.icebox.newprocess.service.impl;


import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szeastroc.common.entity.customer.vo.StoreInfoDtoVo;
import com.szeastroc.common.entity.customer.vo.SubordinateInfoVo;
import com.szeastroc.common.feign.customer.FeignStoreClient;
import com.szeastroc.common.feign.customer.FeignSupplierClient;
import com.szeastroc.common.feign.user.FeignCacheClient;
import com.szeastroc.common.feign.visit.FeignExportRecordsClient;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.common.utils.ImageUploadUtil;
import com.szeastroc.icebox.constant.IceBoxConstant;
import com.szeastroc.icebox.newprocess.dao.IceBoxChangeHistoryDao;
import com.szeastroc.icebox.newprocess.dao.IceBoxDao;
import com.szeastroc.icebox.newprocess.dao.IceModelDao;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.entity.IceBoxChangeHistory;
import com.szeastroc.icebox.newprocess.entity.IceModel;
import com.szeastroc.icebox.newprocess.enums.IceBoxEnums;
import com.szeastroc.icebox.newprocess.service.IceBoxChangeHistoryService;
import com.szeastroc.icebox.newprocess.service.IceBoxService;
import com.szeastroc.icebox.newprocess.vo.IceBoxChangeRecordVo;
import com.szeastroc.icebox.newprocess.vo.IceBoxExcelVo;
import com.szeastroc.icebox.newprocess.vo.request.IceBoxPage;
import com.szeastroc.icebox.newprocess.vo.request.IceChangeHistoryPage;
import com.szeastroc.icebox.util.CreatePathUtil;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
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
    @Autowired
    private IceBoxService iceBoxService;
    @Autowired
    private IceModelDao iceModelDao;
    @Autowired
    private IceBoxDao iceBoxDao;
    @Autowired
    private FeignExportRecordsClient feignExportRecordsClient;
    @Autowired
    private ImageUploadUtil imageUploadUtil;

    @Override
    public IPage<IceBoxChangeHistory> iceBoxChangeHistoryService(IceChangeHistoryPage iceChangeHistoryPage) {

        Integer iceBoxId = iceChangeHistoryPage.getIceBoxId();
        IPage<IceBoxChangeHistory> iPage = iceBoxChangeHistoryDao.selectPage(iceChangeHistoryPage, Wrappers.<IceBoxChangeHistory>lambdaQuery().eq(IceBoxChangeHistory::getIceBoxId, iceBoxId).orderByDesc(IceBoxChangeHistory::getCreateTime));
        iPage.convert(iceBoxChangeHistory -> {
            getIceBoxChangeHistory(iceBoxChangeHistory);
            return iceBoxChangeHistory;
        });

        return iPage;
    }

    @Override
    public void exportChangeRecord(IceBoxPage iceBoxPage) throws Exception {

        if (iceBoxService.dealIceBoxPage(iceBoxPage)) {
            return;
        }
        Map<String, Object> param = new HashMap<>();
        param.put("deptIds", iceBoxPage.getDeptIds());
        param.put("supplierIdList", iceBoxPage.getSupplierIdList());
        param.put("putStoreNumberList", iceBoxPage.getPutStoreNumberList());
        param.put("assetId", iceBoxPage.getAssetId());
        param.put("status", iceBoxPage.getStatus());
        param.put("putStatus", iceBoxPage.getPutStatus());
        param.put("belongObj", iceBoxPage.getBelongObj());

        Integer count = iceBoxDao.exportExcelCount(param);
        if (count == null || count == 0) {
            return;
        }

        // 方法1 如果写到同一个sheet
        String xlsxPath = CreatePathUtil.creatDocPath();
        // 这里 需要指定写用哪个class去写
        ExcelWriter excelWriter = EasyExcel.write(xlsxPath, IceBoxChangeRecordVo.class).build();
        // 这里注意 如果同一个sheet只要创建一次
        WriteSheet writeSheet = EasyExcel.writerSheet("冰柜变更记录报表").build();
        Integer dangQianTiao = 0;
        /**
         *  分页查找数据
         */
        int pageNum = 96; // 每页数量
        int totalPage = (count - 1) / pageNum + 1; // 总页数
        for (int i = 0; i < totalPage; i++) {
            Integer pageCode = i * pageNum;
            param.put("pageCode", pageCode);
            param.put("pageNum", pageNum);
            List<IceBox> iceBoxes = iceBoxDao.exportExcel(param);
            if (CollectionUtils.isEmpty(iceBoxes)) {
                continue;
            }
            List<Integer> iceBoxIds = iceBoxes.stream().map(x -> x.getId()).collect(Collectors.toList());
            List<IceBoxChangeHistory> changeHistoryList = iceBoxChangeHistoryDao.selectList(Wrappers.<IceBoxChangeHistory>lambdaQuery()
                    .in(IceBoxChangeHistory::getIceBoxId, iceBoxIds)
            );
            if (CollectionUtils.isEmpty(changeHistoryList)) {
                continue;
            }
            // 对结果塞入到excel中
            List<IceBoxChangeRecordVo> iceBoxChangeRecordVoList = new ArrayList<>(iceBoxes.size());
            for (IceBoxChangeHistory iceBoxChangeHistory : changeHistoryList) {
                dangQianTiao = dangQianTiao + 1;
                log.info("冰柜变更信息导出:总条数:{},当前条数:{},iceBoxHisId:{},exportRecordId:{}", count, dangQianTiao, iceBoxChangeHistory.getId(), iceBoxPage.getExportRecordId());
                /**
                 * @Date: 2021/1/8 9:10 xiao
                 *  调取梦阳接口,获取原始数据
                 */
                getIceBoxChangeHistory(iceBoxChangeHistory);

                IceBoxChangeRecordVo changeRecordVo = new IceBoxChangeRecordVo();
                changeRecordVo.setCreateByName(iceBoxChangeHistory.getCreateByName())
                        .setCreateTimeStr(iceBoxChangeHistory.getCreateTime() == null ? null : new DateTime(iceBoxChangeHistory.getCreateTime()).toString("yyyy-MM-dd HH:mm:ss"))
                        // 变更前信息
                        .setNewMarkAreaName(iceBoxChangeHistory.getNewMarkAreaName())
                        .setNewAssetId(iceBoxChangeHistory.getNewAssetId())
                        .setNewSupplierName(iceBoxChangeHistory.getNewSupplierName())
                        .setNewSupplierNumber(iceBoxChangeHistory.getNewSupplierNumber())
                        .setNewStoreName(iceBoxChangeHistory.getNewStoreName())
                        .setNewBrandName(iceBoxChangeHistory.getNewBrandName())
                        .setNewChestDepositMoney(iceBoxChangeHistory.getNewChestDepositMoney())
                        .setNewChestMoney(iceBoxChangeHistory.getNewChestMoney())
                        .setNewChestName(iceBoxChangeHistory.getNewChestName())
                        .setNewChestNorm(iceBoxChangeHistory.getNewChestNorm())
                        .setNewModelName(iceBoxChangeHistory.getNewModelName())
                        .setNewRemake(iceBoxChangeHistory.getNewRemake())
                        .setNewStatusStr(iceBoxChangeHistory.getNewStatusStr())

                        // 变更后信息
                        .setOldMarkAreaName(iceBoxChangeHistory.getOldMarkAreaName())
                        .setOldAssetId(iceBoxChangeHistory.getOldAssetId())
                        .setOldSupplierName(iceBoxChangeHistory.getOldSupplierName())
                        .setOldSupplierNumber(iceBoxChangeHistory.getOldSupplierNumber())
                        .setOldStoreName(iceBoxChangeHistory.getOldStoreName())
                        .setOldBrandName(iceBoxChangeHistory.getOldBrandName())
                        .setOldChestDepositMoney(iceBoxChangeHistory.getOldChestDepositMoney())
                        .setOldChestMoney(iceBoxChangeHistory.getOldChestMoney())
                        .setOldChestName(iceBoxChangeHistory.getOldChestName())
                        .setOldChestNorm(iceBoxChangeHistory.getOldChestNorm())
                        .setOldModelName(iceBoxChangeHistory.getOldModelName())
                        .setOldRemake(iceBoxChangeHistory.getOldRemake())
                        .setOldStatusStr(iceBoxChangeHistory.getOldStatusStr());
                iceBoxChangeRecordVoList.add(changeRecordVo);
            }
            // 写入excel
            excelWriter.write(iceBoxChangeRecordVoList, writeSheet);
            iceBoxChangeRecordVoList=null;
        }
        // 千万别忘记finish 会帮忙关闭流
        excelWriter.finish();

        File xlsxFile = new File(xlsxPath);
        @Cleanup InputStream in = new FileInputStream(xlsxFile);
        try {
            String frontName = new DateTime().toString("yyyy-MM-dd-HH-mm-ss");
            // todo 上传临时文件到网络
            String imgUrl = imageUploadUtil.wechatUpload(in, IceBoxConstant.ICE_BOX, "BGBGDC" + frontName, "xlsx");
            // 更新下载列表中的数据
            feignExportRecordsClient.updateExportRecord(imgUrl, 1, iceBoxPage.getExportRecordId());
        } catch (Exception e) {
            log.info("付费陈列导出excel错误", e);
        } finally {
            // 删除临时目录
            if (StringUtils.isNotBlank(xlsxPath)) {
                FileUtils.deleteQuietly(xlsxFile);
            }
        }
    }


    private void getIceBoxChangeHistory(IceBoxChangeHistory iceBoxChangeHistory) {
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
        if (StringUtils.isNotBlank(oldPutStoreNumber)) {
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


        if (StringUtils.isNotBlank(newPutStoreNumber)) {
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

        iceBoxChangeHistory.setOldStatusStr(IceBoxEnums.StatusEnum.getDesc(iceBoxChangeHistory.getOldStatus()));
        iceBoxChangeHistory.setNewStatusStr(IceBoxEnums.StatusEnum.getDesc(iceBoxChangeHistory.getNewStatus()));
    }
}
