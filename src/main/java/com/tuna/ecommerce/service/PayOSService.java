package com.tuna.ecommerce.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuna.ecommerce.domain.Order;
import com.tuna.ecommerce.domain.OrderItem;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PayOSService {

    private static final String PAYOS_API_URL = "https://api-merchant.payos.vn/v2/payment-requests";

    @Value("${payos.client-id}")
    private String clientId;

    @Value("${payos.api-key}")
    private String apiKey;

    @Value("${payos.checksum-key}")
    private String checksumKey;

    @Value("${payos.return-url}")
    private String returnUrl;

    @Value("${payos.cancel-url}")
    private String cancelUrl;

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public PayOSService(ObjectMapper objectMapper, RestTemplate restTemplate) {
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    /**
     * Tạo link thanh toán PayOS cho đơn hàng
     * @return checkoutUrl để redirect khách hàng
     */
    public String createPaymentLink(Order order) throws Exception {
        long orderCode = order.getId();
        int amount = order.getFinalPrice().intValue();
        String description = "DH" + order.getId();

        // Tạo signature theo format PayOS: amount=$amount&cancelUrl=$cancelUrl&description=$description&orderCode=$orderCode&returnUrl=$returnUrl
        String signData = "amount=" + amount
                + "&cancelUrl=" + cancelUrl
                + "&description=" + description
                + "&orderCode=" + orderCode
                + "&returnUrl=" + returnUrl;
        String signature = hmacSHA256(checksumKey, signData);

        // Build request body
        Map<String, Object> body = new HashMap<>();
        body.put("orderCode", orderCode);
        body.put("amount", amount);
        body.put("description", description);
        body.put("cancelUrl", cancelUrl);
        body.put("returnUrl", returnUrl);
        body.put("signature", signature);

        // Optional: thêm danh sách sản phẩm
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            List<Map<String, Object>> items = new ArrayList<>();
            for (OrderItem item : order.getItems()) {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("name", item.getProduct() != null ? truncate(item.getProduct().getName(), 256) : "Sản phẩm");
                itemMap.put("quantity", item.getQuantity());
                itemMap.put("price", item.getPrice().intValue());
                items.add(itemMap);
            }
            body.put("items", items);
        }

        // Optional: thêm thông tin người mua
        if (order.getUser() != null) {
            body.put("buyerEmail", order.getUser().getEmail());
        }
        if (order.getReceiverName() != null) {
            body.put("buyerName", order.getReceiverName());
        }
        if (order.getPhone() != null) {
            body.put("buyerPhone", order.getPhone());
        }

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-client-id", clientId);
        headers.set("x-api-key", apiKey);

        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

        log.info(">>> PayOS: Creating payment link for order #{}, amount: {}", order.getId(), amount);

        ResponseEntity<String> response = restTemplate.postForEntity(PAYOS_API_URL, entity, String.class);
        JsonNode jsonResponse = objectMapper.readTree(response.getBody());

        String code = jsonResponse.has("code") ? jsonResponse.get("code").asText() : "99";

        if ("00".equals(code)) {
            JsonNode data = jsonResponse.get("data");
            String checkoutUrl = data.get("checkoutUrl").asText();
            log.info(">>> PayOS: Payment link created successfully. URL: {}", checkoutUrl);
            return checkoutUrl;
        } else {
            String desc = jsonResponse.has("desc") ? jsonResponse.get("desc").asText() : "Unknown error";
            log.error(">>> PayOS: Failed to create payment link. Code: {}, Desc: {}", code, desc);
            throw new Exception("PayOS Error: " + desc);
        }
    }

    /**
     * Lấy thông tin link thanh toán từ PayOS
     */
    public JsonNode getPaymentLinkInfo(long orderCode) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-client-id", clientId);
            headers.set("x-api-key", apiKey);

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    PAYOS_API_URL + "/" + orderCode,
                    org.springframework.http.HttpMethod.GET,
                    entity,
                    String.class);

            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            log.error(">>> PayOS: Failed to get payment info for order #{}", orderCode, e);
            return null;
        }
    }

    /**
     * Hủy link thanh toán PayOS
     */
    public boolean cancelPaymentLink(long orderCode, String reason) {
        try {
            String url = PAYOS_API_URL + "/" + orderCode + "/cancel";

            Map<String, Object> body = new HashMap<>();
            body.put("cancellationReason", reason != null && !reason.isBlank() ? reason : "Người dùng hủy đơn hàng");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-client-id", clientId);
            headers.set("x-api-key", apiKey);

            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

            log.info(">>> PayOS: Cancelling payment link for order #{}", orderCode);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            JsonNode jsonResponse = objectMapper.readTree(response.getBody());

            String code = jsonResponse.has("code") ? jsonResponse.get("code").asText() : "99";
            
            if ("00".equals(code)) {
                log.info(">>> PayOS: Payment link cancelled successfully");
                return true;
            } else {
                log.warn(">>> PayOS: Failed to cancel link. Response: {}", response.getBody());
                return false;
            }
        } catch (Exception e) {
            log.error(">>> PayOS: Exception while cancelling payment link for order #{}", orderCode, e);
            return false;
        }
    }

    /**
     * Verify webhook signature từ PayOS
     */
    public boolean verifyWebhookSignature(Map<String, Object> data, String receivedSignature) {
        try {
            // Sort data alphabetically and build sign string
            TreeMap<String, Object> sortedData = new TreeMap<>(data);
            StringBuilder signData = new StringBuilder();
            for (Map.Entry<String, Object> entry : sortedData.entrySet()) {
                if (signData.length() > 0) {
                    signData.append("&");
                }
                signData.append(entry.getKey()).append("=").append(entry.getValue());
            }

            String calculatedSignature = hmacSHA256(checksumKey, signData.toString());
            return calculatedSignature.equals(receivedSignature);
        } catch (Exception e) {
            log.error(">>> PayOS: Signature verification failed", e);
            return false;
        }
    }

    /**
     * HMAC-SHA256 signature generation
     */
    public static String hmacSHA256(String key, String data) throws Exception {
        Mac hmacSha256 = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA256");
        hmacSha256.init(secretKeySpec);
        byte[] hash = hmacSha256.doFinal(data.getBytes("UTF-8"));

        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private String truncate(String str, int maxLength) {
        if (str == null) return "";
        return str.length() <= maxLength ? str : str.substring(0, maxLength);
    }
}
