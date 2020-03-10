package com.szeastroc.icebox.newprocess.vo;

import com.szeastroc.icebox.oldprocess.entity.IceEventRecord;
import lombok.*;

@Getter
@Setter
public class IceBoxStoreVo {

    private IceBoxVo iceBoxVo;
    private IcePutApplyRelateBoxVo icePutApplyRelateBoxVo;
    private IceEventRecord iceEventRecord;
}
