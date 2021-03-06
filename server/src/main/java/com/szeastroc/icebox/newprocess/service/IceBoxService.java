package com.szeastroc.icebox.newprocess.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.common.entity.customer.vo.SimpleSupplierInfoVo;
import com.szeastroc.common.entity.icebox.vo.IceBoxRequest;
import com.szeastroc.common.entity.icebox.vo.IceBoxTransferHistoryVo;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.entity.IceBoxExtend;
import com.szeastroc.icebox.newprocess.entity.PutStoreRelateModel;
import com.szeastroc.icebox.newprocess.vo.*;
import com.szeastroc.icebox.newprocess.vo.request.IceBoxPage;
import com.szeastroc.icebox.newprocess.vo.request.IceBoxRequestVo;
import com.szeastroc.icebox.newprocess.vo.request.IceExaminePage;
import com.szeastroc.icebox.newprocess.vo.request.IceTransferRecordPage;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

public interface IceBoxService extends IService<IceBox> {


    List<IceBoxVo> findIceBoxList(IceBoxRequestVo requestVo);

    IceBoxVo findBySupplierIdAndModelId(Integer supplierId, Integer modelId);

    Map<String, Object> submitApply(List<IceBoxRequestVo> iceBoxRequestVos) throws InterruptedException;

    List<IceBoxStoreVo> getIceBoxStoreVoByPxtNumber(String pxtNumber);

    IceBoxDetailVo findIceBoxById(Integer id);

    List<SimpleSupplierInfoVo> findSupplierByDeptId(Integer deptId,String assetId);

    IceBoxStatusVo checkBoxByQrcode(String qrcode, String pxtNumber);

    IceBoxVo getIceBoxByQrcode(String qrcode);

    boolean judgeRecordTime(Integer id);

    void checkIceBox(IceBoxRequest iceBoxRequest);

    IPage findPage(IceBoxPage iceBoxPage);

    boolean dealIceBoxPage(IceBoxPage iceBoxPage);

    Map<String, Object> readBasic(Integer id);

    Map<String, Object> readModule(Integer id);

    Map<String, Object> readEquipNews(Integer id);

    IPage readTransferRecord(IceTransferRecordPage iceTransferRecordPage);

    IPage readExamine(IceExaminePage iceExaminePage);

    List<JSONObject> importByEasyExcel(MultipartFile mfile) throws Exception;

    List<IceBox> getIceBoxList(String pxtNumber);

    Map<String, List<IceBoxVo>> findPutingIceBoxList(IceBoxRequestVo requestVo);

    List<IceBoxVo> findPutIceBoxList(String pxtNumber);

    Map<String, Object> submitApplyNew(List<IceBoxRequestVo> requestNewVos) throws InterruptedException;

    void checkIceBoxNew(IceBoxRequest iceBoxRequest);

//    void dealCheckPassIceBox(IceBoxRequest iceBoxRequest);

    List<IceBoxVo> findIceBoxListNew(IceBoxRequestVo requestVo);

    List<PutStoreRelateModel> getIceBoxListNew(String pxtNumber);

    IceBoxStatusVo checkIceBoxByQrcodeNew(String qrcode, String pxtNumber);

    Map<String, List<IceBoxVo>> findPutingIceBoxListNew(IceBoxRequestVo requestVo);

    IceBoxVo getIceBoxByQrcodeNew(String qrcode, String pxtNumber);

    void autoAddLabel();


    void exportExcel(IceBoxPage iceBoxPage) throws Exception;

    void cancelApplyByNumber(IceBoxVo iceBoxVo);

    void cancelBoxBack(String applyNumber,String cancelRemark);

    PutStoreRelateModel getApplyInfoByNumber(String applyNumber);

    List<IceBoxVo> findIceBoxsBySupplierId(Integer supplierId);

    Map<String, Object> transferIceBoxs(IceBoxTransferHistoryVo historyVo);

    void dealTransferCheck(IceBoxTransferHistoryVo historyVo);

    void dealOldIceBoxNotice();

    IceBoxVo getIceBoxById(Integer id, String pxtNumber);

    IceBoxVo getIceBoxVo(String pxtNumber, IceBoxExtend iceBoxExtend, IceBox iceBox);

    void changeIcebox(IceBoxManagerVo iceBoxManagerVo);

    void test();


    void changeAssetId(Integer iceBoxId, String assetId, boolean reconfirm);


    IceBoxStatusVo checkIceBoxById(Integer id, String pxtNumber);

    List<Map<String, String>> findIceBoxsModelBySupplierId(Integer supplierId);

    List<IceBoxVo> findIceBoxsBySupplierIdAndModelId(Integer supplierId, Integer modelId);

    JSONObject setAssetReportJson(IceBox iceBox, String resourceStr);

    List<Integer> getPutBoxIds(Integer userId);

    List<Integer> getNormalPutBoxIds(Integer userId);

    /**
     * ??????????????????
     *
     * @return
     */
    int getLostScrapCount(List<Integer> putBoxIds);

    /**
     * ?????????????????????????????????
     */
    void changeCustomer(IceBox newIceBox, IceBox oldIcebox);

    String createIcePutData(IceBox iceBox, String newPutStoreNumber);

    int getLostScrapCount(Integer userId);

    void saveIceBoxPutReport(IceBox iceBox, String applyNumber, String putStoreNumber);

    IceBoxCustomerVo getIceBoxCustomerVo(String putStoreNumber);

    void createOldIceBoxSignNotice(IceBox iceBox, String applyNumber, String storeNumber);

    Map<String, Object> checkApplyStatus(List<IceBoxRequestVo> iceBoxRequestVos);

    void helpSignIcebox(String assestId, String applyNumber);

    List<IceBox> getByResponsmanId(Integer userId);

    void handelIceBoxDate(MultipartFile file) throws IOException;

    List<IceBox> getByResponsmanIdAndTime(Integer userId, String endTime) throws ParseException;
}








