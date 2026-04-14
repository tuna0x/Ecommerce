package com.tuna.ecommerce.scheduler;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tuna.ecommerce.domain.Order;
import com.tuna.ecommerce.repository.OrderRepository;
import com.tuna.ecommerce.service.OrderService;
import com.tuna.ecommerce.ultil.constant.OrderStatusEnum;

@Component
public class OrderCleanupScheduler {
    private static final Logger log = LoggerFactory.getLogger(OrderCleanupScheduler.class);

    // VNPay: 15 phút không thanh toán → tự động hủy
    private static final long VNPAY_TIMEOUT_MINUTES = 15;

    // COD: 24 giờ không xác nhận email → tự động hủy (chống đơn ảo)
    private static final long COD_TIMEOUT_HOURS = 24;

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    public OrderCleanupScheduler(OrderRepository orderRepository, OrderService orderService) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    /**
     * Ch\u1EA1y m\u1ED7i 5 ph\u00FAt.
     * H\u1EE7y \u0111\u01A1n VNPay PENDING qu\u00E1 15 ph\u00FAt (kh\u00E1ch kh\u00F4ng thanh to\u00E1n).
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void cancelStaleVnpayOrders() {
        Instant cutoffTime = Instant.now().minus(VNPAY_TIMEOUT_MINUTES, ChronoUnit.MINUTES);
        List<Order> staleOrders = orderRepository.findStaleVnpayPendingOrders(cutoffTime);

        if (staleOrders.isEmpty()) return;

        log.info(">>> Found {} stale VNPay orders (>{}min). Auto-cancelling...",
                staleOrders.size(), VNPAY_TIMEOUT_MINUTES);

        for (Order order : staleOrders) {
            cancelOrder(order, "T\u1EF1 \u0111\u1ED9ng h\u1EE7y - kh\u00F4ng thanh to\u00E1n VNPay trong " + VNPAY_TIMEOUT_MINUTES + " ph\u00FAt");
        }
    }

    /**
     * Ch\u1EA1y m\u1ED7i 1 gi\u1EDD.
     * H\u1EE7y \u0111\u01A1n COD PENDING qu\u00E1 24 gi\u1EDD (kh\u00E1ch kh\u00F4ng x\u00E1c nh\u1EADn email → nghi \u0111\u01A1n \u1EA3o).
     */
    @Scheduled(fixedRate = 60 * 60 * 1000)
    public void cancelStaleCodOrders() {
        Instant cutoffTime = Instant.now().minus(COD_TIMEOUT_HOURS, ChronoUnit.HOURS);
        List<Order> staleOrders = orderRepository.findStaleCodPendingOrders(cutoffTime);

        if (staleOrders.isEmpty()) return;

        log.info(">>> Found {} stale COD orders (>{}h). Auto-cancelling...",
                staleOrders.size(), COD_TIMEOUT_HOURS);

        for (Order order : staleOrders) {
            cancelOrder(order, "T\u1EF1 \u0111\u1ED9ng h\u1EE7y - kh\u00F4ng x\u00E1c nh\u1EADn email trong " + COD_TIMEOUT_HOURS + " gi\u1EDD");
        }
    }

    private void cancelOrder(Order order, String reason) {
        try {
            order.setCancelReason(reason);
            orderRepository.save(order);
            orderService.handleUpdateStatus(order.getId(), OrderStatusEnum.CANCELLED);
            log.info(">>> Auto-cancelled order #{}: {}", order.getId(), reason);
        } catch (Exception e) {
            log.error(">>> Failed to auto-cancel order #{}. Error: {}", order.getId(), e.getMessage());
        }
    }
}
