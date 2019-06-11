package com.leyou.order.service;

import com.leyou.auth.entity.UserInfo;
import com.leyou.common.dto.CartDTO;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.IdWorker;
import com.leyou.item.pojo.Sku;
import com.leyou.order.client.AddressClient;
import com.leyou.order.client.GoodsClient;
import com.leyou.order.dto.AddressDTO;
import com.leyou.order.dto.OrderDTO;
import com.leyou.order.enums.OrderStatusEnum;
import com.leyou.order.enums.PayState;
import com.leyou.order.interceptors.UserInterceptor;
import com.leyou.order.mapper.OrderDetailMapper;
import com.leyou.order.mapper.OrderMapper;
import com.leyou.order.mapper.OrderStatusMapper;
import com.leyou.order.pojo.Order;
import com.leyou.order.pojo.OrderDetail;
import com.leyou.order.pojo.OrderStatus;
import com.leyou.order.utils.PayHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private OrderStatusMapper orderStatusMapper;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private GoodsClient goodsClient;
    
    @Autowired
    private PayHelper payHelper;

    @Transactional
    public Long createOrder(OrderDTO orderDTO) {

        // 1 新增订单
        Order order = new Order();
        // 1.1 订单编号，基本信息 -- 订单ID，雪花算法（snowflake）生成全局唯一的ID
        long orderId = idWorker.nextId();
        order.setOrderId(orderId);
        order.setCreateTime(new Date());
        order.setPaymentType(orderDTO.getPaymentType());

        // 1.2 用户信息
        UserInfo user = UserInterceptor.getUser();
        order.setUserId(user.getId());
        order.setBuyerNick(user.getUsername());
        order.setBuyerRate(false);

        // 1.3 收货人地址信息 -- orderDTO中只有地址ID（addressID），要根据地址ID去数据库中查询(假数据)
        AddressDTO addr = AddressClient.findById(orderDTO.getAddressId());
        order.setReceiver(addr.getName());//收货人
        order.setReceiverMobile(addr.getPhone());//收货人手机号码
        order.setReceiverAddress(addr.getAddress());//收货所在街道
        order.setReceiverState(addr.getState());//收货人所在省
        order.setReceiverCity(addr.getCity());//收货人所在城市
        order.setReceiverDistrict(addr.getDistrict());//收货人所在区
        order.setReceiverZip(addr.getZipCode());//收货人邮编

        // 1.4 金额
        Map<Long, Integer> numMap = orderDTO.getCarts()
                .stream().collect(Collectors.toMap(CartDTO::getSkuId,CartDTO::getNum));
        Set<Long> ids = numMap.keySet();
        List<Sku> skus = goodsClient.querySkuByIds(new ArrayList<>(ids));

        // 准备orderDetail集合
        List<OrderDetail> details = new ArrayList<>();

        Long totalPrice = 0L;
        for (Sku sku : skus) {
            totalPrice += sku.getPrice() * numMap.get(sku.getId());

            //封装orderDetail
            OrderDetail detail = new OrderDetail();
            detail.setImage(StringUtils.substringBefore(sku.getImages(),","));
            detail.setNum(numMap.get(sku.getId()));
            detail.setOrderId(orderId);
            detail.setOwnSpec(sku.getOwnSpec());
            detail.setPrice(sku.getPrice());
            detail.setSkuId(sku.getId());
            detail.setTitle(sku.getTitle());

            details.add(detail);
        }
        order.setTotalPay(totalPrice);
        order.setActualPay(totalPrice + order.getPostFee() - 0 );// 实付金额= 总金额 + 邮费 - 优惠金额

        // 1.5 写入数据库
        int count = orderMapper.insertSelective(order);
        if(count != 1){
            log.error("[创建订单] 创建订单失败，orderID:{}", orderId);
            throw new LyException(ExceptionEnum.CREATE_ORDER_ERROR);
        }

        // 2 新增订单详情
        count = orderDetailMapper.insertList(details);
        if(count != details.size()){
            log.error("[创建订单] 创建订单失败，orderID:{}", orderId);
            throw new LyException(ExceptionEnum.CREATE_ORDER_ERROR);
        }

        // 3 新增订单状态
        OrderStatus orderStatus = new OrderStatus();
        orderStatus.setOrderId(orderId);
        orderStatus.setCreateTime(order.getCreateTime());
        orderStatus.setStatus(OrderStatusEnum.UN_PAY.value());
        count = orderStatusMapper.insertSelective(orderStatus);
        if(count != 1){
            log.error("[创建订单] 创建订单失败，orderID:{}", orderId);
            throw new LyException(ExceptionEnum.CREATE_ORDER_ERROR);
        }

        // 4 减库存 -- 需要调用商品微服务，传递商品id和数量两个参数
        List<CartDTO> cartDTOS = orderDTO.getCarts();
        goodsClient.decreaseStock(cartDTOS);


        return orderId;
    }

    public Order queryOrderById(Long id) {

        Order order = orderMapper.selectByPrimaryKey(id);
        if (order == null) {
            throw new LyException(ExceptionEnum.ORDER_NOT_FOUND);
        }

        // 查询订单详情
        OrderDetail detail = new OrderDetail();
        detail.setOrderId(id);
        List<OrderDetail> orderDetails = orderDetailMapper.select(detail);
        if(CollectionUtils.isEmpty(orderDetails)){
            throw new LyException(ExceptionEnum.ORDER_DETAIL_NOT_FOUNT);
        }
        order.setOrderDetails(orderDetails);

        // 查询订单状态
        OrderStatus orderStatus = orderStatusMapper.selectByPrimaryKey(id);
        if(orderStatus == null){
            throw new LyException(ExceptionEnum.ORDER_STATUS_NOT_FOUND);
        }
        order.setOrderStatus(orderStatus);

        return order;
    }

    public String createPayUrl(Long orderId) {

        // 查询订单获得总金额
        Order order = queryOrderById(orderId);

        // 判断订单状态，如果订单已支付，下面的查询就很多余
        OrderStatus orderStatus = order.getOrderStatus();
        Integer status = orderStatus.getStatus();
        if(status != OrderStatusEnum.UN_PAY.value()){
            throw new LyException(ExceptionEnum.ORDER_STATUS_ERROE);
        }

        Long actualPay = 1L/*order.getActualPay()*/;
        OrderDetail detail = order.getOrderDetails().get(0);//订单中可能有多件商品，获取第一件商品的标题作为订单的描述
        String desc = detail.getTitle();

        return payHelper.createOrder(orderId, actualPay, desc);
    }

    public void handleNotify(Map<String, String> result) {
        // 1 数据校验
        payHelper.isSuccess(result);
        // 2 签名校验
        payHelper.isValidSign(result);

        // 3 金额校验
        String totalFeeStr = result.get("total_fee");
        String tradeNo = result.get("out_trade_no");
        if(StringUtils.isEmpty(totalFeeStr) || StringUtils.isEmpty(tradeNo)){
            throw new LyException(ExceptionEnum.INVALID_ORDER_PARAM);
        }
        Long totalFee = Long.valueOf(totalFeeStr);
        Long orderId = Long.valueOf(tradeNo);
        Order order = orderMapper.selectByPrimaryKey(orderId);
        if(totalFee != /*order.getActualPay()*/ 1){
            // 金额不符
            throw new LyException(ExceptionEnum.INVALID_ORDER_PARAM);
        }

        // 4 修改订单状态
        OrderStatus status = new OrderStatus();
        status.setStatus(OrderStatusEnum.PAYED.value());
        status.setOrderId(orderId);
        status.setPaymentTime(new Date());
        int count = orderStatusMapper.updateByPrimaryKeySelective(status);
        if(count != 1){
            throw new LyException(ExceptionEnum.UPDATE_ORDER_STATUS_ERROR);
        }

        log.info("[订单回调], 订单支付成功! 订单编号:{}", orderId);

    }

    public PayState queryOrderState(Long orderId) {

        OrderStatus orderStatus = orderStatusMapper.selectByPrimaryKey(orderId);
        Integer status = orderStatus.getStatus();
        if(!status.equals(OrderStatusEnum.UN_PAY.value())){
            return PayState.SUCCESS;// 如果是已支付，则是真的已支付
        }

        // 如果未支付,但其实不一定是未支付,必须去微信查询支付状态
        return payHelper.queryPayState(orderId);
    }
}
