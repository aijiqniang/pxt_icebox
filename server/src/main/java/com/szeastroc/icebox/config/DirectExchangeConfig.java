package com.szeastroc.icebox.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Date: 2020/6/12 17:09 xiao
 * direct直连模式的交换机配置,包括一个direct交换机，两个队列，三根网线binding
 */
@Configuration
public class DirectExchangeConfig {
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
}