package com.szeastroc.icebox.newprocess.controller;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.WorkbookUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.utils.ExecutorServiceFactory;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.customer.client.FeignStoreClient;
import com.szeastroc.customer.client.FeignSupplierClient;
import com.szeastroc.customer.common.vo.SimpleSupplierInfoVo;
import com.szeastroc.customer.common.vo.StoreInfoDtoVo;
import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.icebox.newprocess.dao.IceBoxDao;
import com.szeastroc.icebox.newprocess.dao.IceBoxExtendDao;
import com.szeastroc.icebox.newprocess.dao.IceModelDao;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.entity.IceBoxExtend;
import com.szeastroc.icebox.newprocess.entity.IceModel;
import com.szeastroc.icebox.newprocess.service.OldIceBoxOpt;
import com.szeastroc.icebox.newprocess.vo.OldIceBoxImportVo;
import com.szeastroc.icebox.rabbitMQ.DirectProducer;
import com.szeastroc.icebox.rabbitMQ.MethodNameOfMQ;
import com.szeastroc.icebox.vo.DataPack;
import com.szeastroc.icebox.vo.IceBoxAssetReportVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/oldIceBox")
@RequiredArgsConstructor(onConstructor = @_(@Autowired))
public class OldIceBoxController {
    private final FeignStoreClient feignStoreClient;
    private final IceBoxDao iceBoxDao;
    private final IceBoxExtendDao iceBoxExtendDao;
    private final IceModelDao iceModelDao;
    private final FeignSupplierClient feignSupplierClient;
    private final OldIceBoxOpt oldIceBoxOpt;
    private final DirectProducer directProducer;

    @RequestMapping("/import")
    @Transactional(rollbackFor = Exception.class, value = "transactionManager")
    public CommonResponse importExcel(@RequestParam("excelFile") MultipartFile file) throws IOException, ImproperOptionException {
        log.info("开始读取数据");
        Workbook book = WorkbookUtil.createBook(file.getInputStream(), true);
        ExcelReader excelReader = new ExcelReader(book, 0);
        List<List<Object>> reads = excelReader.read();
        log.info("获取excel文件数据,reads的大小-->[{}]", reads.size());
//        List<Integer> pxtIds = new ArrayList<>();
//        reads.forEach(x -> {
//            String s = x.get(6).toString();
//            if (StringUtils.isNotBlank(s)) {
//                pxtIds.add(Integer.valueOf(s));
//            }
//        });
//        log.info("pxtIds的大小-->[{}]",pxtIds.size());
//        List<List<Integer>> partition = Lists.partition(pxtIds, 3000);

        //        for (List<Integer> list : partition) {
//            map.putAll(FeignResponseUtil.getFeignData(feignStoreClient.getDtoVoByPxtIds(list)));
//        }
//        Map<Integer, StoreInfoDtoVo> map = new HashMap<>(FeignResponseUtil.getFeignData(feignStoreClient.getDtoVoByPxtIds(pxtIds)));
//        log.info("map的大小-->[{}]",map.size());

        log.info("开始处理数据");
        List<Object> list = new ArrayList<>();
        for (int i = 0, readsSize = reads.size(); i < readsSize; i++) {
            log.info("---------------第" + i + "次循环---------------");
            List<Object> x = reads.get(i);
            String s = x.get(6).toString();
            if (StringUtils.isNotBlank(s)) {
                StoreInfoDtoVo storeInfoDtoVo = FeignResponseUtil.getFeignData(feignStoreClient.getDtoVoByPxtId(s));
                SimpleSupplierInfoVo supplierInfoVo = FeignResponseUtil.getFeignData(feignSupplierClient.findByPxtId(Integer.valueOf(s)));

                String storeNumber = "";
                Integer deptId = null;
                if (null != storeInfoDtoVo && StringUtils.isNotBlank(storeInfoDtoVo.getStoreNumber()) && null != supplierInfoVo && StringUtils.isNotBlank(supplierInfoVo.getNumber())) {
                    list.add(s);
                    continue;
                } else {
                    if (null != storeInfoDtoVo && StringUtils.isNotBlank(storeInfoDtoVo.getStoreNumber())) {
                        storeNumber = storeInfoDtoVo.getStoreNumber();
                        deptId = storeInfoDtoVo.getMarketArea();

                    } else if (null != supplierInfoVo && StringUtils.isNotBlank(supplierInfoVo.getNumber())) {
                        storeNumber = supplierInfoVo.getNumber();
                        deptId = supplierInfoVo.getMarketAreaId();
                    }
                }
                if (StringUtils.isNotBlank(storeNumber) && null != deptId) {

                    // 资产编号
                    String assetId = x.get(0).toString();

                    Integer integer = iceBoxExtendDao.selectCount(Wrappers.<IceBoxExtend>lambdaQuery().eq(IceBoxExtend::getAssetId, assetId));

                    if (integer == 0) {
                        IceBox iceBox = new IceBox();
                        IceBoxExtend iceBoxExtend = new IceBoxExtend();

                        // 冰柜名称
                        String chestName = x.get(1).toString();
                        // 品牌
                        String brandName = x.get(2).toString();
                        // 冰柜型号
                        String modelName = x.get(3).toString();
                        // 冰柜规格
                        String chestNorm = x.get(4).toString();
                        // 投放的门店
                        iceBox.setPutStoreNumber(storeNumber);
                        // 冰柜所属部门id， 旧冰柜采用的是
                        iceBox.setDeptId(deptId);

                        // 备注
                        Object remarkObject = x.get(8);

                        if (remarkObject != null) {
                            String remark = remarkObject.toString();
                            if (StringUtils.isNotBlank(remark)) {
                                iceBox.setRemark(remark);
                            }
                        }

                        // 押金
                        Object depositMoneyObject = x.get(5);
                        if (null != depositMoneyObject) {
                            String depositMoney = depositMoneyObject.toString();

                            if (StringUtils.isNotBlank(depositMoney)) {

                                iceBox.setDepositMoney(new BigDecimal(depositMoney));
                            }
                        }
                        // 投放日期
                        String lastPutTime = x.get(9).toString();
                        iceBoxExtend.setAssetId(assetId);
                        iceBox.setChestName(chestName);
                        iceBox.setBrandName(brandName);
                        iceBox.setChestNorm(chestNorm);
                        IceModel iceModel = iceModelDao.selectOne(Wrappers.<IceModel>lambdaQuery().eq(IceModel::getChestModel, modelName));
                        if (null != iceModel) {
                            iceBox.setModelId(iceModel.getId());
                        }
                        if (StringUtils.isNotBlank(lastPutTime)) {
                            DateTime parse = DateUtil.parse(lastPutTime, "yyyy-MM-dd HH:mm:ss");
                            iceBoxExtend.setLastPutTime(parse);
                        }

                        iceBoxDao.insert(iceBox);
                        iceBoxExtend.setId(iceBox.getId());
                        iceBoxExtendDao.insert(iceBoxExtend);
                    }
                }
            }
        }
        log.info("处理数据结束");
        log.info("同时存在的数据-->[{}]", JSON.toJSONString(list, true));
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }

    /**
     * 针对旧冰柜需要更新旧冰柜信息需求
     *
     * @param file
     * @return
     * @throws IOException
     * @throws ImproperOptionException
     */
    @RequestMapping("/importOrUpdate")
    public CommonResponse<Void> importOrUpdate(@RequestParam("excelFile") MultipartFile file) throws IOException, ImproperOptionException {

        log.info("开始读取数据");
        List<OldIceBoxImportVo> oldIceBoxImportVoList = EasyExcel.read(file.getInputStream()).head(OldIceBoxImportVo.class).sheet().doReadSync();
        if (CollectionUtil.isNotEmpty(oldIceBoxImportVoList)) {
            List<IceBoxAssetReportVo> lists = oldIceBoxOpt.opt(oldIceBoxImportVoList);

            /**
             * @Date: 2020/10/19 14:50 xiao
             *  将报表中导入数据库中的数据异步更新到报表中
             */
            if (CollectionUtils.isNotEmpty(lists)) {
                DataPack dataPack = new DataPack(); // 数据包
                dataPack.setMethodName(MethodNameOfMQ.CREATE_ICE_BOX_ASSETS_REPORT);
                dataPack.setObj(lists);
                ExecutorServiceFactory.getInstance().execute(() -> {
                    // 发送mq消息
                    directProducer.sendMsg(MqConstant.directRoutingKeyReport, dataPack);
                });
            }
        }
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }
}
