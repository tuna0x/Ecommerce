package com.tuna.ecommerce.controller;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tuna.ecommerce.domain.Order;
import com.tuna.ecommerce.domain.request.order.ReqBulkUpdateStatusDTO;
import com.tuna.ecommerce.domain.request.order.ReqCheckoutDTO;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import java.time.Instant;
import com.tuna.ecommerce.domain.response.order.ResGetOrderDTO;
import com.tuna.ecommerce.service.OrderService;
import com.tuna.ecommerce.ultil.constant.OrderStatusEnum;
import com.tuna.ecommerce.ultil.anotation.APIMessage;
import com.tuna.ecommerce.ultil.err.IdInvalidException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@AllArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping("/order/checkout")
    @APIMessage("checkout")
    public ResponseEntity<ResGetOrderDTO> checkout(@RequestBody ReqCheckoutDTO reqCheckoutDTO, HttpServletRequest request) throws IdInvalidException {
        return ResponseEntity.ok().body(this.orderService.createOrder(reqCheckoutDTO, request));
    }

    @GetMapping("/order/{id}")
    @APIMessage("get order by id")
    public ResponseEntity<ResGetOrderDTO> getOrder(@PathVariable("id") Long id) {
        Order order = this.orderService.getOrder(id);
        return ResponseEntity.ok().body(this.orderService.convertToResGetOrderDTO(order));
    }

    @PutMapping("/order/{id}/cancel")
    @APIMessage("customer cancel order")
    public ResponseEntity<Order> cancelOrder(@PathVariable("id") Long id, @RequestParam("reason") String reason) throws IdInvalidException {
        return ResponseEntity.ok().body(this.orderService.cancelOrder(id, reason));
    }

    @GetMapping("/order/me")
    @APIMessage("get orders by user")
    public ResponseEntity<ResultPaginationDTO> getMyOrders(Pageable pageable) {
        return ResponseEntity.ok().body(this.orderService.fetchOrdersByUser(pageable));
    }

    @GetMapping("/order/admin/all")
    @APIMessage("admin get all orders")
    public ResponseEntity<ResultPaginationDTO> getAllOrders(
            Pageable pageable,
            @RequestParam(value = "status", required = false) OrderStatusEnum status,
            @RequestParam(value = "startDate", required = false) Instant startDate,
            @RequestParam(value = "endDate", required = false) Instant endDate) {
        return ResponseEntity.ok().body(this.orderService.fetchAllOrders(pageable, status, startDate, endDate));
    }

    @PostMapping("/order/bulk-status")
    @APIMessage("bulk update order status")
    public ResponseEntity<Void> bulkUpdateOrderStatus(@RequestBody ReqBulkUpdateStatusDTO req)
            throws IdInvalidException {
        this.orderService.handleBulkUpdateStatus(req.getIds(), req.getStatus());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/order/{id}/status")
    @APIMessage("update order status")
    public ResponseEntity<Order> updateOrderStatus(@PathVariable("id") Long id,
            @RequestParam("status") OrderStatusEnum status) throws IdInvalidException {
        return ResponseEntity.ok().body(this.orderService.handleUpdateStatus(id, status));
    }

    @PostMapping("/order/{id}/ghn")
    @APIMessage("create ghn shipping order")
    public ResponseEntity<ResGetOrderDTO> createGhnOrder(@PathVariable("id") Long id) throws IdInvalidException {
        return ResponseEntity.ok().body(this.orderService.createGhnOrder(id));
    }

    @PostMapping("/order/bulk-ghn")
    @APIMessage("bulk create ghn shipping orders")
    public ResponseEntity<List<ResGetOrderDTO>> bulkCreateGhnOrder(@RequestBody List<Long> ids) {
        return ResponseEntity.ok().body(this.orderService.handleBulkCreateGhnOrders(ids));
    }
}
