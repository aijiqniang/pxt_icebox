package com.szeastroc.icebox.config;

/**
 * @Author xiao
 * @Date create in 2020/6/12 17:09
 * @Description:
 */
public interface MqConstant {
    /*****************************  直连模式的参数   *************************************/
    String directExchange = "pxt_mq_exchange";
    String directQueue = "icebox_export_excel_queue";
    String directRoutingKey = "icebox_export_excel_routing_key";
    /**
     * @Date: 2020/10/19 14:14 xiao
     * 冰柜资产
     * 报表中使用的 queue 和  routingKey
     */
    String ICEBOX_ASSETS_REPORT_QUEUE = "icebox_assets_report_queue";
    String ICEBOX_ASSETS_REPORT_ROUTING_KEY = "icebox_assets_report_routing_key";

    /**
     * 冰柜投放报表
     */
    String iceboxReportQueue = "ice_box_report_queue";
    String iceboxReportKey = "ice_box_report_routing_key";

    /**
     * 冰柜异常报备报表
     */
    String iceboxExceptionReportQueue = "ice_box_exception_report_queue";
    String iceboxExceptionReportKey = "ice_box_exception_report_routing_key";
    /*****************************  直连模式的参数   *************************************/


    /**
     * 冰柜退还报表
     */
    String iceBackApplyReportQueue = "ice_back_apply_report_queue";
    String iceBackApplyReportKey = "ice_back_apply_report_routing_key";


    /**
     * 小程序冰柜巡检报表
     */
    String iceInspectionReportQueue = "ice_inspection_report_queue";
    String iceInspectionReportKey = "ice_inspection_report_routing_key";

    /**
     * 客户信息变更，变更冰柜营销区域
     */

    String E_STORE_CHANGE_EXCHANGE = "E_STORE_CHANGE_EXCHANGE";

    String Q_STORE_CHANGE_ICEBOX_DEPT = "Q_STORE_CHANGE_ICEBOX_DEPT";
    String EXPORT_EXCEL_QUEUE = "export_excel_queue";
    String EXPORT_CHANGE_RECORD_QUEUE = "export_change_record_queue";
//  http://10.136.15.102:15672/#/exchanges/pxt/pxt_mq_exchange
    /**
     * 冰柜退还报表
     */
    String iceRepairOrderQueue = "ice_repair_order_queue";
    String iceRepairOrderKey = "ice_repair_order_routing_key";

}
