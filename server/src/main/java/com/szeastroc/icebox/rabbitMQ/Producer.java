package com.szeastroc.icebox.rabbitMQ;


/**
 * @Author xiao
 * @Date create in 2020/6/12 17:33
 * @Description:
 */
public abstract class Producer {

    abstract void sendMsg(String routingKey, DataPack dataPack); // 向队列中发送消息
}
