package com.szeastroc.icebox.newprocess.controller;

import com.szeastroc.common.constant.Constants;
import com.szeastroc.common.vo.CommonResponse;
import com.szeastroc.icebox.sync.IceChestInfoSync;
import com.szeastroc.icebox.sync.IceOtherSync;
import com.szeastroc.icebox.sync.IcePutSync;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sync")
public class SyncController {

    @Autowired
    private IceChestInfoSync iceChestInfoSync;
    @Autowired
    private IcePutSync icePutSync;
    @Autowired
    private IceOtherSync iceOtherSync;

    /**
     * 同步冰柜相关
     *
     * @return
     * @throws Exception
     */
    @GetMapping("/syncIceChestInfo")
    public CommonResponse<Void> syncIceChestInfo() throws Exception {
        iceChestInfoSync.convertOldIceInfoToIcebox();
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }

    /**
     * 同步投放与退还相关
     *
     * @return
     * @throws Exception
     */
    @GetMapping("/syncIcePutAndBack")
    public CommonResponse<Void> syncIcePutAndBack() throws Exception {
        icePutSync.convertOldPutRecordToIcePutAndBack();
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }

    /**
     * 同步订单
     * @return
     */
    @GetMapping("/syncIceOrder")
    public CommonResponse<Void> syncIceOrder() throws Exception {
        iceOtherSync.syncPutOrder();
        iceOtherSync.syncPutPack();
        iceOtherSync.syncBackOrder();
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }

    /**
     * 同步冰柜扩展表的最近申请编号
     *
     * @return
     * @throws Exception
     */
    @GetMapping("/syncIceBoxExtendFromApplyName")
    public CommonResponse<Void> syncIceBoxExtendFromApplyName() throws Exception {
        iceOtherSync.syncIceBoxExtendFromApplyName();
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }

    @GetMapping("/syncIceBoxDept")
    public CommonResponse<Void> syncIceBoxDept() throws Exception {
        iceOtherSync.syncIceBoxDept();
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }


    @GetMapping("/syncIceBackApplyReport")
    public CommonResponse<Void> syncIceBackApplyReport() {
        try {
            iceOtherSync.syncIceBackApplyReport();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }



    @GetMapping("/syncIceInspectionReport")
    public CommonResponse<Void> syncIceInspectionReport() {
        try {
            iceOtherSync.syncIceInspectionReport();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new CommonResponse<>(Constants.API_CODE_SUCCESS, null);
    }


}
