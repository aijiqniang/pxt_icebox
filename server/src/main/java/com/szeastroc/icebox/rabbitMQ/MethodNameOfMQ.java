package com.szeastroc.icebox.rabbitMQ;

/**
 * @Author xiao
 * @Date create in 2020/6/15 16:58
 * @Description:
 */
public interface MethodNameOfMQ {

    String EXPORT_EXCEL_METHOD = "exportExcel";

    String EXPORT_ICE_REFUND = "exportIceRefund";

    // 创建或更新冰柜资产报表
    String CREATE_ICE_BOX_ASSETS_REPORT="createIceBoxAssetsReport";


    String EXPORT_ICE_TRANSFER = "exportIceTransfer";

}
