package com.tuna.ecommerce.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.tuna.ecommerce.domain.Order;
import com.tuna.ecommerce.domain.OrderItem;
import com.tuna.ecommerce.ultil.constant.PaymentMethodEnum;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GHNService {

    @Value("${ghn.token}")
    private String token;

    @Value("${ghn.shop-id}")
    private String shopId;

    @Value("${ghn.base-url}")
    private String baseUrl;

    @Value("${ghn.pick-district-id}")
    private int pickDistrictId;

    @Value("${ghn.pick-ward-code}")
    private String pickWardCode;
    
    @Value("${ghn.test-mode:false}")
    private boolean testMode;

    private final RestTemplate restTemplate = new RestTemplate();

    @Cacheable(value = "ghn_fee", key = "{#provinceName, #districtName, #wardName, #weight}")
    public Integer calculateFee(String provinceName, String districtName, String wardName, int weight) {
        log.info("--- GHN FEE CALCULATION DEBUG (TestMode={}) ---", testMode);
        if (testMode) {
            return 30000; // Fixed test fee
        }
        try {
            // 1. Get Province ID
            Integer provinceId = getProvinceIdByName(provinceName);
            if (provinceId == null) {
                log.warn("Could not find GHN ProvinceID for: {}", provinceName);
                return 30000;
            }

            // 2. Get District ID
            Integer districtId = getDistrictIdByName(provinceId, districtName);
            if (districtId == null) {
                log.warn("Could not find GHN DistrictID for: {} in province {}", districtName, provinceName);
                return 30000;
            }

            // 3. Get Ward Code
            String wardCode = getWardCodeByName(districtId, wardName);
            // Ward is optional for some cases but better to have it
            
            log.info("GHN Mapping: {} -> ProvinceId={}, DistrictId={}, WardCode={}", 
                    districtName, provinceId, districtId, wardCode);

            // 4. Call GHN Fee API
            return callGhnFeeApi(districtId, wardCode, weight);

        } catch (Exception e) {
            log.error("Failed to calculate GHN fee: {}", e.getMessage());
            return 30000; // Fallback
        }
    }

    private Integer getProvinceIdByName(String name) {
        if (name == null) return null;
        String searchTerm = normalizeName(name);
        
        List<Map<String, Object>> provinces = getProvinces();
        for (Map<String, Object> p : provinces) {
            String pName = normalizeName((String) p.get("ProvinceName"));
            if (pName.contains(searchTerm) || searchTerm.contains(pName)) {
                return (Integer) p.get("ProvinceID");
            }
        }
        return null;
    }

    private Integer getDistrictIdByName(int provinceId, String name) {
        if (name == null) return null;
        String searchTerm = normalizeName(name);
        
        List<Map<String, Object>> districts = getDistricts(provinceId);
        for (Map<String, Object> d : districts) {
            String dName = normalizeName((String) d.get("DistrictName"));
            if (dName.contains(searchTerm) || searchTerm.contains(dName)) {
                return (Integer) d.get("DistrictID");
            }
        }
        return null;
    }

    private String getWardCodeByName(int districtId, String name) {
        if (name == null) return null;
        String searchTerm = normalizeName(name);
        
        List<Map<String, Object>> wards = getWards(districtId);
        for (Map<String, Object> w : wards) {
            String wName = normalizeName((String) w.get("WardName"));
            if (wName.contains(searchTerm) || searchTerm.contains(wName)) {
                return (String) w.get("WardCode");
            }
        }
        return null;
    }

    public String createOrder(Order order, int weight) {
        log.info("--- GHN CREATE ORDER DEBUG (TestMode={}) for Order #{} ---", testMode, order.getId());
        if (testMode) {
            try {
                // Simulate network delay
                Thread.sleep(500); 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            String mockCode = "MOCK_GHN_" + order.getId() + "_" + System.currentTimeMillis() % 1000;
            log.info("GHN Mock Mode: Created Order #{} with Shipping Code: {}", order.getId(), mockCode);
            return mockCode;
        }
        try {
            // 1. Map locations to GHN IDs
            Integer toProvinceId = getProvinceIdByName(order.getProvince());
            if (toProvinceId == null) throw new RuntimeException("Province not found in GHN: " + order.getProvince());
            
            Integer toDistrictId = getDistrictIdByName(toProvinceId, order.getDistrict());
            if (toDistrictId == null) throw new RuntimeException("District not found in GHN: " + order.getDistrict());
            
            String toWardCode = getWardCodeByName(toDistrictId, order.getWard());
            if (toWardCode == null) throw new RuntimeException("Ward not found in GHN: " + order.getWard());

            // 2. Prepare items
            List<Map<String, Object>> items = order.getItems().stream().map(item -> {
                Map<String, Object> itemMap = new java.util.HashMap<>();
                itemMap.put("name", item.getProduct().getName());
                itemMap.put("code", item.getProduct().getId().toString());
                itemMap.put("quantity", item.getQuantity());
                itemMap.put("price", item.getPrice().intValue());
                return itemMap;
            }).collect(Collectors.toList());

            // 3. Prepare request body
            String url = baseUrl + "/shipping-order/create";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Token", token);
            headers.set("ShopId", shopId);
            headers.set("Content-Type", "application/json");

            // COD handling
            int codAmount = 0;
            if (order.getPayment() != null && order.getPayment().getMethod() == PaymentMethodEnum.COD) {
                codAmount = order.getFinalPrice().intValue();
            }

            Map<String, Object> requestBody = new java.util.HashMap<>();
            requestBody.put("payment_type_id", 2);
            requestBody.put("note", "Giao hàng nhanh - Bông Cosmetic");
            requestBody.put("required_note", "CHOXEMHANGKHONGTHU");
            requestBody.put("return_phone", "0865190253");
            requestBody.put("return_address", "180 P. Triều Khúc, Thanh Liệt, Thanh Trì, Hà Nội");
            requestBody.put("to_name", order.getReceiverName());
            requestBody.put("to_phone", order.getPhone());
            requestBody.put("to_address", order.getShippingAddress());
            requestBody.put("to_ward_code", toWardCode);
            requestBody.put("to_district_id", toDistrictId);
            requestBody.put("cod_amount", codAmount);
            requestBody.put("content", "Đơn hàng #" + order.getId());
            requestBody.put("client_order_code", order.getId().toString());

            requestBody.put("weight", weight <= 0 ? 500 : weight);
            requestBody.put("length", 10);
            requestBody.put("width", 10);
            requestBody.put("height", 10);
            requestBody.put("service_type_id", 2); // Chuyên phát tiêu chuẩn
            requestBody.put("items", items);

            log.info("GHN Create Order Request: {}", requestBody);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            Map body = response.getBody();
            log.info("GHN Create Order Response Body: {}", body);

            if (body != null && (String.valueOf(body.get("code")).equals("200") || String.valueOf(body.get("code")).equals("201"))) {
                Map data = (Map) body.get("data");
                if (data != null && data.get("order_code") != null) {
                    return (String) data.get("order_code");
                }
            }
            log.error("GHN Create Order API Error (Code: {}): {}", body != null ? body.get("code") : "N/A", body);
            throw new RuntimeException("Failed to create GHN order: " + (body != null ? body.get("message") : "Unknown error"));

        } catch (Exception e) {
            log.error("GHN Create Order Exception: {}", e.getMessage());
            throw new RuntimeException("GHN Order Creation Exception: " + e.getMessage());
        }
    }

    private Integer callGhnFeeApi(int toDistrictId, String toWardCode, int weight) {
        String url = baseUrl + "/shipping-order/fee";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", token);
        headers.set("ShopId", shopId);
        headers.set("Content-Type", "application/json");

        int finalWeight = weight <= 0 ? 500 : weight;

        Map<String, Object> request = Map.of(
            "from_district_id", pickDistrictId,
            "from_ward_code", pickWardCode,
            "to_district_id", toDistrictId,
            "to_ward_code", toWardCode != null ? toWardCode : "",
            "service_type_id", 2, 
            "weight", finalWeight,
            "length", 10,
            "width", 10,
            "height", 10,
            "insurance_value", 0
        );

        log.info("GHN Request Body (Final): {}", request);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            Map body = response.getBody();
            if (body != null && String.valueOf(body.get("code")).equals("200")) {
                Map data = (Map) body.get("data");
                if (data != null && data.get("total") != null) {
                    return Integer.parseInt(String.valueOf(data.get("total")));
                }
            }
            log.error("GHN Fee API error: {}", body);
        } catch (Exception e) {
            log.error("GHN Fee API exception: {}", e.getMessage());
        }
        return 30000;
    }

    @Cacheable("ghn_provinces")
    public List<Map<String, Object>> getProvinces() {
        String url = baseUrl.replace("/v2", "") + "/master-data/province";
        return fetchMasterData(url, null);
    }

    @Cacheable(value = "ghn_districts", key = "#provinceId")
    public List<Map<String, Object>> getDistricts(int provinceId) {
        String url = baseUrl.replace("/v2", "") + "/master-data/district";
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("province_id", provinceId);
        return fetchMasterData(url, body);
    }

    @Cacheable(value = "ghn_wards", key = "#districtId")
    public List<Map<String, Object>> getWards(int districtId) {
        String url = baseUrl.replace("/v2", "") + "/master-data/ward?district_id=" + districtId;
        return fetchMasterData(url, null);
    }

    private List<Map<String, Object>> fetchMasterData(String url, Map<String, ?> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", token);
        headers.set("Content-Type", "application/json");

        HttpEntity<Map<String, ?>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, body != null ? HttpMethod.POST : HttpMethod.GET, entity, Map.class);
            Map responseBody = response.getBody();
            if (responseBody != null && String.valueOf(responseBody.get("code")).equals("200")) {
                Object data = responseBody.get("data");
                if (data instanceof List) {
                    return (List<Map<String, Object>>) data;
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch GHN master data from {}: {}", url, e.getMessage());
        }
        return Collections.emptyList();
    }

    private String normalizeName(String name) {
        if (name == null) return "";
        // Remove accents and normalize to base characters
        String normalized = java.text.Normalizer.normalize(name.toLowerCase(), java.text.Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        normalized = normalized.replace('đ', 'd').replace('Đ', 'D');

        return normalized
                .replace("thanh pho", "")
                .replace("tinh", "")
                .replace("quan", "")
                .replace("huyen", "")
                .replace("thi xa", "")
                .replace("phuong", "")
                .replace("xa", "")
                .replace("thi tran", "")
                .replace("tp.", "")
                .replace("p.", "")
                .replace("q.", "")
                .replaceAll("[^a-z0-9 ]", "") // Remove special characters
                .trim();
    }
}
