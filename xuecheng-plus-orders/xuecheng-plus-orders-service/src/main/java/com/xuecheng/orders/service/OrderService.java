package com.xuecheng.orders.service;


import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.orders.model.dto.AddOrderDto;
import com.xuecheng.orders.model.dto.PayRecordDto;
import com.xuecheng.orders.model.dto.PayStatusDto;
import com.xuecheng.orders.model.po.XcPayRecord;

/**
 * 订单相关接口
 */
public interface OrderService {

    /**
     * 创建商品订单
     * @param userId 用户id
     * @param addOrderDto 订单信息
     * @return PayRecordDto 支付记录（包括二维码）
     */
    PayRecordDto createOrder(String userId, AddOrderDto addOrderDto);

    /**
     * 查询支付记录
     * @param payNo 支付记录号
     * @return
     */
    XcPayRecord getPayRecordByPayNo(String payNo);

    /**
     * 查询支付结果
     * @param payNo
     * @return
     */
    PayRecordDto queryPayResult(String payNo);

    /**
     * 保存支付状态
     */
    void saveAliPayStatus(PayStatusDto payStatusDto);

    /**
     * 发送通知结果
     */
    void notifyPayResult(MqMessage mqMessage);
}
