package com.szeastroc.icebox.newprocess.controller;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.WorkbookUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.entity.customer.vo.SimpleSupplierInfoVo;
import com.szeastroc.common.entity.customer.vo.StoreInfoDtoVo;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.feign.customer.FeignStoreClient;
import com.szeastroc.common.feign.customer.FeignSupplierClient;
import com.szeastroc.common.utils.ExecutorServiceFactory;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.commondb.config.annotation.RedisLock;
import com.szeastroc.icebox.config.MqConstant;
import com.szeastroc.icebox.newprocess.dao.IceBoxDao;
import com.szeastroc.icebox.newprocess.dao.IceBoxExtendDao;
import com.szeastroc.icebox.newprocess.dao.IceModelDao;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.entity.IceBoxExtend;
import com.szeastroc.icebox.newprocess.entity.IceModel;
import com.szeastroc.icebox.newprocess.service.IceBoxService;
import com.szeastroc.icebox.newprocess.service.OldIceBoxOpt;
import com.szeastroc.icebox.newprocess.vo.OldIceBoxImportVo;
import com.szeastroc.icebox.util.NewExcelUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
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
    private final IceBoxService iceBoxService;
    private final FeignSupplierClient feignSupplierClient;
    private final OldIceBoxOpt oldIceBoxOpt;
    private final RabbitTemplate rabbitTemplate;

    @RequestMapping("/import")
    @Transactional(rollbackFor = Exception.class, value = "transactionManager")
    public CommonResponse importExcel(@RequestParam("excelFile") MultipartFile file) throws IOException, ImproperOptionException {
        log.info("??????????????????");
        Workbook book = WorkbookUtil.createBook(file.getInputStream(), true);
        ExcelReader excelReader = new ExcelReader(book, 0);
        List<List<Object>> reads = excelReader.read();
        log.info("??????excel????????????,reads?????????-->[{}]", reads.size());
//        List<Integer> pxtIds = new ArrayList<>();
//        reads.forEach(x -> {
//            String s = x.get(6).toString();
//            if (StringUtils.isNotBlank(s)) {
//                pxtIds.add(Integer.valueOf(s));
//            }
//        });
//        log.info("pxtIds?????????-->[{}]",pxtIds.size());
//        List<List<Integer>> partition = Lists.partition(pxtIds, 3000);

        //        for (List<Integer> list : partition) {
//            map.putAll(FeignResponseUtil.getFeignData(feignStoreClient.getDtoVoByPxtIds(list)));
//        }
//        Map<Integer, StoreInfoDtoVo> map = new HashMap<>(FeignResponseUtil.getFeignData(feignStoreClient.getDtoVoByPxtIds(pxtIds)));
//        log.info("map?????????-->[{}]",map.size());

        log.info("??????????????????");
        List<Object> list = new ArrayList<>();
        for (int i = 0, readsSize = reads.size(); i < readsSize; i++) {
            log.info("---------------???" + i + "?????????---------------");
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

                    // ????????????
                    String assetId = x.get(0).toString();

                    Integer integer = iceBoxExtendDao.selectCount(Wrappers.<IceBoxExtend>lambdaQuery().eq(IceBoxExtend::getAssetId, assetId));

                    if (integer == 0) {
                        IceBox iceBox = new IceBox();
                        IceBoxExtend iceBoxExtend = new IceBoxExtend();

                        // ????????????
                        String chestName = x.get(1).toString();
                        // ??????
                        String brandName = x.get(2).toString();
                        // ????????????
                        String modelName = x.get(3).toString();
                        // ????????????
                        String chestNorm = x.get(4).toString();
                        // ???????????????
                        iceBox.setPutStoreNumber(storeNumber);
                        // ??????????????????id??? ?????????????????????
                        iceBox.setDeptId(deptId);

                        // ??????
                        Object remarkObject = x.get(8);

                        if (remarkObject != null) {
                            String remark = remarkObject.toString();
                            if (StringUtils.isNotBlank(remark)) {
                                iceBox.setRemark(remark);
                            }
                        }

                        // ??????
                        Object depositMoneyObject = x.get(5);
                        if (null != depositMoneyObject) {
                            String depositMoney = depositMoneyObject.toString();

                            if (StringUtils.isNotBlank(depositMoney)) {

                                iceBox.setDepositMoney(new BigDecimal(depositMoney));
                            }
                        }
                        // ????????????
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
        log.info("??????????????????");
        log.info("?????????????????????-->[{}]", JSON.toJSONString(list, true));
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }


    /**
     * ????????????????????????????????????????????????????????????
     */
    @RequestMapping("dealOldIceBoxNotice")
    public CommonResponse<List<IceBox>> dealOldIceBoxNotice() {
        iceBoxService.dealOldIceBoxNotice();
        return new CommonResponse(Constants.API_CODE_SUCCESS, null);
    }


    /**
     * ????????????????????????????????????????????????
     *
     * @param file
     * @return
     * @throws IOException
     * @throws ImproperOptionException
     */
    @RedisLock(includeToken = true)
    @RequestMapping("/importOrUpdate")
    public CommonResponse<Void> importOrUpdate(@RequestParam("excelFile") MultipartFile file) throws IOException, ImproperOptionException {

        log.info("??????????????????");
        /*List<OldIceBoxImportVo> oldIceBoxImportVoList = EasyExcel.read(file.getInputStream()).head(OldIceBoxImportVo.class).sheet().doReadSync();
        if (CollectionUtil.isNotEmpty(oldIceBoxImportVoList)) {
            List<JSONObject> lists = oldIceBoxOpt.opt(oldIceBoxImportVoList);

            *//**
             * @Date: 2020/10/19 14:50 xiao
             *  ???????????????????????????????????????????????????????????????
             *//*
            if (CollectionUtils.isNotEmpty(lists)) {
                ExecutorServiceFactory.getInstance().execute(() -> {
                    for (JSONObject jsonObject : lists) {
                        // ??????mq??????
                        rabbitTemplate.convertAndSend(MqConstant.directExchange, MqConstant.ICEBOX_ASSETS_REPORT_ROUTING_KEY, jsonObject.toString());
                    }
                });
            }
        }*/
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }


    @GetMapping("/getImportExcel")
    public void getImportExcel(HttpServletResponse response) throws Exception {
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
        String fileName = "?????????????????????";
        String titleName = "?????????????????????";
        String[] columnName = {"?????????", "??????", "?????????", "?????????????????????", "?????????????????????", "????????????", "????????????", "??????", "????????????", "????????????", "????????????", "?????????????????????", "?????????????????????", "????????????", "????????????"};
        NewExcelUtil<OldIceBoxImportVo> excelUtil = new NewExcelUtil<>();
        List<OldIceBoxImportVo> oldIceBoxImportVoList = new ArrayList<OldIceBoxImportVo>();
        oldIceBoxImportVoList.add(OldIceBoxImportVo.builder().type("??????").build());
        oldIceBoxImportVoList.add(OldIceBoxImportVo.builder().type("??????").build());
        oldIceBoxImportVoList.add(OldIceBoxImportVo.builder().type("??????").build());
        oldIceBoxImportVoList.add(OldIceBoxImportVo.builder().type("??????").build());
        excelUtil.oldExportExcel(fileName, titleName, columnName, oldIceBoxImportVoList, response);
    }
}
