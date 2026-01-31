package com.tuna.ecommerce.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tuna.ecommerce.domain.Order;
import com.tuna.ecommerce.domain.Payment;
import com.tuna.ecommerce.domain.request.order.ReqCheckoutDTO;
import com.tuna.ecommerce.domain.response.order.ResGetOrderDTO;
import com.tuna.ecommerce.service.OrderService;
import com.tuna.ecommerce.service.PaymentService;
import com.tuna.ecommerce.ultil.anotation.APIMessage;
import com.tuna.ecommerce.ultil.err.IdInvalidException;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@AllArgsConstructor
public class OrderController {
    private final OrderService orderService;
    private final PaymentService paymentService;

    @PostMapping("/order/checkout")
    @APIMessage("checkout")
    public ResponseEntity<?> checkout(@RequestBody ReqCheckoutDTO reqCheckoutDTO) throws IdInvalidException {
        Order order=this.orderService.createOder(reqCheckoutDTO);
        Payment payment;
        switch (reqCheckoutDTO.getPaymentMethod()) {
            case COD:
                payment= this.paymentService.createCODPayment(order.getId());
                return ResponseEntity.ok().body(payment);
            case VNPAY:
                payment= this.paymentService.createPendingVNPayPayment(order.getId());
                return ResponseEntity.ok().body(payment);
            default:
                break;
        }
        return ResponseEntity.badRequest().body("Invalid Payment Method");
    }

    @GetMapping("/order/{id}")
    @APIMessage("get order by id")
    public ResponseEntity<ResGetOrderDTO> getOrder(@PathVariable("id") Long id) {
        Order order=this.orderService.getOrder(id);
        return ResponseEntity.ok().body(this.orderService.convertToResGetOderDTO(order));
    }
}
