package com.szeastroc.icebox.newprocess.service;

import com.szeastroc.customer.common.vo.SimpleSupplierInfoVo;
import com.szeastroc.icebox.newprocess.entity.IceBox;
import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.icebox.newprocess.vo.IceBoxDetailVo;

import java.util.List;

public interface IceBoxService extends IService<IceBox> {


    IceBoxDetailVo findIceBoxById(Integer id);


    List<SimpleSupplierInfoVo> findSupplierByDeptId(Integer deptId);
}








