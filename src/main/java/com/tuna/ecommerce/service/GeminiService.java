package com.tuna.ecommerce.service;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.tuna.ecommerce.domain.request.chat.ChatMessageDTO;

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
    private final OrderService orderService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public GeminiService(ProductService productService,
            OrderService orderService,
            ObjectMapper objectMapper,
            RestTemplate restTemplate) {
        this.productService = productService;
        this.orderService = orderService;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    public String getChatResponse(String userMessage, List<ChatMessageDTO> history) {
        int maxRetries = 2; // Thử lại tối đa 2 lần nếu gặp lỗi 503
        int retryCount = 0;

        while (retryCount <= maxRetries) {
            try {
                String url = apiUrl + "?key=" + apiKey;

                // Fetch Context from Database
                String productContext = this.productService.getProductsSummaryForChatbot(userMessage);
                String orderContext = this.orderService.getOrdersSummaryForChatbot();

                // Prepare Payload for Gemini 2.5
                Map<String, Object> requestBody = new HashMap<>();

                // System instructions (context)
                Map<String, Object> systemInstructionMap = new HashMap<>();
                Map<String, String> systemPart = new HashMap<>();
                systemPart.put("text", "Bạn là trợ lý ảo thông minh của Tuna Ecommerce. " +
                        "Hãy trả lời khách hàng một cách lịch sự, thân thiện. " +
                        "\n--- DỮ LIỆU SẢN PHẨM ---\n" + productContext + "\n" +
                        "\n--- DỮ LIỆU ĐƠN HÀNG CỦA KHÁCH ---\n" + orderContext + "\n\n"
                        +
                        "Nhiệm vụ quan trọng của bạn: \n" +
                        "1. Luôn ưu tiên trả lời dựa trên thông tin sản phẩm và đơn hàng được cung cấp ở trên.\n" +
                        "2. Khi nhắc đến bất kỳ sản phẩm nào có trong danh sách trên, bạn BẮT BUỘC phải định dạng tên sản phẩm dưới dạng Markdown Link. Cú pháp: [Tên sản phẩm](/product/ID).\n" +
                        "3. Nếu khách hỏi về đơn hàng, hãy dùng DỮ LIỆU ĐƠN HÀNG để trả lời chính xác mã đơn, sản phẩm bên trong và trạng thái. \n" +
                        "4. Nếu khách chê đắt hoặc hỏi khuyến mãi, hãy khuyên họ vào trang chủ tìm mã giảm giá (Coupon), vì cửa hàng thường xuyên có ưu đãi.\n" +
                        "5. Nếu khách muốn hủy đơn hoặc đổi trả, hãy hướng dẫn họ vào mục Tài khoản -> Đơn hàng (hoặc /account) để thao tác, hoặc báo rằng cửa hàng sẽ hỗ trợ sớm.\n" +
                        "6. Luôn dẫn dắt khách hàng mua hàng, giữ thái độ tích cực. Trả lời ngắn gọn, súc tích và dễ hiểu.\n" +
                        "Nếu không biết, hãy khuyên khách liên hệ hotline 1900xxxx hoặc chờ Admin chat trực tiếp.");
                systemInstructionMap.put("parts", List.of(systemPart));
                requestBody.put("system_instruction", systemInstructionMap);

                // Build Conversation Contents (History + Current Message)
                List<Map<String, Object>> contents = new ArrayList<>();

                // Add History
                if (history != null) {
                    for (ChatMessageDTO msg : history) {
                        Map<String, Object> contentPart = new HashMap<>();
                        Map<String, String> part = new HashMap<>();
                        part.put("text", msg.getContent());
                        contentPart.put("role", msg.getRole().equals("assistant") ? "model" : "user");
                        contentPart.put("parts", List.of(part));
                        contents.add(contentPart);
                    }
                }

                // Add Current User Message
                Map<String, Object> contentUser = new HashMap<>();
                Map<String, String> userPart = new HashMap<>();
                userPart.put("text", userMessage);
                contentUser.put("role", "user");
                contentUser.put("parts", List.of(userPart));
                contents.add(contentUser);

                requestBody.put("contents", contents);

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
