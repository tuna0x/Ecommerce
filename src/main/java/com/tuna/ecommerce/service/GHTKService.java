package com.tuna.ecommerce.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.tuna.ecommerce.domain.Order;
import com.tuna.ecommerce.domain.OrderItem;
import com.tuna.ecommerce.ultil.constant.PaymentMethodEnum;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GHTKService {
    @Value("${ghtk.token}")
    private String token;

    @Value("${ghtk.base-url}")
    private String url;

    @Value("${ghtk.pick-province}")
    private String pickProvince;

    @Value("${ghtk.pick-district}")
    private String pickDistrict;

    @Value("${ghtk.pick-address}")
    private String pickAddress;

    @Value("${ghtk.pick-tel}")
    private String pickTel;

    @Value("${ghtk.pick-name}")
    private String pickName;

    private final RestTemplate restTemplate = new RestTemplate();

    public Integer calculateFee(String province, String district, Integer weight) {
        try {
            // Using a standard URL builder pattern
            String api = UriComponentsBuilder.fromUriString(url + "/services/shipment/fee")
                    .queryParam("pick_province", pickProvince)
                    .queryParam("pick_district", pickDistrict)
                    .queryParam("province", province)
                    .queryParam("district", district)
                    .queryParam("weight", weight)
                    .build()
                    .encode()
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Token", token);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    api,
                    HttpMethod.GET,
                    entity,
                    Map.class);

            Map<?, ?> body = response.getBody();
            if (body == null) return 30000;

            Boolean success = (Boolean) body.get("success");
            if (Boolean.TRUE.equals(success)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> feeMap = (Map<String, Object>) body.get("fee");
                if (feeMap != null && feeMap.get("fee") != null) {
                    Object feeValue = feeMap.get("fee");
                    if (feeValue instanceof Integer) {
                        return (Integer) feeValue;
                    } else if (feeValue instanceof Double) {
                        return ((Double) feeValue).intValue();
                    }
                }
            } else {
                log.error("GHTK Fee Calculation Failed: {}", body.get("message"));
            }
        } catch (Exception e) {
            log.error("Failed to calculate GHTK fee: {}", e.getMessage());
        }
        return 30000;
    }

    public String createGHTKOrder(Order order) {
        try {
            String api = url + "/services/shipment/order";

            Map<String, Object> orderData = new HashMap<>();
            orderData.put("id", "ORDER_" + order.getId());
            orderData.put("pick_name", pickName);
            orderData.put("pick_address", pickAddress);
            orderData.put("pick_province", pickProvince);
            orderData.put("pick_district", pickDistrict);
            orderData.put("pick_tel", pickTel);
            
            orderData.put("name", order.getReceiverName());
            orderData.put("address", order.getShippingAddress());
            orderData.put("province", order.getProvince());
            orderData.put("district", order.getDistrict());
            orderData.put("ward", order.getWard());
            orderData.put("tel", order.getPhone());
            orderData.put("email", order.getUser() != null ? order.getUser().getEmail() : "");

            boolean isCOD = order.getPayment() != null && order.getPayment().getMethod() == PaymentMethodEnum.COD;
            orderData.put("pick_money", isCOD ? (order.getFinalPrice() != null ? order.getFinalPrice().intValue() : 0) : 0);
            orderData.put("value", order.getFinalPrice() != null ? order.getFinalPrice().intValue() : 0);

            List<Map<String, Object>> products = new ArrayList<>();
            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    Map<String, Object> p = new HashMap<>();
                    p.put("name", item.getProduct() != null ? item.getProduct().getName() : "Sản phẩm");
                    p.put("weight", 0.5); 
                    p.put("quantity", item.getQuantity());
                    p.put("price", item.getPrice() != null ? item.getPrice().intValue() : 0);
                    products.add(p);
                }
            }
            orderData.put("products", products);

            Map<String, Object> payload = new HashMap<>();
            payload.put("order", orderData);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Token", token);
            headers.set("Content-Type", "application/json");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(api, entity, Map.class);
            Map<?, ?> body = response.getBody();

            if (body != null && Boolean.TRUE.equals(body.get("success"))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> orderInfo = (Map<String, Object>) body.get("order");
                return (String) orderInfo.get("label"); 
            } else {
                log.error("GHTK Shipment Order Failed: {}", body != null ? body.get("message") : "Empty response");
            }
        } catch (Exception e) {
            log.error("API Call to GHTK failed: {}", e.getMessage());
        }
        return null;
    }
}
