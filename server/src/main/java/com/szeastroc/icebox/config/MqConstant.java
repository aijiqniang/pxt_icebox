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
     *  报表中使用的 queue 和  routingKey
     */
    String directQueueReport = "icebox_report_queue";
    String directRoutingKeyReport = "icebox_report_routing_key";
    /*****************************  直连模式的参数   *************************************/


}
