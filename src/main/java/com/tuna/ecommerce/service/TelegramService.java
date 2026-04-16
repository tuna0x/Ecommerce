package com.tuna.ecommerce.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.tuna.ecommerce.domain.Order;
import com.tuna.ecommerce.domain.OrderItem;

import java.util.HashMap;
import java.util.Map;

@Service
public class TelegramService {
    private static final Logger log = LoggerFactory.getLogger(TelegramService.class);

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.chat-id}")
    private String chatId;

    private final RestTemplate restTemplate;

    public TelegramService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Async
    public void sendOrderNotification(Order order) {
        if ("xxx".equals(botToken) || "xxx".equals(chatId)) {
            log.warn(">>> Telegram Bot not configured. Skipping notification.");
            return;
        }

        try {
            String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";

            StringBuilder message = new StringBuilder();
            message.append("<b>🔔 CÓ ĐƠN HÀNG MỚI!</b>\n\n");
            message.append("<b>🆔 Mã đơn:</b> #").append(order.getId()).append("\n");
            message.append("<b>👤 Khách hàng:</b> ").append(order.getReceiverName()).append("\n");
            message.append("<b>📞 SĐT:</b> ").append(order.getPhone()).append("\n");
            message.append("<b>📍 Địa chỉ:</b> ").append(order.getShippingAddress()).append(", ")
                   .append(order.getWard()).append(", ").append(order.getDistrict()).append(", ").append(order.getProvince()).append("\n");
            message.append("<b>💰 Tổng tiền:</b> ").append(String.format("%,.0f VNĐ", order.getFinalPrice().doubleValue())).append("\n");
            message.append("<b>💳 Thanh toán:</b> ").append(order.getPayment() != null ? order.getPayment().getMethod() : "N/A").append("\n\n");
            
            message.append("<b>🛒 Danh sách sản phẩm:</b>\n");
            for (OrderItem item : order.getItems()) {
                message.append("- ").append(item.getProduct().getName())
                       .append(" (SL: ").append(item.getQuantity()).append(")\n");
            }

            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", chatId);
            body.put("text", message.toString());
            body.put("parse_mode", "HTML");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            restTemplate.postForEntity(url, entity, String.class);
            log.info(">>> SUCCESS: Telegram notification sent for order #{}", order.getId());

        } catch (Exception e) {
            log.error(">>> FAILED to send Telegram notification: {}", e.getMessage());
        }
    }
}
