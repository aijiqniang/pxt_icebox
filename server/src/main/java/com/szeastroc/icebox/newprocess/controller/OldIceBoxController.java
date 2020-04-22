package com.szeastroc.icebox.newprocess.controller;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.WorkbookUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.utils.FeignResponseUtil;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.customer.client.FeignStoreClient;
import com.szeastroc.customer.common.vo.StoreInfoDtoVo;
import com.szeastroc.icebox.newprocess.dao.IceBoxDao;
import com.szeastroc.icebox.newprocess.dao.IceBoxExtendDao;
import com.szeastroc.icebox.newprocess.dao.IceModelDao;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.entity.IceBoxExtend;
import com.szeastroc.icebox.newprocess.entity.IceModel;
import lombok.extern.slf4j.Slf4j;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @RequestMapping("/import")
    @Transactional(rollbackFor = Exception.class, value = "transactionManager")
    public CommonResponse importExcel(@RequestParam("file") MultipartFile file) throws IOException, ImproperOptionException {
        Workbook book = WorkbookUtil.createBook(file.getInputStream(), true);
        ExcelReader excelReader = new ExcelReader(book, 0);
        List<List<Object>> reads = excelReader.read();
        List<Integer> pxtIds = new ArrayList<>();
        reads.forEach(x -> {
            String s = x.get(6).toString();
            if (StringUtils.isNotBlank(s)) {
                pxtIds.add(Integer.valueOf(s));
            }
        });
//        List<List<Integer>> partition = Lists.partition(pxtIds, 3000);

        //        for (List<Integer> list : partition) {
//            map.putAll(FeignResponseUtil.getFeignData(feignStoreClient.getDtoVoByPxtIds(list)));
//        }
        Map<Integer, StoreInfoDtoVo> map = new HashMap<>(FeignResponseUtil.getFeignData(feignStoreClient.getDtoVoByPxtIds(pxtIds)));
        reads.forEach(x -> {
            String s = x.get(6).toString();
            if (StringUtils.isNotBlank(s)) {
                IceBox iceBox = new IceBox();
                IceBoxExtend iceBoxExtend = new IceBoxExtend();
                // 资产编号
                String assetId = x.get(0).toString();
                // 冰柜名称
                String chestName = x.get(1).toString();
                // 品牌
                String brandName = x.get(2).toString();
                // 冰柜型号
                String modelName = x.get(3).toString();
                // 冰柜规格
                String chestNorm = x.get(4).toString();

                Integer pxtId = Integer.valueOf(s);
                StoreInfoDtoVo storeInfoDtoVo = map.get(pxtId);
                if (storeInfoDtoVo != null) {
                    String storeNumber = storeInfoDtoVo.getStoreNumber();
                    Integer deptId = storeInfoDtoVo.getMarketArea();
                    iceBox.setPutStoreNumber(storeNumber);
                    iceBox.setDeptId(deptId);
                }
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
                    DateTime parse = DateUtil.parse(lastPutTime, "dd/MM/yyyy HH:mm:ss");
                    iceBoxExtend.setLastPutTime(parse);
                }

                iceBoxDao.insert(iceBox);
                iceBoxExtend.setId(iceBox.getId());
                iceBoxExtendDao.insert(iceBoxExtend);
            }
        });
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }

}
