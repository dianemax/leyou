package com.leyou.order.web;

import com.leyou.order.dto.OrderDTO;
import com.leyou.order.pojo.Order;
import com.leyou.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 创建订单 返回订单ID
     * @param orderDTO
     * @return
     */
    @PostMapping
    public ResponseEntity<Long> createOrder(@RequestBody OrderDTO orderDTO){
        // 创建订单
        return ResponseEntity.ok(orderService.createOrder(orderDTO));
    }

    /**
     * 通过订单编号查询订单
     * @param id
     * @return
     */
    @GetMapping("{id}")
    public ResponseEntity<Order> queryOrderById(@PathVariable("id") Long id){
        return ResponseEntity.ok(orderService.queryOrderById(id));
    }

    @GetMapping("url/{id}")
    public ResponseEntity<String> createPayUrl(@PathVariable("id") Long orderId){
        return ResponseEntity.ok(orderService.createPayUrl(orderId));
    }

    @GetMapping("state/{id}")
    public ResponseEntity<Integer> queryOrderState(@PathVariable("id")Long orederId){
        return ResponseEntity.ok(orderService.queryOrderState(orederId).getValue());
    }

}
