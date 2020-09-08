package com.szeastroc.icebox.newprocess.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.customer.common.vo.SimpleSupplierInfoVo;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.entity.PutStoreRelateModel;
import com.szeastroc.icebox.newprocess.vo.IceBoxDetailVo;
import com.szeastroc.icebox.newprocess.vo.IceBoxStatusVo;
import com.szeastroc.icebox.newprocess.vo.IceBoxStoreVo;
import com.szeastroc.icebox.newprocess.vo.IceBoxVo;
import com.szeastroc.icebox.newprocess.vo.request.IceBoxPage;
import com.szeastroc.icebox.newprocess.vo.request.IceBoxRequestVo;
import com.szeastroc.icebox.newprocess.vo.request.IceExaminePage;
import com.szeastroc.icebox.newprocess.vo.request.IceTransferRecordPage;
import com.szeastroc.icebox.vo.IceBoxRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface IceBoxService extends IService<IceBox> {


    List<IceBoxVo> findIceBoxList(IceBoxRequestVo requestVo);

    IceBoxVo findBySupplierIdAndModelId(Integer supplierId, Integer modelId);

    Map<String, Object> submitApply(List<IceBoxRequestVo> iceBoxRequestVos) throws InterruptedException;

    List<IceBoxStoreVo> getIceBoxStoreVoByPxtNumber(String pxtNumber);

    IceBoxDetailVo findIceBoxById(Integer id);

    List<SimpleSupplierInfoVo> findSupplierByDeptId(Integer deptId);

    IceBoxStatusVo checkBoxByQrcode(String qrcode, String pxtNumber);

    IceBoxVo getIceBoxByQrcode(String qrcode);

    boolean judgeRecordTime(Integer id);

    void checkIceBox(IceBoxRequest iceBoxRequest);

    IPage findPage(IceBoxPage iceBoxPage);

    Map<String, Object> readBasic(Integer id);

    Map<String, Object> readModule(Integer id);

    Map<String, Object> readEquipNews(Integer id);

    IPage readTransferRecord(IceTransferRecordPage iceTransferRecordPage);

    IPage readExamine(IceExaminePage iceExaminePage);

    void importByEasyExcel(MultipartFile mfile) throws Exception;

    List<IceBox> getIceBoxList(String pxtNumber);

    Map<String, List<IceBoxVo>> findPutingIceBoxList(IceBoxRequestVo requestVo);

    List<IceBoxVo> findPutIceBoxList(String pxtNumber);

    Map<String, Object> submitApplyNew(List<IceBoxRequestVo> requestNewVos) throws InterruptedException;

    void checkIceBoxNew(IceBoxRequest iceBoxRequest);

    List<IceBoxVo> findIceBoxListNew(IceBoxRequestVo requestVo);

    List<PutStoreRelateModel> getIceBoxListNew(String pxtNumber);

    IceBoxStatusVo checkIceBoxByQrcodeNew(String qrcode, String pxtNumber);

    Map<String, List<IceBoxVo>> findPutingIceBoxListNew(IceBoxRequestVo requestVo);

    IceBoxVo getIceBoxByQrcodeNew(String qrcode, String pxtNumber);

    void autoAddLabel();


    void exportExcel(IceBoxPage iceBoxPage)throws Exception;

    void cancelApplyByNumber(IceBoxVo iceBoxVo);
}








