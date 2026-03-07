package com.tuna.ecommerce.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.AllArgsConstructor;

@Service
public class GHTKService {
    @Value("${ghtk.token}")
    private String token;
    @Value("${ghtk.base-url}")
    private String url;
    private final RestTemplate restTemplate= new RestTemplate();


    public Integer calculateFee(String province, String district, Integer weight){
           String api = url + "/services/shipment/fee"
            + "?pick_province=Hà Nội"
            + "&pick_district=Thanh Xuân"
            + "&province=" + province
            + "&district=" + district
            + "&weight=" + weight;

    HttpHeaders headers = new HttpHeaders();
    headers.set("Token", token);

    HttpEntity<String> entity = new HttpEntity<>(headers);

    ResponseEntity<Map> response = restTemplate.exchange(
            api,
            HttpMethod.GET,
            entity,
            Map.class
    );

    Map body = response.getBody();

    Map fee = (Map) body.get("fee");

    return (Integer) fee.get("fee");
    }
}
