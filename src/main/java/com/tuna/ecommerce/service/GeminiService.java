package com.tuna.ecommerce.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String getChatResponse(String userMessage) {
        try {
            String url = apiUrl + "?key=" + apiKey;

            // Prepare Payload for Gemini 1.5
            Map<String, Object> requestBody = new HashMap<>();
            
            // System instructions (context)
            Map<String, Object> systemInstructionMap = new HashMap<>();
            Map<String, String> systemPart = new HashMap<>();
            systemPart.put("text", "Bạn là trợ lý ảo thông minh của Tuna Ecommerce. " +
                "Hãy trả lời khách hàng một cách lịch sự, thân thiện. " +
                "Bạn có thể tư vấn sản phẩm, hướng dẫn mua hàng và giải đáp các thắc mắc chung về cửa hàng. " +
                "Nếu khách hàng hỏi về các thông tin bạn không biết, hãy khuyên khách liên hệ hotline 1900xxxx.");
            systemInstructionMap.put("parts", List.of(systemPart));
            requestBody.put("system_instruction", systemInstructionMap);

            Map<String, Object> contentUser = new HashMap<>();
            Map<String, String> userPart = new HashMap<>();
            userPart.put("text", userMessage);
            contentUser.put("parts", List.of(userPart));

            requestBody.put("contents", List.of(contentUser));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            System.out.println(">>> Calling Gemini API: " + url);
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, entity, String.class);
            String responseStr = responseEntity.getBody();
            System.out.println(">>> Raw Response: " + responseStr);

            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                // Parse response
                JsonNode root = objectMapper.readTree(responseStr);
                return root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
            } else {
                return "Lỗi từ Google API: " + responseEntity.getStatusCode() + " - " + responseStr;
            }

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            String errorBody = e.getResponseBodyAsString();
            System.err.println(">>> Gemini API Error Response: " + errorBody);
            return "Lỗi từ Google API (" + e.getStatusCode() + "): " + errorBody;
        } catch (Exception e) {
            System.err.println(">>> Gemini Error: " + e.getMessage());
            e.printStackTrace();
            return "Xin lỗi (Lỗi hệ thống: " + e.getMessage() + ")";
        }
    }
}
