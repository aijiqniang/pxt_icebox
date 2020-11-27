package com.szeastroc.icebox.newprocess.service;

import com.szeastroc.icebox.newprocess.entity.IceModel;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface IceModelService extends IService<IceModel> {


    List<IceModel> getAllModel(Integer type);

}


