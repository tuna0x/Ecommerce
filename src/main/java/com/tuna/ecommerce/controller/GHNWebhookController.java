package com.tuna.ecommerce.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tuna.ecommerce.domain.Order;
import com.tuna.ecommerce.repository.OrderRepository;
import com.tuna.ecommerce.service.OrderService;
import com.tuna.ecommerce.ultil.constant.OrderStatusEnum;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/public/webhooks")
@AllArgsConstructor
@Slf4j
public class GHNWebhookController {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    @PostMapping("/ghn")
    public ResponseEntity<Void> handleGHNWebhook(@RequestBody Map<String, Object> payload) {
        log.info(">>> GHN Webhook received: {}", payload);

        try {
            // GHN gửi OrderCode là mã vận đơn của họ
            String orderCode = (String) payload.get("OrderCode");
            String status = (String) payload.get("Status");

            if (orderCode == null || status == null) {
                log.warn(">>> GHN Webhook: Missing OrderCode or Status");
                return ResponseEntity.ok().build();
            }

            Order order = this.orderRepository.findByShippingCode(orderCode);
            if (order == null) {
                log.warn(">>> GHN Webhook: No order found with shipping code: {}", orderCode);
                return ResponseEntity.ok().build(); 
            }

            OrderStatusEnum nextStatus = mapGHNStatus(status);
            if (nextStatus != null && order.getStatus() != nextStatus) {
                log.info(">>> GHN Webhook: Updating Order #{} from {} to {} (GHN Status: {})", 
                        order.getId(), order.getStatus(), nextStatus, status);
                this.orderService.handleUpdateStatus(order.getId(), nextStatus, "Cập nhật tự động từ GHN (Trạng thái: " + status + ")");
            }

        } catch (Exception e) {
            log.error(">>> GHN Webhook Error: {}", e.getMessage());
        }

        return ResponseEntity.ok().build();
    }

    private OrderStatusEnum mapGHNStatus(String ghnStatus) {
        if (ghnStatus == null) return null;
        switch (ghnStatus.toLowerCase()) {
            case "ready_to_pick":
            case "picking":
            case "money_collect_picking":
                return OrderStatusEnum.PICKING;
            case "picked":
            case "delivering":
            case "money_collect_delivering":
                return OrderStatusEnum.DELIVERING;
            case "delivered":
                return OrderStatusEnum.DELIVERED;
            case "cancel":
                return OrderStatusEnum.CANCELLED;
            case "returning":
            case "returned":
                return OrderStatusEnum.RETURNED;
            default:
                return null;
        }
    }
}
