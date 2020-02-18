package com.szeastroc.icebox.newprocess.vo;

import com.szeastroc.icebox.newprocess.entity.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PutApplyWithPutApplyRelateBox {

    private List<IcePutApply> icePutApplies;
    private List<IcePutApplyRelateBox> icePutApplyRelateBoxes;

    private List<IceBackApply> iceBackApplies;
    private List<IceBackApplyRelateBox> iceBackApplyRelateBoxes;

    private List<IceTransferRecord> iceTransferRecords;

}
