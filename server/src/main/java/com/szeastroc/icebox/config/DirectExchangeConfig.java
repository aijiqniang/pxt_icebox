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
    public SimpleRabbitListenerContainerFactory iceBoxPutContainer(){
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

    /**
     * DEATAIL_EXPORT
     * 队列消费配置
     */
    @Bean(name = "iceBoxExceptionContainer")
    public SimpleRabbitListenerContainerFactory iceBoxExceptionContainer(){
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factoryConfigurer.configure(factory, connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.NONE);
        factory.setPrefetchCount(10);
        return factory;
    }

    /**
     * @Date: 2020/10/19 14:13 xiao
     *  报表使用的消息队列
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

    @Bean(name = "iceExportExcelContainer")
    public SimpleRabbitListenerContainerFactory iceExportExcelContainer(){
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factoryConfigurer.configure(factory, connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.NONE);
        factory.setPrefetchCount(1);
        return factory;
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
}
