package com.tuna.ecommerce.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    private final RestTemplate restTemplate = new RestTemplate();

    //@Cacheable(value = "ghn_fee", key = "{#provinceName, #districtName, #wardName, #weight}")
    public Integer calculateFee(String provinceName, String districtName, String wardName, int weight) {
        log.info("--- GHN FEE CALCULATION DEBUG ---");
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

    private Integer callGhnFeeApi(int toDistrictId, String toWardCode, int weight) {
        String url = baseUrl + "/shipping-order/fee";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", token);
        headers.set("ShopId", shopId);
        headers.set("Content-Type", "application/json");

        Map<String, Object> request = Map.of(
            "from_district_id", pickDistrictId,
            "from_ward_code", pickWardCode,
            "to_district_id", toDistrictId,
            "to_ward_code", toWardCode != null ? toWardCode : "",
            "service_type_id", 2, // 2 is E-commerce service (Standard)
            "weight", weight,
            "length", 10,
            "width", 10,
            "height", 10
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            Map body = response.getBody();
            if (body != null && (Integer) body.get("code") == 200) {
                Map data = (Map) body.get("data");
                if (data != null && data.get("total") != null) {
                    return (Integer) data.get("total");
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
        return fetchMasterData(url, Map.of("province_id", provinceId));
    }

    @Cacheable(value = "ghn_wards", key = "#districtId")
    public List<Map<String, Object>> getWards(int districtId) {
        String url = baseUrl.replace("/v2", "") + "/master-data/ward?district_id=" + districtId;
        return fetchMasterData(url, null);
    }

    private List<Map<String, Object>> fetchMasterData(String url, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", token);
        headers.set("Content-Type", "application/json");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, body != null ? HttpMethod.POST : HttpMethod.GET, entity, Map.class);
            Map responseBody = response.getBody();
            if (responseBody != null && (Integer) responseBody.get("code") == 200) {
                return (List<Map<String, Object>>) responseBody.get("data");
            }
        } catch (Exception e) {
            log.error("Failed to fetch GHN master data from {}: {}", url, e.getMessage());
        }
        return Collections.emptyList();
    }

    private String normalizeName(String name) {
        if (name == null) return "";
        return name.toLowerCase()
                .replace("th\u00E0nh ph\u1ED1", "")
                .replace("t\u1EC9nh", "")
                .replace("qu\u1EADn", "")
                .replace("huy\u1EC7n", "")
                .replace("th\u1ECB x\u00E3", "")
                .replace("ph\u01B0\u1EDDng", "")
                .replace("x\u00E3", "")
                .replace("th\u1ECB tr\u1EA5n", "")
                .replace("tp.", "")
                .replace("p.", "")
                .replace("q.", "")
                .trim();
    }
}
