package com.szeastroc.icebox.oldprocess.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.szeastroc.common.exception.ImproperOptionException;
import com.szeastroc.common.exception.NormalOptionException;
import com.szeastroc.icebox.oldprocess.entity.IceChestInfo;
import com.szeastroc.icebox.oldprocess.entity.IceChestInfoImport;
import com.szeastroc.icebox.oldprocess.vo.IceChestInfoExcelVo;
import com.szeastroc.icebox.oldprocess.vo.IceChestResponse;

import java.util.List;

/**
 * Created by Tulane
 * 2019/5/21
 */
public interface IceChestInfoService extends IService<IceChestInfo>{

    IceChestResponse getIceChestByClientNumber(String clientNumber) throws NormalOptionException;

    IceChestResponse getIceChestByExternalId(String externalId) throws NormalOptionException, ImproperOptionException;

    IceChestResponse getIceChestByQrcode(String qrcode, String clientNumber) throws NormalOptionException, ImproperOptionException;

    String importIceInfoExcelVo(List<IceChestInfoExcelVo> list, IceChestInfoImport iceChestInfoImport);

}
