package com.tuna.ecommerce.service;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.tuna.ecommerce.domain.Cart;
import com.tuna.ecommerce.domain.CartItem;
import com.tuna.ecommerce.domain.User;
import com.tuna.ecommerce.domain.UserActivityLog;
import com.tuna.ecommerce.domain.request.chat.ChatMessageDTO;
import com.tuna.ecommerce.repository.UserActivityLogRepository;
import com.tuna.ecommerce.repository.UserRepository;
import com.tuna.ecommerce.ultil.SecurityUtil;

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

    public enum ChatIntent {
        PRODUCT_SEARCH,        // tìm sản phẩm
        PRODUCT_DETAIL,        // hỏi chi tiết sản phẩm
        PRODUCT_RECOMMEND,     // gợi ý sản phẩm
        
        CART_INQUIRY,          // Thao tác/hỏi về giỏ hàng
        
        ORDER_STATUS,          // kiểm tra đơn
        ORDER_HISTORY,         // lịch sử đơn
        ORDER_CANCEL_REQUEST,  // hỏi hủy đơn (chỉ hướng dẫn)
        
        COUPON_LIST,           // xem mã giảm giá
        COUPON_APPLY,          // hỏi cách dùng mã
        
        USER_GREETING,         // chào hỏi
        SMALL_TALK,            // nói chuyện linh tinh
        COMPLAINT,             // phàn nàn
        UNKNOWN                // không hiểu
    }

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final ProductService productService;
    private final OrderService orderService;
    private final CouponService couponService;
    private final CartService cartService;
    private final UserRepository userRepository;
    private final UserActivityLogRepository userActivityLogRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public GeminiService(ProductService productService,
            OrderService orderService,
            CouponService couponService,
            CartService cartService,
            UserRepository userRepository,
            UserActivityLogRepository userActivityLogRepository,
            ObjectMapper objectMapper,
            RestTemplate restTemplate) {
        this.productService = productService;
        this.orderService = orderService;
        this.couponService = couponService;
        this.cartService = cartService;
        this.userRepository = userRepository;
        this.userActivityLogRepository = userActivityLogRepository;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    private ChatIntent determineIntent(String query) {
        if (query == null || query.trim().isEmpty()) return ChatIntent.UNKNOWN;
        String q = query.toLowerCase();

        // 1. Phàn nàn
        if (q.contains("tồi") || q.contains("kém") || q.contains("chậm") || q.contains("lâu") || q.contains("thất vọng") || q.contains("bực") || q.contains("tệ")) {
            return ChatIntent.COMPLAINT;
        }

        // 2. Liên quan đến đơn hàng
        if (q.contains("hủy")) return ChatIntent.ORDER_CANCEL_REQUEST;
        if (q.contains("lịch sử") && (q.contains("đơn") || q.contains("mua"))) return ChatIntent.ORDER_HISTORY;
        if (q.contains("đơn") || q.contains("giao hàng") || q.contains("vận chuyển") || q.contains("tình trạng") || q.contains("bill") || q.contains("chưa nhận")) {
            return ChatIntent.ORDER_STATUS;
        }

        // 3. Liên quan đến giỏ hàng
        if (q.contains("giỏ hàng") || q.contains("giỏ")) {
            return ChatIntent.CART_INQUIRY;
        }

        // 4. Liên quan đến mã giảm giá
        if (q.contains("cách dùng") && (q.contains("mã") || q.contains("voucher") || q.contains("coupon"))) return ChatIntent.COUPON_APPLY;
        if (q.contains("mã") || q.contains("giảm giá") || q.contains("khuyến mãi") || q.contains("voucher") || q.contains("sale") || q.contains("flashsale") || q.contains("freeship")) {
            return ChatIntent.COUPON_LIST;
        }

        // 5. Chào hỏi
        if (q.equals("chào") || q.equals("hi") || q.equals("hello") || q.equals("alo") || q.contains("có ai không")) {
            return ChatIntent.USER_GREETING;
        }

        // 6. Small Talk / Trò chuyện linh tinh
        if (q.contains("thời tiết") || q.contains("tên là gì") || q.contains("khỏe không") || q.contains("haha") || q.contains("đẹp không")) {
            return ChatIntent.SMALL_TALK;
        }

        // 7. Liên quan đến Sản phẩm
        if (q.contains("chi tiết") || q.contains("thành phần") || q.contains("hướng dẫn sử dụng") || q.contains("loại da")) return ChatIntent.PRODUCT_DETAIL;
        if (q.contains("tư vấn") || q.contains("gợi ý") || q.contains("nên mua") || q.contains("bán chạy") || q.contains("nào tốt") || q.contains("phù hợp")) return ChatIntent.PRODUCT_RECOMMEND;
        if (q.contains("tìm") || q.contains("có bán") || q.contains("giá") || q.contains("bao nhiêu") || q.contains("sản phẩm")) return ChatIntent.PRODUCT_SEARCH;

        return ChatIntent.UNKNOWN;
    }

    public String getChatResponse(String userMessage, List<ChatMessageDTO> history) {
        int maxRetries = 2; // Thử lại tối đa 2 lần nếu gặp lỗi 503
        int retryCount = 0;

        ChatIntent intent = determineIntent(userMessage);

        while (retryCount <= maxRetries) {
            try {
                String url = apiUrl + "?key=" + apiKey;

                // Dynamically fetch Contexts based on Intent to save tokens
                String productContext = "";
                String orderContext = "";
                String couponContext = "";
                String cartContext = "";
                
                String policyContext = "- Đổi trả miễn phí 100% trong 7 ngày đầu nếu lỗi nhà sản xuất.\n" +
                                       "- Miễn phí vận chuyển (Freeship) cho toàn bộ đơn hàng từ 500,000 VNĐ.\n" +
                                       "- Giao hàng tiêu chuẩn 2-4 ngày trên toàn quốc.\n" +
                                       "- Bông Cosmetic bán mỹ phẩm chính hãng 100%.";

                // Routing variables based on sophisticated 15 Intention Enum
                switch (intent) {
                    case ORDER_STATUS:
                    case ORDER_HISTORY:
                    case ORDER_CANCEL_REQUEST:
                        orderContext = this.orderService.getOrdersSummaryForChatbot();
                        break;
                    case COUPON_LIST:
                    case COUPON_APPLY:
                        couponContext = this.couponService.getCouponsSummaryForChatbot();
                        // Lấy thêm sản phẩm vì khách có thể hỏi "sản phẩm nào đang sale/flashsale"
                        productContext = this.productService.getProductsSummaryForChatbot(userMessage);
                        break;
                    case PRODUCT_SEARCH:
                    case PRODUCT_DETAIL:
                    case PRODUCT_RECOMMEND:
                        productContext = this.productService.getProductsSummaryForChatbot(userMessage);
                        break;
                    case CART_INQUIRY:
                        // Giỏ hàng sẽ được load ở phía dưới (chung cho mọi request cần giỏ)
                        break;
                    case COMPLAINT:
                    case SMALL_TALK:
                    case USER_GREETING:
                        // No slow DB calls for general greetings or small talk
                        break;
                    case UNKNOWN:
                        // Fallback behavior: provide top-selling products as a suggestion
                        productContext = this.productService.getProductsSummaryForChatbot("");
                        break;
                }
                
                // User, Cart & Activity context
                String currentUserEmail = SecurityUtil.getCurrentUserLogin().orElse(null);
                String userNameContext = "Khách lạ (Chưa đăng nhập)";
                String userActivityContext = "";
                
                if (currentUserEmail != null) {
                    User user = userRepository.findByEmail(currentUserEmail);
                    if (user != null && user.getUserProfile() != null) {
                        userNameContext = user.getUserProfile().getName();
                    }
                    
                    Cart cart = cartService.getOrCreateCart();
                    if (cart != null && cart.getItems() != null && !cart.getItems().isEmpty()) {
                        StringBuilder cartSb = new StringBuilder();
                        for (CartItem item : cart.getItems()) {
                            cartSb.append("- ").append(item.getProduct().getName())
                                  .append(" (Số lượng: ").append(item.getQuantity()).append(")\n");
                        }
                        cartContext = cartSb.toString();
                    } else {
                        cartContext = "Giỏ hàng trống.";
                    }

                    // Lấy 5 hành động gần nhất
                    List<UserActivityLog> recentLogs = userActivityLogRepository.findTop5ByUserEmailOrderByCreatedAtDesc(currentUserEmail);
                    if (recentLogs != null && !recentLogs.isEmpty()) {
                        StringBuilder logSb = new StringBuilder();
                        for (UserActivityLog l : recentLogs) {
                            logSb.append("- ").append(l.getActionType().name())
                                 .append(" (").append(l.getMetadata() != null ? l.getMetadata() : "N/A").append(")\n");
                        }
                        userActivityContext = logSb.toString();
                    }
                }

                // Prepare Payload for Gemini 2.5
                Map<String, Object> requestBody = new HashMap<>();

                // System instructions (context)
                Map<String, Object> systemInstructionMap = new HashMap<>();
                Map<String, String> systemPart = new HashMap<>();
                systemPart.put("text", "Bạn là 'Bông', Trợ lý AI dẻo miệng của thương hiệu Bông Cosmetic. " +
                        "Hãy gọi khách là 'Nàng/Cậu' nếu không biết tên, hiện tại bạn đang chat với: [" + userNameContext + "]. " +
                        "Hãy chủ động chào tên thật của khách để tạo sự thân thiết.\n" +
                        "TUYỆT ĐỐI TUÂN THỦ: NẾU KHÁCH HỎI MÀ TRONG DỮ LIỆU ĐƯỢC CUNG CẤP BÊN DƯỚI KHÔNG CÓ, HÃY XIN LỖI KHÉO LÉO VÀ BÁO RẰNG CHƯA TÌM THẤY. KHÔNG ĐƯỢC BỊA ĐẶT HOẶC MÔ PHỎNG DỮ LIỆU GIẢ!" +
                        (productContext.isEmpty() ? "" : "\n\n--- DỮ LIỆU SẢN PHẨM ---\n" + productContext) +
                        (couponContext.isEmpty() ? "" : "\n\n--- DỮ LIỆU VOUCHER KHUYẾN MÃI ---\n" + couponContext) +
                        (orderContext.isEmpty() ? "" : "\n\n--- DỮ LIỆU ĐƠN HÀNG CỦA KHÁCH ---\n" + orderContext) +
                        "\n\n--- DỮ LIỆU GIỎ HÀNG HIỆN TẠI ---\n" + cartContext +
                        (userActivityContext.isEmpty() ? "" : "\n\n--- LỊCH SỬ HÀNH ĐỘNG GẦN NHẤT CỦA KHÁCH ---\n" + userActivityContext) +
                        "\n\n--- CHÍNH SÁCH ---\n" + policyContext +
                        "\n\nNhiệm vụ khi tư vấn: " +
                        "1. Soi GIỎ HÀNG và LỊCH SỬ HÀNH ĐỘNG: Gợi ý các món bổ trợ để Up-sale dựa vào sản phẩm họ vừa xem/tương tác (Tuyệt đối định dạng Markdown [Tên sản phẩm](/product/ID)).\n" +
                        "2. Đơn hàng: Nếu khách muốn mua tiếp thì khuyên, nếu hủy thì báo khách bấm vào mục Tài khoản -> Đơn hàng (bạn ko có quyền hủy).\n" +
                        "3. Luôn dùng thái độ rạng rỡ (🌸💖) và xin lỗi chân thành nếu không tìm thấy món đồ/đơn hàng khách cần."
                );
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
