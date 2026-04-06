package com.tuna.ecommerce.controller;

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
import com.tuna.ecommerce.domain.Payment;
import com.tuna.ecommerce.domain.request.order.ReqCheckoutDTO;
import com.tuna.ecommerce.domain.response.ResultPaginationDTO;
import com.tuna.ecommerce.domain.response.order.ResGetOrderDTO;
import com.tuna.ecommerce.domain.response.payment.ResPaymentVNPAYDTO;
import com.tuna.ecommerce.service.OrderService;
import com.tuna.ecommerce.service.PaymentService;
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
    private final PaymentService paymentService;

    @PostMapping("/order/checkout")
    @APIMessage("checkout")
    public ResponseEntity<?> checkout(@RequestBody ReqCheckoutDTO reqCheckoutDTO, HttpServletRequest request) throws IdInvalidException {
        Order order = this.orderService.createOder(reqCheckoutDTO);
        Payment payment;
        switch (reqCheckoutDTO.getPaymentMethod()) {
            case COD:
                this.paymentService.createCODPayment(order.getId());
                return ResponseEntity.ok().body(this.orderService.convertToResGetOderDTO(order));
            case VNPAY:
                payment = this.paymentService.createPendingVNPayPayment(order.getId());
                ResPaymentVNPAYDTO res = this.paymentService.createVnPayPayment(request, payment.getId());
                return ResponseEntity.ok().body(res);
            default:
                break;
        }
        return ResponseEntity.badRequest().body("Invalid Payment Method");
    }

    @GetMapping("/order/{id}")
    @APIMessage("get order by id")
    public ResponseEntity<ResGetOrderDTO> getOrder(@PathVariable("id") Long id) {
        Order order = this.orderService.getOrder(id);
        return ResponseEntity.ok().body(this.orderService.convertToResGetOderDTO(order));
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
            @RequestParam(value = "status", required = false) OrderStatusEnum status) {
        return ResponseEntity.ok().body(this.orderService.fetchAllOrders(pageable, status));
    }

    @PutMapping("/order/{id}/status")
    @APIMessage("update order status")
    public ResponseEntity<Order> updateOrderStatus(@PathVariable("id") Long id, @RequestParam("status") OrderStatusEnum status) throws IdInvalidException {
        return ResponseEntity.ok().body(this.orderService.handleUpdateStatus(id, status));
    }
}
