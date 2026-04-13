package com.tuna.ecommerce.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tuna.ecommerce.service.OrderService;
import com.tuna.ecommerce.ultil.anotation.APIMessage;
import com.tuna.ecommerce.ultil.err.IdInvalidException;

import java.net.URI;
import com.tuna.ecommerce.domain.Order;

@RestController
@RequestMapping("/api/v1/public")
public class PublicOrderController {

    private final OrderService orderService;

    @Value("${tuna.frontend-url}")
    private String frontendUrl;

    public PublicOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/order/confirm")
    @APIMessage("Xác nhận đơn hàng thành công")
    public ResponseEntity<Void> confirmOrder(@RequestParam("token") String token) throws IdInvalidException {
        Order order = this.orderService.handleConfirmOrder(token);
        
        String method = "cod";
        String transactionId = "ORD-" + order.getId();
        
        if (order.getPayment() != null) {
            if (order.getPayment().getMethod() != null) {
                method = order.getPayment().getMethod().name().toLowerCase();
            }
            if (order.getPayment().getTransactionId() != null) {
                transactionId = order.getPayment().getTransactionId();
            }
        }

        // Redirect to frontend confirmation success page
        String redirectUrl = frontendUrl + "/payment-result?status=confirmed&orderId=" + order.getId() 
            + "&method=" + method + "&transactionId=" + transactionId;
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirectUrl))
                .build();
    }
}
