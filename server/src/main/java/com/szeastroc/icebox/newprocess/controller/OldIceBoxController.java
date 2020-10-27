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
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.customer.client.FeignStoreClient;
import com.szeastroc.customer.client.FeignSupplierClient;
import com.szeastroc.customer.common.vo.SimpleSupplierInfoVo;
import com.szeastroc.customer.common.vo.StoreInfoDtoVo;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("oldIceBox")
public class OldIceBoxController {
    @Autowired
    private FeignStoreClient feignStoreClient;
    @Autowired
    private IceBoxDao iceBoxDao;
    @Autowired
    private IceBoxExtendDao iceBoxExtendDao;
    @Autowired
    private IceModelDao iceModelDao;
    @Autowired
    private IceBoxService iceBoxService;
    @Autowired
    private FeignSupplierClient feignSupplierClient;
    @Resource
    private OldIceBoxOpt oldIceBoxOpt;

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
     * 查询已投放未重新签收的旧冰柜发送签收通知
     */
    @RequestMapping("dealOldIceBoxNotice")
    public CommonResponse<List<IceBox>> dealOldIceBoxNotice() {
        iceBoxService.dealOldIceBoxNotice();
        return new CommonResponse(Constants.API_CODE_SUCCESS, null);
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
            oldIceBoxOpt.opt(oldIceBoxImportVoList);
        }
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }


    @GetMapping("/getImportExcel")
    public void getImportExcel(HttpServletResponse response) throws Exception {
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
        String fileName = "旧冰柜导入模板";
        String titleName = "旧冰柜导入模板";
        String[] columnName = {"事业部", "大区", "服务处", "所属经销商编号", "所属经销商名称", "冰柜编号", "冰柜名称", "品牌", "冰柜型号", "冰柜规格", "押金金额", "现投放门店编号", "现投放门店名称", "冰柜状态", "导入类型"};
        NewExcelUtil<OldIceBoxImportVo> excelUtil = new NewExcelUtil<>();
        List<OldIceBoxImportVo> oldIceBoxImportVoList = new ArrayList<OldIceBoxImportVo>();
        oldIceBoxImportVoList.add(OldIceBoxImportVo.builder().type("新增").build());
        oldIceBoxImportVoList.add(OldIceBoxImportVo.builder().type("退仓").build());
        oldIceBoxImportVoList.add(OldIceBoxImportVo.builder().type("报废").build());
        excelUtil.oldExportExcel(fileName, titleName, columnName, oldIceBoxImportVoList, response);
    }
}
