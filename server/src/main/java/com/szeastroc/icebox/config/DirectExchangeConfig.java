package com.szeastroc.icebox.config;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Date: 2020/6/12 17:09 xiao
 * direct直连模式的交换机配置,包括一个direct交换机，两个队列，三根网线binding
 */
@Configuration
@RequiredArgsConstructor
public class DirectExchangeConfig {

    private final SimpleRabbitListenerContainerFactoryConfigurer factoryConfigurer;
    private final CachingConnectionFactory connectionFactory;

    // 定义交换机
    @Bean
    public DirectExchange directExchange() {
        DirectExchange directExchange = new DirectExchange(MqConstant.directExchange);
        return directExchange;
    }

    @Bean
    public DirectExchange storeExportExchange(){
        return (DirectExchange)ExchangeBuilder.directExchange(MqConstant.E_EXCHANGE).durable(true).build();
    }

    // 定义交队列
    @Bean
    public Queue directQueue() {
        Queue queue = new Queue(MqConstant.directQueue);
        return queue;
    }

    // 定义队列跟交换机的绑定关系
    @Bean
    public Binding bindingExchange() {
        Binding binding = BindingBuilder.bind(directQueue()).to(directExchange()).with(MqConstant.directRoutingKey);
        return binding;
    }

    // 定义冰柜投放队列
    @Bean(name = "iceboxReportQueue")
    public Queue iceboxReportQueue() {
        Queue queue = new Queue(MqConstant.iceboxReportQueue);
        return queue;
    }

    // 定义队列跟交换机的绑定关系
    @Bean
    public Binding bindingIceBoxPutExchange() {
        Binding binding = BindingBuilder.bind(iceboxReportQueue()).to(directExchange()).with(MqConstant.iceboxReportKey);
        return binding;
    }

    /**
     * DEATAIL_EXPORT
     * 队列消费配置
     */
    @Bean(name = "iceBoxPutContainer")
    public SimpleRabbitListenerContainerFactory iceBoxPutContainer() {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factoryConfigurer.configure(factory, connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.NONE);
        factory.setPrefetchCount(10);
        return factory;
    }

    // 定义冰柜异常报备队列
    @Bean(name = "iceboxExceptionReportQueue")
    public Queue iceboxExceptionReportQueue() {
        Queue queue = new Queue(MqConstant.iceboxExceptionReportQueue);
        return queue;
    }

    // 定义队列跟交换机的绑定关系
    @Bean
    public Binding bindingIceBoxExceptionExchange() {
        Binding binding = BindingBuilder.bind(iceboxExceptionReportQueue()).to(directExchange()).with(MqConstant.iceboxExceptionReportKey);
        return binding;
    }


    // 定义冰柜异常报备队列
    @Bean(name = "iceBackApplyReportQueue")
    public Queue iceBackApplyReportQueue() {
        Queue queue = new Queue(MqConstant.iceBackApplyReportQueue);
        return queue;
    }

    // 定义队列跟交换机的绑定关系
    @Bean
    public Binding bindingIceBackApplyReportExchange() {
        Binding binding = BindingBuilder.bind(iceBackApplyReportQueue()).to(directExchange()).with(MqConstant.iceBackApplyReportKey);
        return binding;
    }


    // 定义冰柜异常报备队列
    @Bean(name = "iceInspectionReportQueue")
    public Queue iceInspectionReportQueue() {
        Queue queue = new Queue(MqConstant.iceInspectionReportQueue);
        return queue;
    }

    // 定义队列跟交换机的绑定关系
    @Bean
    public Binding bindingIceInspectionReportExchange() {
        Binding binding = BindingBuilder.bind(iceInspectionReportQueue()).to(directExchange()).with(MqConstant.iceInspectionReportKey);
        return binding;
    }

    /**
     * DEATAIL_EXPORT
     * 队列消费配置
     */
    @Bean(name = "iceBoxExceptionContainer")
    public SimpleRabbitListenerContainerFactory iceBoxExceptionContainer() {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factoryConfigurer.configure(factory, connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        factory.setPrefetchCount(10);
        return factory;
    }

    /**
     * @Date: 2020/10/19 14:13 xiao
     * 报表使用的消息队列
     */
    // 定义交队列
    @Bean
    public Queue directQueueReport() {
        Queue queue = new Queue(MqConstant.ICEBOX_ASSETS_REPORT_QUEUE);
        return queue;
    }

    // 定义队列跟交换机的绑定关系
    @Bean
    public Binding bindingExchangeReport() {
        Binding binding = BindingBuilder.bind(directQueueReport()).to(directExchange()).with(MqConstant.ICEBOX_ASSETS_REPORT_ROUTING_KEY);
        return binding;
    }

    /**
     * 客户信息修改mq配置
     */
    @Bean
    public FanoutExchange storeChangeExchange() {
        return (FanoutExchange) ExchangeBuilder.fanoutExchange(MqConstant.E_STORE_CHANGE_EXCHANGE).durable(true).build();
    }

    // 修改冰柜营销区域
    @Bean
    public Queue storeInfoChangeQueue() {
        return QueueBuilder.durable(MqConstant.Q_STORE_CHANGE_ICEBOX_DEPT).build();
    }

    @Bean
    public Binding storeInfoChangeBinding(Queue storeInfoChangeQueue, FanoutExchange storeChangeExchange) {
        return BindingBuilder.bind(storeInfoChangeQueue).to(storeChangeExchange);
    }

    @Bean(name = "iceExportExcelContainer")
    public SimpleRabbitListenerContainerFactory iceExportExcelContainer() {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factoryConfigurer.configure(factory, connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.NONE);
        factory.setPrefetchCount(1);
        return factory;
    }


    // 定义冰柜保修订单队列
    @Bean(name = "iceRepairOrderQueue")
    public Queue iceRepairOrderQueue() {
        Queue queue = new Queue(MqConstant.iceRepairOrderQueue);
        return queue;
    }

    // 定义队列跟交换机的绑定关系
    @Bean
    public Binding bindingIceRepairOrderExchange() {
        Binding binding = BindingBuilder.bind(iceRepairOrderQueue()).to(directExchange()).with(MqConstant.iceRepairOrderKey);
        return binding;
    }

    @Bean
    public Queue exportExcelQueue() {
        Queue queue = new Queue(MqConstant.EXPORT_EXCEL_QUEUE);
        return queue;
    }

    @Bean
    public Binding exportExcelBinding() {
        Binding binding = BindingBuilder.bind(exportExcelQueue()).to(directExchange()).with(MqConstant.EXPORT_EXCEL_QUEUE);
        return binding;
    }

    @Bean
    public Queue exportChangeRecordQueue() {
        Queue queue = new Queue(MqConstant.EXPORT_CHANGE_RECORD_QUEUE);
        return queue;
    }

    @Bean
    public Binding exportChangeRecordBinding() {
        Binding binding = BindingBuilder.bind(exportChangeRecordQueue()).to(directExchange()).with(MqConstant.EXPORT_CHANGE_RECORD_QUEUE);
        return binding;
    }

    /**
     * 冰柜事件推送
     */
    @Bean
    public Queue iceboxEventPushQueue() {
        return new Queue(MqConstant.ICEBOX_EVENT_PUSH_QUEUE);
    }

    /**
     * 冰柜事件推送
     */
    @Bean
    public Binding iceboxEventPushQueueBinding() {
        return BindingBuilder.bind(iceboxEventPushQueue()).to(directExchange()).with(MqConstant.ICEBOX_EVENT_PUSH_QUEUE);
    }

    /**
     * 冰柜事件推送
     */
    @Bean(name = "iceboxEventPushContainer")
    public SimpleRabbitListenerContainerFactory iceboxEventPushContainer() {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factoryConfigurer.configure(factory, connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.NONE);
        factory.setPrefetchCount(1);
        return factory;
    }

    /**
     * 冰柜申请审批推送
     */
    @Bean(name = "iceboxPutApplyContainer")
    public SimpleRabbitListenerContainerFactory iceboxPutApplyContainer() {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factoryConfigurer.configure(factory, connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        factory.setPrefetchCount(1);
        return factory;
    }

    // 冰柜申请审批通知
    @Bean
    public Queue iceBoxPutApplyQueue() {
        return QueueBuilder.durable(MqConstant.ICE_BOX_PUT_APPLY_Q).build();
    }

    @Bean
    public Binding iceBoxPutApplyBinding(Queue iceBoxPutApplyQueue, DirectExchange storeExportExchange) {
        return BindingBuilder.bind(iceBoxPutApplyQueue).to(storeExportExchange).with(MqConstant.ICE_BOX_PUT_APPLY_K);
    }

    /**
     * 冰柜交接
     */
    @Bean
    public Queue iceboxHandoverQueue() {
        return new Queue(MqConstant.ICE_BOX_HANDOVER_QUEUE);
    }

    @Bean
    public Binding iceboxHandoverQueueBinding() {
        return BindingBuilder.bind(iceboxHandoverQueue()).to(directExchange()).with(MqConstant.ICE_BOX_HANDOVER_QUEUE);
    }

    @Bean(name = "iceboxHandoverContainer")
    public SimpleRabbitListenerContainerFactory iceboxHandoverContainer() {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factoryConfigurer.configure(factory, connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        factory.setPrefetchCount(1);
        return factory;
    }

    /**
     * 货架申请审批推送
     */
    @Bean(name = "shelfPutApplyContainer")
    public SimpleRabbitListenerContainerFactory shelfPutApplyContainer() {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factoryConfigurer.configure(factory, connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        factory.setPrefetchCount(1);
        return factory;
    }

    /**
     * 陈列架退还审批推送
     */
    @Bean(name = "shelfBackApplyContainer")
    public SimpleRabbitListenerContainerFactory shelfBackApplyContainer() {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factoryConfigurer.configure(factory, connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        factory.setPrefetchCount(1);
        return factory;
    }

    // 冰柜申请审批通知
    @Bean
    public Queue shelfPutApplyQueue() {
        return QueueBuilder.durable(MqConstant.SHELF_PUT_APPLY_Q).build();
    }

    @Bean
    public Binding shelfPutApplyBinding(Queue shelfPutApplyQueue, DirectExchange storeExportExchange) {
        return BindingBuilder.bind(shelfPutApplyQueue).to(storeExportExchange).with(MqConstant.SHELF_PUT_APPLY_K);
    }

    //陈列架退还
    @Bean
    public Queue shelfBackApplyQueue() {
        return QueueBuilder.durable(MqConstant.SHELF_RETURN_APPLY_Q).build();
    }

    @Bean
    public Binding shelfBackApplyBinding(Queue shelfBackApplyQueue, DirectExchange storeExportExchange) {
        return BindingBuilder.bind(shelfBackApplyQueue).to(storeExportExchange).with(MqConstant.SHELF_RETURN_APPLY_K);
    }

    @Bean(name = "shelfPutReportQueue")
    public Queue shelfPutReportQueue() {
        Queue queue = new Queue(MqConstant.shelfPutReportQueue);
        return queue;
    }

    @Bean
    public Binding bindShelfPutReportQueue() {
        Binding binding = BindingBuilder.bind(shelfPutReportQueue()).to(directExchange()).with(MqConstant.shelfPutReportKey);
        return binding;
    }



    @Bean(name = "shelfInspectReportQueue")
    public Queue shelfInspectReportQueue() {
        Queue queue = new Queue(MqConstant.shelfInspectReportQueue);
        return queue;
    }

    @Bean
    public Binding bindShelfInspectReportQueue() {
        Binding binding = BindingBuilder.bind(shelfInspectReportQueue()).to(directExchange()).with(MqConstant.shelfInspectReportKey);
        return binding;
    }
}
