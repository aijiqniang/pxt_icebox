package com.szeastroc.icebox.newprocess.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.customer.common.vo.SimpleSupplierInfoVo;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.szeastroc.icebox.newprocess.vo.IceBoxDetailVo;
import com.szeastroc.icebox.newprocess.vo.IceBoxStatusVo;
import com.szeastroc.icebox.newprocess.vo.IceBoxStoreVo;
import com.szeastroc.icebox.newprocess.vo.IceBoxVo;
import com.szeastroc.icebox.newprocess.vo.request.IceBoxRequestVo;

import java.util.List;
import java.util.Map;

public interface IceBoxService extends IService<IceBox> {


    List<IceBoxVo> findIceBoxList(IceBoxRequestVo requestVo);

    IceBoxVo findBySupplierIdAndModelId(Integer supplierId, Integer modelId);

    Map<String, String> submitApply(IceBoxRequestVo iceBoxRequestVo);

    List<IceBoxStoreVo> getIceBoxStoreVoByPxtNumber(String pxtNumber);

    IceBoxDetailVo findIceBoxById(Integer id);

    List<SimpleSupplierInfoVo> findSupplierByDeptId(Integer deptId);

    IceBoxStatusVo checkBoxByQrcode(String qrcode, String pxtNumber);

    IceBoxVo getIceBoxByQrcode(String qrcode);
}








