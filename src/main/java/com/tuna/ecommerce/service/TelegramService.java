package com.tuna.ecommerce.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    private final DashboardService dashboardService;
    private final ObjectMapper objectMapper;
    private long lastUpdateId = 0;

    public TelegramService(RestTemplate restTemplate, DashboardService dashboardService, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.dashboardService = dashboardService;
        this.objectMapper = objectMapper;
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
            message.append("<b>🔔 CÓ ĐƠN HÀNG MỚI! (Chờ xác thực)</b>\n\n");
            message.append("<b>🆔 Mã đơn:</b> #").append(order.getId()).append("\n");
            message.append("<b>👤 Khách hàng:</b> ").append(order.getReceiverName()).append("\n");
            message.append("<b>📞 SĐT:</b> ").append(order.getPhone()).append("\n");
            message.append("<b>📍 Địa chỉ:</b> ").append(order.getShippingAddress()).append(", ")
                   .append(order.getWard()).append(", ").append(order.getDistrict()).append(", ").append(order.getProvince()).append("\n");
            message.append("<b>💰 Tổng tiền:</b> ").append(String.format("%,.0f VNĐ", order.getFinalPrice().doubleValue())).append("\n");
            String paymentMethodName = "N/A";
            if (order.getPayment() != null && order.getPayment().getMethod() != null) {
                switch (order.getPayment().getMethod()) {
                    case COD: paymentMethodName = "💵 COD (Thanh toán khi nhận hàng)"; break;
                    case VNPAY: paymentMethodName = "🏦 VNPay"; break;
                    case PAYOS: paymentMethodName = "🏦 PayOS"; break;
                    default: paymentMethodName = order.getPayment().getMethod().name(); break;
                }
            }
            message.append("<b>💳 Thanh toán:</b> ").append(paymentMethodName).append("\n");
            message.append("<b>📊 Trạng thái:</b> 🟠 Đang chờ\n\n");
            
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

    @Async
    public void sendOrderConfirmedNotification(Order order) {
        if ("xxx".equals(botToken) || "xxx".equals(chatId)) return;

        try {
            String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";

            StringBuilder message = new StringBuilder();
            message.append("<b>✅ ĐƠN HÀNG ĐÃ XÁC NHẬN!</b>\n\n");
            message.append("<b>🆔 Mã đơn:</b> #").append(order.getId()).append("\n");
            message.append("<b>👥 Khách hàng:</b> ").append(order.getReceiverName()).append("\n");
            message.append("<b>💰 Tổng tiền:</b> ").append(String.format("%,.0f VNĐ", order.getFinalPrice().doubleValue())).append("\n");
            message.append("<b>📊 Trạng thái:</b> 🟢 Đã chốt (Sẵn sàng đóng gói)\n\n");
            message.append("👉 Admin hãy kiểm tra và tiến hành giao hàng cho khách nhé!");

            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", chatId);
            body.put("text", message.toString());
            body.put("parse_mode", "HTML");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            restTemplate.postForEntity(url, entity, String.class);
            log.info(">>> SUCCESS: Telegram Confirmation notification sent for order #{}", order.getId());
        } catch (Exception e) {
            log.error(">>> FAILED to send Confirmation notification: {}", e.getMessage());
        }
    }

    @Async
    public void sendAiAlert(String userEmail, String query, String reason) {
        if ("xxx".equals(botToken) || "xxx".equals(chatId)) return;

        try {
            String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";

            StringBuilder message = new StringBuilder();
            message.append("<b>⚠️ CẢNH BÁO AI CẦN HỖ TRỢ!</b>\n\n");
            message.append("<b>📧 Khách hàng:</b> ").append(userEmail != null ? userEmail : "Khách vãng lai").append("\n");
            message.append("<b>🔍 Vấn đề:</b> ").append(reason).append("\n");
            message.append("<b>💬 Câu hỏi:</b> <i>\"").append(query).append("\"</i>\n\n");
            message.append("👉 Nàng/Cậu admin hãy vào kiểm tra hệ thống chat hoặc đơn hàng ngay nhé!");

            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", chatId);
            body.put("text", message.toString());
            body.put("parse_mode", "HTML");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            restTemplate.postForEntity(url, entity, String.class);
        } catch (Exception e) {
            log.error(">>> FAILED to send AI Alert: {}", e.getMessage());
        }
    }

    @Async
    public void sendLowStockAlert(String productName, String sku, int currentStock) {
        if ("xxx".equals(botToken) || "xxx".equals(chatId)) return;

        try {
            String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";

            StringBuilder message = new StringBuilder();
            message.append("<b>🔴 CẢNH BÁO: SẮP HẾT HÀNG!</b>\n\n");
            message.append("<b>📦 Sản phẩm:</b> ").append(productName).append("\n");
            message.append("<b>🆔 SKU:</b> <code>").append(sku).append("</code>\n");
            message.append("<b>⚠️ Tồn kho hiện tại:</b> <pre>").append(currentStock).append("</pre>\n\n");
            message.append("👉 Nàng/Cậu hãy kiểm tra và nhập thêm hàng để tránh gián đoạn kinh doanh nhé!");

            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", chatId);
            body.put("text", message.toString());
            body.put("parse_mode", "HTML");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            restTemplate.postForEntity(url, entity, String.class);
            log.info(">>> SUCCESS: Low stock alert sent for variant {}", sku);
        } catch (Exception e) {
            log.error(">>> FAILED to send Low Stock Alert: {}", e.getMessage());
        }
    }

    @Scheduled(fixedDelayString = "${telegram.bot.polling-delay-ms:30000}")
    public void pollUpdates() {
        if ("xxx".equals(botToken) || "xxx".equals(chatId)) return;

        try {
            String url = "https://api.telegram.org/bot" + botToken + "/getUpdates?offset=" + (lastUpdateId + 1);
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode updates = root.path("result");

            for (JsonNode update : updates) {
                lastUpdateId = update.path("update_id").asLong();
                JsonNode message = update.path("message");
                String senderId = message.path("chat").path("id").asText();
                String text = message.path("text").asText();

                // Bảo mật: Chỉ xử lý nếu gửi từ đúng Admin ChatId
                if (chatId.equals(senderId)) {
                    handleCommand(text);
                } else if (text != null && text.startsWith("/")) {
                    log.warn(">>> UNAUTHORIZED access attempt from chatId: {}", senderId);
                }
            }
        } catch (Exception e) {
            log.trace(">>> Telegram polling error: {}", e.getMessage());
        }
    }

    private void handleCommand(String command) {
        if (command == null) return;
        
        String responseText = "";
        String cmd = command.toLowerCase().trim();

        if (cmd.startsWith("/stats")) {
            responseText = dashboardService.getQuickStatsForTelegram();
        } else if (cmd.startsWith("/top")) {
            responseText = dashboardService.getTopProductsForTelegram();
        } else if (cmd.startsWith("/start") || cmd.startsWith("/help")) {
            responseText = "<b>🌟 CHÀO MỪNG QUẢN LÝ BÔNG COSMETIC!</b>\n\n" +
                          "Tôi là trợ lý ảo hỗ trợ bạn điều hành shop.\n\n" +
                          "<b>Các lệnh có sẵn:</b>\n" +
                          "📊 <code>/stats</code> - Xem doanh thu nhanh hôm nay\n" +
                          "🏆 <code>/top</code> - Top 5 sản phẩm bán chạy (7 ngày qua)\n" +
                          "❔ <code>/help</code> - Xem lại danh sách lệnh";
        }

        if (!responseText.isEmpty()) {
            sendGeneralMessage(responseText);
        }
    }

    private void sendGeneralMessage(String text) {
        try {
            String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", chatId);
            body.put("text", text);
            body.put("parse_mode", "HTML");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            restTemplate.postForEntity(url, entity, String.class);
        } catch (Exception e) {
            log.error(">>> FAILED to send command response: {}", e.getMessage());
        }
    }
}
