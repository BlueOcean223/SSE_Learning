package com.xuecheng.learning.service.impl;


import com.alibaba.fastjson.JSON;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.learning.config.PayNotifyConfig;
import com.xuecheng.learning.service.MyCourseTablesService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;


/**
 * 接受消息通知
 */
@Slf4j
@Service
public class ReceivePayNotifyService {

    @Resource
    private MyCourseTablesService myCourseTablesService;

    @RabbitListener(queues = PayNotifyConfig.PAYNOTIFY_QUEUE)
    public void receive(Message message){
        // 让程序休眠5秒
        try{
            Thread.sleep(5000);
        }catch (Exception e){
            e.printStackTrace();
        }

        // 解析出消息
        byte[] body = message.getBody();
        String jsonString = new String(body);
        MqMessage mqMessage = JSON.parseObject(jsonString, MqMessage.class);
        // 解析消息内容
        // 选课id
        String chooseCourseId = mqMessage.getBusinessKey1();
        // 订单类型
        String orderType = mqMessage.getBusinessKey2();
        // 学习中心服务只要购买课程的支付订单结果
        if("60201".equals(orderType)){
            boolean result = myCourseTablesService.saveChooseCourseSuccess(chooseCourseId);
            if(!result){
                XueChengPlusException.cast("保存选课记录失败");
            }
        }
    }
}
