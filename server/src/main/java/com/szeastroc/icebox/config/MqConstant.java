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


}
