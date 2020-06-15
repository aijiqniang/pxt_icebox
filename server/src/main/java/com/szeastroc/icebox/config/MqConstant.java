package com.szeastroc.icebox.config;

/**
 * @Author xiao
 * @Date create in 2020/6/12 17:09
 * @Description:
 */
public interface MqConstant {

    // mq 的路由接口,表示都用所有消费者都在这个队列中串行的消费消息
    String method1 = "";

    /*****************************  直连模式的参数   *************************************/
    String directExchange = "pxt_mq_exchange";
    String directQueue = "icebox_export_excel_queue";
    String directRoutingKey = "icebox_export_excel_routing_key";
    /*****************************  直连模式的参数   *************************************/


}
