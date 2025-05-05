package com.xuecheng.orders.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.utils.IdWorkerUtils;
import com.xuecheng.base.utils.QRCodeUtil;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xuecheng.orders.config.AlipayConfig;
import com.xuecheng.orders.config.PayNotifyConfig;
import com.xuecheng.orders.mapper.XcOrdersGoodsMapper;
import com.xuecheng.orders.mapper.XcOrdersMapper;
import com.xuecheng.orders.mapper.XcPayRecordMapper;
import com.xuecheng.orders.model.dto.AddOrderDto;
import com.xuecheng.orders.model.dto.PayRecordDto;
import com.xuecheng.orders.model.dto.PayStatusDto;
import com.xuecheng.orders.model.po.XcOrders;
import com.xuecheng.orders.model.po.XcOrdersGoods;
import com.xuecheng.orders.model.po.XcPayRecord;
import com.xuecheng.orders.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


/**
 * 订单相关接口
 */
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Resource
    private XcOrdersMapper xcOrdersMapper;

    @Resource
    private XcOrdersGoodsMapper xcOrdersGoodsMapper;

    @Resource
    private MqMessageService mqMessageService;

    @Resource
    private XcPayRecordMapper xcPayRecordMapper;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Value("{pay.qrcodeurl}")
    private String qrcodeurl;


    /**
     * 创建商品订单
     * @param userId 用户id
     * @param addOrderDto 订单信息
     * @return PayRecordDto 支付记录（包括二维码）
     */
    @Transactional
    @Override
    public PayRecordDto createOrder(String userId, AddOrderDto addOrderDto){
        // 插入订单表
        XcOrders xcOrders = saveXcOrders(userId,addOrderDto);

        // 插入支付记录
        XcPayRecord xcPayRecord = createPayRecord(xcOrders);
        Long payNo = xcPayRecord.getPayNo();

        // 生成二维码
        QRCodeUtil qrCodeUtil = new QRCodeUtil();
        // 支付二维码的url
        String url = String.format(qrcodeurl,payNo);
        // 二维码图片
        String qrCode = null;
        try{
            qrCode = qrCodeUtil.createQRCode(url,200,200);
        }catch (IOException e){
            XueChengPlusException.cast("生成二维码出错");
        }

        PayRecordDto payRecordDto = new PayRecordDto();
        BeanUtils.copyProperties(xcPayRecord,payRecordDto);
        payRecordDto.setQrcode(qrCode);

        return payRecordDto;
    }


    /**
     * 插入订单表
     * @param userId 用户Id
     * @param addOrderDto 添加信息
     * @return
     */
    private XcOrders saveXcOrders(String userId, AddOrderDto addOrderDto) {
        // 进行幂等性判断，同一个选课记录只能有一个订单
        XcOrders xcOrders = getOrderByBusinessId(addOrderDto.getOutBusinessId());
        if(xcOrders != null){
            return xcOrders;
        }

        // 插入订单主表
        xcOrders = new XcOrders();
        // 使用雪花算法生成订单号
        xcOrders.setId(IdWorkerUtils.getInstance().nextId());
        xcOrders.setTotalPrice(addOrderDto.getTotalPrice());
        xcOrders.setCreateDate(LocalDateTime.now());
        xcOrders.setStatus("600001"); // 未支付
        xcOrders.setUserId(userId);
        xcOrders.setOrderType("60201");// 订单类型
        xcOrders.setOrderName(addOrderDto.getOrderName());
        xcOrders.setOrderDescrip(addOrderDto.getOrderDescrip());
        xcOrders.setOrderDetail(addOrderDto.getOrderDetail());
        xcOrders.setOutBusinessId(addOrderDto.getOutBusinessId());

        int insert = xcOrdersMapper.insert(xcOrders);
        if(insert <= 0){
            XueChengPlusException.cast("添加订单失败");
        }

        // 订单id
        Long orderId = xcOrders.getId();
        // 插入订单明细表
        // 将前端传入的明细json转为List
        String orderDetailJson = addOrderDto.getOrderDetail();
        List<XcOrdersGoods> xcOrdersGoods = JSON.parseArray(orderDetailJson, XcOrdersGoods.class);
        // 插入数据
        int result = xcOrdersGoodsMapper.insertBatch(xcOrdersGoods);
        if(result <= 0){
            XueChengPlusException.cast("插入订单明细表失败");
        }
        return xcOrders;
    }

    /**
     * 插入支付记录
     * @param xcOrders 订单信息
     * @return
     */
    private XcPayRecord createPayRecord(XcOrders xcOrders) {
        // 订单id
        Long orderId = xcOrders.getId();
        XcOrders orders = xcOrdersMapper.selectById(orderId);
        // 如果此订单不存在则不能添加支付记录
        if(orders == null){
            XueChengPlusException.cast("订单不存在");
        }
        // 订单状态
        String status = orders.getStatus();
        // 如果此订单支付结果为成功，不再添加支付记录，避免重复支付
        if("601002".equals(status)){
            // 支付成功
            XueChengPlusException.cast("此订单已支付");
        }
        // 添加支付记录
        XcPayRecord xcPayRecord = new XcPayRecord();
        xcPayRecord.setPayNo(IdWorkerUtils.getInstance().nextId());// 支付记录号，传给支付宝
        xcPayRecord.setOrderId(orderId);
        xcPayRecord.setOrderName(orders.getOrderName());
        xcPayRecord.setTotalPrice(orders.getTotalPrice());
        xcPayRecord.setCurrency("CNY");
        xcPayRecord.setStatus("601001"); // 未支付
        xcPayRecord.setUserId(orders.getUserId());

        int insert = xcPayRecordMapper.insert(xcPayRecord);
        if(insert <= 0){
            XueChengPlusException.cast("添加支付记录失败");
        }
        return xcPayRecord;
    }

    /**
     * 根据业务id查询订单，业务id为选课记录表中的主键
     * @param businessId 业务id
     */
    private XcOrders getOrderByBusinessId(String businessId){
        return xcOrdersMapper.selectOne(new LambdaQueryWrapper<XcOrders>().eq(XcOrders::getOutBusinessId,businessId));
    }


    /**
     * 查询支付记录
     * @param payNo 支付记录号
     * @return
     */
    @Override
    public XcPayRecord getPayRecordByPayNo(String payNo) {
        return xcPayRecordMapper.selectOne(new LambdaQueryWrapper<XcPayRecord>().eq(XcPayRecord::getPayNo,payNo));
    }

    @Override
    public PayRecordDto queryPayResult(String payNo) {
        // 调用支付宝接口查询支付结果
        PayStatusDto payStatusDto = queryPayResultFromAlipay(payNo);

        // 拿到支付结果更新支付记录和订单表的支付状态
        transactionTemplate.execute(e -> {
            saveAliPayStatus(payStatusDto);
            return null;
        });

        // 返回最新的支付记录信息
        XcPayRecord payRecordByPayno = getPayRecordByPayNo(payNo);
        PayRecordDto payRecordDto = new PayRecordDto();
        BeanUtils.copyProperties(payRecordByPayno,payRecordDto);

        return payRecordDto;
    }

    /**
     * 请求支付宝查询支付结果
     * @param payNo 支付交易号
     * @return 支付结果
     */
    private PayStatusDto queryPayResultFromAlipay(String payNo) {
        AlipayClient alipayClient = new DefaultAlipayClient(AlipayConfig.URL, AlipayConfig.APPID, AlipayConfig.RSA_PRIVATE_KEY, AlipayConfig.FORMAT, AlipayConfig.CHARSET, AlipayConfig.ALIPAY_PUBLIC_KEY,AlipayConfig.SIGNTYPE);
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", payNo);
        //bizContent.put("trade_no", "2014112611001004680073956707");
        request.setBizContent(bizContent.toString());
        //支付宝返回的信息
        String body = null;
        try {
            AlipayTradeQueryResponse response = alipayClient.execute(request); //通过alipayClient调用API，获得对应的response类
            if(!response.isSuccess()){//交易不成功
                XueChengPlusException.cast("请求支付宝查询支付结果失败");
            }
            body = response.getBody();
        } catch (AlipayApiException e) {
            e.printStackTrace();
            XueChengPlusException.cast("请求支付查询支付结果异常");
        }
        Map bodyMap = JSON.parseObject(body, Map.class);
        Map alipay_trade_query_response = (Map) bodyMap.get("alipay_trade_query_response");

        //解析支付结果
        String trade_no = (String) alipay_trade_query_response.get("trade_no");
        String trade_status = (String) alipay_trade_query_response.get("trade_status");
        String total_amount = (String) alipay_trade_query_response.get("total_amount");
        PayStatusDto payStatusDto = new PayStatusDto();
        payStatusDto.setOut_trade_no(payNo);
        payStatusDto.setTrade_no(trade_no);//支付宝的交易号
        payStatusDto.setTrade_status(trade_status);//交易状态
        payStatusDto.setApp_id(AlipayConfig.APPID);
        payStatusDto.setTotal_amount(total_amount);//总金额


        return payStatusDto;
    }


    /**
     * 保存支付宝支付结果
     * @param payStatusDto  支付结果信息 从支付宝查询到的信息
     */
    @Override
    public void saveAliPayStatus(PayStatusDto payStatusDto) {
        // 支付记录号
        String payNo = payStatusDto.getOut_trade_no();
        XcPayRecord payRecordByPayNo = getPayRecordByPayNo(payNo);
        if(payRecordByPayNo == null){
            XueChengPlusException.cast("支付记录不存在");
        }
        // 拿到相关联的订单id
        Long orderId = payRecordByPayNo.getId();
        XcOrders orders = xcOrdersMapper.selectById(orderId);
        if(orders == null){
            XueChengPlusException.cast("订单不存在");
        }
        // 支付状态
        String statusFromDb = payRecordByPayNo.getStatus();
        // 如果数据库支付的状态是成功，则不再处理
        if("601002".equals(statusFromDb)){
            return;
        }

        // 如果支付成功
        String tradeStatus = payStatusDto.getTrade_status();// 从支付宝查询到的支付结果
        if("TRADE_SUCCESS".equals(tradeStatus)){// 从支付宝返回的消息为支付成功
            // 更新支付记录表为支付成功
            payRecordByPayNo.setStatus("601002");
            // 支付宝的订单号
            payRecordByPayNo.setOutPayNo(payStatusDto.getTrade_no());
            // 第三方支付渠道编号
            payRecordByPayNo.setOutPayChannel("ALIPAY");
            // 支付成功时间
            payRecordByPayNo.setPaySuccessTime(LocalDateTime.now());
            xcPayRecordMapper.updateById(payRecordByPayNo);

            // 更新订单表的状态为支付成功
            orders.setStatus("600002");
            xcOrdersMapper.updateById(orders);

            // 将消息写到数据库
            MqMessage mqMessage = mqMessageService.addMessage("payresult_notify", orders.getOutBusinessId(),orders.getOrderType(),null);

            // 发送消息
            notifyPayResult(mqMessage);
        }
    }


    @Override
    public void notifyPayResult(MqMessage mqMessage) {
        // 消息内容
        String jsonString = JSON.toJSONString(mqMessage);
        // 创建一个持久化消息
        Message messageObj = MessageBuilder.withBody(jsonString.getBytes(StandardCharsets.UTF_8)).setDeliveryMode(MessageDeliveryMode.PERSISTENT).build();
        // 消息id
        Long id = mqMessage.getId();

        // 全局消息id

        CorrelationData correlationData = new CorrelationData(id.toString());
        // 使用correlationData指定回调方法
        correlationData.getFuture().addCallback(result -> {
            if(result.isAck()){
                // 消息成功发送到了交换机
                log.debug("消息发送成功:{}",jsonString);
                // 将消息从信息表删除
                mqMessageService.completed(id);
            }else{
                // 消息发送失败
                log.debug("消息发送失败:{}",jsonString);
            }
        },ex->{
            // 发生异常
            log.debug("消息发送异常:{}",jsonString);
        });

        // 发送消息
        rabbitTemplate.convertAndSend(PayNotifyConfig.PAYNOTIFY_EXCHANGE_FANOUT,"",messageObj,correlationData);
    }
}
