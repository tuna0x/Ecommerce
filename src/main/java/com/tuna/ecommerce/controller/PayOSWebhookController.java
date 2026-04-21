package com.tuna.ecommerce.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tuna.ecommerce.domain.Order;
import com.tuna.ecommerce.domain.Payment;
import com.tuna.ecommerce.repository.OrderRepository;
import com.tuna.ecommerce.service.PayOSService;
import com.tuna.ecommerce.service.PaymentService;
import com.tuna.ecommerce.ultil.anotation.APIMessage;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/public/webhooks")
@Slf4j
public class PayOSWebhookController {

    private final PayOSService payOSService;
    private final PaymentService paymentService;
    private final OrderRepository orderRepository;

    public PayOSWebhookController(PayOSService payOSService, PaymentService paymentService,
            OrderRepository orderRepository) {
        this.payOSService = payOSService;
        this.paymentService = paymentService;
        this.orderRepository = orderRepository;
    }

    @PostMapping("/payos")
    @APIMessage("Handle PayOS webhook")
    @SuppressWarnings("unchecked")
    public ResponseEntity<String> handlePayOSWebhook(@RequestBody Map<String, Object> payload) {
        log.info(">>> PayOS Webhook received: {}", payload);

        try {
            String code = (String) payload.get("code");
            String signature = (String) payload.get("signature");
            Map<String, Object> data = (Map<String, Object>) payload.get("data");

            if (data == null) {
                log.warn(">>> PayOS Webhook: No data in payload");
                return ResponseEntity.ok("{\"success\": true}");
            }

            // Verify signature
            if (signature != null && !payOSService.verifyWebhookSignature(data, signature)) {
                log.warn(">>> PayOS Webhook: Invalid signature");
                return ResponseEntity.ok("{\"success\": true}");
            }

            if (!"00".equals(code)) {
                log.info(">>> PayOS Webhook: Non-success code: {}", code);
                return ResponseEntity.ok("{\"success\": true}");
            }

            // Extract orderCode
            Object orderCodeObj = data.get("orderCode");
            if (orderCodeObj == null) {
                log.warn(">>> PayOS Webhook: No orderCode");
                return ResponseEntity.ok("{\"success\": true}");
            }

            long orderCode;
            if (orderCodeObj instanceof Number) {
                orderCode = ((Number) orderCodeObj).longValue();
            } else {
                orderCode = Long.parseLong(orderCodeObj.toString());
            }

            // Find order
            Order order = this.orderRepository.findById(orderCode).orElse(null);
            if (order == null) {
                log.warn(">>> PayOS Webhook: Order #{} not found", orderCode);
                return ResponseEntity.ok("{\"success\": true}");
            }

            Payment payment = order.getPayment();
            if (payment == null) {
                log.warn(">>> PayOS Webhook: No payment for order #{}", orderCode);
                return ResponseEntity.ok("{\"success\": true}");
            }

            // Check if already processed
            if (payment.getStatus() == com.tuna.ecommerce.ultil.constant.OrderStatusEnum.CONFIRMED) {
                log.info(">>> PayOS Webhook: Payment already confirmed for order #{}", orderCode);
                return ResponseEntity.ok("{\"success\": true}");
            }

            // Process success
            String reference = data.get("reference") != null ? data.get("reference").toString() : "PAYOS-" + orderCode;
            String rawData = payload.toString();

            log.info(">>> PayOS Webhook: Processing payment success for order #{}, reference: {}", orderCode, reference);
            this.paymentService.processPaymentSuccess(payment, reference, rawData);

            return ResponseEntity.ok("{\"success\": true}");
        } catch (Exception e) {
            log.error(">>> PayOS Webhook: Error processing webhook", e);
            // Always return 200 to avoid PayOS retries
            return ResponseEntity.ok("{\"success\": true}");
        }
    }
}
