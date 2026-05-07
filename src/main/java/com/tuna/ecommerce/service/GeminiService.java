package com.tuna.ecommerce.service;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.tuna.ecommerce.domain.Cart;
import com.tuna.ecommerce.domain.CartItem;
import com.tuna.ecommerce.domain.User;
import com.tuna.ecommerce.domain.UserProfile;
import com.tuna.ecommerce.domain.UserBehavior;
import com.tuna.ecommerce.domain.ChatInteraction;
import com.tuna.ecommerce.domain.request.chat.ChatMessageDTO;
import com.tuna.ecommerce.repository.UserBehaviorRepository;
import com.tuna.ecommerce.repository.UserRepository;
import com.tuna.ecommerce.repository.ChatInteractionRepository;
import com.tuna.ecommerce.repository.UserProfileRepository;
import com.tuna.ecommerce.ultil.SecurityUtil;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
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
    private final CouponService couponService;
    private final CartService cartService;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserBehaviorRepository userBehaviorRepository;
    private final ChatInteractionRepository chatInteractionRepository;
    private final TrackingService trackingService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final TelegramService telegramService;
    private final BlogService blogService;
    private final CategoryService categoryService;
    private final FlashSaleService flashSaleService;
    private final ReviewService reviewService;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;

    public GeminiService(ProductService productService,
            OrderService orderService,
            CouponService couponService,
            CartService cartService,
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            UserBehaviorRepository userBehaviorRepository,
            ChatInteractionRepository chatInteractionRepository,
            TrackingService trackingService,
            ObjectMapper objectMapper,
            RestTemplate restTemplate,
            TelegramService telegramService,
            BlogService blogService,
            CategoryService categoryService,
            FlashSaleService flashSaleService,
            ReviewService reviewService,
            org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate,
            NotificationService notificationService) {
        this.productService = productService;
        this.orderService = orderService;
        this.couponService = couponService;
        this.cartService = cartService;
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.userBehaviorRepository = userBehaviorRepository;
        this.chatInteractionRepository = chatInteractionRepository;
        this.trackingService = trackingService;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
        this.telegramService = telegramService;
        this.blogService = blogService;
        this.categoryService = categoryService;
        this.flashSaleService = flashSaleService;
        this.reviewService = reviewService;
        this.messagingTemplate = messagingTemplate;
        this.notificationService = notificationService;
    }

    public String getChatResponse(String userMessage, List<ChatMessageDTO> history, String sessionId, String deviceType,
            String pageUrl) {
        try {
            String url = apiUrl + "?key=" + apiKey;

            // 1. Phân tích ngữ cảnh người dùng
            String currentUserEmail = SecurityUtil.getCurrentUserLogin().orElse(null);
            StringBuilder context = new StringBuilder();
            String userName = "Nàng";
            String userSkinType = "Chưa rõ";

            if (currentUserEmail != null) {
                User user = userRepository.findByEmail(currentUserEmail);
                if (user != null && user.getUserProfile() != null) {
                    userName = user.getUserProfile().getName();
                    userSkinType = user.getUserProfile().getSkinType() != null ? user.getUserProfile().getSkinType()
                            : "Chưa rõ";
                }

                List<UserBehavior> behaviors = userBehaviorRepository
                        .findTop10ByUserEmailOrderByCreatedAtDesc(currentUserEmail);
                if (behaviors != null && !behaviors.isEmpty()) {
                    context.append("\n--- HÀNH VI GẦN ĐÂY ---\n");
                    Map<String, Long> actionCounts = behaviors.stream()
                            .collect(Collectors.groupingBy(b -> b.getActionType().toString(), Collectors.counting()));
                    context.append("- Hành động: ").append(actionCounts).append("\n");
                    String lastPages = behaviors.stream().map(UserBehavior::getPageUrl).distinct().limit(3)
                            .collect(Collectors.joining(", "));
                    context.append("- Trang xem: ").append(lastPages).append("\n");
                }
            }

            // 2. Định nghĩa Tools
            List<Map<String, Object>> tools = List.of(Map.of("function_declarations", List.of(
                    Map.of("name", "tra_cuu_san_pham", "description",
                            "Tìm kiếm sản phẩm. Hãy đính kèm loại da của khách vào từ khóa để tìm chính xác hơn (Vd: 'kem chống nắng da dầu'). Kiểm tra kỹ trường 'Loại da' trong kết quả.",
                            "parameters",
                            Map.of("type", "OBJECT", "properties", Map.of("query", Map.of("type", "STRING")))),
                    Map.of("name", "kiem_tra_don_hang", "description", "Xem đơn hàng.", "parameters",
                            Map.of("type", "OBJECT", "properties", Map.of())),
                    Map.of("name", "tim_ma_giam_gia", "description", "Tìm mã giảm giá.", "parameters",
                            Map.of("type", "OBJECT", "properties", Map.of())),
                    Map.of("name", "xem_gio_hang", "description", "Xem giỏ hàng.", "parameters",
                            Map.of("type", "OBJECT", "properties", Map.of())),
                    Map.of("name", "doc_bai_viet_blog", "description", "Tra cứu kiến thức blog.", "parameters",
                            Map.of("type", "OBJECT", "properties", Map.of("keyword", Map.of("type", "STRING")))),
                    Map.of("name", "tra_cuu_flash_sale", "description", "Xem Flash Sale.", "parameters",
                            Map.of("type", "OBJECT", "properties", Map.of())),
                    Map.of("name", "tra_cuu_lich_su_mua_hang", "description", "Xem lịch sử mua hàng.", "parameters",
                            Map.of("type", "OBJECT", "properties", Map.of())),
                    Map.of("name", "xem_danh_gia_san_pham", "description", "Xem review khách.", "parameters",
                            Map.of("type", "OBJECT", "properties", Map.of("productId", Map.of("type", "NUMBER")))),
                    Map.of("name", "cap_nhat_loai_da", "description",
                            "Cập nhật loại da vào hồ sơ khách hàng sau khi trắc nghiệm xong.", "parameters",
                            Map.of("type", "OBJECT", "properties", Map.of("skinType", Map.of("type", "STRING",
                                    "description", "Loại da xác định được (Vd: Da dầu, Da khô, Da nhạy cảm...)")))))));

            // 3. Huấn luyện Persona "Bông"
            String purchaseHistory = orderService.getPurchaseHistorySummaryForChatbot();
            String flashSaleSummary = flashSaleService.getFlashSaleSummaryForChatbot();

            String systemPrompt = "Bạn là 'Bông' - Chuyên gia tư vấn da liễu và trợ lý mua sắm cá nhân của Bông Cosmetic.\n"
                    +
                    "KHÁCH HÀNG: " + userName + ".\n" +
                    "LOẠI DA: " + userSkinType + ".\n" +
                    "PHONG CÁCH: Ngọt ngào, tận tâm, dùng emoji (🌸, 💖, ✨).\n\n" +
                    "QUY TẮC VÀNG:\n" +
                    "1. THẤU HIỂU HÀNH VI: Dựa vào 'HÀNH VI GẦN ĐÂY' để đưa ra gợi ý trúng đích. Ví dụ nếu khách vừa xem nhiều kem chống nắng, hãy chủ động hỏi họ có muốn tìm loại phù hợp nhất không.\n"
                    +
                    "2. RICH CARDS (QUAN TRỌNG): Khi bạn giới thiệu bất kỳ sản phẩm nào, bạn BẮT BUỘC phải đính kèm thẻ tag định dạng: [PRODUCT_CARD:id|name|price|thumbnail] ngay sau tên sản phẩm đó. Thông tin này lấy từ công cụ 'tra_cuu_san_pham'.\n"
                    +
                    "3. QUICK REPLIES: Mỗi khi trả lời xong, hãy gợi ý 2-3 câu hỏi tiếp theo mà khách có thể muốn hỏi vào cuối tin nhắn theo định dạng: [QUICK_REPLY:Câu hỏi gợi ý 1|Câu hỏi gợi ý 2].\n"
                    +
                    "4. ƯU TIÊN LOẠI DA: Luôn gợi ý sản phẩm khớp với loại da '" + userSkinType + "'.\n" +
                    "5. FLASH SALE: Luôn cập nhật thông tin Flash Sale mới nhất: " + flashSaleSummary + ".\n" +
                    "6. GIẢI THÍCH LÝ DO: Khi gợi ý sản phẩm, hãy giải thích ngắn gọn tại sao sản phẩm đó phù hợp với tình trạng da của khách.\n\n"
                    +
                    "LỊCH SỬ MUA HÀNG CỦA KHÁCH: " + purchaseHistory + "\n\n" +
                    context.toString();

            Map<String, Object> systemInstruction = Map.of("parts", List.of(Map.of("text", systemPrompt)));

            // 4. Interaction Loop
            List<Map<String, Object>> contents = new ArrayList<>();
            if (history != null) {
                for (ChatMessageDTO msg : history) {
                    contents.add(Map.of("role", msg.getRole().equals("assistant") ? "model" : "user", "parts",
                            List.of(Map.of("text", msg.getContent()))));
                }
            }
            contents.add(Map.of("role", "user", "parts", List.of(Map.of("text", userMessage))));

            String lastFunctionName = "NONE";
            for (int i = 0; i < 5; i++) {
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("system_instruction", systemInstruction);
                requestBody.put("contents", contents);
                requestBody.put("tools", tools);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, new HttpHeaders());
                ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, entity, String.class);
                JsonNode root = objectMapper.readTree(responseEntity.getBody());
                JsonNode candidate = root.path("candidates").get(0);
                JsonNode parts = candidate.path("content").path("parts").get(0);

                if (parts.has("functionCall")) {
                    JsonNode call = parts.get("functionCall");
                    lastFunctionName = call.get("name").asText();
                    String result = executeFunction(lastFunctionName, call.get("args"));
                    contents.add(Map.of("role", "model", "parts", List.of(parts)));
                    contents.add(Map.of("role", "function", "parts", List.of(Map.of("functionResponse",
                            Map.of("name", lastFunctionName, "response", Map.of("content", result))))));
                } else {
                    String aiResponse = parts.path("text").asText();
                    saveChatInteraction(currentUserEmail, userMessage, aiResponse, sessionId, deviceType, pageUrl,
                            lastFunctionName);
                    logChatActivity(currentUserEmail, userMessage, aiResponse, sessionId, deviceType, pageUrl);
                    return aiResponse;
                }
            }
            return "Bông đang suy nghĩ chút nhé... 🌸";
        } catch (Exception e) {
            e.printStackTrace();
            return "Bông gặp sự cố nhỏ, Nàng đợi chút nha! 💖";
        }
    }

    private String executeFunction(String name, JsonNode args) {
        switch (name) {
            case "tra_cuu_san_pham":
                return productService.getProductsSummaryForChatbot(args.path("query").asText());
            case "kiem_tra_don_hang":
                return orderService.getOrdersSummaryForChatbot();
            case "tim_ma_giam_gia":
                return couponService.getCouponsSummaryForChatbot();
            case "xem_gio_hang":
                return getCartSummary();
            case "doc_bai_viet_blog":
                return blogService.getBlogsSummaryForChatbot(args.path("keyword").asText());
            case "tra_cuu_flash_sale":
                return flashSaleService.getFlashSaleSummaryForChatbot();
            case "tra_cuu_lich_su_mua_hang":
                return orderService.getPurchaseHistorySummaryForChatbot();
            case "xem_danh_gia_san_pham":
                return reviewService.getReviewsSummaryForChatbot(args.path("productId").asLong());
            case "cap_nhat_loai_da":
                return updateSkinType(args.path("skinType").asText());
            default:
                return "Không tìm thấy công cụ.";
        }
    }

    private String updateSkinType(String skinType) {
        try {
            String email = SecurityUtil.getCurrentUserLogin().orElse(null);
            if (email == null)
                return "Lỗi: Khách chưa đăng nhập nên không lưu được.";
            User user = userRepository.findByEmail(email);
            if (user != null && user.getUserProfile() != null) {
                user.getUserProfile().setSkinType(skinType);
                userProfileRepository.save(user.getUserProfile());

                // Real-time notification to the user
                notificationService.createNotification(
                        user,
                        "Cập nhật loại da",
                        "AI đã xác định và cập nhật loại da của bạn là: " + skinType,
                        "AI_INSIGHT");

                // Refresh message via WebSocket
                messagingTemplate.convertAndSendToUser(
                        email.toLowerCase(),
                        "/queue/skin-type-updates",
                        Map.of("skinType", skinType));

                return "Thành công: Đã cập nhật loại da là " + skinType + " vào hồ sơ của "
                        + user.getUserProfile().getName();
            }
            return "Lỗi: Không tìm thấy hồ sơ.";
        } catch (Exception e) {
            return "Lỗi hệ thống khi cập nhật loại da.";
        }
    }

    private void saveChatInteraction(String email, String msg, String resp, String sid, String device, String url,
            String intent) {
        try {
            ChatInteraction interaction = new ChatInteraction();
            interaction.setUserEmail(email);
            interaction.setUserMessage(msg);
            interaction.setAiResponse(resp);
            interaction.setSessionId(sid);
            interaction.setDeviceType(device);
            interaction.setPageUrl(url);
            interaction.setIntent(intent);
            chatInteractionRepository.save(interaction);
        } catch (Exception e) {
        }
    }

    private String getCartSummary() {
        try {
            Cart cart = cartService.getOrCreateCart();
            if (cart == null || cart.getItems() == null || cart.getItems().isEmpty())
                return "Giỏ hàng trống.";
            StringBuilder sb = new StringBuilder("Giỏ hàng:\n");
            for (CartItem item : cart.getItems())
                sb.append("- ").append(item.getProduct().getName()).append(" (SL: ").append(item.getQuantity())
                        .append(")\n");
            return sb.toString();
        } catch (Exception e) {
            return "Lỗi giỏ hàng.";
        }
    }

    private void logChatActivity(String email, String userMsg, String aiResp, String sid, String device, String url) {
        try {
            Map<String, Object> logMeta = new HashMap<>();
            logMeta.put("userMessage", userMsg);
            logMeta.put("responsePreview", aiResp.length() > 50 ? aiResp.substring(0, 50) + "..." : aiResp);
            trackingService.logActivity(email != null ? email : "anonymous", "server", "CHAT_WITH_BOT",
                    objectMapper.writeValueAsString(logMeta), sid, device, null, url);
        } catch (Exception e) {
        }
    }
}
