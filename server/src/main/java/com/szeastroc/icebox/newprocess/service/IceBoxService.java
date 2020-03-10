package com.szeastroc.icebox.newprocess.service;

import com.szeastroc.customer.common.vo.SimpleSupplierInfoVo;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.icebox.newprocess.vo.IceBoxDetailVo;
import com.szeastroc.icebox.newprocess.vo.IceBoxStatusVo;
import com.szeastroc.icebox.newprocess.vo.IceBoxStoreVo;


import java.util.List;

public interface IceBoxService extends IService<IceBox> {

    List<IceBoxStoreVo> getIceBoxStoreVoByPxtNumber(String pxtNumber);

    IceBoxDetailVo findIceBoxById(Integer id);

    List<SimpleSupplierInfoVo> findSupplierByDeptId(Integer deptId);

    IceBoxStatusVo checkBoxByQrcode(String qrcode, String pxtNumber);

}








