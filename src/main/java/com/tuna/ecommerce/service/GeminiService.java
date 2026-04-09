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

    private final ProductService productService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeminiService(ProductService productService) {
        this.productService = productService;
    }

    public String getChatResponse(String userMessage) {
        int maxRetries = 2; // Thử lại tối đa 2 lần nếu gặp lỗi 503
        int retryCount = 0;

        while (retryCount <= maxRetries) {
            try {
                String url = apiUrl + "?key=" + apiKey;

                // Fetch Product Context from Database
                String productContext = this.productService.getProductsSummaryForChatbot(userMessage);

                // Prepare Payload for Gemini 1.5
                Map<String, Object> requestBody = new HashMap<>();

                // System instructions (context)
                Map<String, Object> systemInstructionMap = new HashMap<>();
                Map<String, String> systemPart = new HashMap<>();
                systemPart.put("text", "Bạn là trợ lý ảo thông minh của Tuna Ecommerce. " +
                        "Hãy trả lời khách hàng một cách lịch sự, thân thiện. " +
                        "Dưới đây là thông tin các sản phẩm của cửa hàng lấy từ Database: \n" + productContext + "\n"
                        +
                        "Nhiệm vụ của bạn: \n" +
                        "1. Luôn ưu tiên trả lời dựa trên thông tin sản phẩm được cung cấp ở trên.\n" +
                        "2. Nếu khách hỏi về giá hoặc tư vấn, hãy dùng dữ liệu trên để trả lời chính xác.\n" +
                        "3. Nếu sản phẩm khách hỏi không có trong danh sách trên, hãy nói rằng bạn không tìm thấy chính xác sản phẩm đó nhưng có thể gợi ý các sản phẩm tương đương (nếu có).\n"
                        +
                        "4. Luôn dẫn dắt khách hàng mua hàng và giữ thái độ tích cực.\n" +
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

                System.out.println(">>> Calling Gemini API: " + url + " (Attempt: " + (retryCount + 1) + ")");
                ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, entity, String.class);
                String responseStr = responseEntity.getBody();

                if (responseEntity.getStatusCode().is2xxSuccessful()) {
                    JsonNode root = objectMapper.readTree(responseStr);
                    return root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
                }

                return "Hệ thống đang bận, bạn vui lòng thử lại sau giây lát nhé! 🙏";

            } catch (org.springframework.web.client.HttpServerErrorException.ServiceUnavailable e) {
                // Lỗi 503 - Quá tải
                retryCount++;
                if (retryCount <= maxRetries) {
                    try {
                        Thread.sleep(1000 * retryCount); // Đợi 1-2s rồi thử lại
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    continue;
                }
                return "Máy chủ AI của Google đang quá tải. Bạn hãy quay lại sau ít phút nhé! ☕";
            } catch (org.springframework.web.client.HttpClientErrorException.TooManyRequests e) {
                return "Bạn đang gửi yêu cầu hơi nhanh, hãy đợi một chút rồi nhắn lại nhé! 🚀";
            } catch (Exception e) {
                System.err.println(">>> Gemini Error: " + e.getMessage());
                return "Rất tiếc, tôi đang gặp một chút sự cố kỹ thuật. Bạn có thể nhắn lại sau được không?";
            }
        }
        return "Xin lỗi, hiện tại tôi không thể kết nối tới máy chủ AI. Vui lòng thử lại sau.";
    }
}
